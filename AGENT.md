# Культурный маршрут: `POST /route-adventure`

Контракт TatarTouristGuideService. Бэкенд сам подбирает места из DataSet, строит пешее расписание по Казани и учитывает оценки анонимного пользователя. Это не OSRM-конструктор: клиент передаёт параметры прогулки, сервер возвращает готовый упорядоченный список точек.

База: `http://192.168.3.11:5137`  
Swagger: `http://192.168.3.11:5137/swagger/index.html`

```text
POST /users/anonymous  ->  userId
GPS / выбранные точки  ->  RouteAdventureRequest
POST /route-adventure  ->  RouteAdventureResponse
карта / прогресс тура  <-  places[] + длительности
```

## Заголовок

| Заголовок | Обязателен | Описание |
| --- | --- | --- |
| `X-User-Id` | да | UUID анонимного пользователя из `POST /users/anonymous`. По нему бэкенд подтягивает оценки. |
| `Content-Type` | да | `application/json` |
| `Accept` | да | `application/json` |

Без существующего `X-User-Id` запрос не считается валидным.

## Запрос `RouteAdventureRequest`

`POST /route-adventure`

| Поле | Тип | Обязательно | Описание |
| --- | --- | --- | --- |
| `durationMinutes` | int | да | Желаемая длительность прогулки, 30–720 минут. |
| `interests` | string[] | да | Культурные интересы, например `история`, `татарская культура`, `архитектура`, `музеи`, `мечети`, `парки`. |
| `pace` | string | да | Темп: `быстрый`, `обычный`, `неспешный`. |
| `userLocation` | `{ lat, lon }` | да | Текущая точка пользователя. Широта и долгота в десятичных градусах. |
| `requiredPlaces` | `{ lat, lon }[]` | нет | Обязательные точки DataSet по координатам. |
| `visitedPlaces` | string | нет | Имена уже посещённых мест через запятую. |
| `aiRequest` | string | нет | Свободное пожелание по культурному туризму. |
| `startAt` | string | нет | Старт ISO 8601 со смещением, например `2026-09-12T10:00:00+03:00`. Без поля бэкенд берёт текущее время. |
| `optimizeVisitOrder` | bool | нет | Оптимизировать порядок посещения моделью. |

`additionalProperties` запрещены.

### Пример запроса

```json
{
  "durationMinutes": 240,
  "interests": ["история", "татарская культура"],
  "pace": "обычный",
  "requiredPlaces": [{ "lat": 55.798551, "lon": 49.106324 }],
  "visitedPlaces": "Казанский Кремль",
  "aiRequest": "История Казани",
  "userLocation": { "lat": 55.796127, "lon": 49.106405 },
  "startAt": "2026-09-12T10:00:00+03:00",
  "optimizeVisitOrder": true
}
```

## Ответ `RouteAdventureResponse`

| Поле | Тип | Описание |
| --- | --- | --- |
| `startedAt` | date-time | Начало маршрута, часовой пояс Казани. |
| `finishedAt` | date-time | Время завершения. |
| `totalDurationMinutes` | int | Дорога + осмотр + ожидание. |
| `totalTravelDurationMinutes` | int | Суммарное проверенное время в пути. |
| `totalVisitDurationMinutes` | int | Суммарное проверенное время осмотра. |
| `places` | `RouteAdventurePlaceResponse[]` | Точки в фактическом порядке посещения. |

Каждая точка:

| Поле | Описание |
| --- | --- |
| `order` | Порядок с единицы. |
| `id` | Идентификатор места из DataSet. |
| `name`, `address`, `description` | Данные из БД. |
| `lat`, `lon` | Координаты. |
| `type` | Категория (`музей`, `tatar_food`, `walks_parks` и т.д.). |
| `categoryIconUrl`, `imageUrl` | URL иконки и фото; могут быть `null`. `localhost` в URL нужно заменить на хост бэкенда. |
| `arrivalAt`, `departureAt` | Проверенное расписание. |
| `travelDurationMinutes` | Время пути от предыдущей точки. |
| `visitDurationMinutes` | Время осмотра. |

### Пример ответа

```json
{
  "startedAt": "2026-09-12T10:00:00+03:00",
  "finishedAt": "2026-09-12T11:04:00+03:00",
  "totalDurationMinutes": 64,
  "totalTravelDurationMinutes": 4,
  "totalVisitDurationMinutes": 60,
  "places": [
    {
      "order": 1,
      "id": "place-001",
      "name": "Казанский Кремль",
      "address": "Казань, Кремлёвская улица",
      "description": "Исторический комплекс.",
      "lat": 55.798551,
      "lon": 49.106324,
      "type": "история",
      "categoryIconUrl": null,
      "imageUrl": "https://s3go.kzn.ru/....jpg",
      "arrivalAt": "2026-09-12T10:04:00+03:00",
      "departureAt": "2026-09-12T11:04:00+03:00",
      "travelDurationMinutes": 4,
      "visitDurationMinutes": 60
    }
  ]
}
```

## Ошибки

Тело: `{ "message": "..." }` — публичный текст без внутренних деталей.

| HTTP | Когда |
| --- | --- |
| 400 | Невалидный запрос. |
| 404 | Пользователь или связанные данные не найдены. |
| 502 | Upstream бэкенда недоступен. |
| 500 | Внутренняя ошибка сервиса. |

## Перестроение: `POST /route-adventure/rebuild`

Нужен тот же `X-User-Id`. Сервер сопоставляет посещённые и оставшиеся координаты с Places, исключает посещённое и возвращает тот же `RouteAdventureResponse`. Пустой выполнимый результат — маршрут с нулевыми итогами.

`RouteRebuildRequest`:

| Поле | Обязательно | Описание |
| --- | --- | --- |
| `visitedPlaces` | да | Массив `{ lat, lon }`, может быть пустым. |
| `remainingPlaces` | да | Непосещённая часть исходного маршрута. |
| `userLocation` | да | Текущая позиция. |
| `aiRequest` | да | Пожелание, 1–1000 символов. |
| `currentAt` | нет | ISO 8601 со смещением. Без поля — текущее время `Europe/Moscow`. |

Пример:

```json
{
  "visitedPlaces": [{ "lat": 55.798551, "lon": 49.106324 }],
  "remainingPlaces": [{ "lat": 55.796289, "lon": 49.108795 }],
  "userLocation": { "lat": 55.795122, "lon": 49.110442 },
  "currentAt": "2026-09-13T15:30:00+03:00",
  "aiRequest": "Я устал, осталось 30 минут"
}
```

Дополнительно возможны 504 при таймауте.

## Клиент в приложении

- HTTP: `GuideApiClient.postJson`, база `GuideApiConfig.BASE_URL`.
- Вызов: `RouteApiSource.buildRoute` / `rebuildRoute` → `RouteRepository` → `BuildAdventureRouteUseCase` / `ContinueRouteUseCase`.
- Перед запросом `AnonymousUserSource.ensureUserId()` кладёт UUID в `X-User-Id`.
- Координаты приводятся к Казани (`clampToKazan`).
- Сейчас клиент по умолчанию шлёт `durationMinutes=240`, `pace=обычный`, интересы история/культура/архитектура/музеи/мечети/парки, `optimizeVisitOrder=true`.
- Ответ мапится в `AdventureRoute`; точка — в `OsmPlace` (`id`, `name`, `lat`, `lon`, `type` → категория, `description`, `address`, `imageUrl`, `categoryIconUrl`). Поля расписания `order` / `arrivalAt` / `departureAt` / `travelDurationMinutes` / `visitDurationMinutes` в сущность пока не переносятся.
- `localhost:9000` в медиа-URL переписывается на `192.168.3.11:9000`.
- Кнопка «Построить» на карте вызывает OSRM по выбранным точкам, а не этот эндпоинт. `POST /route-adventure` остаётся в data-слое для автосборки культурного маршрута.
