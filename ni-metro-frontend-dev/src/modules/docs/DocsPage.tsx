import React, { useState } from 'react';
import { Button, Group, Stack, Title, Paper, Tabs, AppShell, NavLink, ScrollArea } from '@mantine/core';
import { useNavigate, useLocation } from 'react-router-dom';
import { IconHome, IconBook, IconSettings } from '@tabler/icons-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

export function DocsPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [activeTab, setActiveTab] = useState<string | null>('api-reference');

  return (
    <AppShell
      header={{ height: 60 }}
      navbar={{ width: 200, breakpoint: 'sm' }}
      padding="md"
      style={{ height: '100vh', display: 'flex', flexDirection: 'column' }}
    >
      <AppShell.Header>
        <Group justify="space-between" align="center" h="100%" px="md">
          <Title order={3}>Документация NI-Metro</Title>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="md">
        <NavLink
          label="Проекты"
          leftSection={<IconHome size={16} />}
          onClick={() => navigate('/')}
          active={location.pathname === '/'}
        />
        <NavLink
          label="Документация"
          leftSection={<IconBook size={16} />}
          onClick={() => navigate('/docs')}
          active={location.pathname === '/docs'}
        />
        <NavLink
          label="Настройки"
          leftSection={<IconSettings size={16} />}
          onClick={() => navigate('/settings')}
          active={location.pathname === '/settings'}
        />
      </AppShell.Navbar>

      <AppShell.Main>
        <ScrollArea h="100%" offsetScrollbars>
          <div style={{ maxWidth: 1200, margin: '0 auto' }}>
            <Tabs value={activeTab} onChange={setActiveTab}>
              <Tabs.List>
                <Tabs.Tab value="api-reference">API Reference</Tabs.Tab>
                <Tabs.Tab value="map-structure">Структура карты</Tabs.Tab>
                <Tabs.Tab value="notifications">Оповещения</Tabs.Tab>
              </Tabs.List>

              <Tabs.Panel value="api-reference" pt="md">
                <Paper p="md" withBorder>
                  <ReactMarkdown remarkPlugins={[remarkGfm]}>
                    {apiReferenceContent}
                  </ReactMarkdown>
                </Paper>
              </Tabs.Panel>

              <Tabs.Panel value="map-structure" pt="md">
                <Paper p="md" withBorder>
                  <ReactMarkdown remarkPlugins={[remarkGfm]}>
                    {mapStructureContent}
                  </ReactMarkdown>
                </Paper>
              </Tabs.Panel>

              <Tabs.Panel value="notifications" pt="md">
                <Paper p="md" withBorder>
                  <ReactMarkdown remarkPlugins={[remarkGfm]}>
                    {notificationsContent}
                  </ReactMarkdown>
                </Paper>
              </Tabs.Panel>
            </Tabs>
          </div>
        </ScrollArea>
      </AppShell.Main>
    </AppShell>
  );
}

const apiReferenceContent = `
# API Reference

## Базовый URL

\`\`\`
http://localhost:8080/api/v1
\`\`\`

## Публичные Endpoints

### Получить список всех карт

\`\`\`
GET /maps
\`\`\`

**Ответ:**
\`\`\`json
[
  {
    "id": "uuid",
    "name": "Название карты",
    "country": "Россия",
    "fileName": "metromap_1",
    "createdAt": "2024-11-18T12:00:00",
    "updatedAt": "2024-11-18T12:00:00"
  }
]
\`\`\`

### Получить карту по ID

\`\`\`
GET /maps/{id}
\`\`\`

### Получить карту по имени файла

\`\`\`
GET /maps/by-name/{fileName}
\`\`\`

### Получить оповещения

\`\`\`
GET /notifications?stationId={id}&lineId={id}
\`\`\`

Параметры запроса (опционально):
- \`stationId\` - ID станции для фильтрации
- \`lineId\` - ID линии для фильтрации

## Защищенные Endpoints (требуют API ключ)

Все защищенные endpoints требуют заголовок:
\`\`\`
X-API-Key: ваш-api-ключ
\`\`\`

### Создать карту

\`\`\`
POST /maps
Content-Type: application/json

{
  "name": "Название",
  "fileName": "metromap_1",
  "data": { ... }
}
\`\`\`

### Обновить карту

\`\`\`
PUT /maps/{id}
Content-Type: application/json

{
  "name": "Обновленное название",
  "data": { ... }
}
\`\`\`

### Удалить карту

\`\`\`
DELETE /maps/{id}
\`\`\`

### Создать оповещение

\`\`\`
POST /notifications
Content-Type: application/json

{
  "id": "notification_1",
  "type": "normal",
  "triggerType": "date_range",
  "triggerDateStart": "2024-12-01",
  "triggerDateEnd": "2024-12-31",
  "contentText": "Текст оповещения"
}
\`\`\`

## Получение API ключа

API ключи генерируются администратором. Обратитесь к администратору для получения ключа.
`;

const mapStructureContent = `
# Структура данных карты

Карта метро представляется в формате JSON со следующей структурой:

\`\`\`json
{
  "info": {
    "version": "1.0",
    "author": "Nicorp",
    "country": "Россия",
    "name": "Москва",
    "icon": "https://..."
  },
  "metro_map": {
    "lines": [
      {
        "id": "1",
        "name": "Сокольническая линия",
        "color": "#E40520",
        "stations": [
          {
            "id": "METRO_100",
            "name": "Бульвар Рокоссовского",
            "x": 1659,
            "y": 1252,
            "textPosition": 3,
            "neighbors": [
              ["METRO_101", 3]
            ]
          }
        ]
      }
    ],
    "transfers": [
      {
        "id": "transfer_1",
        "stations": ["METRO_100", "METRO_200"],
        "time": 5,
        "type": "default"
      }
    ],
    "intermediatePoints": [
      {
        "neighborsId": ["METRO_100", "METRO_101"],
        "points": [
          { "x": 1650, "y": 1275 },
          { "x": 1660, "y": 1280 }
        ]
      }
    ]
  }
}
\`\`\`

## Основные элементы

### Линии (lines)

Каждая линия содержит:
- \`id\` - уникальный идентификатор
- \`name\` - название линии
- \`color\` - цвет в формате HEX
- \`stations\` - массив станций

### Станции (stations)

Каждая станция содержит:
- \`id\` - уникальный идентификатор
- \`name\` - название станции
- \`x\`, \`y\` - координаты
- \`textPosition\` - позиция подписи (0-8)
- \`neighbors\` - массив соседних станций: \`[["stationId", timeInSeconds]]\`

### Переходы (transfers)

Переход между станциями:
- \`id\` - уникальный идентификатор
- \`stations\` - массив ID станций
- \`time\` - время перехода в секундах
- \`type\` - тип перехода (\`default\`, \`crossplatform\`, и т.д.)

### Промежуточные точки (intermediatePoints)

Для создания кривых линий:
- \`neighborsId\` - пара ID станций
- \`points\` - массив из 2 контрольных точек для кривой Безье
`;

const notificationsContent = `
# Система оповещений

Оповещения могут отображаться пользователям в зависимости от различных условий (триггеров).

## Типы оповещений

- \`normal\` - обычное оповещение
- \`important\` - важное оповещение

## Типы триггеров

### once - один раз

Оповещение показывается один раз после первого открытия приложения.

\`\`\`json
{
  "triggerType": "once"
}
\`\`\`

### date_range - диапазон дат

Оповещение показывается только в указанный период.

\`\`\`json
{
  "triggerType": "date_range",
  "triggerDateStart": "2024-12-01",
  "triggerDateEnd": "2024-12-31"
}
\`\`\`

### station - для станции

Оповещение показывается при выборе определенной станции.

\`\`\`json
{
  "triggerType": "station",
  "triggerStationId": "METRO_100",
  "triggerDateStart": "2024-12-01",
  "triggerDateEnd": "2024-12-31"
}
\`\`\`

Может включать \`date_range\` для ограничения по времени.

### line - для линии

Оповещение показывается на станциях определенной линии.

\`\`\`json
{
  "triggerType": "line",
  "triggerLineId": "19",
  "triggerDateStart": "2024-12-01",
  "triggerDateEnd": "2024-12-31"
}
\`\`\`

## Типы контента

### Текстовое оповещение

\`\`\`json
{
  "contentText": "Текст оповещения"
}
\`\`\`

### Оповещение с изображением

\`\`\`json
{
  "contentImageUrl": "https://example.com/image.png",
  "contentImageResource": "image_resource_name",
  "contentCaption": "Подпись к изображению"
}
\`\`\`

Можно использовать либо \`contentImageUrl\` (URL), либо \`contentImageResource\` (локальный ресурс Android).

## Полный пример

\`\`\`json
{
  "id": "notification_d2_changes",
  "type": "important",
  "triggerType": "line",
  "triggerLineId": "19",
  "triggerDateStart": "2025-11-17",
  "triggerDateEnd": "2025-11-21",
  "contentText": "После 23:00 17-20 ноября интервалы увеличат до 20-50 минут..."
}
\`\`\`
`;

