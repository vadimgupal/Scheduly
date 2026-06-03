# Scheduly

**Scheduly** — Telegram-бот для управления расписанием и планирования досуга. Приложение позволяет пользователю работать с календарями Google, событиями, задачами, напоминаниями, сводками и свободными временными интервалами через интерфейс Telegram.

## Возможности

* авторизация пользователя через Google OAuth 2.0;
* интеграция с Google Calendar API;
* создание, просмотр, изменение и удаление календарей;
* выбор календаря по умолчанию;
* создание, просмотр, изменение и удаление событий;
* поддержка напоминаний и повторяющихся событий;
* создание, просмотр, изменение и удаление задач;
* установка пользовательского часового пояса;
* ежедневные и недельные сводки;
* поиск свободных временных интервалов;
* хранение состояния пошаговых диалогов;
* контейнерный запуск приложения через Docker Compose.

## Стек технологий

* Java 23
* Spring Boot
* Maven
* PostgreSQL
* Redis
* Liquibase
* Google Calendar API
* Google OAuth 2.0
* Telegram Bot API
* Docker, Docker Compose
* JUnit 5, Mockito
* Checkstyle

### bot

Модуль `bot` отвечает за взаимодействие с пользователем через Telegram:

* обработка команд;
* обработка callback-запросов от inline-кнопок;
* пошаговые диалоги;
* отправка сообщений пользователю;
* взаимодействие с `core` через REST API.

### core

Модуль `core` содержит основную бизнес-логику приложения:

* авторизация через Google OAuth;
* работа с Google Calendar API;
* работа с пользователями, задачами и настройками;
* поиск свободных временных интервалов;
* напоминания и сводки;
* взаимодействие с PostgreSQL и Redis.

### dto

Модуль `dto` содержит общие DTO-классы, которые используются для обмена данными между `bot` и `core`.

## Основные команды бота

### Календарь

```text
/createCalendar
/updateCalendar
/deleteCalendar
/getCalendars
/setDefaultCalendar
/deleteDefaultCalendar
/getDefaultCalendar
```

### События

```text
/createEvent
/updateEvent
/deleteEvent
/getEvents
```

### Задачи

```text
/createTask
/updateTask
/deleteTask
/getTasks
```

### Свободные интервалы

```text
/freeSlots
```

### Настройки

```text
/setTimezone
/deleteTimezone
/getTimezone
```

### Справка

```text
/help
```

## Хранение данных

В проекте используются два типа хранилищ:

### PostgreSQL

PostgreSQL используется для постоянных данных:

* пользователи;
* refresh token;
* задачи;
* календарь по умолчанию;
* часовой пояс пользователя.

Структура базы данных управляется с помощью Liquibase.

### Redis

Redis используется для временных данных:

* состояние диалога пользователя;
* режим текущей операции;
* черновики введённых данных;
* выбранные элементы inline-кнопок;
* временные access token;
* защита от повторной отправки уведомлений.

## Переменные окружения

Для запуска приложения необходимо указать переменные окружения.

Пример `.env`:

```env
TOKEN=telegram_bot_token

AUTH_URI=https://accounts.google.com/o/oauth2/v2/auth
TOKEN_URI=https://oauth2.googleapis.com/token
CLIENT_ID=google_client_id
CLIENT_SECRET=google_client_secret
REDIRECT_URI=http://localhost:8081/auth/google/callback
```
