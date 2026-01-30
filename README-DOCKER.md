# NI-Metro Docker Setup

## Быстрый старт

```bash
# Запустить все сервисы
docker-compose up -d

# Проверить статус
docker-compose ps

# Посмотреть логи
docker-compose logs -f
```

## Доступ к сервисам

- **API**: http://localhost:8080/api/v1
- **Фронтенд разработчиков**: http://localhost:5173
- **Фронтенд пользователей**: http://localhost:5174
- **PostgreSQL**: localhost:5432

## Первая проверка

### 1. Проверка API (публичные endpoints)

```bash
# Список карт
curl http://localhost:8080/api/v1/maps

# Список оповещений
curl http://localhost:8080/api/v1/notifications
```

### 2. Проверка фронтенда разработчиков

1. Откройте http://localhost:5173
2. Перейдите в Settings
3. Введите API ключ: `nmi-admin-2024-11-18-default-key-change-in-production`
4. Создайте тестовый проект

### 3. Проверка фронтенда пользователей

Откройте http://localhost:5174 и убедитесь, что лендинг отображается.

## Полное тестирование

Смотрите файл [TESTING.md](./TESTING.md) для детальных инструкций по тестированию всех компонентов.

## Остановка

```bash
# Остановить сервисы
docker-compose stop

# Остановить и удалить контейнеры
docker-compose down

# Полная очистка (включая данные БД)
docker-compose down -v
```

## Проблемы?

Смотрите раздел "Устранение проблем" в [DOCKER.md](./DOCKER.md).

