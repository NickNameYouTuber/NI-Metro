# NI-Metro API

Spring Boot API service for managing metro maps and notifications.

## Prerequisites

- Java 17+
- PostgreSQL 12+
- Gradle 8+

## Setup

1. Create PostgreSQL database:
```sql
CREATE DATABASE nimetro;
CREATE USER nimetro_user WITH PASSWORD 'nimetro_password';
GRANT ALL PRIVILEGES ON DATABASE nimetro TO nimetro_user;
```

2. Update `application.yml` with your database credentials if needed.

3. Build and run:
```bash
./gradlew bootRun
```

## Database Migrations

Flyway will automatically run migrations on startup. Initial schema is in `src/main/resources/db/migration/V1__initial_schema.sql`.

## Importing Data

To import existing maps and notifications from JSON files:

```bash
# Import maps
./gradlew bootRun --args='-Dimport.maps.enabled=true'

# Import notifications
./gradlew bootRun --args='-Dimport.notifications.enabled=true'
```

## API Endpoints

### Public Endpoints (No authentication required)

- `GET /api/v1/maps` - List all active maps
- `GET /api/v1/maps/{id}` - Get map by ID
- `GET /api/v1/maps/by-name/{fileName}` - Get map by file name
- `GET /api/v1/notifications` - Get all active notifications
- `GET /api/v1/notifications?stationId={id}` - Get notifications for station
- `GET /api/v1/notifications?lineId={id}` - Get notifications for line

### Protected Endpoints (Require API Key in `X-API-Key` header)

- `POST /api/v1/maps` - Create new map
- `PUT /api/v1/maps/{id}` - Update map
- `DELETE /api/v1/maps/{id}` - Delete map (soft delete)
- `POST /api/v1/notifications` - Create notification
- `PUT /api/v1/notifications/{id}` - Update notification
- `DELETE /api/v1/notifications/{id}` - Delete notification

### Admin Endpoints (Require Admin role)

- `POST /api/v1/admin/api-keys/generate` - Generate API key for user
- `GET /api/v1/admin/api-keys/my-key` - Get current user's API key

## Default Admin User

A default admin user is created on first migration:
- Username: `admin`
- API Key: `nmi-admin-2024-11-18-default-key-change-in-production`

**⚠️ Change this in production!**

