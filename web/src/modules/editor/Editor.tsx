import { useEffect, useMemo, useRef, useState, useCallback } from 'react';
import { Button, Checkbox, Group, NumberInput, Paper, Select, Stack, Text, TextInput, Title, ScrollArea, Divider, Badge } from '@mantine/core';

type Station = {
  id: string;
  name: string;
  x: number;
  y: number;
  textPosition?: number;
  neighbors?: [string, number][];
};

type Line = {
  id: string;
  name: string;
  color: string;
  stations: Station[];
};

type Transfer = { id: string; stations: string[]; time: number; type?: string };

type MetroMap = {
  lines: Line[];
  transfers?: Transfer[];
  intermediatePoints?: { neighborsId: [string, string]; points: { x: number; y: number }[] }[];
  rivers?: any[];
  objects?: any[];
};

type FileShape = { info: any; metro_map?: MetroMap; suburban_map?: MetroMap; rivertram_map?: MetroMap; tram_map?: MetroMap };

export function Editor({ mapPath }: { mapPath: string }) {
  const [content, setContent] = useState<FileShape | null>(null);
  const [error, setError] = useState<string | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const overlayCanvasRef = useRef<HTMLCanvasElement | null>(null);
  const [activeSection, setActiveSection] = useState<'metro_map' | 'suburban_map' | 'rivertram_map' | 'tram_map'>('metro_map');
  const [selectedLineId, setSelectedLineId] = useState<string | null>(null);
  const [selectedStationId, setSelectedStationId] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState<boolean>(false);
  const dragOffset = useRef<{dx:number, dy:number}>({dx:0, dy:0});
  const [snapToGrid, setSnapToGrid] = useState<boolean>(true);
  const [selectedTransferId, setSelectedTransferId] = useState<string | null>(null);
  const [selectedSegment, setSelectedSegment] = useState<{ lineId: string; aId: string; bId: string } | null>(null);
  const [selectedControlPointIdx, setSelectedControlPointIdx] = useState<number | null>(null);
  const [isDraggingControl, setIsDraggingControl] = useState<boolean>(false);
  const [defaultNeighborTime, setDefaultNeighborTime] = useState<number>(3);
  const [autoNeighborsOnSave, setAutoNeighborsOnSave] = useState<boolean>(true);
  const [validationErrors, setValidationErrors] = useState<string[]>([]);
  const [viewBounds, setViewBounds] = useState({ minX: 0, minY: 0, maxX: 1000, maxY: 1000 });
  const [scale, setScale] = useState(1);
  const [offsetX, setOffsetX] = useState(0);
  const [offsetY, setOffsetY] = useState(0);
  const renderRequestRef = useRef<number | null>(null);
  const lastRenderTime = useRef<number>(0);
  const [isDebouncedRendering, setIsDebouncedRendering] = useState(false);
  const tempDragState = useRef<{ x: number; y: number; stationId: string; lineId: string } | null>(null);
  const staticCacheCanvasRef = useRef<HTMLCanvasElement | null>(null);
  const staticCacheValidRef = useRef<boolean>(false);
  const rafDragRef = useRef<number | null>(null);

  useEffect(() => {
    setError(null);
    // Пытаемся сначала через локальный API (запуск npm run api), иначе fallback на статический импорт
    const load = async () => {
      const pathTrim = (mapPath || '').trim();
      const isNew = !pathTrim || pathTrim.toUpperCase().startsWith('NEW') || pathTrim === '__NEW__';
      if (isNew) {
        const empty: FileShape = { info: { version: '1.0', author: 'Nicorp', country: '', name: '' }, metro_map: { lines: [], transfers: [], intermediatePoints: [], rivers: [], objects: [] } } as any;
        setContent(empty);
        return;
      }
      try {
        const res = await fetch(`http://localhost:5174/api/load?path=${encodeURIComponent(pathTrim)}`);
        if (res.ok) {
          const data = await res.json();
          const json = JSON.parse(data.content) as FileShape;
          setContent(json);
          return;
        }
        throw new Error('API unavailable');
      } catch {
        // fallback: статический путь (работает, если mapPath доступен из dev-сервера vite)
        try {
          const r = await fetch(`/${pathTrim}`);
          if (!r.ok) throw new Error(`HTTP ${r.status}`);
          const text = await r.text();
          // Если пришел index.html от Vite — не парсим как JSON
          if (text.trim().startsWith('<')) throw new Error('Not a JSON (HTML received)');
          const json = JSON.parse(text) as FileShape;
          setContent(json);
        } catch (e) {
          // Если не удалось ничего загрузить — создаем новый пустой проект
          const empty: FileShape = { info: { version: '1.0', author: 'Nicorp', country: '', name: '' }, metro_map: { lines: [], transfers: [], intermediatePoints: [], rivers: [], objects: [] } } as any;
          setContent(empty);
          setError(String(e));
        }
      }
    };
    load();
  }, [mapPath]);

  const metro = useMemo(() => content?.[activeSection], [content, activeSection]);
  const selectedLine = useMemo(() => metro?.lines?.find(l => l.id === selectedLineId) || null, [metro, selectedLineId]);
  const selectedStation = useMemo(() => selectedLine?.stations?.find(s => s.id === selectedStationId) || null, [selectedLine, selectedStationId]);
  const selectedSegmentData = useMemo(() => {
    if (!selectedSegment || !metro) return null;
    const line = metro.lines?.find(l => l.id === selectedSegment.lineId);
    const stationA = line?.stations?.find(s => s.id === selectedSegment.aId);
    const stationB = line?.stations?.find(s => s.id === selectedSegment.bId);
    const intermediatePoint = metro.intermediatePoints?.find(ip => 
      (ip.neighborsId[0] === selectedSegment.aId && ip.neighborsId[1] === selectedSegment.bId) ||
      (ip.neighborsId[0] === selectedSegment.bId && ip.neighborsId[1] === selectedSegment.aId)
    );
    return line && stationA && stationB ? { line, stationA, stationB, intermediatePoint } : null;
  }, [selectedSegment, metro]);
  const transfers = useMemo(() => metro?.transfers ?? [], [metro]);
  const selectedTransfer = useMemo(() => transfers.find(t => t.id === selectedTransferId) || null, [transfers, selectedTransferId]);
  const allStationsFlat = useMemo(() => {
    const list: { id: string; name: string }[] = [];
    for (const l of metro?.lines ?? []) {
      for (const s of l.stations ?? []) list.push({ id: s.id, name: `${s.name} (${l.name})` });
    }
    return list;
  }, [metro]);

  // Оптимизированная система рендеринга с viewport culling
  const debouncedRender = useCallback(() => {
    if (renderRequestRef.current) {
      cancelAnimationFrame(renderRequestRef.current);
    }
    
    renderRequestRef.current = requestAnimationFrame(() => {
      const now = performance.now();
      if (now - lastRenderTime.current < 16) return; // 60 FPS limit
      lastRenderTime.current = now;
      
      const canvas = canvasRef.current;
      if (!canvas || !metro) return;
      
      const ctx = canvas.getContext('2d');
      if (!ctx) return;
      
      performOptimizedRender(ctx, canvas);
      setIsDebouncedRendering(false);
    });
  }, [metro, selectedSegment, selectedControlPointIdx, selectedLineId, selectedStationId, selectedTransferId]);

  const performOptimizedRender = useCallback((ctx: CanvasRenderingContext2D, canvas: HTMLCanvasElement) => {
    const width = canvas.width = canvas.clientWidth;
    const height = canvas.height = canvas.clientHeight;
    
    // Update viewport bounds
    const padding = 100;
    const newViewBounds = {
      minX: offsetX - padding,
      minY: offsetY - padding,
      maxX: offsetX + width + padding,
      maxY: offsetY + height + padding
    };
    setViewBounds(newViewBounds);
    
    ctx.clearRect(0, 0, width, height);
    ctx.fillStyle = '#0b0f13';
    ctx.fillRect(0, 0, width, height);

    // Grid (simplified for performance)
    if (scale > 0.3) {
      ctx.strokeStyle = '#1e2530';
      ctx.lineWidth = 1;
      const gridSize = 50;
      for (let x = 0; x <= width; x += gridSize) {
        ctx.beginPath();
        ctx.moveTo(x, 0);
        ctx.lineTo(x, height);
        ctx.stroke();
      }
      for (let y = 0; y <= height; y += gridSize) {
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(width, y);
        ctx.stroke();
      }
    }

    const lines = metro?.lines ?? [];
    
    // Viewport culling - only render visible elements
    const isInViewport = (x: number, y: number) => {
      return x >= newViewBounds.minX && x <= newViewBounds.maxX && 
             y >= newViewBounds.minY && y <= newViewBounds.maxY;
    };

    // Render lines with culling
    for (const line of lines) {
      const stations = line.stations ?? [];
      
      for (let i = 0; i < stations.length - 1; i++) {
        const a = stations[i];
        const b = stations[i + 1];
        
        // Skip if both stations are outside viewport
        if (!isInViewport(a.x, a.y) && !isInViewport(b.x, b.y)) {
          // Additional check for lines crossing viewport
          const lineIntersectsViewport = 
            (a.x < newViewBounds.minX && b.x > newViewBounds.maxX) ||
            (a.x > newViewBounds.maxX && b.x < newViewBounds.minX) ||
            (a.y < newViewBounds.minY && b.y > newViewBounds.maxY) ||
            (a.y > newViewBounds.maxY && b.y < newViewBounds.minY);
          
          if (!lineIntersectsViewport) continue;
        }
        
        const isSelectedSeg = selectedSegment && selectedSegment.lineId === line.id && 
          ((selectedSegment.aId === a.id && selectedSegment.bId === b.id) || 
           (selectedSegment.aId === b.id && selectedSegment.bId === a.id));
        
        const intermediatePoint = metro!.intermediatePoints?.find(ip => 
          (ip.neighborsId[0] === a.id && ip.neighborsId[1] === b.id) ||
          (ip.neighborsId[0] === b.id && ip.neighborsId[1] === a.id)
        );

        // Main line
        ctx.strokeStyle = line.color || '#cccccc';
        ctx.lineWidth = 6;
        ctx.lineJoin = 'round';
        ctx.lineCap = 'round';
        ctx.beginPath();
        
        if (intermediatePoint && intermediatePoint.points.length === 2) {
          const [c1, c2] = intermediatePoint.points;
          ctx.moveTo(a.x, a.y);
          ctx.bezierCurveTo(c1.x, c1.y, c2.x, c2.y, b.x, b.y);
        } else {
          ctx.moveTo(a.x, a.y);
          ctx.lineTo(b.x, b.y);
        }
        ctx.stroke();

        // Highlight selected segment
        if (isSelectedSeg) {
          ctx.save();
          ctx.strokeStyle = '#ffcc00';
          ctx.lineWidth = 10;
          ctx.beginPath();
          if (intermediatePoint && intermediatePoint.points.length === 2) {
            const [c1, c2] = intermediatePoint.points;
            ctx.moveTo(a.x, a.y);
            ctx.bezierCurveTo(c1.x, c1.y, c2.x, c2.y, b.x, b.y);
          } else {
            ctx.moveTo(a.x, a.y);
            ctx.lineTo(b.x, b.y);
          }
          ctx.stroke();
          
          // Control points
          if (intermediatePoint && intermediatePoint.points.length === 2) {
            for (let idx = 0; idx < intermediatePoint.points.length; idx++) {
              const p = intermediatePoint.points[idx];
              if (!isInViewport(p.x, p.y)) continue;
              
              const isSelected = selectedControlPointIdx === idx;
              ctx.fillStyle = isSelected ? '#ff0000' : '#ffaa00';
              ctx.beginPath();
              ctx.arc(p.x, p.y, isSelected ? 6 : 4, 0, Math.PI * 2);
              ctx.fill();
            }
          }
          ctx.restore();
        }
      }
      
      // Stations with culling
      for (const s of stations) {
        if (!isInViewport(s.x, s.y)) continue;
        
        const isSel = s.id === selectedStationId;
        ctx.fillStyle = isSel ? '#00e0ff' : '#ffffff';
        ctx.beginPath();
        ctx.arc(s.x, s.y, 5, 0, Math.PI * 2);
        ctx.fill();
        
        if (scale > 0.5) { // Only show labels when zoomed in enough
          drawStationLabel(ctx, s);
        }
      }
    }

    // Transfers with culling
    const transfers = metro?.transfers ?? [];
    if (transfers.length > 0 && scale > 0.4) {
      for (const tr of transfers) {
        if (!tr.stations || tr.stations.length < 2) continue;
        const [aId, bId] = tr.stations;
        const findPos = (id: string): { x: number, y: number } | null => {
          for (const l of lines) {
            const s = l.stations.find(st => st.id === id);
            if (s) return { x: s.x, y: s.y };
          }
          return null;
        };
        const a = findPos(aId);
        const b = findPos(bId);
        if (!a || !b) continue;
        
        if (!isInViewport(a.x, a.y) && !isInViewport(b.x, b.y)) continue;
        
        ctx.save();
        ctx.setLineDash([5, 5]);
        ctx.strokeStyle = tr.id === selectedTransferId ? '#ffcc00' : '#aaaaaa';
        ctx.beginPath();
        ctx.moveTo(a.x, a.y);
        ctx.lineTo(b.x, b.y);
        ctx.stroke();
        ctx.restore();
      }
    }
  }, [metro, selectedSegment, selectedControlPointIdx, selectedLineId, selectedStationId, selectedTransferId, offsetX, offsetY, scale]);

  // Optimized render trigger
  useEffect(() => {
    setIsDebouncedRendering(true);
    debouncedRender();
  }, [debouncedRender]);

  // Cleanup animation frames on unmount
  useEffect(() => {
    return () => {
      if (renderRequestRef.current) {
        cancelAnimationFrame(renderRequestRef.current);
      }
    };
  }, []);

  const findStationAt = (x:number, y:number): { lineId: string; stationId: string } | null => {
    const lines = metro?.lines ?? [];
    for (const line of lines) {
      for (const s of line.stations ?? []) {
        const dx = x - s.x;
        const dy = y - s.y;
        if (dx*dx + dy*dy <= 10*10) return { lineId: line.id, stationId: s.id };
      }
    }
    return null;
  };

  // Семплирование кривой Безье для точного определения попадания клика
  const sampleBezier = (a: Station, c1: {x:number, y:number}, c2: {x:number, y:number}, b: Station, numSamples: number) => {
    const samples: {x:number, y:number}[] = [];
    for (let i = 0; i <= numSamples; i++) {
      const t = i / numSamples;
      const t2 = t * t;
      const t3 = t2 * t;
      const mt = 1 - t;
      const mt2 = mt * mt;
      const mt3 = mt2 * mt;
      
      const x = mt3 * a.x + 3 * mt2 * t * c1.x + 3 * mt * t2 * c2.x + t3 * b.x;
      const y = mt3 * a.y + 3 * mt2 * t * c1.y + 3 * mt * t2 * c2.y + t3 * b.y;
      samples.push({ x, y });
    }
    return samples;
  };

  const onCanvasPointerDown = (e: React.PointerEvent<HTMLCanvasElement>) => {
    const rect = (e.target as HTMLCanvasElement).getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    const hit = findStationAt(x, y);
    if (hit) {
      setSelectedLineId(hit.lineId);
      setSelectedStationId(hit.stationId);
      setSelectedTransferId(null);
      const st = metro?.lines?.find(l=>l.id===hit.lineId)?.stations.find(s=>s.id===hit.stationId);
      if (st) {
        dragOffset.current = { dx: x - st.x, dy: y - st.y };
        setIsDragging(true);
        tempDragState.current = { x: st.x, y: st.y, stationId: hit.stationId, lineId: hit.lineId };
        staticCacheValidRef.current = false;
        buildStaticCacheForDrag(hit.stationId);
      }
    } else {
      setSelectedStationId(null);
      setSelectedControlPointIdx(null);
      setIsDraggingControl(false);
      // выбор перехода/сегмента — оставляем как есть ниже
      const tol = 8;
      const distanceToSegment = (px:number,py:number, ax:number,ay:number,bx:number,by:number) => {
        const vx = bx - ax, vy = by - ay;
        const wx = px - ax, wy = py - ay;
        const c1 = vx*wx + vy*wy;
        if (c1 <= 0) return Math.hypot(px-ax, py-ay);
        const c2 = vx*vx + vy*vy;
        if (c2 <= c1) return Math.hypot(px-bx, py-by);
        const t = c1 / c2;
        const projx = ax + t*vx, projy = ay + t*vy;
        return Math.hypot(px-projx, py-projy);
      };
      let clickedTransfer: string | null = null;
      let clickedSegment: { lineId: string; aId: string; bId: string } | null = null;
      let clickedControlIdx: number | null = null;
      for (const tr of transfers) {
        if (!tr.stations || tr.stations.length < 2) continue;
        const [aId, bId] = tr.stations;
        const findPos = (id:string): {x:number,y:number}|null => {
          for (const l of metro?.lines ?? []) {
            const s = l.stations.find(st => st.id === id);
            if (s) return { x: s.x, y: s.y };
          }
          return null;
        };
        const a = findPos(aId); const b = findPos(bId);
        if (!a || !b) continue;
        const d = distanceToSegment(x,y,a.x,a.y,b.x,b.y);
        if (d <= tol) { clickedTransfer = tr.id; break; }
      }
      setSelectedTransferId(clickedTransfer);
      if (!clickedTransfer) {
        for (const line of metro!.lines ?? []) {
          const stations = line.stations ?? [];
          for (let i = 0; i < stations.length - 1; i++) {
            const a = stations[i];
            const b = stations[i + 1];
            const intermediatePoint = metro!.intermediatePoints?.find(ip => 
              (ip.neighborsId[0] === a.id && ip.neighborsId[1] === b.id) ||
              (ip.neighborsId[0] === b.id && ip.neighborsId[1] === a.id)
            );
            let hitSegment = false;
            if (intermediatePoint && intermediatePoint.points.length === 2) {
              const samples = sampleBezier(a, intermediatePoint.points[0], intermediatePoint.points[1], b, 32);
              for (let k = 0; k < samples.length - 1; k++) {
                const d = distanceToSegment(x, y, samples[k].x, samples[k].y, samples[k+1].x, samples[k+1].y);
                if (d <= tol) { hitSegment = true; break; }
              }
              for (let idx = 0; idx < intermediatePoint.points.length; idx++) {
                const p = intermediatePoint.points[idx];
                if (Math.hypot(p.x - x, p.y - y) <= 8) {
                  clickedControlIdx = idx;
                  hitSegment = true;
                  break;
                }
              }
            } else {
              const d = distanceToSegment(x, y, a.x, a.y, b.x, b.y);
              if (d <= tol) hitSegment = true;
            }
            if (hitSegment) { clickedSegment = { lineId: line.id, aId: a.id, bId: b.id }; break; }
          }
          if (clickedSegment) break;
        }
      }
      setSelectedSegment(clickedSegment);
      setSelectedControlPointIdx(clickedControlIdx);
      if (clickedControlIdx !== null && clickedSegment) {
        setIsDraggingControl(true);
        dragOffset.current = { dx: 0, dy: 0 };
      }
    }
  };

  const renderDragOverlay = useCallback((dragX: number, dragY: number, stationId: string) => {
    const overlay = overlayCanvasRef.current;
    if (!overlay || !metro) return;
    const ctx = overlay.getContext('2d');
    if (!ctx) return;

    // ensure size
    if (overlay.width !== overlay.clientWidth || overlay.height !== overlay.clientHeight) {
      overlay.width = overlay.clientWidth;
      overlay.height = overlay.clientHeight;
    }

    // clear overlay only
    ctx.clearRect(0, 0, overlay.width, overlay.height);

    // draw dynamic segments for moving station
    const lines = metro?.lines ?? [];
    let movingStationLine: Line | null = null;
    let movingStation: Station | null = null;
    for (const l of lines) {
      const s = l.stations.find(st => st.id === stationId);
      if (s) { movingStationLine = l; movingStation = s; break; }
    }
    if (!movingStation || !movingStationLine) return;

    const neighbors = movingStation.neighbors ?? [];
    ctx.strokeStyle = movingStationLine.color || '#cccccc';
    ctx.lineWidth = 6;
    ctx.lineJoin = 'round';
    ctx.lineCap = 'round';

    for (const [neighborId] of neighbors) {
      let neighbor: Station | null = null;
      for (const l of lines) { const s = l.stations.find(st => st.id === neighborId); if (s) { neighbor = s; break; } }
      if (!neighbor) continue;

      const ip = metro!.intermediatePoints?.find(ip =>
        (ip.neighborsId[0] === stationId && ip.neighborsId[1] === neighborId) ||
        (ip.neighborsId[0] === neighborId && ip.neighborsId[1] === stationId)
      );

      ctx.beginPath();
      if (ip && ip.points.length === 2) {
        const [c1, c2] = ip.points;
        ctx.moveTo(dragX, dragY);
        ctx.bezierCurveTo(c1.x, c1.y, c2.x, c2.y, neighbor.x, neighbor.y);
      } else {
        ctx.moveTo(dragX, dragY);
        ctx.lineTo(neighbor.x, neighbor.y);
      }
      ctx.stroke();
    }

    for (const tr of metro?.transfers ?? []) {
      if (!tr.stations || tr.stations.length < 2) continue;
      const [aId, bId] = tr.stations;
      if (aId !== stationId && bId !== stationId) continue;
      const otherId = aId === stationId ? bId : aId;
      let other: Station | null = null;
      for (const l of lines) { const s = l.stations.find(st => st.id === otherId); if (s) { other = s; break; } }
      if (!other) continue;
      ctx.save();
      ctx.setLineDash([5, 5]);
      ctx.strokeStyle = tr.id === selectedTransferId ? '#ffcc00' : '#aaaaaa';
      ctx.beginPath();
      ctx.moveTo(dragX, dragY);
      ctx.lineTo(other.x, other.y);
      ctx.stroke();
      ctx.restore();
    }

    ctx.fillStyle = '#00e0ff';
    ctx.beginPath();
    ctx.arc(dragX, dragY, 5, 0, Math.PI * 2);
    ctx.fill();
    if (scale > 0.5) drawStationLabel(ctx as unknown as CanvasRenderingContext2D, { ...movingStation, x: dragX, y: dragY });
  }, [metro, scale, selectedTransferId]);

  const onCanvasPointerMove = useCallback((e: React.PointerEvent<HTMLCanvasElement>) => {
    if (!content) return;
    const rect = (e.target as HTMLCanvasElement).getBoundingClientRect();

    if (isDragging && tempDragState.current) {
      let x = e.clientX - rect.left - dragOffset.current.dx;
      let y = e.clientY - rect.top - dragOffset.current.dy;
      if (snapToGrid) { x = Math.round(x / 10) * 10; y = Math.round(y / 10) * 10; }
      tempDragState.current.x = x; tempDragState.current.y = y;

      if (rafDragRef.current) cancelAnimationFrame(rafDragRef.current);
      rafDragRef.current = requestAnimationFrame(() => {
        renderDragOverlay(x, y, tempDragState.current!.stationId);
      });
      return;
    }

    if (isDraggingControl && selectedSegment && selectedControlPointIdx !== null) {
      let x = e.clientX - rect.left; let y = e.clientY - rect.top;
      if (snapToGrid) { x = Math.round(x / 10) * 10; y = Math.round(y / 10) * 10; }
      const now = performance.now(); if (now - lastRenderTime.current < 32) return;
      setContent(prev => {
        if (!prev) return prev;
        const section = prev[activeSection]; if (!section) return prev;
        const intermediatePoints = section.intermediatePoints ?? [];
        const idx = intermediatePoints.findIndex(ip => 
          (ip.neighborsId[0] === selectedSegment.aId && ip.neighborsId[1] === selectedSegment.bId) ||
          (ip.neighborsId[0] === selectedSegment.bId && ip.neighborsId[1] === selectedSegment.aId)
        );
        if (idx === -1) return prev;
        const updated = [...intermediatePoints]; const points = [...updated[idx].points];
        points[selectedControlPointIdx!] = { x, y }; updated[idx] = { ...updated[idx], points };
        return { ...prev, [activeSection]: { ...section, intermediatePoints: updated } };
      });
      return;
    }
  }, [content, isDragging, isDraggingControl, activeSection, selectedSegment, selectedControlPointIdx, snapToGrid, renderDragOverlay]);

  const onCanvasPointerUp = useCallback(() => {
    if (isDragging && tempDragState.current) {
      const { x, y, stationId } = tempDragState.current;
      setContent(prev => {
        if (!prev) return prev;
        const section = prev[activeSection]; if (!section) return prev;
        const lines = section.lines.map(l => {
          return {
            ...l,
            stations: l.stations.map(s => s.id === stationId ? { ...s, x, y } : s)
          };
        });
        return { ...prev, [activeSection]: { ...section, lines } };
      });
    }
    setIsDragging(false);
    tempDragState.current = null;
    staticCacheValidRef.current = false;
    if (rafDragRef.current) cancelAnimationFrame(rafDragRef.current);
    // Full rerender after commit
    debouncedRender();
  }, [isDragging, activeSection, debouncedRender]);

  const download = () => {
    if (!content) return;
    const blob = new Blob([JSON.stringify(content, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = mapPath.split('/').pop() || 'map.json';
    a.click();
    URL.revokeObjectURL(url);
  };

  const saveToDisk = async () => {
    if (!content) return;
    let dataToSave = content;
    if (autoNeighborsOnSave) {
      dataToSave = rebuildAllNeighbors(content);
      setContent(dataToSave);
    }
    const errors = validateFile(dataToSave);
    setValidationErrors(errors);
    if (errors.length > 0) {
      alert('Исправьте ошибки перед сохранением');
      return;
    }
    try {
      const res = await fetch('http://localhost:5174/api/save', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ path: mapPath, content: JSON.stringify(dataToSave, null, 2) })
      });
      if (!res.ok) throw new Error(await res.text());
      alert('Сохранено');
    } catch (e) {
      alert('Не удалось сохранить через API. Запустите npm run api. Ошибка: ' + String(e));
    }
  };

  const onLocalFileSelected = async (file?: File) => {
    if (!file) return;
    const text = await file.text();
    const json = JSON.parse(text) as FileShape;
    setContent(json);
  };

  const newProject = () => {
    const empty: FileShape = {
      info: { version: '1.0', author: 'Nicorp', country: '', name: '' },
      metro_map: { lines: [], transfers: [], intermediatePoints: [], rivers: [], objects: [] }
    } as any;
    setContent(empty);
  };

  const updateSelectedStation = (patch: Partial<Station>) => {
    if (!content || !selectedLineId || !selectedStationId) return;
    setContent(prev => {
      if (!prev) return prev;
      const section = prev[activeSection];
      if (!section) return prev;
      const lines = section.lines.map(l => {
        if (l.id !== selectedLineId) return l;
        return {
          ...l,
          stations: l.stations.map(s => s.id === selectedStationId ? { ...s, ...patch } : s)
        };
      });
      return { ...prev, [activeSection]: { ...section, lines } };
    });
  };

  const addTransfer = () => {
    if (!content) return;
    const newId = `TR_${Date.now()}`;
    setContent(prev => {
      if (!prev) return prev;
      const section = prev[activeSection];
      if (!section) return prev;
      const existing = section.transfers ?? [];
      const firstTwo = allStationsFlat.slice(0, 2).map(s => s.id);
      const tr: Transfer = { id: newId, stations: firstTwo as string[], time: 3, type: 'default' };
      return { ...prev, [activeSection]: { ...section, transfers: [...existing, tr] } };
    });
    setSelectedTransferId(newId);
  };

  const updateSelectedTransfer = (patch: Partial<Transfer>) => {
    if (!content || !selectedTransferId) return;
    setContent(prev => {
      if (!prev) return prev;
      const section = prev[activeSection];
      if (!section) return prev;
      const transfers = (section.transfers ?? []).map(t => t.id === selectedTransferId ? { ...t, ...patch } : t);
      return { ...prev, [activeSection]: { ...section, transfers } };
    });
  };

  const removeSelectedTransfer = () => {
    if (!content || !selectedTransferId) return;
    setContent(prev => {
      if (!prev) return prev;
      const section = prev[activeSection];
      if (!section) return prev;
      const transfers = (section.transfers ?? []).filter(t => t.id !== selectedTransferId);
      return { ...prev, [activeSection]: { ...section, transfers } };
    });
    setSelectedTransferId(null);
  };

  // Линии
  const addLine = () => {
    if (!content) return;
    const id = genId('LINE');
    const newLine: Line = { id, name: `Line ${id.slice(-4)}`, color: '#0080ff', stations: [] };
    setContent(prev => {
      if (!prev) return prev;
      const section = prev[activeSection];
      if (!section) return prev;
      return { ...prev, [activeSection]: { ...section, lines: [...section.lines, newLine] } };
    });
    setSelectedLineId(id);
    setSelectedStationId(null);
  };

  const updateSelectedLine = (patch: Partial<Line>) => {
    if (!content || !selectedLineId) return;
    setContent(prev => {
      if (!prev) return prev;
      const section = prev[activeSection];
      if (!section) return prev;
      const lines = section.lines.map(l => l.id === selectedLineId ? { ...l, ...patch } : l);
      return { ...prev, [activeSection]: { ...section, lines } };
    });
  };

  const removeSelectedLine = () => {
    if (!content || !selectedLineId) return;
    setContent(prev => {
      if (!prev) return prev;
      const section = prev[activeSection];
      if (!section) return prev;
      const lines = section.lines.filter(l => l.id !== selectedLineId);
      return { ...prev, [activeSection]: { ...section, lines } };
    });
    setSelectedLineId(null);
    setSelectedStationId(null);
  };

  // Станции
  const addStation = () => {
    if (!content || !selectedLineId) return;
    const id = genId('ST');
    const newStation: Station = { id, name: `Station ${id.slice(-4)}`, x: 100, y: 100, textPosition: 3, neighbors: [] };
    setContent(prev => {
      if (!prev) return prev;
      const section = prev[activeSection];
      if (!section) return prev;
      const lines = section.lines.map(l => l.id === selectedLineId ? { ...l, stations: [...l.stations, newStation] } : l);
      return { ...prev, [activeSection]: { ...section, lines } };
    });
    setSelectedStationId(id);
  };

  const removeSelectedStation = () => {
    if (!content || !selectedLineId || !selectedStationId) return;
    setContent(prev => {
      if (!prev) return prev;
      const section = prev[activeSection];
      if (!section) return prev;
      const lines = section.lines.map(l => l.id === selectedLineId ? { ...l, stations: l.stations.filter(s => s.id !== selectedStationId) } : l);
      return { ...prev, [activeSection]: { ...section, lines } };
    });
    setSelectedStationId(null);
  };

  const moveSelectedStation = (dir: -1 | 1) => {
    if (!content || !selectedLineId || !selectedStationId) return;
    setContent(prev => {
      if (!prev) return prev;
      const section = prev[activeSection];
      if (!section) return prev;
      const lines = section.lines.map(l => {
        if (l.id !== selectedLineId) return l;
        const idx = l.stations.findIndex(s => s.id === selectedStationId);
        if (idx < 0) return l;
        const nextIdx = Math.max(0, Math.min(l.stations.length - 1, idx + dir));
        if (idx === nextIdx) return l;
        const copy = l.stations.slice();
        const [spliced] = copy.splice(idx, 1);
        copy.splice(nextIdx, 0, spliced);
        return { ...l, stations: copy };
      });
      return { ...prev, [activeSection]: { ...section, lines } };
    });
  };

  // Пересбор соседей
  const rebuildNeighborsForLine = (lineId: string) => {
    if (!content) return;
    setContent(prev => {
      if (!prev) return prev;
      const section = prev[activeSection];
      if (!section) return prev;
      const lines = section.lines.map((l: Line) => l.id === lineId ? applyNeighbors(l, defaultNeighborTime) : l);
      return { ...prev, [activeSection]: { ...section, lines } };
    });
  };

  // Функция для добавления точек скривления к выбранному сегменту
  const addCurvaturePoints = () => {
    if (!content || !selectedSegment) return;
    setContent(prev => {
      if (!prev) return prev;
      const section = prev[activeSection];
      if (!section) return prev;
      
      const a = findStationInSection(section, selectedSegment.aId);
      const b = findStationInSection(section, selectedSegment.bId);
      if (!a || !b) return prev;
      
      // Создаем контрольные точки рядом с концами сегмента
      const midx = (a.x + b.x) / 2;
      const midy = (a.y + b.y) / 2;
      const c1 = { x: (a.x + midx) / 2, y: (a.y + midy) / 2 };
      const c2 = { x: (b.x + midx) / 2, y: (b.y + midy) / 2 };
      
      const intermediatePoints = section.intermediatePoints ?? [];
      const existingIdx = intermediatePoints.findIndex(ip => 
        (ip.neighborsId[0] === selectedSegment.aId && ip.neighborsId[1] === selectedSegment.bId) ||
        (ip.neighborsId[0] === selectedSegment.bId && ip.neighborsId[1] === selectedSegment.aId)
      );
      
      const newEntry = { neighborsId: [a.id, b.id] as [string, string], points: [c1, c2] };
      const updated = existingIdx === -1 
        ? [...intermediatePoints, newEntry] 
        : intermediatePoints.map((item, idx) => idx === existingIdx ? newEntry : item);
      
      return { ...prev, [activeSection]: { ...section, intermediatePoints: updated } };
    });
  };

  const findStationInSection = (section: MetroMap, stationId: string): Station | null => {
    for (const line of section.lines) {
      const station = line.stations.find(s => s.id === stationId);
      if (station) return station;
    }
    return null;
  };

  const getSegmentTime = (): number => {
    if (!selectedSegmentData) return defaultNeighborTime;
    const { stationA, stationB } = selectedSegmentData;
    const neighborA = stationA.neighbors?.find(([id]) => id === stationB.id);
    if (neighborA) return neighborA[1];
    const neighborB = stationB.neighbors?.find(([id]) => id === stationA.id);
    if (neighborB) return neighborB[1];
    return defaultNeighborTime;
  };

  const buildStaticCacheForDrag = useCallback((excludeStationId: string) => {
    const mainCanvas = canvasRef.current;
    if (!mainCanvas || !metro) return;
    let off = staticCacheCanvasRef.current;
    if (!off) {
      off = document.createElement('canvas');
      staticCacheCanvasRef.current = off;
    }
    off.width = mainCanvas.clientWidth;
    off.height = mainCanvas.clientHeight;
    const ctx = off.getContext('2d');
    if (!ctx) return;

    // draw static content excluding the moving station and its adjacent segments and transfers
    ctx.clearRect(0, 0, off.width, off.height);
    ctx.fillStyle = '#0b0f13';
    ctx.fillRect(0, 0, off.width, off.height);

    if (scale > 0.3) {
      ctx.strokeStyle = '#1e2530';
      ctx.lineWidth = 1;
      const gridSize = 50;
      for (let x = 0; x <= off.width; x += gridSize) { ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, off.height); ctx.stroke(); }
      for (let y = 0; y <= off.height; y += gridSize) { ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(off.width, y); ctx.stroke(); }
    }

    const lines = metro?.lines ?? [];
    const isExcluded = (sId: string) => sId === excludeStationId;

    for (const line of lines) {
      const stations = line.stations ?? [];
      for (let i = 0; i < stations.length - 1; i++) {
        const a = stations[i];
        const b = stations[i + 1];
        if (isExcluded(a.id) || isExcluded(b.id)) continue; // skip segments touching moving station

        const intermediatePoint = metro!.intermediatePoints?.find(ip =>
          (ip.neighborsId[0] === a.id && ip.neighborsId[1] === b.id) ||
          (ip.neighborsId[0] === b.id && ip.neighborsId[1] === a.id)
        );

        ctx.strokeStyle = line.color || '#cccccc';
        ctx.lineWidth = 6;
        ctx.lineJoin = 'round';
        ctx.lineCap = 'round';
        ctx.beginPath();
        if (intermediatePoint && intermediatePoint.points.length === 2) {
          const [c1, c2] = intermediatePoint.points;
          ctx.moveTo(a.x, a.y);
          ctx.bezierCurveTo(c1.x, c1.y, c2.x, c2.y, b.x, b.y);
        } else {
          ctx.moveTo(a.x, a.y);
          ctx.lineTo(b.x, b.y);
        }
        ctx.stroke();
      }

      for (const s of stations) {
        if (isExcluded(s.id)) continue; // skip moving station
        ctx.fillStyle = '#ffffff';
        ctx.beginPath();
        ctx.arc(s.x, s.y, 5, 0, Math.PI * 2);
        ctx.fill();
        if (scale > 0.5) drawStationLabel(ctx, s);
      }
    }

    // transfers excluding moving station
    for (const tr of metro?.transfers ?? []) {
      if (!tr.stations || tr.stations.length < 2) continue;
      const [aId, bId] = tr.stations;
      if (isExcluded(aId) || isExcluded(bId)) continue;
      const findPos = (id: string): { x: number, y: number } | null => {
        for (const l of lines) { const s = l.stations.find(st => st.id === id); if (s) return { x: s.x, y: s.y }; }
        return null;
      };
      const a = findPos(aId); const b = findPos(bId);
      if (!a || !b) continue;
      ctx.save();
      ctx.setLineDash([5, 5]);
      ctx.strokeStyle = tr.id === selectedTransferId ? '#ffcc00' : '#aaaaaa';
      ctx.beginPath();
      ctx.moveTo(a.x, a.y);
      ctx.lineTo(b.x, b.y);
      ctx.stroke();
      ctx.restore();
    }

    staticCacheValidRef.current = true;
  }, [metro, scale, selectedTransferId]);

  return (
    <div>
      <Group gap={8} align="center" mb={8} wrap="wrap">
        <Group gap={6}>
          <Button variant="light" onClick={() => setActiveSection('metro_map')}>Метро</Button>
          <Button variant="light" onClick={() => setActiveSection('suburban_map')}>Пригород</Button>
          <Button variant="light" onClick={() => setActiveSection('rivertram_map')}>Речной трамвай</Button>
          <Button variant="light" onClick={() => setActiveSection('tram_map')}>Трамвай</Button>
        </Group>
        <Button onClick={download} variant="light">Скачать JSON</Button>
        <Button onClick={newProject} variant="outline">Новый проект</Button>
        <Group gap={8}>
          <Text size="sm">Загрузить файл</Text>
          <input type="file" accept="application/json" onChange={(e)=>onLocalFileSelected(e.target.files?.[0]||undefined)} />
        </Group>
        <Checkbox checked={snapToGrid} onChange={(e)=>setSnapToGrid(e.currentTarget.checked)} label="Привязка к сетке" />
        <NumberInput min={0} value={defaultNeighborTime} onChange={(v)=>setDefaultNeighborTime(Number(v)||0)} w={90} label="Время соседа" />
        <Checkbox checked={autoNeighborsOnSave} onChange={(e)=>setAutoNeighborsOnSave(e.currentTarget.checked)} label="Авто-соседи при сохранении" />
        <Button onClick={saveToDisk} title="Сохранить в файл проекта через локальный API">Сохранить в проект</Button>
        {error && <Badge color="red">Ошибка: {error}</Badge>}
        {isDebouncedRendering && <Badge color="blue">Рендеринг...</Badge>}
        <Badge color="green">Станций: {metro?.lines?.reduce((acc, l) => acc + (l.stations?.length || 0), 0) || 0}</Badge>
      </Group>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 360px', gap: 12, height: '70vh' }}>
        <Paper withBorder radius="md" style={{ position: 'relative' }}>
          <canvas
            ref={canvasRef}
            style={{ width: '100%', height: '100%' }}
            onPointerDown={onCanvasPointerDown}
            onPointerMove={onCanvasPointerMove}
            onPointerUp={onCanvasPointerUp}
            onPointerLeave={onCanvasPointerUp}
          />
          <canvas
            ref={overlayCanvasRef}
            style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', pointerEvents: 'none' }}
          />
        </Paper>
        <Paper withBorder radius="md" p="sm" style={{ overflow: 'hidden' }}>
          <ScrollArea style={{ height: '100%' }}>
            <Title order={4}>Линии</Title>
            <Group gap={8} mb={8}>
              <Button onClick={()=>addLine()} size="xs">Добавить линию</Button>
              {selectedLine && <Button onClick={()=>removeSelectedLine()} color="red" variant="light" size="xs">Удалить линию</Button>}
            </Group>
            {selectedLine && (
              <Stack gap={6} mb={10}>
                <TextInput label="Название" value={selectedLine.name} onChange={(e)=>updateSelectedLine({ name: e.currentTarget.value })} />
                <Group gap={8}>
                  <Text size="sm">Цвет</Text>
                  <input type="color" value={selectedLine.color} onChange={(e)=>updateSelectedLine({ color: e.target.value })} />
                </Group>
              </Stack>
            )}
            <div>
              {(metro?.lines ?? []).map(l => (
                <Paper key={l.id} withBorder radius="sm" p={6} style={{ background: l.id===selectedLineId? 'rgba(255,255,255,0.03)':'transparent', marginBottom: 6 }}>
                  <Group justify="space-between" align="center">
                    <Button variant="subtle" onClick={()=>{ setSelectedLineId(l.id); setSelectedStationId(null); }} style={{ fontWeight: 600 }}>{l.name}</Button>
                    <div style={{ width: 14, height: 14, background: l.color, borderRadius: 3 }} />
                  </Group>
                  <div style={{ marginTop: 6 }}>
                    {l.id===selectedLineId && (
                      <Group gap={8} mb={6}>
                        <Button size="xs" onClick={()=>addStation()}>Добавить станцию</Button>
                        {selectedStation && <Button size="xs" color="red" variant="light" onClick={()=>removeSelectedStation()}>Удалить станцию</Button>}
                        {selectedStation && <Button size="xs" variant="outline" onClick={()=>moveSelectedStation(-1)}>Выше</Button>}
                        {selectedStation && <Button size="xs" variant="outline" onClick={()=>moveSelectedStation(1)}>Ниже</Button>}
                        <Button size="xs" variant="default" onClick={()=>rebuildNeighborsForLine(l.id)}>Пересобрать соседей</Button>
                      </Group>
                    )}
                    {(l.stations ?? []).map(s => (
                      <Group key={s.id} justify="space-between" gap={6}>
                        <Button variant="light" size="xs" onClick={()=>{ setSelectedLineId(l.id); setSelectedStationId(s.id); }}>{s.name}</Button>
                        <Text size="xs" c="dimmed">{s.x},{s.y}</Text>
                      </Group>
                    ))}
                  </div>
                </Paper>
              ))}
            </div>

            <Divider label="Станция" labelPosition="left" my="sm" />
            {selectedStation ? (
              <Stack gap={6}>
                <TextInput label="Название" value={selectedStation.name} onChange={e=>updateSelectedStation({ name: e.currentTarget.value })} />
                <NumberInput label="X" value={selectedStation.x} onChange={(v)=>updateSelectedStation({ x: Number(v) })} />
                <NumberInput label="Y" value={selectedStation.y} onChange={(v)=>updateSelectedStation({ y: Number(v) })} />
                <NumberInput label="textPosition" value={selectedStation.textPosition ?? 0} onChange={(v)=>updateSelectedStation({ textPosition: Number(v) })} />
              </Stack>
            ) : (
              <Text c="dimmed">Не выбрана</Text>
            )}

            <Divider label="Переходы" labelPosition="left" my="sm" />
            <Group gap={8} align="center" mb={8}>
              <Button size="xs" onClick={addTransfer}>Добавить переход</Button>
              {selectedTransfer && <Button size="xs" color="red" variant="light" onClick={removeSelectedTransfer}>Удалить выбранный</Button>}
            </Group>
            <div>
              {(transfers ?? []).map(t => (
                <Paper key={t.id} withBorder radius="sm" p={6} style={{ background: t.id===selectedTransferId? 'rgba(255,255,255,0.03)':'transparent', marginBottom: 6 }}>
                  <Button variant="subtle" onClick={()=>setSelectedTransferId(t.id)} style={{ fontWeight: 600 }}>Переход {t.id}</Button>
                  <Stack gap={6} mt={6}>
                    <Select label="Станция A" data={allStationsFlat.map(opt => ({ value: opt.id, label: opt.name }))} value={t.stations?.[0] ?? ''} onChange={(val)=>{
                      const b = t.stations?.[1] ?? '';
                      updateSelectedTransfer({ stations: [val || '', b] as string[] });
                    }} />
                    <Select label="Станция B" data={allStationsFlat.map(opt => ({ value: opt.id, label: opt.name }))} value={t.stations?.[1] ?? ''} onChange={(val)=>{
                      const a = t.stations?.[0] ?? '';
                      updateSelectedTransfer({ stations: [a, val || ''] as string[] });
                    }} />
                    <NumberInput label="Время (сек)" value={t.time} onChange={(v)=>updateSelectedTransfer({ time: Number(v) })} />
                    <TextInput label="Тип" value={t.type ?? 'default'} onChange={(e)=>updateSelectedTransfer({ type: e.currentTarget.value })} />
                  </Stack>
                </Paper>
              ))}
            </div>

            <Divider label="Сегмент" labelPosition="left" my="sm" />
            {selectedSegmentData ? (
              <Stack gap={6} mb={10}>
                <Text>Линия: {selectedSegmentData.line.name}</Text>
                <Text>От: {selectedSegmentData.stationA.name}</Text>
                <Text>До: {selectedSegmentData.stationB.name}</Text>
                <Text>Текущее время: {getSegmentTime()}</Text>
                <Button onClick={addCurvaturePoints}>
                  {selectedSegmentData.intermediatePoint ? 'Обновить точки скривления' : 'Добавить точки скривления'}
                </Button>
                {selectedSegmentData.intermediatePoint && (
                  <Text size="xs" c="dimmed">Контрольные точки: {selectedSegmentData.intermediatePoint.points.length}</Text>
                )}
              </Stack>
            ) : (
              <Text c="dimmed">Не выбран</Text>
            )}

            <Divider label="Валидация" labelPosition="left" my="sm" />
            <Group gap={8} mb={8}>
              <Button size="xs" variant="default" onClick={()=>setValidationErrors(validateFile(content))}>Проверить JSON</Button>
              <Button size="xs" variant="default" onClick={()=>{ if(content){ const v = rebuildAllNeighbors(content); setContent(v);} }}>Пересобрать соседей для всех линий</Button>
            </Group>
            {validationErrors.length>0 ? (
              <ul style={{ margin: 0 }}>
                {validationErrors.map((e,i)=>(<li key={i} style={{ color:'#ff8080' }}>{e}</li>))}
              </ul>
            ) : (
              <Text c="dimmed">Ошибок не обнаружено</Text>
            )}
          </ScrollArea>
        </Paper>
      </div>
    </div>
  );
}

function genId(prefix: string) {
  return `${prefix}_${Math.random().toString(36).slice(2, 8)}`;
}

function applyNeighbors(line: Line, defaultTime: number): Line {
  const stations = line.stations ?? [];
  const updated = stations.map((s, i) => {
    const neighbors: [string, number][] = [];
    if (i > 0) neighbors.push([stations[i - 1].id, defaultTime]);
    if (i < stations.length - 1) neighbors.push([stations[i + 1].id, defaultTime]);
    return { ...s, neighbors } as Station;
  });
  return { ...line, stations: updated };
}

function rebuildAllNeighbors(file: FileShape): FileShape {
  const sections: (keyof FileShape)[] = ['metro_map', 'suburban_map', 'rivertram_map', 'tram_map'];
  const result: FileShape = JSON.parse(JSON.stringify(file));
  for (const key of sections) {
    const map = result[key];
    if (!map) continue;
    map.lines = map.lines.map((l: Line) => applyNeighbors(l, 3));
  }
  return result;
}

function validateFile(file: FileShape | null): string[] {
  const errors: string[] = [];
  if (!file) return ['Файл не загружен'];
  const checkMap = (map: MetroMap | undefined, name: string) => {
    if (!map) return;
    const ids = new Set<string>();
    for (const line of map.lines ?? []) {
      if (!line.id) errors.push(`${name}: У линии без id`);
      for (const s of line.stations ?? []) {
        if (!s.id) errors.push(`${name}: Станция без id в линии ${line.name}`);
        if (ids.has(s.id)) errors.push(`${name}: Дубликат station id ${s.id}`);
        ids.add(s.id);
      }
    }
    for (const tr of map.transfers ?? []) {
      if (!tr.id) errors.push(`${name}: Переход без id`);
      if (!tr.stations || tr.stations.length < 2) errors.push(`${name}: Переход ${tr.id} должен иметь 2 станции`);
      else {
        for (const sid of tr.stations) {
          if (!ids.has(sid)) errors.push(`${name}: Переход ${tr.id} ссылается на неизвестную станцию ${sid}`);
        }
      }
      if (typeof tr.time !== 'number' || tr.time < 0) errors.push(`${name}: Переход ${tr.id} имеет некорректное время`);
    }
  };
  checkMap(file.metro_map, 'metro_map');
  checkMap(file.suburban_map, 'suburban_map');
  checkMap(file.rivertram_map, 'rivertram_map');
  checkMap(file.tram_map, 'tram_map');
  return errors;
}

function drawStationLabel(ctx: CanvasRenderingContext2D, station: Station) {
  const pos = Math.max(0, Math.min(9, station.textPosition ?? 0));
  if (pos === 9) return;
  const pad = 12;
  const offsets = [
    { dx: 0, dy: -pad },
    { dx: pad, dy: -pad },
    { dx: pad, dy: 0 },
    { dx: pad, dy: pad },
    { dx: 0, dy: pad },
    { dx: -pad, dy: pad },
    { dx: -pad, dy: 0 },
    { dx: -pad, dy: -pad },
    { dx: 0, dy: 0 }
  ];
  const { dx, dy } = offsets[pos] || { dx: 0, dy: 0 };
  const x = station.x + dx;
  const y = station.y + dy;

  ctx.font = '12px system-ui, -apple-system, Segoe UI, Roboto, Arial';
  ctx.textBaseline = 'middle';
  ctx.textAlign = 'center';
  ctx.lineWidth = 4;
  ctx.strokeStyle = '#ffffff';
  ctx.strokeText(station.name, x, y);
  ctx.fillStyle = '#111111';
  ctx.fillText(station.name, x, y);
}


