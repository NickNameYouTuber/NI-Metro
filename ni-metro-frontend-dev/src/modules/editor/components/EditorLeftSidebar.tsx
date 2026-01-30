import { useState, useMemo } from 'react';
import { Paper, Stack, TextInput, SegmentedControl, ScrollArea, Group, Text, Button, ActionIcon } from '@mantine/core';
import { IconPlus, IconSearch, IconX } from '@tabler/icons-react';

type Station = {
  id: string;
  name: string;
  x: number;
  y: number;
};

type Line = {
  id: string;
  name: string;
  color: string;
  stations: Station[];
};

type Transfer = { id: string; stations: string[]; time: number; type?: string };

interface EditorLeftSidebarProps {
  leftTab: 'lines' | 'stations' | 'transfers';
  onTabChange: (tab: 'lines' | 'stations' | 'transfers') => void;
  lines: Line[] | undefined;
  stations: Station[];
  transfers: Transfer[] | undefined;
  selectedLineId: string | null;
  selectedStationId: string | null;
  selectedTransferId: string | null;
  onSelectLine: (lineId: string) => void;
  onSelectStation: (lineId: string, stationId: string) => void;
  onSelectTransfer: (transferId: string) => void;
  onAddLine: () => void;
  onAddStation: () => void;
  onAddTransfer: () => void;
  findStationById: (id: string) => Station | null;
  stationFilter: string;
  onStationFilterChange: (value: string) => void;
}

export function EditorLeftSidebar({
  leftTab,
  onTabChange,
  lines,
  stations,
  transfers,
  selectedLineId,
  selectedStationId,
  selectedTransferId,
  onSelectLine,
  onSelectStation,
  onSelectTransfer,
  onAddLine,
  onAddStation,
  onAddTransfer,
  findStationById,
  stationFilter,
  onStationFilterChange
}: EditorLeftSidebarProps) {
  const filteredStations = useMemo(() => {
    if (!stationFilter) return stations;
    const lower = stationFilter.toLowerCase();
    return stations.filter(s => s.name.toLowerCase().includes(lower));
  }, [stations, stationFilter]);

  const allStationsWithLines = useMemo(() => {
    if (!lines) return [];
    return lines.flatMap(line => 
      (line.stations || []).map(station => ({ ...station, line }))
    );
  }, [lines]);

  const filteredStationsWithLines = useMemo(() => {
    if (!stationFilter) return allStationsWithLines;
    const lower = stationFilter.toLowerCase();
    return allStationsWithLines.filter(s => s.name.toLowerCase().includes(lower));
  }, [allStationsWithLines, stationFilter]);

  return (
    <Paper withBorder radius="md" style={{ height: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden' }} p="sm">
      <Stack gap="sm" style={{ height: '100%', overflow: 'hidden' }}>
        {/* Search */}
        {leftTab === 'stations' && (
          <TextInput
            placeholder="Поиск станций"
            value={stationFilter}
            onChange={(e) => onStationFilterChange(e.currentTarget.value)}
            leftSection={<IconSearch size={16} />}
            rightSection={stationFilter ? (
              <ActionIcon size="xs" onClick={() => onStationFilterChange('')}>
                <IconX size={14} />
              </ActionIcon>
            ) : null}
            size="xs"
          />
        )}

        {/* Tabs */}
        <SegmentedControl
          value={leftTab}
          onChange={(value) => onTabChange(value as typeof leftTab)}
          data={[
            { value: 'lines', label: 'Линии' },
            { value: 'stations', label: 'Станции' },
            { value: 'transfers', label: 'Переходы' },
          ]}
          fullWidth
          size="xs"
        />

        {/* Content */}
        <ScrollArea style={{ flex: 1 }}>
          <Stack gap="xs">
            {leftTab === 'lines' && (
              <>
                <Button size="xs" leftSection={<IconPlus size={14} />} onClick={onAddLine} fullWidth>
                  Добавить линию
                </Button>
                {(lines || []).map(line => (
                  <Paper
                    key={line.id}
                    withBorder
                    radius="sm"
                    p={6}
                    style={{
                      background: line.id === selectedLineId ? 'var(--mantine-color-blue-9)' : 'transparent',
                      cursor: 'pointer'
                    }}
                    onClick={() => onSelectLine(line.id)}
                  >
                    <Group justify="space-between" align="center" gap="xs">
                      <Text size="sm" fw={line.id === selectedLineId ? 600 : 400}>
                        {line.name}
                      </Text>
                      <div
                        style={{
                          width: 14,
                          height: 14,
                          background: line.color,
                          borderRadius: 3,
                          flexShrink: 0
                        }}
                      />
                    </Group>
                    {line.id === selectedLineId && (
                      <Text size="xs" c="dimmed" mt={4}>
                        Станций: {line.stations?.length || 0}
                      </Text>
                    )}
                  </Paper>
                ))}
                {(!lines || lines.length === 0) && (
                  <Text size="xs" c="dimmed" ta="center" py="md">
                    Нет линий
                  </Text>
                )}
              </>
            )}

            {leftTab === 'stations' && (
              <>
                {filteredStationsWithLines.map(({ id, name, x, y, line }) => (
                  <Paper
                    key={`${line.id}:${id}`}
                    withBorder
                    radius="sm"
                    p={6}
                    style={{
                      background: id === selectedStationId ? 'var(--mantine-color-blue-9)' : 'transparent',
                      cursor: 'pointer'
                    }}
                    onClick={() => onSelectStation(line.id, id)}
                  >
                    <Group justify="space-between" align="center" gap="xs">
                      <Text size="sm" fw={id === selectedStationId ? 600 : 400}>
                        {name}
                      </Text>
                      <div
                        style={{
                          width: 12,
                          height: 12,
                          background: line.color,
                          borderRadius: '50%',
                          flexShrink: 0
                        }}
                      />
                    </Group>
                    <Text size="xs" c="dimmed" mt={4}>
                      ({x}, {y}) • {line.name}
                    </Text>
                  </Paper>
                ))}
                {filteredStationsWithLines.length === 0 && (
                  <Text size="xs" c="dimmed" ta="center" py="md">
                    {stationFilter ? 'Не найдено' : 'Нет станций'}
                  </Text>
                )}
              </>
            )}

            {leftTab === 'transfers' && (
              <>
                <Button size="xs" leftSection={<IconPlus size={14} />} onClick={onAddTransfer} fullWidth>
                  Добавить переход
                </Button>
                {(transfers || []).map(transfer => (
                  <Paper
                    key={transfer.id}
                    withBorder
                    radius="sm"
                    p={6}
                    style={{
                      background: transfer.id === selectedTransferId ? 'var(--mantine-color-blue-9)' : 'transparent',
                      cursor: 'pointer'
                    }}
                    onClick={() => onSelectTransfer(transfer.id)}
                  >
                    <Group justify="space-between" align="center" gap="xs">
                      <Text size="sm" fw={transfer.id === selectedTransferId ? 600 : 400}>
                        {transfer.id}
                      </Text>
                    </Group>
                    <Text size="xs" c="dimmed" mt={4}>
                      Станций: {transfer.stations?.length || 0} • {transfer.time}с
                    </Text>
                  </Paper>
                ))}
                {(!transfers || transfers.length === 0) && (
                  <Text size="xs" c="dimmed" ta="center" py="md">
                    Нет переходов
                  </Text>
                )}
              </>
            )}
          </Stack>
        </ScrollArea>
      </Stack>
    </Paper>
  );
}

