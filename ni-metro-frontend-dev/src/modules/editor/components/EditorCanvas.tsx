import { useEffect, useRef } from 'react';
import { Paper, Group, Text, Badge } from '@mantine/core';
import * as PIXI from 'pixi.js';

interface EditorCanvasProps {
  pixiContainerRef: React.RefObject<HTMLDivElement>;
  appRef: React.RefObject<PIXI.Application | null>;
  rootContainerRef: React.RefObject<PIXI.Container | null>;
  canvasDomRef: React.RefObject<HTMLCanvasElement | null>;
  zoomLevel: number;
  coordinateScale: number;
  hasMapData: boolean;
  onCanvasReady?: () => void;
}

export function EditorCanvas({
  pixiContainerRef,
  appRef,
  rootContainerRef,
  canvasDomRef,
  zoomLevel,
  coordinateScale,
  hasMapData,
  onCanvasReady
}: EditorCanvasProps) {
  const infoRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (appRef.current?.canvas && canvasDomRef) {
      (canvasDomRef as any).current = appRef.current.canvas;
      if (onCanvasReady) {
        onCanvasReady();
      }
    }
  }, [appRef.current?.canvas, canvasDomRef, onCanvasReady]);

  return (
    <Paper
      withBorder
      radius="md"
      style={{
        position: 'relative',
        height: '100%',
        minWidth: 0,
        overflow: 'hidden',
        background: 'var(--mantine-color-dark-7)'
      }}
    >
      <div
        ref={pixiContainerRef}
        style={{
          width: '100%',
          height: '100%',
          minWidth: 0,
          overflow: 'hidden'
        }}
      />
      
      {!hasMapData && (
        <div
          style={{
            position: 'absolute',
            inset: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'var(--mantine-color-dimmed)'
          }}
        >
          <Text c="dimmed">Нет данных карты. Укажите путь и загрузите.</Text>
        </div>
      )}

      {/* Zoom/Scale indicator */}
      <div
        ref={infoRef}
        style={{
          position: 'absolute',
          bottom: 8,
          left: 8,
          pointerEvents: 'none'
        }}
      >
        <Group gap="xs">
          <Badge size="sm" variant="filled" color="dark">
            Зум: {Math.round(zoomLevel * 100)}%
          </Badge>
          <Badge size="sm" variant="filled" color="dark">
            Координаты: {Math.round(coordinateScale * 100)}%
          </Badge>
        </Group>
      </div>
    </Paper>
  );
}

