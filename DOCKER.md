# Docker Compose Setup для NI-Metro

## Структура

Проект состоит из следующих сервисов:
- **postgres** - PostgreSQL база данных
- **api** - Spring Boot API сервис
- **frontend-dev** - Фронтенд для разработчиков (редактор карт)
- **frontend-user** - Фронтенд для пользователей (лендинг)

## Быстрый старт

### 1. Запуск всех сервисов

```bash
docker-compose up -d
```

Эта команда:
- Создаст и запустит PostgreSQL
- Построит и запустит API сервис
- Построит и запустит оба фронтенда
- Настроит сети между сервисами

### 2. Проверка статуса

```bash
docker-compose ps
```

Все сервисы должны быть в статусе "Up" и здоровы.

### 3. Просмотр логов

```bash
# Все сервисы
docker-compose logs -f

# Конкретный сервис
docker-compose logs -f api
docker-compose logs -f postgres
docker-compose logs -f frontend-dev
docker-compose logs -f frontend-user
```

## Доступ к сервисам

После запуска сервисы доступны по следующим адресам:

- **API**: http://localhost:8080
- **Фронтенд разработчиков**: http://localhost:5173
- **Фронтенд пользователей**: http://localhost:5174
- **PostgreSQL**: localhost:5432

## Проверка работы

### 1. Проверка базы данных

```bash
# Подключение к PostgreSQL
docker-compose exec postgres psql -U nimetro_user -d nimetro

# В psql консоли:
\dt                    # Список таблиц
SELECT * FROM users;   # Проверка пользователей
SELECT COUNT(*) FROM maps;  # Количество карт
SELECT COUNT(*) FROM notifications;  # Количество оповещений
\q                     # Выход
```

### 2. Проверка API

#### Публичные endpoints (без API ключа):

```bash
# Список всех карт
curl http://localhost:8080/api/v1/maps

# Получить оповещения
curl http://localhost:8080/api/v1/notifications

# Получить оповещения для станции
curl "http://localhost:8080/api/v1/notifications?stationId=METRO_100"

# Получить оповещения для линии
curl "http://localhost:8080/api/v1/notifications?lineId=19"
```

#### Защищенные endpoints (требуют API ключ):

```bash
# Используем дефолтный API ключ админа
API_KEY="nmi-admin-2024-11-18-default-key-change-in-production"

# Создать новую карту
curl -X POST http://localhost:8080/api/v1/maps \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Тестовая карта",
    "fileName": "test_map",
    "data": {"info": {"name": "Test"}, "metro_map": {"lines": []}}
  }'

# Получить свой API ключ (если авторизован как админ)
curl http://localhost:8080/api/v1/admin/api-keys/my-key \
  -H "X-API-Key: $API_KEY"
```

### 3. Проверка фронтенда разработчиков

1. Откройте http://localhost:5173
2. Перейдите в Settings для настройки API ключа
3. Используйте дефолтный ключ: `nmi-admin-2024-11-18-default-key-change-in-production`
4. Проверьте создание/редактирование карт
5. Откройте раздел Documentation для просмотра документации

### 4. Проверка фронтенда пользователей

1. Откройте http://localhost:5174
2. Проверьте отображение лендинга
3. Проверьте все секции (Hero, Features, Screenshots, Footer)

## Импорт данных

### Автоматический импорт при первом запуске

API сервис попытается импортировать данные из `/app/data` (который маппится на `./app/src/main/assets/raw`).

Чтобы запустить импорт вручную:

```bash
# Импорт карт
docker-compose exec api java -jar app.jar --import.maps.enabled=true

# Импорт оповещений
docker-compose exec api java -jar app.jar --import.notifications.enabled=true
```

### Ручной импорт через SQL

```bash
# Подключение к БД
docker-compose exec postgres psql -U nimetro_user -d nimetro

# Затем выполните SQL скрипты из миграций или импортируйте данные вручную
```

## Устранение проблем

### API не запускается

```bash
# Проверьте логи
docker-compose logs api

# Проверьте подключение к БД
docker-compose exec api ping postgres
```

### Фронтенды не компилируются

```bash
# Пересоберите образы
docker-compose build --no-cache frontend-dev
docker-compose build --no-cache frontend-user

# Проверьте логи
docker-compose logs frontend-dev
```

### База данных не подключается

```bash
# Проверьте статус PostgreSQL
docker-compose ps postgres

# Проверьте логи
docker-compose logs postgres

# Пересоздайте том с данными (⚠️ удалит все данные!)
docker-compose down -v
docker-compose up -d postgres
```

### Порт уже занят

Измените порты в `docker-compose.yml`:

```yaml
ports:
  - "8081:8080"  # вместо 8080:8080
```

## Остановка сервисов

```bash
# Остановка с сохранением данных
docker-compose stop

# Остановка и удаление контейнеров (данные сохраняются)
docker-compose down

# Полная очистка (включая данные БД)
docker-compose down -v
```

## Пересборка образов

```bash
# Пересборка всех сервисов
docker-compose build --no-cache

# Пересборка конкретного сервиса
docker-compose build --no-cache api
docker-compose build --no-cache frontend-dev
docker-compose build --no-cache frontend-user
```

## Переменные окружения

Для изменения конфигурации создайте `.env` файл в корне проекта:

```env
POSTGRES_PASSWORD=your_password
POSTGRES_USER=your_user
POSTGRES_DB=your_db
API_PORT=8080
FRONTEND_DEV_PORT=5173
FRONTEND_USER_PORT=5174
```

И обновите `docker-compose.yml` для использования переменных:

```yaml
environment:
  POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-nimetro_password}
```

## Разработка

### Режим разработки (hot reload)

Для API (если нужно):
```bash
# Запустите только БД и API локально
docker-compose up postgres -d
# Затем запустите API локально с gradle bootRun
```

Для фронтендов - используйте Docker Compose как есть, изменения в коде подхватываются автоматически через volume mounts.

### Подключение к БД из внешних инструментов

```
Host: localhost
Port: 5432
Database: nimetro
Username: nimetro_user
Password: nimetro_password
```

## Проверочный чеклист

После запуска проверьте:

- [ ] PostgreSQL запущен и здоров
- [ ] API отвечает на `/api/v1/maps` без ошибок
- [ ] Фронтенд разработчиков открывается на порту 5173
- [ ] Фронтенд пользователей открывается на порту 5174
- [ ] Можно создать карту через API с API ключом
- [ ] Можно просмотреть список карт без API ключа
- [ ] Документация доступна во фронтенде разработчиков
- [ ] Лендинг отображается корректно

## Производственное развертывание

Для production:

1. Соберите production образы:
   ```bash
   docker-compose -f docker-compose.prod.yml build
   ```

2. Используйте secrets для паролей и API ключей

3. Настройте reverse proxy (nginx) для фронтендов

4. Настройте SSL сертификаты

5. Используйте внешнюю PostgreSQL для production данных

