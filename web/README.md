# NI-Metro Web Editor

Веб-редактор карты метро для проекта NI-Metro.

## Требования

- Node.js (версия 18 или выше)
- npm или yarn

## Установка

1. Перейдите в директорию `web`:
```bash
cd web
```

2. Установите зависимости:
```bash
npm install
```

## Запуск

Проект состоит из двух частей: фронтенд (Vite) и API сервер (Express).

### Вариант 1: Запуск в двух терминалах (рекомендуется)

**Терминал 1 - Фронтенд (Vite):**
```bash
npm run dev
```
Фронтенд будет доступен по адресу: http://localhost:5173

**Терминал 2 - API сервер:**
```bash
npm run api
```
API сервер будет доступен по адресу: http://localhost:5174

### Вариант 2: Запуск через один терминал

Можно запустить оба сервера параллельно, используя пакет `concurrently` (если установлен) или просто открыть два терминала.

## Доступные команды

- `npm run dev` - запуск dev-сервера Vite (порт 5173)
- `npm run build` - сборка проекта для production
- `npm run preview` - предпросмотр production сборки (порт 5173)
- `npm run api` - запуск API сервера (порт 5174)

## Структура проекта

```
web/
├── src/
│   ├── main.tsx          # Точка входа
│   ├── modules/
│   │   ├── app/          # Основное приложение
│   │   └── editor/       # Компоненты редактора карты
├── server.mjs            # Express API сервер
├── vite.config.ts        # Конфигурация Vite
└── package.json          # Зависимости и скрипты
```

## API Endpoints

- `GET /api/load?path=<путь>` - загрузка файла
- `POST /api/save` - сохранение файла
  ```json
  {
    "path": "app/src/main/assets/raw/metromap_1.json",
    "content": "..."
  }
  ```

## Технологии

- **React 19** - UI библиотека
- **TypeScript** - типизация
- **Vite** - сборщик и dev-сервер
- **Mantine** - UI компоненты
- **Pixi.js** - рендеринг карты
- **Express** - API сервер

