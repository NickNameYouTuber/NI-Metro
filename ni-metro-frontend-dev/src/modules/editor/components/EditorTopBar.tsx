import { Button, Group, SegmentedControl, TextInput, Badge, Tooltip, ActionIcon, Slider, Text } from '@mantine/core';
import { 
  IconPlus, 
  IconDownload, 
  IconEye, 
  IconEyeOff, 
  IconZoomIn, 
  IconZoomOut, 
  IconArrowsMaximize,
  IconSettings,
  IconCheck
} from '@tabler/icons-react';

interface EditorTopBarProps {
  projectName: string;
  saving: boolean;
  loading: boolean;
  hasApiKey: boolean;
  activeSection: 'metro_map' | 'suburban_map' | 'rivertram_map' | 'tram_map';
  onSectionChange: (section: 'metro_map' | 'suburban_map' | 'rivertram_map' | 'tram_map') => void;
  onAddLine: () => void;
  onAddStation: () => void;
  onDownloadJson: () => void;
  onZoomIn: () => void;
  onZoomOut: () => void;
  onFitToContent: () => void;
  zoomLevel: number;
  onZoomChange: (value: number) => void;
  showGrid: boolean;
  onToggleGrid: () => void;
  showLines: boolean;
  onToggleLines: () => void;
  showTransfers: boolean;
  onToggleTransfers: () => void;
  showStations: boolean;
  onToggleStations: () => void;
  showLabels: boolean;
  onToggleLabels: () => void;
  stationCount: number;
}

export function EditorTopBar({
  projectName,
  saving,
  loading,
  hasApiKey,
  activeSection,
  onSectionChange,
  onAddLine,
  onAddStation,
  onDownloadJson,
  onZoomIn,
  onZoomOut,
  onFitToContent,
  zoomLevel,
  onZoomChange,
  showGrid,
  onToggleGrid,
  showLines,
  onToggleLines,
  showTransfers,
  onToggleTransfers,
  showStations,
  onToggleStations,
  showLabels,
  onToggleLabels,
  stationCount
}: EditorTopBarProps) {
  const sectionOptions = [
    { value: 'metro_map', label: 'Метро' },
    { value: 'suburban_map', label: 'Пригород' },
    { value: 'rivertram_map', label: 'Речной трамвай' },
    { value: 'tram_map', label: 'Трамвай' },
  ];

  return (
    <Group justify="space-between" align="center" px="md" py="sm" style={{ borderBottom: '1px solid var(--mantine-color-dark-4)' }}>
      {/* Left section */}
      <Group gap="sm">
        <Text fw={600} size="md">{projectName}</Text>
        {loading ? (
          <Badge color="blue" size="sm">Загрузка…</Badge>
        ) : saving ? (
          <Badge color="yellow" size="sm">Сохранение…</Badge>
        ) : hasApiKey ? (
          <Badge color="green" leftSection={<IconCheck size={12} />} size="sm">Сохранено</Badge>
        ) : (
          <Badge color="orange" size="sm">Только чтение</Badge>
        )}
        <SegmentedControl
          size="xs"
          value={activeSection}
          onChange={(value) => onSectionChange(value as typeof activeSection)}
          data={sectionOptions}
        />
      </Group>

      {/* Center section */}
      <Group gap="xs">
        <Tooltip label="Добавить линию">
          <Button size="xs" leftSection={<IconPlus size={16} />} onClick={onAddLine}>
            Линия
          </Button>
        </Tooltip>
        <Tooltip label="Добавить станцию">
          <Button size="xs" leftSection={<IconPlus size={16} />} onClick={onAddStation}>
            Станция
          </Button>
        </Tooltip>
        <Tooltip label="Сохранить JSON">
          <Button size="xs" leftSection={<IconDownload size={16} />} onClick={onDownloadJson} variant="light">
            Сохранить
          </Button>
        </Tooltip>
      </Group>

      {/* Right section */}
      <Group gap="xs">
        {/* Visibility toggles */}
        <Tooltip label={showGrid ? 'Скрыть сетку' : 'Показать сетку'}>
          <ActionIcon 
            variant={showGrid ? 'filled' : 'subtle'} 
            onClick={onToggleGrid}
            size="sm"
          >
            {showGrid ? <IconEye size={16} /> : <IconEyeOff size={16} />}
          </ActionIcon>
        </Tooltip>
        <Tooltip label={showLines ? 'Скрыть линии' : 'Показать линии'}>
          <ActionIcon 
            variant={showLines ? 'filled' : 'subtle'} 
            onClick={onToggleLines}
            size="sm"
          >
            {showLines ? <IconEye size={16} /> : <IconEyeOff size={16} />}
          </ActionIcon>
        </Tooltip>
        <Tooltip label={showTransfers ? 'Скрыть переходы' : 'Показать переходы'}>
          <ActionIcon 
            variant={showTransfers ? 'filled' : 'subtle'} 
            onClick={onToggleTransfers}
            size="sm"
          >
            {showTransfers ? <IconEye size={16} /> : <IconEyeOff size={16} />}
          </ActionIcon>
        </Tooltip>
        <Tooltip label={showStations ? 'Скрыть станции' : 'Показать станции'}>
          <ActionIcon 
            variant={showStations ? 'filled' : 'subtle'} 
            onClick={onToggleStations}
            size="sm"
          >
            {showStations ? <IconEye size={16} /> : <IconEyeOff size={16} />}
          </ActionIcon>
        </Tooltip>
        <Tooltip label={showLabels ? 'Скрыть подписи' : 'Показать подписи'}>
          <ActionIcon 
            variant={showLabels ? 'filled' : 'subtle'} 
            onClick={onToggleLabels}
            size="sm"
          >
            {showLabels ? <IconEye size={16} /> : <IconEyeOff size={16} />}
          </ActionIcon>
        </Tooltip>

        {/* Zoom controls */}
        <Tooltip label="Уменьшить">
          <ActionIcon onClick={onZoomOut} size="sm" variant="subtle">
            <IconZoomOut size={16} />
          </ActionIcon>
        </Tooltip>
        <Group gap={4} style={{ alignItems: 'center', minWidth: 120 }}>
          <Text size="xs" style={{ minWidth: 50 }}>Зум:</Text>
          <Slider
            value={zoomLevel}
            onChange={onZoomChange}
            min={0.1}
            max={5}
            step={0.1}
            style={{ width: 60, flex: 1 }}
            size="xs"
          />
          <Text size="xs" style={{ minWidth: 40 }}>{Math.round(zoomLevel * 100)}%</Text>
        </Group>
        <Tooltip label="Увеличить">
          <ActionIcon onClick={onZoomIn} size="sm" variant="subtle">
            <IconZoomIn size={16} />
          </ActionIcon>
        </Tooltip>
        <Tooltip label="Вписать в размер">
          <ActionIcon onClick={onFitToContent} size="sm" variant="subtle">
            <IconArrowsMaximize size={16} />
          </ActionIcon>
        </Tooltip>

        {/* Stats */}
        <Badge color="green" size="sm">Станций: {stationCount}</Badge>
      </Group>
    </Group>
  );
}

