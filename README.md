# 💳 PayPulse

> **Современная микросервисная платёжная система** с API Gateway, аутентификацией, управлением балансами и транзакциями. Полностью контейнеризированная архитектура.

## 🏗️ Архитектура

```
                    API Gateway (8080)
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
    Auth (8081)    Wallet (8082)    Transaction (8083)
        │                  │                  │
        │                  │                  │
Notification (8084)  Analytics (8085)    Kafka Events
        │                  │                  │
        └──────────────────┼──────────────────┘
                           │
                    PostgreSQL Databases
                           │
                        Redis (Rate Limiting)
                           │
                    Prometheus + Grafana
```

## 🔧 Сервисы

| Сервис | Порт | Назначение |
|--------|------|------------|
| 🚪 API Gateway | 8080 | Единая точка входа |
| 🔐 Auth | 8081 | Аутентификация, JWT |
| 💰 Wallet | 8082 | Управление балансами |
| 💸 Transaction | 8083 | Транзакции |
| 📢 Notification | 8084 | Уведомления |
| 📊 Analytics | 8085 | Аналитика |

## 🚀 Запуск проекта

### 🐳 Docker Compose

```bash
# 1. Настроить переменные окружения
cp env.example .env

# 2. Запустить все сервисы
docker-compose up -d

# 3. Проверить статус
docker-compose ps
```

**Доступные сервисы:**
- 🌐 **API Gateway**: http://localhost:8080
- 📚 **Swagger UI**: http://localhost:8080/docs/
- 📊 **Grafana**: http://localhost:3000 (admin/admin)
- 🔍 **Health Check**: http://localhost:8080/actuator/health

---

## 🧪 Тестирование

### 📋 Тестовые файлы

**🐳 Docker Compose:**
- `test-requests-docker.http` - тестирование в Docker Compose

### 📝 Инструкции по тестированию

**Для Docker Compose:**
```bash
# 1. Запустить проект
docker-compose up -d

# 2. Использовать test-requests-docker.http
# 3. Заменить YOUR_ACCESS_TOKEN_HERE на реальный токен
```

## ⚙️ Технологии

### 🚀 **Backend Stack**
- **Java 17** + **Spring Boot 3.2.4** - современная JVM экосистема
- **Spring Cloud Gateway** - API Gateway с маршрутизацией и фильтрами
- **Spring Security** + **JWT** - безопасная аутентификация с контекстом
- **Spring Data JPA** + **Liquibase** - работа с БД и миграции
- **Lombok** - упрощение boilerplate кода

### 🗄️ **Базы данных & Кэширование**
- **PostgreSQL 15** - надежная реляционная БД (5 отдельных БД)
- **Redis 7** - rate limiting и межсервисная коммуникация
- **Apache Kafka** - асинхронная обработка событий

### 🐳 **Контейнеризация & Оркестрация**
- **Docker** - контейнеризация приложений
- **Docker Compose** - локальная разработка

### 📊 **Мониторинг & Наблюдаемость**
- **Prometheus** - сбор метрик и алерты
- **Grafana** - визуализация и дашборды
- **Spring Boot Actuator** - health checks и метрики
- **Structured Logging** - централизованное логирование

### 🏗️ **Архитектурные паттерны**
- **Microservices** - модульная архитектура
- **Event-Driven Architecture** - асинхронная коммуникация
- **Saga Pattern** - распределенные транзакции
- **Outbox Pattern** - надежная доставка событий
- **Circuit Breaker** - отказоустойчивость
- **Rate Limiting** - защита от перегрузки
- **Security Context** - безопасное получение пользовательского контекста



## Автор

**GitinDalgatov** - разработчик микросервисной архитектуры PayPulse 