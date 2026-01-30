import { useState } from 'react';
import { Paper, Stack, SegmentedControl, ScrollArea, Divider, TextInput, NumberInput, Select, Group, Button, Text, FileInput, Collapse, ActionIcon, Modal, MultiSelect, Checkbox } from '@mantine/core';
import { IconChevronDown, IconChevronUp, IconTrash, IconPlus, IconX } from '@tabler/icons-react';

type Station = {
  id: string;
  name: string;
  x: number;
  y: number;
  neighbors?: [string, number][];
};

type Line = {
  id: string;
  name: string;
  color: string;
  stations: Station[];
};

type Transfer = { id: string; stations: string[]; time: number; type?: string };

interface EditorRightSidebarProps {
  rightTab: 'properties' | 'settings' | 'background';
  onTabChange: (tab: 'properties' | 'settings' | 'background') => void;
  selectedLine: Line | null;
  selectedStation: Station | null;
  selectedTransferId: string | null;
  selectedTransfer: Transfer | null;
  selectedSegment: { lineId: string; aId: string; bId: string } | null;
  onUpdateLine: (updates: { name?: string; color?: string }) => void;
  onUpdateStationName: (name: string) => void;
  onUpdateStationCoord: (coord: 'x' | 'y', value: number | string) => void;
  onUpdateNeighborTime: (neighborId: string, time: number) => void;
  onRemoveNeighbor: (neighborId: string) => void;
  onAddNeighbor: () => void;
  editingTransferTime: number | '';
  editingTransferType: string;
  onUpdateTransferMeta: (time: number, type: string) => void;
  onAddSegmentPoints: () => void;
  onRemoveSegmentPoints: () => void;
  defaultNeighborTime: number;
  onDefaultNeighborTimeChange: (value: number) => void;
  snapToGrid: boolean;
  onSnapToGridChange: (value: boolean) => void;
  snapStep: number;
  onSnapStepChange: (value: number | string) => void;
  autoNeighborsOnSave: boolean;
  onAutoNeighborsOnSaveChange: (value: boolean) => void;
  coordinateScale: number;
  onCoordinateScaleChange: (value: number) => void;
  bgUrl: string;
  bgX: number;
  bgY: number;
  bgW: number;
  bgH: number;
  bgAlpha: number;
  onBgUrlChange: (value: string) => void;
  onBgXChange: (value: number | string) => void;
  onBgYChange: (value: number | string) => void;
  onBgWChange: (value: number | string) => void;
  onBgHChange: (value: number | string) => void;
  onBgAlphaChange: (value: number | string) => void;
  onBgFileChange: (file: File | null) => void;
  onClearBg: () => void;
  findStationById: (id: string) => Station | null;
  onAddStationsToTransfer: (transferId: string, stationIds: string[]) => void;
  onRemoveStationFromTransfer: (transferId: string, stationId: string) => void;
  addStationsModalOpen: Record<string, boolean>;
  addStationsModalValues: Record<string, string[]>;
  onSetAddStationsModalOpen: (id: string, open: boolean) => void;
  onSetAddStationsModalValues: (id: string, values: string[]) => void;
  allStationsForSelect: Array<{ value: string; label: string }>;
  onRemoveTransfer: (transferId: string) => void;
  onAddNeighborClick: () => void;
  addNeighborModalOpen: boolean;
  addNeighborModalValues: string[];
  onSetAddNeighborModalOpen: (open: boolean) => void;
  onSetAddNeighborModalValues: (values: string[]) => void;
  availableNeighborOptions: Array<{ value: string; label: string }>;
}

export function EditorRightSidebar({
  rightTab,
  onTabChange,
  selectedLine,
  selectedStation,
  selectedTransferId,
  selectedTransfer,
  selectedSegment,
  onUpdateLine,
  onUpdateStationName,
  onUpdateStationCoord,
  onUpdateNeighborTime,
  onRemoveNeighbor,
  onAddNeighbor,
  editingTransferTime,
  editingTransferType,
  onUpdateTransferMeta,
  onAddSegmentPoints,
  onRemoveSegmentPoints,
  defaultNeighborTime,
  onDefaultNeighborTimeChange,
  snapToGrid,
  onSnapToGridChange,
  snapStep,
  onSnapStepChange,
  autoNeighborsOnSave,
  onAutoNeighborsOnSaveChange,
  coordinateScale,
  onCoordinateScaleChange,
  bgUrl,
  bgX,
  bgY,
  bgW,
  bgH,
  bgAlpha,
  onBgUrlChange,
  onBgXChange,
  onBgYChange,
  onBgWChange,
  onBgHChange,
  onBgAlphaChange,
  onBgFileChange,
  onClearBg,
  findStationById,
  onAddStationsToTransfer,
  onRemoveStationFromTransfer,
  addStationsModalOpen,
  addStationsModalValues,
  onSetAddStationsModalOpen,
  onSetAddStationsModalValues,
  allStationsForSelect,
  onRemoveTransfer,
  onAddNeighborClick,
  addNeighborModalOpen,
  addNeighborModalValues,
  onSetAddNeighborModalOpen,
  onSetAddNeighborModalValues,
  availableNeighborOptions
}: EditorRightSidebarProps) {
  const [linePropertiesOpen, setLinePropertiesOpen] = useState(true);
  const [stationPropertiesOpen, setStationPropertiesOpen] = useState(true);
  const [neighborsOpen, setNeighborsOpen] = useState(true);
  const [transferPropertiesOpen, setTransferPropertiesOpen] = useState(true);
  const [segmentPropertiesOpen, setSegmentPropertiesOpen] = useState(true);

  return (
    <Paper withBorder radius="md" style={{ height: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden' }} p="sm">
      <Stack gap="sm" style={{ height: '100%', overflow: 'hidden' }}>
        {/* Tabs */}
        <SegmentedControl
          value={rightTab}
          onChange={(value) => onTabChange(value as typeof rightTab)}
          data={[
            { value: 'properties', label: 'Свойства' },
            { value: 'settings', label: 'Настройки' },
            { value: 'background', label: 'Фон' },
          ]}
          fullWidth
          size="xs"
        />

        {/* Content */}
        <ScrollArea style={{ flex: 1 }}>
          <Stack gap="md">
            {rightTab === 'properties' && (
              <>
                {/* Line Properties */}
                {selectedLine && (
                  <div>
                    <Group justify="space-between" align="center" mb="xs">
                      <Text size="sm" fw={600}>Свойства линии</Text>
                      <ActionIcon
                        size="sm"
                        variant="subtle"
                        onClick={() => setLinePropertiesOpen(!linePropertiesOpen)}
                      >
                        {linePropertiesOpen ? <IconChevronUp size={14} /> : <IconChevronDown size={14} />}
                      </ActionIcon>
                    </Group>
                    <Collapse in={linePropertiesOpen}>
                      <Stack gap="xs">
                        <TextInput
                          label="Название линии"
                          value={selectedLine.name}
                          onChange={(e) => onUpdateLine({ name: e.currentTarget.value })}
                          size="xs"
                        />
                        <TextInput
                          label="Цвет (hex)"
                          value={selectedLine.color}
                          onChange={(e) => onUpdateLine({ color: e.currentTarget.value })}
                          size="xs"
                        />
                      </Stack>
                    </Collapse>
                  </div>
                )}

                <Divider />

                {/* Station Properties */}
                {selectedStation ? (
                  <div>
                    <Group justify="space-between" align="center" mb="xs">
                      <Text size="sm" fw={600}>Свойства станции</Text>
                      <ActionIcon
                        size="sm"
                        variant="subtle"
                        onClick={() => setStationPropertiesOpen(!stationPropertiesOpen)}
                      >
                        {stationPropertiesOpen ? <IconChevronUp size={14} /> : <IconChevronDown size={14} />}
                      </ActionIcon>
                    </Group>
                    <Collapse in={stationPropertiesOpen}>
                      <Stack gap="xs">
                        <TextInput
                          label="Название"
                          value={selectedStation.name}
                          onChange={(e) => onUpdateStationName(e.currentTarget.value)}
                          size="xs"
                        />
                        <Group gap="xs" grow>
                          <NumberInput
                            label="X"
                            value={selectedStation.x}
                            onChange={(v) => onUpdateStationCoord('x', v as any)}
                            size="xs"
                          />
                          <NumberInput
                            label="Y"
                            value={selectedStation.y}
                            onChange={(v) => onUpdateStationCoord('y', v as any)}
                            size="xs"
                          />
                        </Group>

                        <Divider label="Смежные станции" labelPosition="left" my="xs" />
                        <Collapse in={neighborsOpen}>
                          <Group justify="space-between" align="center" mb="xs">
                            <Text size="xs" c="dimmed">Смежные станции</Text>
                            <ActionIcon
                              size="xs"
                              variant="subtle"
                              onClick={() => setNeighborsOpen(!neighborsOpen)}
                            >
                              {neighborsOpen ? <IconChevronUp size={12} /> : <IconChevronDown size={12} />}
                            </ActionIcon>
                          </Group>
                          <Stack gap="xs">
                            {(selectedStation.neighbors || []).map(([neighborId, time]) => {
                              const neighborStation = findStationById(neighborId);
                              return (
                                <Group key={neighborId} justify="space-between" align="center" gap="xs">
                                  <Text size="xs" style={{ flex: 1 }}>
                                    {neighborStation?.name || neighborId}
                                  </Text>
                                  <NumberInput
                                    size="xs"
                                    w={70}
                                    value={time}
                                    onChange={(v) => onUpdateNeighborTime(neighborId, Number(v) || 0)}
                                  />
                                  <ActionIcon
                                    size="xs"
                                    color="red"
                                    variant="light"
                                    onClick={() => onRemoveNeighbor(neighborId)}
                                  >
                                    <IconTrash size={12} />
                                  </ActionIcon>
                                </Group>
                              );
                            })}
                            {(selectedStation.neighbors || []).length === 0 && (
                              <Text size="xs" c="dimmed">Нет смежных станций</Text>
                            )}
                            <Button size="xs" variant="light" leftSection={<IconPlus size={12} />} onClick={onAddNeighborClick}>
                              Добавить смежную станцию
                            </Button>
                            <Modal
                              opened={addNeighborModalOpen}
                              onClose={() => {
                                onSetAddNeighborModalOpen(false);
                                onSetAddNeighborModalValues([]);
                              }}
                              title="Добавить смежную станцию"
                              centered
                            >
                              <Stack gap="xs">
                                <MultiSelect
                                  searchable
                                  nothingFoundMessage="Ничего не найдено"
                                  placeholder="Выберите станции"
                                  data={availableNeighborOptions}
                                  value={addNeighborModalValues}
                                  onChange={(val) => onSetAddNeighborModalValues(val)}
                                />
                                <NumberInput
                                  label="Время перехода (сек)"
                                  value={defaultNeighborTime}
                                  onChange={(v) => onDefaultNeighborTimeChange(Number(v) || 0)}
                                  size="xs"
                                />
                                <Group justify="end">
                                  <Button
                                    variant="light"
                                    onClick={() => {
                                      onSetAddNeighborModalOpen(false);
                                      onSetAddNeighborModalValues([]);
                                    }}
                                  >
                                    Отмена
                                  </Button>
                                  <Button
                                    onClick={() => {
                                      if (addNeighborModalValues.length === 0) return;
                                      onAddNeighbor();
                                      onSetAddNeighborModalOpen(false);
                                      onSetAddNeighborModalValues([]);
                                    }}
                                  >
                                    Добавить
                                  </Button>
                                </Group>
                              </Stack>
                            </Modal>
                          </Stack>
                        </Collapse>
                      </Stack>
                    </Collapse>
                  </div>
                ) : (
                  <Text size="xs" c="dimmed">Станция не выбрана</Text>
                )}

                <Divider />

                {/* Segment Properties */}
                {selectedSegment ? (
                  <div>
                    <Group justify="space-between" align="center" mb="xs">
                      <Text size="sm" fw={600}>Сегмент</Text>
                      <ActionIcon
                        size="sm"
                        variant="subtle"
                        onClick={() => setSegmentPropertiesOpen(!segmentPropertiesOpen)}
                      >
                        {segmentPropertiesOpen ? <IconChevronUp size={14} /> : <IconChevronDown size={14} />}
                      </ActionIcon>
                    </Group>
                    <Collapse in={segmentPropertiesOpen}>
                      <Group gap="xs">
                        <Button size="xs" variant="light" onClick={onAddSegmentPoints}>
                          Добавить точки
                        </Button>
                        <Button size="xs" color="red" variant="light" onClick={onRemoveSegmentPoints}>
                          Удалить точки
                        </Button>
                      </Group>
                    </Collapse>
                  </div>
                ) : (
                  <Text size="xs" c="dimmed">Сегмент не выбран</Text>
                )}

                <Divider />

                {/* Transfer Properties */}
                {selectedTransfer ? (
                  <div>
                    <Group justify="space-between" align="center" mb="xs">
                      <Text size="sm" fw={600}>Переход</Text>
                      <ActionIcon
                        size="sm"
                        variant="subtle"
                        onClick={() => setTransferPropertiesOpen(!transferPropertiesOpen)}
                      >
                        {transferPropertiesOpen ? <IconChevronUp size={14} /> : <IconChevronDown size={14} />}
                      </ActionIcon>
                    </Group>
                    <Collapse in={transferPropertiesOpen}>
                      <Stack gap="xs">
                        <NumberInput
                          label="Время (сек)"
                          value={editingTransferTime}
                          onChange={(v) => onUpdateTransferMeta(Number(v) || 0, editingTransferType)}
                          size="xs"
                        />
                        <Select
                          label="Тип"
                          data={[
                            { value: 'default', label: 'Обычный' },
                            { value: 'crossplatform', label: 'Кроссплатформенный' },
                            { value: 'ground', label: 'По земле' },
                            { value: 'TR_6', label: 'TR_6' },
                            { value: 'TR_7', label: 'TR_7' }
                          ]}
                          value={editingTransferType}
                          onChange={(val) => onUpdateTransferMeta(Number(editingTransferTime) || 0, val || 'default')}
                          size="xs"
                        />
                        <Button
                          size="xs"
                          onClick={() => onSetAddStationsModalOpen(selectedTransferId || '', true)}
                        >
                          Добавить станции
                        </Button>
                        <Modal
                          opened={!!addStationsModalOpen[selectedTransferId || '']}
                          onClose={() => {
                            onSetAddStationsModalOpen(selectedTransferId || '', false);
                            onSetAddStationsModalValues(selectedTransferId || '', []);
                          }}
                          title="Добавить станции в переход"
                          centered
                        >
                          {(() => {
                            const inTransfer = new Set<string>(selectedTransfer?.stations || []);
                            const options = allStationsForSelect.filter(o => !inTransfer.has(o.value));
                            const values = addStationsModalValues[selectedTransferId || ''] || [];
                            return (
                              <Stack gap="xs">
                                <MultiSelect
                                  searchable
                                  nothingFoundMessage="Ничего не найдено"
                                  placeholder="Выберите станции"
                                  data={options}
                                  value={values}
                                  onChange={(val) => onSetAddStationsModalValues(selectedTransferId || '', val)}
                                />
                                <Group justify="end">
                                  <Button
                                    variant="light"
                                    onClick={() => {
                                      onSetAddStationsModalOpen(selectedTransferId || '', false);
                                      onSetAddStationsModalValues(selectedTransferId || '', []);
                                    }}
                                  >
                                    Отмена
                                  </Button>
                                  <Button
                                    onClick={() => {
                                      const pick = addStationsModalValues[selectedTransferId || ''] || [];
                                      if (pick.length) {
                                        onAddStationsToTransfer(selectedTransferId || '', pick);
                                      }
                                      onSetAddStationsModalOpen(selectedTransferId || '', false);
                                      onSetAddStationsModalValues(selectedTransferId || '', []);
                                    }}
                                  >
                                    Добавить
                                  </Button>
                                </Group>
                              </Stack>
                            );
                          })()}
                        </Modal>
                        <Stack gap="xs">
                          {(selectedTransfer.stations || []).map(stId => {
                            const st = findStationById(stId);
                            return (
                              <Group key={stId} justify="space-between" align="center" gap="xs">
                                <Text size="xs" style={{ flex: 1 }}>{st?.name || stId}</Text>
                                <ActionIcon
                                  size="xs"
                                  variant="light"
                                  onClick={() => onRemoveStationFromTransfer(selectedTransferId || '', stId)}
                                >
                                  <IconX size={12} />
                                </ActionIcon>
                              </Group>
                            );
                          })}
                          {(selectedTransfer.stations || []).length === 0 && (
                            <Text size="xs" c="dimmed">Нет станций</Text>
                          )}
                        </Stack>
                        <Button
                          size="xs"
                          color="red"
                          variant="light"
                          leftSection={<IconTrash size={12} />}
                          onClick={() => onRemoveTransfer(selectedTransferId || '')}
                        >
                          Удалить переход
                        </Button>
                      </Stack>
                    </Collapse>
                  </div>
                ) : (
                  <Text size="xs" c="dimmed">Переход не выбран</Text>
                )}
              </>
            )}

            {rightTab === 'settings' && (
              <Stack gap="md">
                <div>
                  <Text size="sm" fw={600} mb="xs">Редактирование</Text>
                  <Stack gap="xs">
                    <Checkbox
                      label="Привязка к сетке"
                      checked={snapToGrid}
                      onChange={(e) => onSnapToGridChange(e.currentTarget.checked)}
                      size="xs"
                    />
                    <NumberInput
                      label="Шаг сетки"
                      value={snapStep}
                      onChange={(v) => onSnapStepChange(v as any)}
                      min={1}
                      max={200}
                      size="xs"
                    />
                    <NumberInput
                      label="Время соседа по умолчанию"
                      value={defaultNeighborTime}
                      onChange={(v) => onDefaultNeighborTimeChange(Number(v) || 0)}
                      min={0}
                      size="xs"
                    />
                    <Checkbox
                      label="Авто-соседи при сохранении"
                      checked={autoNeighborsOnSave}
                      onChange={(e) => onAutoNeighborsOnSaveChange(e.currentTarget.checked)}
                      size="xs"
                    />
                  </Stack>
                </div>

                <Divider />

                <div>
                  <Text size="sm" fw={600} mb="xs">Масштаб</Text>
                  <NumberInput
                    label="Масштаб координат"
                    value={coordinateScale}
                    onChange={(v) => onCoordinateScaleChange(Number(v) || 1)}
                    min={0.1}
                    max={5}
                    step={0.1}
                    size="xs"
                  />
                </div>
              </Stack>
            )}

            {rightTab === 'background' && (
              <Stack gap="xs">
                <FileInput
                  accept="image/*"
                  placeholder="Выбрать изображение"
                  onChange={onBgFileChange}
                  size="xs"
                />
                <TextInput
                  label="URL"
                  value={bgUrl}
                  onChange={(e) => onBgUrlChange(e.currentTarget.value)}
                  placeholder="https://.../map.png"
                  size="xs"
                />
                <Group gap="xs" grow>
                  <NumberInput
                    label="X"
                    value={bgX}
                    onChange={(v) => onBgXChange(v as any)}
                    size="xs"
                  />
                  <NumberInput
                    label="Y"
                    value={bgY}
                    onChange={(v) => onBgYChange(v as any)}
                    size="xs"
                  />
                </Group>
                <Group gap="xs" grow>
                  <NumberInput
                    label="Ширина"
                    value={bgW}
                    onChange={(v) => onBgWChange(v as any)}
                    size="xs"
                  />
                  <NumberInput
                    label="Высота"
                    value={bgH}
                    onChange={(v) => onBgHChange(v as any)}
                    size="xs"
                  />
                </Group>
                <NumberInput
                  label="Прозрачность"
                  min={0}
                  max={1}
                  step={0.05}
                  value={bgAlpha}
                  onChange={(v) => onBgAlphaChange(v as any)}
                  size="xs"
                />
                <Button variant="light" size="xs" onClick={onClearBg}>
                  Очистить фон
                </Button>
              </Stack>
            )}
          </Stack>
        </ScrollArea>
      </Stack>
    </Paper>
  );
}

