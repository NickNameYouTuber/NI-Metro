# Инструкция по тестированию NI-Metro

## Подготовка

1. Убедитесь, что Docker и Docker Compose установлены
2. Клонируйте репозиторий
3. Перейдите в корневую директорию проекта

## Запуск системы

### Шаг 1: Запуск всех сервисов

```bash
docker-compose up -d
```

Подождите 1-2 минуты, пока все сервисы запустятся.

### Шаг 2: Проверка статуса

```bash
docker-compose ps
```

Все сервисы должны быть в статусе "Up (healthy)".

## Тестирование API

### 1. Проверка публичных endpoints

```bash
# Тест 1: Получить список карт
curl -v http://localhost:8080/api/v1/maps

# Ожидаемый результат: JSON массив карт или пустой массив []

# Тест 2: Получить оповещения
curl -v http://localhost:8080/api/v1/notifications

# Ожидаемый результат: JSON массив оповещений

# Тест 3: Получить оповещения для станции
curl -v "http://localhost:8080/api/v1/notifications?stationId=METRO_100"

# Ожидаемый результат: JSON массив оповещений для станции

# Тест 4: Получить оповещения для линии
curl -v "http://localhost:8080/api/v1/notifications?lineId=19"

# Ожидаемый результат: JSON массив оповещений для линии D2
```

### 2. Тестирование защищенных endpoints

#### Шаг 1: Получите API ключ

По умолчанию создается админ пользователь с ключом:
```
nmi-admin-2024-11-18-default-key-change-in-production
```

#### Шаг 2: Создайте тестовую карту

```bash
API_KEY="nmi-admin-2024-11-18-default-key-change-in-production"

curl -X POST http://localhost:8080/api/v1/maps \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Тестовая карта",
    "fileName": "test_map_001",
    "country": "Россия",
    "version": "1.0",
    "author": "Test User",
    "data": {
      "info": {
        "name": "Тестовая карта",
        "version": "1.0",
        "author": "Test User"
      },
      "metro_map": {
        "lines": [],
        "transfers": []
      }
    }
  }'
```

**Ожидаемый результат:** JSON объект с созданной картой (включая UUID `id`)

#### Шаг 3: Получите созданную карту

```bash
# Замените {id} на реальный ID из предыдущего ответа
curl -v http://localhost:8080/api/v1/maps/{id}
```

#### Шаг 4: Обновите карту

```bash
# Замените {id} на реальный ID
curl -X PUT http://localhost:8080/api/v1/maps/{id} \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Обновленная тестовая карта",
    "fileName": "test_map_001",
    "data": {
      "info": {"name": "Обновленная"},
      "metro_map": {"lines": []}
    }
  }'
```

#### Шаг 5: Удалите карту (soft delete)

```bash
# Замените {id} на реальный ID
curl -X DELETE http://localhost:8080/api/v1/maps/{id} \
  -H "X-API-Key: $API_KEY"
```

**Ожидаемый результат:** HTTP 204 No Content

#### Шаг 6: Проверьте, что карта скрыта

```bash
# Карта больше не должна появляться в списке
curl http://localhost:8080/api/v1/maps
```

### 3. Тестирование оповещений

#### Создайте оповещение

```bash
curl -X POST http://localhost:8080/api/v1/notifications \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "test_notification_1",
    "type": "normal",
    "triggerType": "once",
    "contentText": "Тестовое оповещение"
  }'
```

#### Получите созданное оповещение

```bash
curl http://localhost:8080/api/v1/notifications/test_notification_1
```

#### Обновите оповещение

```bash
curl -X PUT http://localhost:8080/api/v1/notifications/test_notification_1 \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "test_notification_1",
    "type": "important",
    "triggerType": "date_range",
    "triggerDateStart": "2024-12-01",
    "triggerDateEnd": "2024-12-31",
    "contentText": "Обновленное важное оповещение"
  }'
```

## Тестирование фронтенда разработчиков

### 1. Откройте браузер

Перейдите на http://localhost:5173

### 2. Настройка API ключа

1. Нажмите кнопку "Настройки" в правом верхнем углу
2. Введите API ключ: `nmi-admin-2024-11-18-default-key-change-in-production`
3. Нажмите "Сохранить"

### 3. Создание проекта

1. На главной странице нажмите "Новый проект"
2. Введите название проекта
3. Откроется редактор карт

### 4. Редактирование карты

1. Добавьте линию через кнопку "Добавить линию"
2. Выберите линию в списке
3. Добавьте станцию через кнопку "Добавить станцию"
4. Переместите станцию на карте (drag & drop)
5. Измените название станции в правой панели
6. Проверьте автоматическое сохранение (индикатор "Сохранено")

### 5. Проверка документации

1. Нажмите "Документация" в меню
2. Проверьте вкладки:
   - API Reference
   - Структура карты
   - Оповещения
3. Убедитесь, что Markdown отображается корректно

### 6. Импорт/Экспорт

1. Создайте проект
2. Добавьте данные
3. Экспортируйте через кнопку "Экспорт"
4. Импортируйте обратно через "Импорт проекта (.json)"

## Тестирование фронтенда пользователей

### 1. Откройте браузер

Перейдите на http://localhost:5174

### 2. Проверка лендинга

1. Проверьте Hero секцию:
   - Заголовок "NI-Metro"
   - Описание
   - Кнопки

2. Проверьте Features секцию:
   - 4 карточки с преимуществами
   - Иконки и описания

3. Проверьте Screenshots секцию:
   - Плейсхолдеры для скриншотов

4. Проверьте Footer:
   - Ссылки
   - Контакты
   - Копирайт

### 3. Проверка адаптивности

Измените размер окна браузера и убедитесь, что:
- Контент корректно отображается на мобильных устройствах
- Grid адаптируется под ширину экрана
- Навигация остается доступной

## Тестирование базы данных

### 1. Подключение к PostgreSQL

```bash
docker-compose exec postgres psql -U nimetro_user -d nimetro
```

### 2. Проверка таблиц

```sql
-- Список всех таблиц
\dt

-- Проверка структуры таблицы maps
\d maps

-- Проверка структуры таблицы notifications
\d notifications

-- Проверка структуры таблицы users
\d users
```

### 3. Проверка данных

```sql
-- Количество карт
SELECT COUNT(*) FROM maps WHERE is_active = true;

-- Количество оповещений
SELECT COUNT(*) FROM notifications WHERE is_active = true;

-- Список пользователей
SELECT username, role, created_at FROM users;

-- Проверка API ключей
SELECT username, LEFT(api_key, 20) || '...' as api_key_preview FROM users WHERE api_key IS NOT NULL;
```

### 4. Проверка индексов

```sql
-- Список индексов
\di

-- Проверка использования индексов
EXPLAIN ANALYZE SELECT * FROM maps WHERE file_name = 'test_map_001';
```

## Автоматизированное тестирование

### Тест API с помощью скрипта

Создайте файл `test-api.sh`:

```bash
#!/bin/bash

API_URL="http://localhost:8080/api/v1"
API_KEY="nmi-admin-2024-11-18-default-key-change-in-production"

echo "Testing public endpoints..."
curl -s "$API_URL/maps" | jq '. | length' && echo "✓ Maps list"
curl -s "$API_URL/notifications" | jq '. | length' && echo "✓ Notifications list"

echo "Testing protected endpoints..."
RESPONSE=$(curl -s -X POST "$API_URL/maps" \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Auto Test",
    "fileName": "auto_test",
    "data": {"info": {"name": "Test"}, "metro_map": {"lines": []}}
  }')

MAP_ID=$(echo $RESPONSE | jq -r '.id')
echo "✓ Created map: $MAP_ID"

curl -s -X DELETE "$API_URL/maps/$MAP_ID" \
  -H "X-API-Key: $API_KEY" && echo "✓ Deleted map"

echo "All tests passed!"
```

Запуск:
```bash
chmod +x test-api.sh
./test-api.sh
```

## Проверка безопасности

### 1. Тест без API ключа (должен вернуть 401/403)

```bash
# Попытка создать карту без ключа
curl -X POST http://localhost:8080/api/v1/maps \
  -H "Content-Type: application/json" \
  -d '{"name": "Test"}'

# Ожидаемый результат: HTTP 401/403 Unauthorized
```

### 2. Тест с неверным API ключом

```bash
curl -X POST http://localhost:8080/api/v1/maps \
  -H "X-API-Key: invalid-key-12345" \
  -H "Content-Type: application/json" \
  -d '{"name": "Test"}'

# Ожидаемый результат: HTTP 401/403 Unauthorized
```

### 3. Тест SQL инъекции

```bash
# Попытка SQL инъекции в параметрах
curl "http://localhost:8080/api/v1/maps/by-name/test'; DROP TABLE maps;--"

# Ожидаемый результат: Ошибка валидации, таблица должна остаться нетронутой
```

## Проверка производительности

### 1. Нагрузочное тестирование API

```bash
# Установите Apache Bench или используйте curl в цикле
for i in {1..100}; do
  curl -s http://localhost:8080/api/v1/maps > /dev/null
done

# Замерьте время
time for i in {1..100}; do curl -s http://localhost:8080/api/v1/maps > /dev/null; done
```

### 2. Проверка ответа API

```bash
# Замер времени ответа
time curl -s http://localhost:8080/api/v1/maps

# Должно быть < 100ms для локального запроса
```

## Отладка

### Просмотр логов

```bash
# Все логи
docker-compose logs -f

# Логи API
docker-compose logs -f api | grep ERROR

# Логи PostgreSQL
docker-compose logs -f postgres | grep ERROR
```

### Проверка подключений

```bash
# Проверка связи между контейнерами
docker-compose exec api ping postgres
docker-compose exec frontend-dev ping api
```

### Перезапуск сервисов

```bash
# Перезапуск конкретного сервиса
docker-compose restart api

# Пересборка и перезапуск
docker-compose up -d --build api
```

## Чеклист перед коммитом

- [ ] Все сервисы запускаются без ошибок
- [ ] API отвечает на публичные endpoints
- [ ] Защищенные endpoints требуют API ключ
- [ ] Фронтенд разработчиков открывается и работает
- [ ] Фронтенд пользователей отображается корректно
- [ ] Можно создать/обновить/удалить карту
- [ ] Можно создать/обновить/удалить оповещение
- [ ] Документация отображается корректно
- [ ] Нет ошибок в логах
- [ ] База данных сохраняет данные между перезапусками

