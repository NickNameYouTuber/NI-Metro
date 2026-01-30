import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import * as PIXI from 'pixi.js';
import { Group, Stack } from '@mantine/core';
import { EditorTopBar } from './components/EditorTopBar';
import { EditorLeftSidebar } from './components/EditorLeftSidebar';
import { EditorRightSidebar } from './components/EditorRightSidebar';
import { EditorCanvas } from './components/EditorCanvas';

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

export function PixiMapEditor({ mapPath, initialData, onChange, projectName = '', saving = false, loading = false, hasApiKey = false }: { mapPath: string; initialData?: any; onChange?: (next: any) => void; projectName?: string; saving?: boolean; loading?: boolean; hasApiKey?: boolean }) {
  const [content, setContent] = useState<FileShape | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [activeSection, setActiveSection] = useState<'metro_map' | 'suburban_map' | 'rivertram_map' | 'tram_map'>('metro_map');
  const [selectedLineId, setSelectedLineId] = useState<string | null>(null);
  const [selectedStationId, setSelectedStationId] = useState<string | null>(null);
  const [selectedTransferId, setSelectedTransferId] = useState<string | null>(null);
  const [selectedSegment, setSelectedSegment] = useState<{ lineId: string; aId: string; bId: string } | null>(null);
  const [isDragging, setIsDragging] = useState<boolean>(false);
  const [dragTarget, setDragTarget] = useState<{ lineId: string; stationId: string } | null>(null);
  const [snapToGrid, setSnapToGrid] = useState<boolean>(true);
  const [defaultNeighborTime, setDefaultNeighborTime] = useState<number>(3);
  const [autoNeighborsOnSave, setAutoNeighborsOnSave] = useState<boolean>(true);
  const [validationErrors, setValidationErrors] = useState<string[]>([]);
  const [leftTab, setLeftTab] = useState<'lines' | 'stations' | 'transfers'>('lines');
  const [stationFilter, setStationFilter] = useState<string>('');
  // removed old single-station adder state
  const [editingTransferTime, setEditingTransferTime] = useState<number | ''>('');
  const [editingTransferType, setEditingTransferType] = useState<string>('default');

  const pixiContainerRef = useRef<HTMLDivElement>(null);
  const appRef = useRef<PIXI.Application | null>(null);
  const rootContainerRef = useRef<PIXI.Container | null>(null);
  const gridRef = useRef<PIXI.Graphics | null>(null);
  const lineLayerRef = useRef<PIXI.Container | null>(null);
  const riverLayerRef = useRef<PIXI.Container | null>(null);
  const transferLayerRef = useRef<PIXI.Container | null>(null);
  const stationLayerRef = useRef<PIXI.Container | null>(null);
  const neighborLayerRef = useRef<PIXI.Container | null>(null);
  const controlLayerRef = useRef<PIXI.Container | null>(null);
  const backgroundSpriteRef = useRef<PIXI.Sprite | null>(null);
  const stationContainersRef = useRef<Map<string, PIXI.Container>>(new Map());
  const lineGraphicsRef = useRef<Map<string, PIXI.Graphics>>(new Map());
  const transferGraphicsRef = useRef<Map<string, PIXI.Graphics>>(new Map());
  const resizeHandlerRef = useRef<(() => void) | null>(null);
  const canvasDomRef = useRef<HTMLCanvasElement | null>(null);
  const pendingFitRef = useRef<boolean>(false);
  const initializedRef = useRef<boolean>(false);
  const appliedSettingsRef = useRef<boolean>(false);
  const [pixiReady, setPixiReady] = useState<boolean>(false);
  const [showGrid, setShowGrid] = useState<boolean>(true);
  const [showLines, setShowLines] = useState<boolean>(true);
  const [showTransfers, setShowTransfers] = useState<boolean>(true);
  const [showStations, setShowStations] = useState<boolean>(true);
  const [showLabels, setShowLabels] = useState<boolean>(true);
  const [snapStep, setSnapStep] = useState<number>(10);
  const [draggingControlPoint, setDraggingControlPoint] = useState<null | { index: 0 | 1 }>(null);
  const [zoomLevel, setZoomLevel] = useState<number>(1);
  const [coordinateScale, setCoordinateScale] = useState<number>(1);
  const contentSnapshotRef = useRef<FileShape | null>(null);
  const [isAddingRiver, setIsAddingRiver] = useState<boolean>(false);
  const tempRiverPointsRef = useRef<{ x: number; y: number }[]>([]);
  const tempRiverGraphicsRef = useRef<PIXI.Graphics | null>(null);
  const [addStationsModalOpen, setAddStationsModalOpen] = useState<Record<string, boolean>>({});
  const [addStationsModalValues, setAddStationsModalValues] = useState<Record<string, string[]>>({});
  const [addNeighborModalOpen, setAddNeighborModalOpen] = useState<boolean>(false);
  const [addNeighborModalValues, setAddNeighborModalValues] = useState<string[]>([]);
  const [rightTab, setRightTab] = useState<'properties' | 'settings' | 'background'>('properties');

  // Background image state
  const [bgUrl, setBgUrl] = useState<string>('');
  const [bgX, setBgX] = useState<number>(0);
  const [bgY, setBgY] = useState<number>(0);
  const [bgW, setBgW] = useState<number>(1000);
  const [bgH, setBgH] = useState<number>(700);
  const [bgAlpha, setBgAlpha] = useState<number>(0.4);
  const lastObjectUrlRef = useRef<string | null>(null);

  const historyRef = useRef<FileShape[]>([]);

  const cloneDeep = useCallback(<T,>(obj: T): T => {
    try { return (structuredClone as any)(obj); } catch { return JSON.parse(JSON.stringify(obj)); }
  }, []);

  const pushHistory = useCallback((snapshot: FileShape | null) => {
    if (!snapshot) return;
    const copy = cloneDeep(snapshot);
    historyRef.current.push(copy);
    const MAX = 50;
    if (historyRef.current.length > MAX) historyRef.current.shift();
  }, [cloneDeep]);

  const generateTransferId = useCallback(() => `transfer_${Date.now()}_${Math.random().toString(36).slice(2,7)}`, []);

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z') {
        if (historyRef.current.length > 0) {
          e.preventDefault();
          const prevSnapshot = historyRef.current.pop()!;
          setContent(prevSnapshot);
          contentSnapshotRef.current = prevSnapshot as any;
          if (onChange) {
            console.log('[PixiEditor] onChange after undo');
            onChange(prevSnapshot as any);
          }
        }
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, []);

  // ВАЖНО: вычисляем metro/selected раньше, чтобы не попасть в TDZ в зависимостях хуков ниже
  const metro = useMemo(() => content?.[activeSection], [content, activeSection]);
  const selectedLine = useMemo(() => metro?.lines?.find(l => l.id === selectedLineId) || null, [metro, selectedLineId]);
  const selectedStation = useMemo(() => selectedLine?.stations?.find(s => s.id === selectedStationId) || null, [selectedLine, selectedStationId]);

  const getStationPosition = useCallback((station: Station) => {
    const cont = stationContainersRef.current.get(station.id);
    return { x: cont ? cont.x : station.x * coordinateScale, y: cont ? cont.y : station.y * coordinateScale };
  }, [coordinateScale]);

  const findStationById = useCallback((id: string): Station | null => {
    const map = metro;
    if (!map) return null;
    for (const line of map.lines ?? []) {
      const st = line.stations?.find(s => s.id === id);
      if (st) return st;
    }
    return null;
  }, [metro]);

  const buildLineGraphics = useCallback((line: Line) => {
    const graphics = new PIXI.Graphics();
    const stations = line.stations ?? [];
    for (let i = 0; i < stations.length - 1; i++) {
      const a = stations[i];
      const b = stations[i + 1];
      const aPos = getStationPosition(a);
      const bPos = getStationPosition(b);
      const isSelectedSeg = selectedSegment && selectedSegment.lineId === line.id &&
        ((selectedSegment.aId === a.id && selectedSegment.bId === b.id) ||
         (selectedSegment.aId === b.id && selectedSegment.bId === a.id));
      const intermediatePoint = metro?.intermediatePoints?.find(ip =>
        (ip.neighborsId[0] === a.id && ip.neighborsId[1] === b.id) ||
        (ip.neighborsId[0] === b.id && ip.neighborsId[1] === a.id)
      );
      // путь сегмента, затем обводка
      graphics.moveTo(aPos.x, aPos.y);
      if (intermediatePoint && intermediatePoint.points.length === 2) {
        const [c1, c2] = intermediatePoint.points;
        graphics.bezierCurveTo(c1.x * coordinateScale, c1.y * coordinateScale, c2.x * coordinateScale, c2.y * coordinateScale, bPos.x, bPos.y);
      } else {
        graphics.lineTo(bPos.x, bPos.y);
      }
      graphics.stroke({ width: 6, color: Number((line.color || '#cccccc').replace('#', '0x')) });
        // Подсветка выбранного сегмента
      if (isSelectedSeg) {
        graphics.moveTo(aPos.x, aPos.y);
        if (intermediatePoint && intermediatePoint.points.length === 2) {
          const [c1, c2] = intermediatePoint.points;
          graphics.bezierCurveTo(c1.x * coordinateScale, c1.y * coordinateScale, c2.x * coordinateScale, c2.y * coordinateScale, bPos.x, bPos.y);
        } else {
          graphics.lineTo(bPos.x, bPos.y);
        }
        graphics.stroke({ width: 10, color: 0xffcc00 });
      }
    }
    // замыкание для кольцевых: соединяем последнюю и первую, если они соседи
    if ((stations?.length ?? 0) > 2) {
      const first = stations[0];
      const last = stations[stations.length - 1];
      const isNeighbors = (first.neighbors?.some(n => n[0] === last.id) || last.neighbors?.some(n => n[0] === first.id));
      if (isNeighbors) {
        const a = last;
        const b = first;
        const aPos = getStationPosition(a);
        const bPos = getStationPosition(b);
        const intermediatePoint = metro?.intermediatePoints?.find(ip =>
          (ip.neighborsId[0] === a.id && ip.neighborsId[1] === b.id) ||
          (ip.neighborsId[0] === b.id && ip.neighborsId[1] === a.id)
        );
        graphics.moveTo(aPos.x, aPos.y);
        if (intermediatePoint && intermediatePoint.points.length === 2) {
          const [c1, c2] = intermediatePoint.points;
          graphics.bezierCurveTo(c1.x, c1.y, c2.x, c2.y, bPos.x, bPos.y);
        } else {
          graphics.lineTo(bPos.x, bPos.y);
        }
        graphics.stroke({ width: 6, color: Number((line.color || '#cccccc').replace('#', '0x')) });
      }
    }
    return graphics;
  }, [getStationPosition, selectedSegment, metro, coordinateScale]);

  const buildTransfersGraphics = useCallback((transfer: Transfer) => {
    const g = new PIXI.Graphics();
    const ids = transfer?.stations ?? [];
    if (!ids || ids.length < 2) return g;
    const positions: { id: string; x: number; y: number }[] = [];
    ids.forEach(id => {
      const st = findStationById(id);
      if (st) {
        const pos = getStationPosition(st);
        positions.push({ id, x: pos.x, y: pos.y });
      }
    });
    for (let i = 0; i < positions.length; i++) {
      for (let j = i + 1; j < positions.length; j++) {
        const a = positions[i];
        const b = positions[j];
        g.moveTo(a.x, a.y).lineTo(b.x, b.y);
      }
    }
    const isSelected = selectedTransferId === transfer.id;
    g.stroke({ width: isSelected ? 5 : 2, color: isSelected ? 0xffd400 : 0x888888, alpha: 1 });
    return g;
  }, [findStationById, getStationPosition, selectedTransferId]);

  // Отрисовка линий
  const redrawLines = useCallback(() => {
    if (!appRef.current) { console.log('[PixiEditor] redrawLines: no app'); return; }
    if (!metro) { console.log('[PixiEditor] redrawLines: no metro'); return; }
    if (!lineLayerRef.current) { console.log('[PixiEditor] redrawLines: no line layer'); return; }
    const layer = lineLayerRef.current;

    // Очистка старых линий
    layer.removeChildren().forEach((c: any) => c.destroy?.());
    lineGraphicsRef.current.clear();

    // Новые линии
    const count = metro.lines?.length || 0;
    console.log('[PixiEditor] redrawLines: lines=', count);
    metro.lines?.forEach(line => {
      const g = buildLineGraphics(line) as any;
      g.__lineId = line.id;
      lineGraphicsRef.current.set(line.id, g);
      layer.addChild(g);
    });
  }, [metro, buildLineGraphics]);

  // Живое обновление линий для станций
  const redrawLinesForStation = useCallback((stationId: string) => {
    const map = metro;
    const layer = lineLayerRef.current;
    if (!map || !layer) return;
    (map.lines ?? []).forEach(line => {
      if (line.stations?.some(s => s.id === stationId)) {
        const prev = lineGraphicsRef.current.get(line.id);
        if (prev) {
          try {
            layer.removeChild(prev);
            prev.destroy();
          } catch {}
        }
        const g = buildLineGraphics(line) as any;
        g.__lineId = line.id;
        lineGraphicsRef.current.set(line.id, g);
        layer.addChild(g);
      }
    });
  }, [metro, buildLineGraphics]);

  const redrawSegmentControls = useCallback(() => {
    const layer = controlLayerRef.current; const map = metro;
    if (!layer) return;
    layer.removeChildren().forEach((c: any) => c.destroy?.());
    const seg = selectedSegment; if (!map || !seg) return;
    const line = (map.lines || []).find(l => l.id === seg.lineId); if (!line) return;
    const stations = line.stations || [];
    const a = stations.find(s => s.id === seg.aId); const b = stations.find(s => s.id === seg.bId);
    if (!a || !b) return;
    const aPos = getStationPosition(a); const bPos = getStationPosition(b);
    const ip = map.intermediatePoints?.find(ip => (ip.neighborsId[0] === a.id && ip.neighborsId[1] === b.id) || (ip.neighborsId[0] === b.id && ip.neighborsId[1] === a.id));
    const points = ip?.points ? ip.points.map(p => ({ x: p.x * coordinateScale, y: p.y * coordinateScale })) : [
      { x: aPos.x + (bPos.x - aPos.x) * 0.33, y: aPos.y + (bPos.y - aPos.y) * 0.33 },
      { x: aPos.x + (bPos.x - aPos.x) * 0.66, y: aPos.y + (bPos.y - aPos.y) * 0.66 },
    ];

    points.forEach((p, idx) => {
      const g = new PIXI.Graphics();
      g.circle(0, 0, 6).fill(0xffd400);
      g.position.set(p.x, p.y);
      (g as any).__ctrlIndex = idx as 0 | 1;
      g.eventMode = 'static' as any;
      g.cursor = 'grab' as any;
      g.on('pointerdown', () => { setDraggingControlPoint({ index: idx as 0 | 1 }); });
      layer.addChild(g);
    });
  }, [metro, selectedSegment, getStationPosition, coordinateScale]);

  useEffect(() => {
    redrawSegmentControls();
  }, [selectedSegment, redrawSegmentControls]);

  // Отрисовка переходов
  const redrawTransfers = useCallback(() => {
    if (!appRef.current) { console.log('[PixiEditor] redrawTransfers: no app'); return; }
    if (!metro) { console.log('[PixiEditor] redrawTransfers: no metro'); return; }
    if (!transferLayerRef.current) { console.log('[PixiEditor] redrawTransfers: no transfer layer'); return; }
    const layer = transferLayerRef.current;
    layer.removeChildren().forEach((c: any) => c.destroy?.());
    const tcount = (metro.transfers ?? []).length;
    console.log('[PixiEditor] redrawTransfers: transfers=', tcount);
    (metro.transfers ?? []).forEach(t => {
      const g = buildTransfersGraphics(t);
      layer.addChild(g);
    });
  }, [metro, buildTransfersGraphics]);

  // Рисуем реки
  const redrawRivers = useCallback(() => {
    if (!appRef.current || !metro || !riverLayerRef.current) return;
    const layer = riverLayerRef.current;
    layer.removeChildren().forEach((c: any) => c.destroy?.());
    const rivers = metro.rivers || [];
    rivers.forEach((river: any) => {
      const g = new PIXI.Graphics();
      const pts: { x: number; y: number }[] = river.points || [];
      if (pts.length > 0) {
        g.moveTo(pts[0].x * coordinateScale, pts[0].y * coordinateScale);
        for (let i = 1; i < pts.length; i++) g.lineTo(pts[i].x * coordinateScale, pts[i].y * coordinateScale);
        g.stroke({ width: 6, color: 0x5dade2, alpha: 0.7 });
      }
      layer.addChild(g);
    });
    // временная полилиния при добавлении
    if (tempRiverGraphicsRef.current) layer.addChild(tempRiverGraphicsRef.current);
  }, [metro, coordinateScale]);

  // Отрисовка станций
  const redrawStations = useCallback(() => {
    if (!appRef.current) { console.log('[PixiEditor] redrawStations: no app'); return; }
    if (!metro) { console.log('[PixiEditor] redrawStations: no metro'); return; }
    if (!stationLayerRef.current) { console.log('[PixiEditor] redrawStations: no station layer'); return; }
    const layer = stationLayerRef.current;

    // Очистка старых станций
    layer.removeChildren().forEach((c: any) => c.destroy?.());
    stationContainersRef.current.clear();

    // Собираем все смежные станции для выбранной
    const neighborIds = new Set<string>();
    if (selectedStationId) {
      const selectedStation = findStationById(selectedStationId);
      if (selectedStation?.neighbors) {
        selectedStation.neighbors.forEach(([id]) => neighborIds.add(id));
      }
    }

    // Новые станции
    let scount = 0;
    metro.lines?.forEach(line => {
      line.stations?.forEach(station => {
        scount++;
        const container = new PIXI.Container();
        container.x = station.x * coordinateScale;
        container.y = station.y * coordinateScale;

        // Круг
        const circle = new PIXI.Graphics();
        const isSelected = station.id === selectedStationId;
        const isNeighbor = neighborIds.has(station.id);
        circle.circle(0, 0, 5);
        if (isSelected) {
          circle.fill(0x00e0ff);
        } else if (isNeighbor) {
          circle.fill(0xffaa00);
        } else {
          circle.fill(0x111111);
        }
        container.addChild(circle);

        // Подпись (над кругом внутри контейнера)
        if (station.textPosition !== 9) {
          const text = new PIXI.Text({
            text: station.name,
            style: {
              fontSize: 12,
              fill: 0x111111,
              stroke: { color: 0xffffff, width: 4 },
              fontFamily: 'system-ui, Arial'
            }
          } as any);
          text.anchor.set(0.5);
          text.y = -15;
          ;(text as any).isText = true;
          text.visible = showLabels;
          container.addChild(text);
        }

        // Интерактивность
        container.eventMode = 'static' as any;
        container.cursor = 'pointer' as any;
        container.on('pointerdown', () => {
          setSelectedLineId(line.id);
          setSelectedStationId(station.id);
          setSelectedTransferId(null);
          setSelectedSegment(null);
          // выбор станции теперь делается через селект внутри карточки перехода
          setIsDragging(true);
          setDragTarget({ lineId: line.id, stationId: station.id });
        });

        stationContainersRef.current.set(station.id, container);
        layer.addChild(container);
      });
    });
    console.log('[PixiEditor] redrawStations: stations=', scount);
  }, [metro, selectedStationId, findStationById, coordinateScale]);

  // Отрисовка связей смежных станций
  const redrawNeighborConnections = useCallback(() => {
    if (!appRef.current || !metro || !neighborLayerRef.current) return;
    const layer = neighborLayerRef.current;
    layer.removeChildren().forEach((c: any) => c.destroy?.());

    if (!selectedStationId) return;

    const selectedStation = findStationById(selectedStationId);
    if (!selectedStation || !selectedStation.neighbors || selectedStation.neighbors.length === 0) return;

    selectedStation.neighbors.forEach(([neighborId, time]) => {
      const neighborStation = findStationById(neighborId);
      if (!neighborStation) return;

      const graphics = new PIXI.Graphics();
      const dx = neighborStation.x * coordinateScale - selectedStation.x * coordinateScale;
      const dy = neighborStation.y * coordinateScale - selectedStation.y * coordinateScale;
      const dist = Math.sqrt(dx * dx + dy * dy);
      const dashLength = 5;
      const gapLength = 5;
      const segments = Math.floor(dist / (dashLength + gapLength));
      const stepX = dx / segments;
      const stepY = dy / segments;
      const dashStepX = stepX * (dashLength / (dashLength + gapLength));
      const dashStepY = stepY * (dashLength / (dashLength + gapLength));
      for (let i = 0; i < segments; i++) {
        const startX = selectedStation.x * coordinateScale + stepX * i;
        const startY = selectedStation.y * coordinateScale + stepY * i;
        graphics.moveTo(startX, startY);
        graphics.lineTo(startX + dashStepX, startY + dashStepY);
      }
      graphics.stroke({ width: 2, color: 0xffaa00, alpha: 0.6 });
      layer.addChild(graphics);

      if (time > 0) {
        const midX = (selectedStation.x + neighborStation.x) / 2 * coordinateScale;
        const midY = (selectedStation.y + neighborStation.y) / 2 * coordinateScale;
        const timeText = new PIXI.Text({
          text: `${time}с`,
          style: {
            fontSize: 10,
            fill: 0xffaa00,
            stroke: { color: 0xffffff, width: 2 },
            fontFamily: 'system-ui, Arial'
          }
        } as any);
        timeText.anchor.set(0.5);
        timeText.position.set(midX, midY);
        layer.addChild(timeText);
      }
    });
  }, [metro, selectedStationId, findStationById]);

  // Перерисовка при изменении данных (без авто-fit)
  useEffect(() => {
    console.log('[PixiEditor] content changed, metro lines=', metro?.lines?.length || 0, 'pixiReady=', pixiReady);
    // Сохраняем снапшот контента для безопасного persistEditorSettings
    contentSnapshotRef.current = content as any;
    if (!pixiReady) return; // дождемся инициализации PIXI
    redrawStations();
    redrawRivers();
    redrawLines();
    redrawTransfers();
    redrawNeighborConnections();
  }, [metro, selectedSegment, selectedStationId, pixiReady, redrawLines, redrawTransfers, redrawStations, redrawRivers, redrawNeighborConnections]);

  // После инициализации PIXI отрисовать текущие данные
  useEffect(() => {
    if (!pixiReady) return;
    console.log('[PixiEditor] pixiReady -> redraw, lines=', metro?.lines?.length || 0);
    redrawStations();
    redrawLines();
    redrawTransfers();
    redrawNeighborConnections();
  }, [pixiReady, redrawStations, redrawLines, redrawTransfers, redrawNeighborConnections]);

  // Инициализация PIXI приложения
  useEffect(() => {
    if (!pixiContainerRef.current) return;

    const initApp = async () => {
      const app = new PIXI.Application();

      // Создаем DOM-канвас и передаем его в init, чтобы гарантировать HTMLCanvasElement
      const domCanvas = document.createElement('canvas');
      await app.init({
        canvas: domCanvas as any,
        background: 0xffffff as any,
        antialias: true,
        resolution: window.devicePixelRatio || 1,
      });
      appRef.current = app;

      if (!pixiContainerRef.current) return; // компонент мог размонтироваться
      pixiContainerRef.current.appendChild(domCanvas);
      canvasDomRef.current = domCanvas;

      // Начальные размеры по контейнеру
      const resizeToContainer = () => {
        if (!pixiContainerRef.current || !appRef.current) return;
        const w = Math.max(1, pixiContainerRef.current.clientWidth);
        const h = Math.max(1, pixiContainerRef.current.clientHeight);
        appRef.current.renderer.resize(w, h);
        // Перерисовать сетку под новый размер (в корневых координатах)
        redrawGrid();
      };

      resizeHandlerRef.current = resizeToContainer;
      resizeToContainer();
      window.addEventListener('resize', resizeToContainer);

      // Включаем события на сцене (требуется для Pixi v8)
      // @ts-ignore
      if ((app.stage as any).eventMode !== undefined) {
        // @ts-ignore
        (app.stage as any).eventMode = 'static';
        // @ts-ignore
        (app.stage as any).hitArea = app.screen;
      }

      // Корневой контейнер для содержимого карты
      const root = new PIXI.Container();
      rootContainerRef.current = root;
      app.stage.addChild(root);

      // Слои (снизу вверх)
      // Фон (спрайт) под сеткой: вставляем первым
      const bgSprite = new PIXI.Sprite();
      backgroundSpriteRef.current = bgSprite;
      root.addChild(bgSprite);

      const grid = new PIXI.Graphics();
      gridRef.current = grid;
      root.addChild(grid);

      const lineLayer = new PIXI.Container();
      lineLayerRef.current = lineLayer;
      const riverLayer = new PIXI.Container();
      riverLayerRef.current = riverLayer;
      root.addChild(riverLayer);

      root.addChild(lineLayer);

      const transferLayer = new PIXI.Container();
      transferLayerRef.current = transferLayer;
      root.addChild(transferLayer);

      const stationLayer = new PIXI.Container();
      stationLayerRef.current = stationLayer;
      root.addChild(stationLayer);

      const neighborLayer = new PIXI.Container();
      neighborLayerRef.current = neighborLayer;
      root.addChild(neighborLayer);

      const ctrlLayer = new PIXI.Container();
      controlLayerRef.current = ctrlLayer;
      root.addChild(ctrlLayer);

      // Первая отрисовка сразу после инициализации (сначала станции)
      redrawStations();
      redrawRivers();
      redrawLines();
      redrawTransfers();
      fitToContent();
      redrawGrid();
      setPixiReady(true);
    };

    initApp();

    return () => {
      try {
        if (resizeHandlerRef.current) {
          window.removeEventListener('resize', resizeHandlerRef.current);
          resizeHandlerRef.current = null;
        }
        const app = appRef.current;
        appRef.current = null;
        setPixiReady(false);
        if (app) {
          try { app.destroy(true); } catch {}
        }
      } finally {
        rootContainerRef.current = null;
        gridRef.current = null;
        lineLayerRef.current = null;
        transferLayerRef.current = null;
        stationLayerRef.current = null;
        controlLayerRef.current = null;
        neighborLayerRef.current = null;
        backgroundSpriteRef.current = null;
        canvasDomRef.current = null;
        stationContainersRef.current.clear();
        lineGraphicsRef.current.clear();
      }
    };
  }, []);

  const SNAP_STEP = snapStep || 10;

  const redrawGrid = useCallback(() => {
    const app = appRef.current;
    const root = rootContainerRef.current;
    const grid = gridRef.current;
    if (!app || !root || !grid) return;
    grid.clear();
    if (!showGrid) return;
    const tl = root.toLocal(new PIXI.Point(0, 0));
    const br = root.toLocal(new PIXI.Point(app.screen.width, app.screen.height));
    const minX = Math.min(tl.x, br.x);
    const maxX = Math.max(tl.x, br.x);
    const minY = Math.min(tl.y, br.y);
    const maxY = Math.max(tl.y, br.y);

    const startX = Math.floor(minX / SNAP_STEP) * SNAP_STEP;
    const endX = Math.ceil(maxX / SNAP_STEP) * SNAP_STEP;
    const startY = Math.floor(minY / SNAP_STEP) * SNAP_STEP;
    const endY = Math.ceil(maxY / SNAP_STEP) * SNAP_STEP;

    for (let x = startX; x <= endX; x += SNAP_STEP) {
      grid.moveTo(x, minY).lineTo(x, maxY);
    }
    for (let y = startY; y <= endY; y += SNAP_STEP) {
      grid.moveTo(minX, y).lineTo(maxX, y);
    }
    grid.stroke({ width: 1, color: 0xe5e7eb, alpha: 1 });
  }, [showGrid]);

  // Подгонка содержимого под экран
  const fitToContent = useCallback(() => {
    const app = appRef.current;
    const root = rootContainerRef.current;
    const map = metro;
    if (!app || !root || !map) return;

    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
    (map.lines ?? []).forEach(l => {
      (l.stations ?? []).forEach(s => {
        if (typeof s.x === 'number' && typeof s.y === 'number') {
          if (s.x < minX) minX = s.x;
          if (s.y < minY) minY = s.y;
          if (s.x > maxX) maxX = s.x;
          if (s.y > maxY) maxY = s.y;
        }
      });
    });
    if (!isFinite(minX) || !isFinite(minY) || !isFinite(maxX) || !isFinite(maxY)) return;

    const padding = 20;
    const contentW = Math.max(1, maxX - minX);
    const contentH = Math.max(1, maxY - minY);
    const scale = Math.min(
      (app.screen.width - padding * 2) / contentW,
      (app.screen.height - padding * 2) / contentH
    );

    root.scale.set(scale);
    setZoomLevel(scale);
    root.position.set(
      padding + -minX * scale,
      padding + -minY * scale
    );
  }, [metro]);

  // Подгонка только после первой загрузки карты, не при каждом изменении
  useEffect(() => {
    if (pendingFitRef.current && (metro?.lines?.length ?? 0) > 0) {
      fitToContent();
      pendingFitRef.current = false;
    }
  }, [metro, fitToContent]);

  // Загрузка данных
  useEffect(() => {
    setError(null);
    console.log('[PixiEditor] load by mapPath start', mapPath);
    const load = async () => {
      const pathTrim = (mapPath || '').trim();
      if (!pathTrim || pathTrim === '__NEW__') {
        console.log('[PixiEditor] no path, init new');
        setContent({ info: { name: 'New Map' }, metro_map: { lines: [] } });
        historyRef.current = [];
        pendingFitRef.current = true;
        return;
      }
      
      try {
        console.log('[PixiEditor] fetch', pathTrim);
        const response = await fetch(`http://localhost:5174/api/load?path=${encodeURIComponent(pathTrim)}`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const data = await response.json();
        console.log('[PixiEditor] api data', data);

        // Нормализация формата: поддерживаем как корневой { lines, transfers, ... }, так и { metro_map: {...} }
        let normalized: FileShape | null = null;
        if (data && (data.metro_map || data.suburban_map || data.rivertram_map || data.tram_map)) {
          normalized = data as FileShape;
        } else if (data && Array.isArray(data.lines)) {
          normalized = {
            info: data.info ?? {},
            metro_map: {
              lines: data.lines ?? [],
              transfers: data.transfers ?? [],
              intermediatePoints: data.intermediatePoints ?? [],
              rivers: data.rivers ?? [],
              objects: data.objects ?? []
            }
          } as FileShape;
        }

        if (!normalized) {
          console.warn('[PixiEditor] unsupported format');
          setError('Неподдерживаемый формат файла карты');
          setContent({ info: { name: 'New Map' }, metro_map: { lines: [] } });
          historyRef.current = [];
          pendingFitRef.current = true;
          return;
        }

        // Normalize: ensure each transfer has an id
        const withIds = cloneDeep(normalized);
        (['metro_map','suburban_map','rivertram_map','tram_map'] as const).forEach((key) => {
          const section = (withIds as any)[key];
          if (section && Array.isArray(section.transfers)) {
            section.transfers = section.transfers.map((t: any) => ({
              id: t.id || generateTransferId(),
              stations: Array.isArray(t.stations) ? t.stations : [],
              time: typeof t.time === 'number' ? t.time : (Number(defaultNeighborTime) || 3),
              type: t.type || 'default'
            }));
          }
        });
        console.log('[PixiEditor] normalized setContent lines=', withIds.metro_map?.lines?.length);
        setContent(withIds);
        historyRef.current = [];
        if (!initializedRef.current) {
          initializedRef.current = true;
          pendingFitRef.current = true;
        }
      } catch (err) {
        console.error('[PixiEditor] API load failed:', err);
        setContent({ info: { name: 'New Map' }, metro_map: { lines: [] } });
        historyRef.current = [];
        if (!initializedRef.current) pendingFitRef.current = true;
      }
    };
    load();
  }, [mapPath]);

  // Загрузка из initialData (файл)
  useEffect(() => {
    if (!initialData) return;
    try {
      console.log('[PixiEditor] initialData received');
      const data = initialData;
      let normalized: FileShape | null = null;
      const findLinesCarrier = (obj: any, depth = 0): any | null => {
        if (!obj || typeof obj !== 'object' || depth > 4) return null;
        if (Array.isArray((obj as any).lines)) return obj;
        for (const key of Object.keys(obj)) {
          const child = (obj as any)[key];
          const found = findLinesCarrier(child, depth + 1);
          if (found) return found;
        }
        return null;
      };
      if (data && (data.metro_map || data.suburban_map || data.rivertram_map || data.tram_map)) {
        normalized = data as FileShape;
      } else if (data && Array.isArray(data.lines)) {
        normalized = {
          info: data.info ?? {},
          metro_map: {
            lines: data.lines ?? [],
            transfers: data.transfers ?? [],
            intermediatePoints: data.intermediatePoints ?? [],
            rivers: data.rivers ?? [],
            objects: data.objects ?? []
          }
        } as FileShape;
      } else if (data && (data.metromap_1 || data.metro_map_1 || data.metromap)) {
        const mm = (data.metromap_1 || data.metro_map_1 || data.metromap) as any;
        normalized = {
          info: data.info ?? {},
          metro_map: {
            lines: mm.lines ?? [],
            transfers: mm.transfers ?? [],
            intermediatePoints: mm.intermediatePoints ?? [],
            rivers: mm.rivers ?? [],
            objects: mm.objects ?? []
          }
        } as FileShape;
      } else {
        const carrier = findLinesCarrier(data);
        if (carrier) {
          normalized = {
            info: (data as any).info ?? {},
            metro_map: {
              lines: carrier.lines ?? [],
              transfers: carrier.transfers ?? [],
              intermediatePoints: carrier.intermediatePoints ?? [],
              rivers: carrier.rivers ?? [],
              objects: carrier.objects ?? []
            }
          } as FileShape;
        }
      }
      if (!normalized) {
        console.warn('[PixiEditor] initialData unsupported format');
        setError('Неподдерживаемый формат файла карты');
        setContent({ info: { name: 'New Map' }, metro_map: { lines: [] } });
        historyRef.current = [];
        pendingFitRef.current = true;
        return;
      }
      // Normalize: ensure each transfer has an id
      const withIds = cloneDeep(normalized);
      (['metro_map','suburban_map','rivertram_map','tram_map'] as const).forEach((key) => {
        const section = (withIds as any)[key];
        if (section && Array.isArray(section.transfers)) {
          section.transfers = section.transfers.map((t: any) => ({
            id: t.id || generateTransferId(),
            stations: Array.isArray(t.stations) ? t.stations : [],
            time: typeof t.time === 'number' ? t.time : (Number(defaultNeighborTime) || 3),
            type: t.type || 'default'
          }));
        }
      });
      console.log('[PixiEditor] setContent from initialData lines=', withIds.metro_map?.lines?.length);
      setContent(withIds);
      historyRef.current = [];
      if (!initializedRef.current) {
        initializedRef.current = true;
        pendingFitRef.current = true;
      }
    } catch (e) {
      console.error('[PixiEditor] initialData error', e);
      setError('Ошибка чтения файла');
      historyRef.current = [];
      if (!initializedRef.current) pendingFitRef.current = true;
    }
  }, [initialData]);

  // Обработка перетаскивания
  useEffect(() => {
    const app = appRef.current;
    const root = rootContainerRef.current;
    if (!app || !root) return;

    const handlePointerMove = (event: PIXI.FederatedPointerEvent) => {
      if (draggingControlPoint && selectedSegment && metro) {
        const local = root.toLocal(event.global);
        const idx = draggingControlPoint.index;
        const [aId, bId] = [selectedSegment.aId, selectedSegment.bId];
        setContent(prev => {
          if (!prev) return prev as any;
          const section = (prev as any)[activeSection]; if (!section) return prev as any;
          const ips = [...(section.intermediatePoints || [])];
          let entry = ips.find((ip: any) => (ip.neighborsId[0] === aId && ip.neighborsId[1] === bId) || (ip.neighborsId[0] === bId && ip.neighborsId[1] === aId));
          if (!entry) { entry = { neighborsId: [aId, bId], points: [{ x: local.x / coordinateScale, y: local.y / coordinateScale }, { x: local.x / coordinateScale, y: local.y / coordinateScale }] }; ips.push(entry); }
          entry.points = [...entry.points];
          entry.points[idx] = { x: local.x / coordinateScale, y: local.y / coordinateScale };
          return { ...prev, [activeSection]: { ...section, intermediatePoints: ips } } as FileShape;
        });
        redrawLinesForStation(selectedSegment.aId);
        redrawSegmentControls();
        return;
      }
      if (isDragging && dragTarget) {
        const container = stationContainersRef.current.get(dragTarget.stationId);
        if (container) {
          const local = root.toLocal(event.global);
          let x = local.x;
          let y = local.y;
          if (snapToGrid) {
            x = Math.round(x / SNAP_STEP) * SNAP_STEP;
            y = Math.round(y / SNAP_STEP) * SNAP_STEP;
          }
          container.x = x;
          container.y = y;
          // Живое обновление связанных сегментов и переходов
          redrawLinesForStation(dragTarget.stationId);
          redrawTransfers();
          redrawNeighborConnections();
        }
      }
      if (isAddingRiver) {
        const local = root.toLocal(event.global);
        if (!tempRiverGraphicsRef.current) tempRiverGraphicsRef.current = new PIXI.Graphics();
        const g = tempRiverGraphicsRef.current;
        g.clear();
        const pts = tempRiverPointsRef.current.concat([{ x: local.x, y: local.y }]);
        if (pts.length > 0) {
          g.moveTo(pts[0].x, pts[0].y);
          for (let i = 1; i < pts.length; i++) g.lineTo(pts[i].x, pts[i].y);
          g.stroke({ width: 6, color: 0x5dade2, alpha: 0.5 });
        }
        redrawRivers();
      }
    };

    const handlePointerUp = () => {
      if (draggingControlPoint) {
        setDraggingControlPoint(null);
        // Сохраним изменения кривых
        if (onChange) {
          const base = contentSnapshotRef.current || content;
          if (base) {
            console.log('[PixiEditor] onChange after control drag');
            onChange(base);
          }
        }
        return;
      }
      if (isDragging && dragTarget) {
        const container = stationContainersRef.current.get(dragTarget.stationId);
        if (container) {
          // Коммитим изменения в состояние (делим на coordinateScale для сохранения оригинальных координат)
          let computed: FileShape | null = null;
          setContent(prev => {
            pushHistory(prev);
            if (!prev) { computed = prev as any; return prev as any; }
            const section = prev[activeSection as keyof FileShape] as any;
            if (!section) { computed = prev as any; return prev; }
            const lines = section.lines.map((l: Line) => {
              if (l.id !== dragTarget.lineId) return l;
              return {
                ...l,
                stations: l.stations.map((s: Station) =>
                  s.id === dragTarget.stationId
                    ? { ...s, x: container.x / coordinateScale, y: container.y / coordinateScale }
                    : s
                )
              } as Line;
            });
            computed = { ...prev, [activeSection]: { ...section, lines } } as FileShape;
            return computed as FileShape;
          });
          if (onChange && computed) {
            console.log('[PixiEditor] onChange after station drag');
            onChange(computed);
            contentSnapshotRef.current = computed as any;
          }
        }
      }
      setIsDragging(false);
      setDragTarget(null);
      // Глобальная перерисовка переходов после завершения
      redrawTransfers();
      redrawNeighborConnections();
      if (isAddingRiver) {
        // коммит временной реки в данные
        let computed: FileShape | null = null;
        setContent(prev => {
          if (!prev) return prev as any;
          const section = (prev as any)[activeSection] || { lines: [], rivers: [] };
          const rivers = Array.isArray(section.rivers) ? section.rivers : [];
          const river = { id: `river_${Date.now()}`, points: [...tempRiverPointsRef.current] };
          const nextMap = { ...section, rivers: [...rivers, river] };
          computed = { ...prev, [activeSection]: nextMap } as FileShape;
          return computed;
        });
        if (onChange && computed) onChange(computed);
        tempRiverPointsRef.current = [];
        if (tempRiverGraphicsRef.current) { try { tempRiverGraphicsRef.current.destroy(); } catch {} tempRiverGraphicsRef.current = null; }
        setIsAddingRiver(false);
        redrawRivers();
      }
    };

    app.stage.on('pointermove', handlePointerMove);
    app.stage.on('pointerup', handlePointerUp);
    app.stage.on('pointerupoutside', handlePointerUp);

    return () => {
      try {
        (app.stage as any)?.off?.('pointermove', handlePointerMove);
        (app.stage as any)?.off?.('pointerup', handlePointerUp);
        (app.stage as any)?.off?.('pointerupoutside', handlePointerUp);
      } catch {}
    };
  }, [isDragging, dragTarget, activeSection, snapToGrid, redrawTransfers, redrawLinesForStation, draggingControlPoint, selectedSegment, metro, redrawSegmentControls, coordinateScale]);

  // Поворот колеса: зум к курсору, панорамирование по зажатию
  useEffect(() => {
    const app = appRef.current;
    const root = rootContainerRef.current;
    const canvas = canvasDomRef.current;
    if (!pixiReady || !app || !root || !canvas) return;

    const minScale = 0.2;
    const maxScale = 5;

    const onWheel = (e: WheelEvent) => {
      e.preventDefault();
      const direction = -Math.sign(e.deltaY);
      const factor = 1 + direction * 0.1;
      const oldScale = root.scale.x;
      const newScale = Math.min(maxScale, Math.max(minScale, oldScale * factor));
      if (newScale === oldScale) return;

      const rect = canvas.getBoundingClientRect();
      const gx = e.clientX - rect.left;
      const gy = e.clientY - rect.top;

      const before = root.toLocal(new PIXI.Point(gx, gy));
      root.scale.set(newScale);
      setZoomLevel(newScale);
      const after = root.toLocal(new PIXI.Point(gx, gy));
      root.position.x += (after.x - before.x) * newScale;
      root.position.y += (after.y - before.y) * newScale;
      redrawGrid();
    };

    let panning = false;
    let panStart = new PIXI.Point();
    let startPos = new PIXI.Point();

    const onPointerDown = (e: PointerEvent) => {
      if (isDragging) return;
      panning = true;
      panStart.set(e.clientX, e.clientY);
      startPos.set(root.position.x, root.position.y);
    };
    const onPointerMove = (e: PointerEvent) => {
      if (!panning) return;
      const dx = e.clientX - panStart.x;
      const dy = e.clientY - panStart.y;
      root.position.set(startPos.x + dx, startPos.y + dy);
      redrawGrid();
    };
    const onPointerUp = () => { panning = false; };

    canvas.addEventListener('wheel', onWheel, { passive: false });
    canvas.addEventListener('pointerdown', onPointerDown);
    window.addEventListener('pointermove', onPointerMove);
    window.addEventListener('pointerup', onPointerUp);

    return () => {
      canvas.removeEventListener('wheel', onWheel as any);
      canvas.removeEventListener('pointerdown', onPointerDown as any);
      window.removeEventListener('pointermove', onPointerMove as any);
      window.removeEventListener('pointerup', onPointerUp as any);
    };
  }, [pixiReady, isDragging, redrawGrid]);

  // Перерисовать/очистить сетку при переключении showGrid
  useEffect(() => {
    redrawGrid();
  }, [showGrid, redrawGrid]);
  useEffect(() => { redrawGrid(); }, [snapStep, redrawGrid]);

  // Update background sprite when properties change
  const updateBackgroundSprite = useCallback(async () => {
    const sprite = backgroundSpriteRef.current;
    if (!sprite) { console.log('[PixiEditor] updateBackgroundSprite: no sprite yet'); return; }
    if (!bgUrl) {
      sprite.visible = false;
      return;
    }
    try {
      // Создаем HTML Image элемент для загрузки
      const img = new Image();
      img.crossOrigin = 'anonymous';
      await new Promise((resolve, reject) => {
        img.onload = resolve;
        img.onerror = reject;
        img.src = bgUrl;
      });
      
      // Создаем текстуру из загруженного изображения
      const texture = PIXI.Texture.from(img as any);
      
      sprite.texture = texture;
      sprite.x = bgX;
      sprite.y = bgY;
      sprite.width = bgW;
      sprite.height = bgH;
      sprite.alpha = Math.max(0, Math.min(1, bgAlpha));
      sprite.visible = true;
      console.log('[PixiEditor] background applied', { bgUrl, bgX, bgY, bgW, bgH, bgAlpha });
    } catch (e) {
      console.warn('Failed to load background image:', e);
      sprite.visible = false;
    }
  }, [bgUrl, bgX, bgY, bgW, bgH, bgAlpha]);

  useEffect(() => {
    updateBackgroundSprite();
  }, [updateBackgroundSprite]);

  // Ensure background redraws once Pixi is ready (in case bg state set before sprite existed)
  useEffect(() => {
    if (!pixiReady) return;
    updateBackgroundSprite();
    console.log('[PixiEditor] background updated after pixiReady');
  }, [pixiReady, updateBackgroundSprite]);

  // Convert blob: url to data: url on first load to persist across reloads
  useEffect(() => {
    const convert = async (blobUrl: string) => {
      try {
        const res = await fetch(blobUrl);
        const blob = await res.blob();
        const fr = new FileReader();
        fr.onload = () => setBgUrl(fr.result as string);
        fr.readAsDataURL(blob);
      } catch (e) {
        console.warn('[PixiEditor] failed to convert blob url', e);
      }
    };
    if (bgUrl && bgUrl.startsWith('blob:')) {
      convert(bgUrl);
    }
  }, [bgUrl]);

  // Load editor settings from initial data on mount/update (apply once per mount)
  useEffect(() => {
    const s = (initialData && initialData.info && initialData.info.editorSettings) || null;
    if (!s) return;
    if (appliedSettingsRef.current) return;
    if (typeof s.showGrid === 'boolean') setShowGrid(!!s.showGrid);
    if (typeof s.showLines === 'boolean') setShowLines(!!s.showLines);
    if (typeof s.showTransfers === 'boolean') setShowTransfers(!!s.showTransfers);
    if (typeof s.showStations === 'boolean') setShowStations(!!s.showStations);
    if (typeof s.showLabels === 'boolean') setShowLabels(!!s.showLabels);
    // Restore canvas view if provided
    const root = rootContainerRef.current;
    if (s.canvasView && root) {
      const { scale, posX, posY } = s.canvasView;
      if (typeof scale === 'number' && scale > 0) {
        root.scale.set(scale);
        setZoomLevel(scale);
      }
      if (typeof posX === 'number') root.position.x = posX;
      if (typeof posY === 'number') root.position.y = posY;
      pendingFitRef.current = false;
      redrawGrid();
    }
    if (s.background) {
      if (typeof s.background.url === 'string') setBgUrl(s.background.url);
      if (typeof s.background.x === 'number') setBgX(s.background.x);
      if (typeof s.background.y === 'number') setBgY(s.background.y);
      if (typeof s.background.w === 'number') setBgW(s.background.w);
      if (typeof s.background.h === 'number') setBgH(s.background.h);
      if (typeof s.background.alpha === 'number') setBgAlpha(s.background.alpha);
    }
    appliedSettingsRef.current = true;
  }, [initialData]);

  const persistEditorSettings = useCallback(() => {
    if (!onChange || !initializedRef.current) return;
    const base = (contentSnapshotRef.current || content);
    const linesCount = base?.metro_map?.lines?.length || 0;
    if (!base || linesCount === 0) {
      console.log('[PixiEditor] persistEditorSettings skipped (empty base)');
      return;
    }
    const next = cloneDeep(base);
    const info = next.info || {};
    (info as any).editorSettings = {
      showGrid,
      showLines,
      showTransfers,
      showStations,
      showLabels,
      snapStep,
      canvasView: {
        scale: zoomLevel,
        posX: rootContainerRef.current?.position?.x || 0,
        posY: rootContainerRef.current?.position?.y || 0,
      },
      background: { url: bgUrl, x: bgX, y: bgY, w: bgW, h: bgH, alpha: bgAlpha }
    };
    next.info = info;
    onChange(next);
  }, [onChange, content, showGrid, showLines, showTransfers, showStations, showLabels, snapStep, bgUrl, bgX, bgY, bgW, bgH, bgAlpha, zoomLevel, cloneDeep]);

  // Persist settings when toggled/changed
  useEffect(() => { persistEditorSettings(); }, [showGrid]);
  useEffect(() => { persistEditorSettings(); }, [showLines, showTransfers, showStations, showLabels]);
  useEffect(() => { persistEditorSettings(); }, [bgUrl, bgX, bgY, bgW, bgH, bgAlpha]);

  // Apply layer visibility effects
  useEffect(() => {
    if (lineLayerRef.current) (lineLayerRef.current as any).visible = showLines;
  }, [showLines]);
  useEffect(() => {
    if (transferLayerRef.current) (transferLayerRef.current as any).visible = showTransfers;
  }, [showTransfers]);
  useEffect(() => {
    if (stationLayerRef.current) (stationLayerRef.current as any).visible = showStations;
  }, [showStations]);

  const handleBgFile = useCallback(async (file: File | null) => {
    if (!file) return;
    try {
      const reader = new FileReader();
      reader.onload = () => {
        const dataUrl = reader.result as string;
        console.log('[PixiEditor] background file -> data URL length=', dataUrl?.length || 0);
        setBgUrl(dataUrl);
      };
      reader.readAsDataURL(file);
    } catch (e) {
      console.warn('[PixiEditor] background read error', e);
    }
  }, []);

  const addLine = useCallback(() => {
    if (!content) return;
    const newLine: Line = {
      id: `line_${Date.now()}`,
      name: `Линия ${(metro?.lines?.length || 0) + 1}`,
      color: '#ff0000',
      stations: []
    };
    let computed: any = null;
    setContent(prev => {
      pushHistory(prev);
      if (!prev) { computed = prev; return prev as any; }
      const section = (prev as any)[activeSection] || { lines: [] };
      computed = { ...prev, [activeSection]: { ...section, lines: [...section.lines, newLine] } } as FileShape;
      return computed;
    });
    if (onChange && computed) onChange(computed);
    setSelectedLineId(newLine.id);
  }, [content, metro, activeSection, pushHistory, onChange]);

  const addStation = useCallback(() => {
    if (!selectedLine) return;
    const newStation: Station = {
      id: `station_${Date.now()}`,
      name: `Станция ${selectedLine.stations.length + 1}`,
      x: 100 + selectedLine.stations.length * 50,
      y: 100,
      textPosition: 0,
      neighbors: []
    };
    let computed: any = null;
    setContent(prev => {
      pushHistory(prev);
      if (!prev) { computed = prev; return prev as any; }
      const section = (prev as any)[activeSection];
      if (!section) { computed = prev as any; return prev as any; }
      const lines = section.lines.map((l: Line) => {
        if (l.id !== selectedLine.id) return l;
        const updatedStations = [...l.stations, newStation];
        if (updatedStations.length >= 2) {
          const a = updatedStations[updatedStations.length - 2];
          const b = updatedStations[updatedStations.length - 1];
          const t = Number(defaultNeighborTime) || 0;
          a.neighbors = (a.neighbors || []).filter(n => n && n[0] !== b.id);
          b.neighbors = (b.neighbors || []).filter(n => n && n[0] !== a.id);
          a.neighbors.push([b.id, t]);
          b.neighbors.push([a.id, t]);
        }
        return { ...l, stations: updatedStations } as Line;
      });
      computed = { ...prev, [activeSection]: { ...section, lines } } as FileShape;
      return computed;
    });
    if (onChange && computed) onChange(computed);
    setSelectedStationId(newStation.id);
  }, [selectedLine, activeSection, pushHistory, defaultNeighborTime, onChange]);

  const updateSelectedStationName = useCallback((name: string) => {
    if (!content || !selectedLine || !selectedStation) return;
    let computed: any = null;
    setContent(prev => {
      pushHistory(prev);
      if (!prev) { computed = prev; return prev as any; }
      const section = (prev as any)[activeSection]; if (!section) { computed = prev as any; return prev as any; }
      const lines = section.lines.map((l: Line) => l.id !== selectedLine.id ? l : ({
        ...l,
        stations: l.stations.map(s => s.id === selectedStation.id ? { ...s, name } : s)
      } as Line));
      computed = { ...prev, [activeSection]: { ...section, lines } } as FileShape;
      return computed;
    });
    if (onChange && computed) onChange(computed);
    const cont = stationContainersRef.current.get(selectedStation.id);
    if (cont) {
      const label = cont.children.find((c: any) => (c as any).isText);
      if (label && (label as any).text !== undefined) { (label as any).text = name; }
    }
  }, [content, selectedLine, selectedStation, activeSection, pushHistory, onChange]);

  const updateSelectedStationCoord = useCallback((key: 'x' | 'y', valueRaw: number | string) => {
    if (!content || !selectedLine || !selectedStation) return;
    const value = Number(valueRaw) || 0;
    let computed: any = null;
    setContent(prev => {
      pushHistory(prev);
      if (!prev) { computed = prev; return prev as any; }
      const section = (prev as any)[activeSection]; if (!section) { computed = prev as any; return prev as any; }
      const lines = section.lines.map((l: Line) => l.id !== selectedLine.id ? l : ({
        ...l,
        stations: l.stations.map(s => s.id === selectedStation.id ? { ...s, [key]: value } : s)
      } as Line));
      computed = { ...prev, [activeSection]: { ...section, lines } } as FileShape;
      return computed;
    });
    if (onChange && computed) onChange(computed);
    const cont = stationContainersRef.current.get(selectedStation.id);
    if (cont) {
      if (key === 'x') cont.x = value * coordinateScale; else cont.y = value * coordinateScale;
    }
    redrawLinesForStation(selectedStation.id);
    redrawTransfers();
    redrawNeighborConnections();
  }, [content, selectedLine, selectedStation, activeSection, pushHistory, onChange, redrawLinesForStation, redrawTransfers, redrawNeighborConnections]);

  const updateSelectedLine = useCallback((patch: Partial<Line>) => {
    if (!content || !selectedLine) return;
    let computed: any = null;
    setContent(prev => {
      pushHistory(prev);
      if (!prev) { computed = prev; return prev as any; }
      const section = (prev as any)[activeSection]; if (!section) { computed = prev as any; return prev as any; }
      const lines = section.lines.map((l: Line) => l.id === selectedLine.id ? { ...l, ...patch } : l);
      computed = { ...prev, [activeSection]: { ...section, lines } } as FileShape;
      return computed;
    });
    if (onChange && computed) onChange(computed);
    if (patch.color !== undefined) { redrawLines(); }
  }, [content, selectedLine, activeSection, pushHistory, onChange, redrawLines]);

  const ensureTransfersArray = useCallback((map?: MetroMap) => {
    if (!map) return;
    if (!Array.isArray(map.transfers)) (map as any).transfers = [];
  }, []);

  const addTransfer = useCallback(() => {
    if (!content) return;
    let computed: any = null;
    const id = generateTransferId();
    setContent(prev => {
      pushHistory(prev);
      if (!prev) { computed = prev; return prev as any; }
      const section = (prev as any)[activeSection] || { lines: [], transfers: [] };
      const transfers = Array.isArray(section.transfers) ? section.transfers : [];
      const nextMap = { ...section, transfers: [...transfers, { id, stations: [], time: Number(defaultNeighborTime) || 3, type: 'default' }] };
      computed = { ...prev, [activeSection]: nextMap } as FileShape;
      return computed;
    });
    if (onChange && computed) onChange(computed);
    setSelectedTransferId(id);
    setEditingTransferTime(Number(defaultNeighborTime) || 3);
    setEditingTransferType('default');
  }, [content, activeSection, defaultNeighborTime, pushHistory, onChange]);

  const removeTransfer = useCallback((transferId: string) => {
    if (!content) return;
    let computed: any = null;
    setContent(prev => {
      pushHistory(prev);
      if (!prev) { computed = prev; return prev as any; }
      const section = (prev as any)[activeSection] || { lines: [], transfers: [] };
      const transfers = (section.transfers || []).filter((t: Transfer) => t.id !== transferId);
      computed = { ...prev, [activeSection]: { ...section, transfers } } as FileShape;
      return computed;
    });
    if (onChange && computed) onChange(computed);
    if (selectedTransferId === transferId) setSelectedTransferId(null);
  }, [content, activeSection, pushHistory, onChange, selectedTransferId]);

  const addStationsToTransfer = useCallback((transferId: string, stationIds: string[]) => {
    if (!content || !transferId || !stationIds || stationIds.length === 0) return;
    let computed: any = null;
    setContent(prev => {
      pushHistory(prev);
      if (!prev) { computed = prev; return prev as any; }
      const section = (prev as any)[activeSection] || { lines: [], transfers: [] };
      const transfers: Transfer[] = section.transfers || [];
      const next = transfers.map(t => {
        if (t.id !== transferId) return t;
        const existing = new Set<string>(t.stations || []);
        stationIds.forEach(id => existing.add(id));
        return { ...t, id: t.id || generateTransferId(), stations: Array.from(existing) } as Transfer;
      });
      computed = { ...prev, [activeSection]: { ...section, transfers: next } } as FileShape;
      return computed;
    });
    if (onChange && computed) onChange(computed);
  }, [content, activeSection, pushHistory, onChange]);

  const removeStationFromTransfer = useCallback((transferId: string, stationId: string) => {
    if (!content) return;
    let computed: any = null;
    setContent(prev => {
      pushHistory(prev);
      if (!prev) { computed = prev; return prev as any; }
      const section = (prev as any)[activeSection] || { lines: [], transfers: [] };
      const transfers: Transfer[] = section.transfers || [];
      const next = transfers.map(t => t.id === transferId
        ? { ...t, stations: (t.stations || []).filter(id => id !== stationId) }
        : t);
      computed = { ...prev, [activeSection]: { ...section, transfers: next } } as FileShape;
      return computed;
    });
    if (onChange && computed) onChange(computed);
  }, [content, activeSection, pushHistory, onChange]);

  const updateSelectedTransferMeta = useCallback((time: number | '', type: string) => {
    if (!content || !selectedTransferId) return;
    let computed: any = null;
    setContent(prev => {
      pushHistory(prev);
      if (!prev) { computed = prev; return prev as any; }
      const section = (prev as any)[activeSection] || { lines: [], transfers: [] };
      const transfers: Transfer[] = section.transfers || [];
      const tVal = time === '' ? undefined : Number(time) || 0;
      const next = transfers.map(t => t.id === selectedTransferId
        ? { ...t, id: t.id || generateTransferId(), time: typeof tVal === 'number' ? tVal : (t.time || 0), type }
        : t);
      computed = { ...prev, [activeSection]: { ...section, transfers: next } } as FileShape;
      return computed;
    });
    if (onChange && computed) onChange(computed);
  }, [content, selectedTransferId, activeSection, pushHistory, onChange]);

  // Генерация соседей для линии (сохранение возможного кольца между first<->last)
  const generateNeighborsForLine = useCallback((line: Line, defaultTime: number): Line => {
    const stations = line.stations ?? [];
    if (stations.length === 0) return line;
    // Определяем, было ли кольцо ранее
    const first = stations[0];
    const last = stations[stations.length - 1];
    const hadRing = Boolean(
      (first.neighbors || []).some(n => n && n[0] === last.id) &&
      (last.neighbors || []).some(n => n && n[0] === first.id)
    );
    // Сбрасываем и пересобираем соседей для последовательных пар
    stations.forEach((s) => { s.neighbors = []; });
    for (let i = 0; i < stations.length - 1; i++) {
      const a = stations[i];
      const b = stations[i + 1];
      a.neighbors!.push([b.id, defaultTime]);
      b.neighbors!.push([a.id, defaultTime]);
    }
    if (hadRing && stations.length > 2) {
      first.neighbors!.push([last.id, defaultTime]);
      last.neighbors!.push([first.id, defaultTime]);
    }
    return { ...line, stations: [...stations] };
  }, []);

  // Генерация соседей для секции карты
  const generateNeighborsForSection = useCallback((section?: MetroMap, defaultTime?: number): MetroMap | undefined => {
    if (!section) return section;
    const t = Number(defaultTime) || 0;
    const lines = (section.lines || []).map(l => generateNeighborsForLine(l, t));
    // гарантируем, что у каждой станции есть массив neighbors
    lines.forEach(l => l.stations?.forEach(s => { if (!s.neighbors) s.neighbors = []; }));
    return { ...section, lines };
  }, [generateNeighborsForLine]);

  const generateNeighborsForFile = useCallback((file: FileShape, defaultTime: number): FileShape => {
    const metro_map = generateNeighborsForSection(file.metro_map, defaultTime);
    const suburban_map = generateNeighborsForSection(file.suburban_map, defaultTime);
    const rivertram_map = generateNeighborsForSection(file.rivertram_map, defaultTime);
    const tram_map = generateNeighborsForSection(file.tram_map, defaultTime);
    return { ...file, metro_map, suburban_map, rivertram_map, tram_map } as FileShape;
  }, [generateNeighborsForSection]);

  const downloadJson = useCallback(() => {
    // Формируем данные для выгрузки (весь файл целиком)
    let data: FileShape = content ?? ({ info: { name: 'New Map' }, metro_map: { lines: [] } } as any);
    if (autoNeighborsOnSave) {
      data = generateNeighborsForFile(cloneDeep(data), Number(defaultNeighborTime) || 0);
    } else {
      // даже без автогенерации гарантируем наличие массивов neighbors
      (data.metro_map?.lines || []).forEach(l => l.stations?.forEach(s => { if (!s.neighbors) s.neighbors = []; }));
      (data.suburban_map?.lines || []).forEach(l => l.stations?.forEach(s => { if (!s.neighbors) s.neighbors = []; }));
      (data.rivertram_map?.lines || []).forEach(l => l.stations?.forEach(s => { if (!s.neighbors) s.neighbors = []; }));
      (data.tram_map?.lines || []).forEach(l => l.stations?.forEach(s => { if (!s.neighbors) s.neighbors = []; }));
    }
    const json = JSON.stringify(data, null, 2);
    const blob = new Blob([json], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    const fileNameBase = (data.info?.name || 'map').toString().replace(/[^\w\-]+/g, '_');
    a.download = `${fileNameBase || 'map'}.json`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  }, [content, autoNeighborsOnSave, defaultNeighborTime, generateNeighborsForFile, cloneDeep]);

  // Callbacks for neighbor operations
  const onUpdateNeighborTime = useCallback((neighborId: string, time: number) => {
    if (!content || !selectedLine || !selectedStation) return;
    let computed: any = null;
    setContent(prev => {
      pushHistory(prev);
      if (!prev) { computed = prev; return prev as any; }
      const section = (prev as any)[activeSection]; if (!section) { computed = prev as any; return prev as any; }
      const lines = section.lines.map((l: Line) => l.id !== selectedLine.id ? l : ({
        ...l,
        stations: l.stations.map(s => s.id === selectedStation.id ? {
          ...s,
          neighbors: (s.neighbors || []).map(([id, t]) => id === neighborId ? [id, time] : [id, t])
        } : s)
      } as Line));
      computed = { ...prev, [activeSection]: { ...section, lines } } as FileShape;
      return computed;
    });
    if (onChange && computed) onChange(computed);
    redrawNeighborConnections();
  }, [content, selectedLine, selectedStation, activeSection, pushHistory, onChange, redrawNeighborConnections]);

  const onRemoveNeighbor = useCallback((neighborId: string) => {
    if (!content || !selectedLine || !selectedStation) return;
    let computed: any = null;
    setContent(prev => {
      pushHistory(prev);
      if (!prev) { computed = prev; return prev as any; }
      const section = (prev as any)[activeSection]; if (!section) { computed = prev as any; return prev as any; }
      const lines = section.lines.map((l: Line) => l.id !== selectedLine.id ? l : ({
        ...l,
        stations: l.stations.map(s => s.id === selectedStation.id ? {
          ...s,
          neighbors: (s.neighbors || []).filter(([id]) => id !== neighborId)
        } : s)
      } as Line));
      computed = { ...prev, [activeSection]: { ...section, lines } } as FileShape;
      return computed;
    });
    if (onChange && computed) onChange(computed);
    redrawNeighborConnections();
  }, [content, selectedLine, selectedStation, activeSection, pushHistory, onChange, redrawNeighborConnections]);

  const onAddNeighbor = useCallback(() => {
    const pick = addNeighborModalValues;
    if (pick.length === 0) return;
    if (!content || !selectedLine || !selectedStation) return;
    let computed: any = null;
    setContent(prev => {
      pushHistory(prev);
      if (!prev) { computed = prev; return prev as any; }
      const section = (prev as any)[activeSection]; if (!section) { computed = prev as any; return prev as any; }
      const lines = section.lines.map((l: Line) => l.id !== selectedLine.id ? l : ({
        ...l,
        stations: l.stations.map(s => s.id === selectedStation.id ? {
          ...s,
          neighbors: [
            ...(s.neighbors || []),
            ...pick.map(id => [id, Number(defaultNeighborTime) || 0] as [string, number])
          ]
        } : s)
      } as Line));
      computed = { ...prev, [activeSection]: { ...section, lines } } as FileShape;
      return computed;
    });
    if (onChange && computed) onChange(computed);
    redrawNeighborConnections();
  }, [content, selectedLine, selectedStation, activeSection, pushHistory, onChange, addNeighborModalValues, defaultNeighborTime, redrawNeighborConnections]);

  const onAddNeighborClick = useCallback(() => {
    if (!selectedStation) return;
    const allStations = (metro?.lines ?? []).flatMap(l => (l.stations || []).map(s => ({ value: s.id, label: `${s.name} (${l.name})` })));
    const existingNeighborIds = new Set<string>((selectedStation.neighbors || []).map(([id]) => id));
    const availableOptions = allStations.filter(o => o.value !== selectedStation.id && !existingNeighborIds.has(o.value));
    if (availableOptions.length === 0) {
      alert('Нет доступных станций для добавления');
      return;
    }
    setAddNeighborModalOpen(true);
    setAddNeighborModalValues([]);
  }, [selectedStation, metro]);

  // Callbacks for segment operations
  const onAddSegmentPoints = useCallback(() => {
    const seg = selectedSegment; if (!seg) return;
    const map = metro; if (!map) return;
    const line = (map.lines || []).find(l => l.id === seg.lineId); if (!line) return;
    const a = line.stations.find(s => s.id === seg.aId); const b = line.stations.find(s => s.id === seg.bId);
    if (!a || !b) return;
    const aPos = getStationPosition(a); const bPos = getStationPosition(b);
    setContent(prev => {
      if (!prev) return prev as any;
      const section = (prev as any)[activeSection]; if (!section) return prev as any;
      const ips = [...(section.intermediatePoints || [])];
      let entry = ips.find((ip: any) => (ip.neighborsId[0] === a.id && ip.neighborsId[1] === b.id) || (ip.neighborsId[0] === b.id && ip.neighborsId[1] === a.id));
      if (!entry) {
        entry = { neighborsId: [a.id, b.id], points: [
          { x: aPos.x + (bPos.x - aPos.x) * 0.33, y: aPos.y + (bPos.y - aPos.y) * 0.33 },
          { x: aPos.x + (bPos.x - aPos.x) * 0.66, y: aPos.y + (bPos.y - aPos.y) * 0.66 },
        ] };
        ips.push(entry);
      }
      return { ...prev, [activeSection]: { ...section, intermediatePoints: ips } } as FileShape;
    });
    redrawLines(); redrawSegmentControls();
  }, [selectedSegment, metro, activeSection, getStationPosition, redrawLines, redrawSegmentControls]);

  const onRemoveSegmentPoints = useCallback(() => {
    const seg = selectedSegment; if (!seg) return;
    setContent(prev => {
      if (!prev) return prev as any;
      const section = (prev as any)[activeSection]; if (!section) return prev as any;
      const next = (section.intermediatePoints || []).filter((ip: any) => !(
        (ip.neighborsId[0] === seg.aId && ip.neighborsId[1] === seg.bId) ||
        (ip.neighborsId[0] === seg.bId && ip.neighborsId[1] === seg.aId)
      ));
      return { ...prev, [activeSection]: { ...section, intermediatePoints: next } } as FileShape;
    });
    redrawLines(); redrawSegmentControls();
  }, [selectedSegment, activeSection, redrawLines, redrawSegmentControls]);

  // Callbacks for zoom operations
  const onZoomIn = useCallback(() => {
    const c = canvasDomRef.current; const root = rootContainerRef.current; if (!c || !root) return;
    const rect = c.getBoundingClientRect(); const cx = rect.left + rect.width / 2; const cy = rect.top + rect.height / 2;
    const before = root.toLocal(new PIXI.Point(cx - rect.left, cy - rect.top));
    const newScale = Math.min(5, root.scale.x * 1.1); 
    root.scale.set(newScale);
    setZoomLevel(newScale);
    const after = root.toLocal(new PIXI.Point(cx - rect.left, cy - rect.top));
    root.position.x += (after.x - before.x) * newScale; root.position.y += (after.y - before.y) * newScale; 
    redrawGrid(); persistEditorSettings();
  }, [redrawGrid]);

  const onZoomOut = useCallback(() => {
    const c = canvasDomRef.current; const root = rootContainerRef.current; if (!c || !root) return;
    const rect = c.getBoundingClientRect(); const cx = rect.left + rect.width / 2; const cy = rect.top + rect.height / 2;
    const before = root.toLocal(new PIXI.Point(cx - rect.left, cy - rect.top));
    const newScale = Math.max(0.2, root.scale.x / 1.1); 
    root.scale.set(newScale);
    setZoomLevel(newScale);
    const after = root.toLocal(new PIXI.Point(cx - rect.left, cy - rect.top));
    root.position.x += (after.x - before.x) * newScale; root.position.y += (after.y - before.y) * newScale; 
    redrawGrid(); persistEditorSettings();
  }, [redrawGrid]);

  const onZoomChange = useCallback((value: number) => {
    const root = rootContainerRef.current;
    const canvas = canvasDomRef.current;
    if (!root || !canvas) return;
    const rect = canvas.getBoundingClientRect();
    const cx = rect.left + rect.width / 2;
    const cy = rect.top + rect.height / 2;
    const before = root.toLocal(new PIXI.Point(cx - rect.left, cy - rect.top));
    root.scale.set(value);
    setZoomLevel(value);
    const after = root.toLocal(new PIXI.Point(cx - rect.left, cy - rect.top));
    root.position.x += (after.x - before.x) * value;
    root.position.y += (after.y - before.y) * value;
    redrawGrid();
    persistEditorSettings();
  }, [redrawGrid]);

  const onFitToContent = useCallback(() => {
    fitToContent();
    persistEditorSettings();
  }, [fitToContent]);

  // Computed values
  const selectedTransfer = useMemo(() => metro?.transfers?.find(t => t.id === selectedTransferId) || null, [metro, selectedTransferId]);
  const allStationsForSelect = useMemo(() => (metro?.lines ?? []).flatMap(l => (l.stations || []).map(s => ({ value: s.id, label: `${s.name} (${l.name})` }))), [metro]);
  const availableNeighborOptions = useMemo(() => {
    if (!selectedStation) return [];
    const allStations = (metro?.lines ?? []).flatMap(l => (l.stations || []).map(s => ({ value: s.id, label: `${s.name} (${l.name})` })));
    const existingNeighborIds = new Set<string>((selectedStation.neighbors || []).map(([id]) => id));
    return allStations.filter(o => o.value !== selectedStation.id && !existingNeighborIds.has(o.value));
  }, [selectedStation, metro]);

  const stationCount = useMemo(() => metro?.lines?.reduce((acc, l) => acc + (l.stations?.length || 0), 0) || 0, [metro]);
  const allStations = useMemo(() => (metro?.lines ?? []).flatMap(l => l.stations || []), [metro]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}>
      <EditorTopBar
        projectName={projectName}
        saving={saving}
        loading={loading}
        hasApiKey={hasApiKey}
        activeSection={activeSection}
        onSectionChange={setActiveSection}
        onAddLine={addLine}
        onAddStation={addStation}
        onDownloadJson={downloadJson}
        onZoomIn={onZoomIn}
        onZoomOut={onZoomOut}
        onFitToContent={onFitToContent}
        zoomLevel={zoomLevel}
        onZoomChange={onZoomChange}
        showGrid={showGrid}
        onToggleGrid={() => setShowGrid(!showGrid)}
        showLines={showLines}
        onToggleLines={() => setShowLines(!showLines)}
        showTransfers={showTransfers}
        onToggleTransfers={() => setShowTransfers(!showTransfers)}
        showStations={showStations}
        onToggleStations={() => setShowStations(!showStations)}
        showLabels={showLabels}
        onToggleLabels={() => setShowLabels(!showLabels)}
        stationCount={stationCount}
      />
      <div style={{ display: 'grid', gridTemplateColumns: '250px 1fr 300px', gap: 12, flex: 1, overflow: 'hidden', minHeight: 0 }}>
        <EditorLeftSidebar
          leftTab={leftTab}
          onTabChange={setLeftTab}
          lines={metro?.lines}
          stations={allStations}
          transfers={metro?.transfers}
          selectedLineId={selectedLineId}
          selectedStationId={selectedStationId}
          selectedTransferId={selectedTransferId}
          onSelectLine={(id) => { setSelectedLineId(id); setSelectedStationId(null); setSelectedTransferId(null); setSelectedSegment(null); }}
          onSelectStation={(lineId, stationId) => { setSelectedLineId(lineId); setSelectedStationId(stationId); setSelectedTransferId(null); setSelectedSegment(null); }}
          onSelectTransfer={(id) => { setSelectedTransferId(id); setSelectedLineId(null); setSelectedStationId(null); setSelectedSegment(null); setEditingTransferTime(metro?.transfers?.find(t => t.id === id)?.time || 0); setEditingTransferType(metro?.transfers?.find(t => t.id === id)?.type || 'default'); }}
          onAddLine={addLine}
          onAddStation={addStation}
          onAddTransfer={addTransfer}
          findStationById={findStationById}
          stationFilter={stationFilter}
          onStationFilterChange={setStationFilter}
        />
        <EditorCanvas
          pixiContainerRef={pixiContainerRef}
          appRef={appRef}
          rootContainerRef={rootContainerRef}
          canvasDomRef={canvasDomRef}
          zoomLevel={zoomLevel}
          coordinateScale={coordinateScale}
          hasMapData={!!metro && (metro.lines?.length ?? 0) > 0}
        />
        <EditorRightSidebar
          rightTab={rightTab}
          onTabChange={setRightTab}
          selectedLine={selectedLine}
          selectedStation={selectedStation}
          selectedTransferId={selectedTransferId}
          selectedTransfer={selectedTransfer}
          selectedSegment={selectedSegment}
          onUpdateLine={updateSelectedLine}
          onUpdateStationName={updateSelectedStationName}
          onUpdateStationCoord={updateSelectedStationCoord}
          onUpdateNeighborTime={onUpdateNeighborTime}
          onRemoveNeighbor={onRemoveNeighbor}
          onAddNeighbor={onAddNeighbor}
          editingTransferTime={editingTransferTime}
          editingTransferType={editingTransferType}
          onUpdateTransferMeta={updateSelectedTransferMeta}
          onAddSegmentPoints={onAddSegmentPoints}
          onRemoveSegmentPoints={onRemoveSegmentPoints}
          defaultNeighborTime={defaultNeighborTime}
          onDefaultNeighborTimeChange={setDefaultNeighborTime}
          snapToGrid={snapToGrid}
          onSnapToGridChange={setSnapToGrid}
          snapStep={snapStep}
          onSnapStepChange={(v) => setSnapStep(Number(v) || 10)}
          autoNeighborsOnSave={autoNeighborsOnSave}
          onAutoNeighborsOnSaveChange={setAutoNeighborsOnSave}
          coordinateScale={coordinateScale}
          onCoordinateScaleChange={(v) => {
            if (v <= 0 || v === coordinateScale) return;
            setCoordinateScale(v);
            redrawStations();
            redrawLines();
            redrawTransfers();
            redrawNeighborConnections();
            redrawSegmentControls();
            redrawRivers();
          }}
          bgUrl={bgUrl}
          bgX={bgX}
          bgY={bgY}
          bgW={bgW}
          bgH={bgH}
          bgAlpha={bgAlpha}
          onBgUrlChange={setBgUrl}
          onBgXChange={(v) => setBgX(Number(v) || 0)}
          onBgYChange={(v) => setBgY(Number(v) || 0)}
          onBgWChange={(v) => setBgW(Number(v) || 0)}
          onBgHChange={(v) => setBgH(Number(v) || 0)}
          onBgAlphaChange={(v) => setBgAlpha(Number(v) || 0)}
          onBgFileChange={handleBgFile}
          onClearBg={() => setBgUrl('')}
          findStationById={findStationById}
          onAddStationsToTransfer={addStationsToTransfer}
          onRemoveStationFromTransfer={removeStationFromTransfer}
          addStationsModalOpen={addStationsModalOpen}
          addStationsModalValues={addStationsModalValues}
          onSetAddStationsModalOpen={(id, open) => setAddStationsModalOpen(prev => ({ ...prev, [id]: open }))}
          onSetAddStationsModalValues={(id, values) => setAddStationsModalValues(prev => ({ ...prev, [id]: values }))}
          allStationsForSelect={allStationsForSelect}
          onRemoveTransfer={removeTransfer}
          onAddNeighborClick={onAddNeighborClick}
          addNeighborModalOpen={addNeighborModalOpen}
          addNeighborModalValues={addNeighborModalValues}
          onSetAddNeighborModalOpen={setAddNeighborModalOpen}
          onSetAddNeighborModalValues={setAddNeighborModalValues}
          availableNeighborOptions={availableNeighborOptions}
        />
      </div>
      {error && (
        <div style={{ padding: '8px', backgroundColor: '#fee', color: '#c00', marginTop: '8px' }}>
          Ошибка: {error}
        </div>
      )}
    </div>
  );
}
