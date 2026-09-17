# Диагностика построения маршрутов в Android-приложении

Дата анализа: 2026-09-17.  
Репозиторий: `com.example.homework` (Kazan Walk).  
Единственный экран: `LiveMapScreen` в `MainActivity`.  
Метод: **статический анализ исходников**. Runtime-сборка не запускалась: `adb devices` пуст, HTTP-логирования в клиенте нет.

Повторные прямые запросы к `POST /route-adventure` **не выполнялись** (уже воспроизведены заказчиком: 502 за ~45 мс).

---

## A. Краткий вывод

### Доказанные факты

1. Пользователь может «построить маршрут» на мобильном, потому что кнопка **«Построить» вызывает публичный OSRM**, а не TatarTouristGuideService.
2. Endpoint работающего сценария:
   - пешком: `GET https://routing.openstreetmap.de/routed-foot/route/v1/driving/{coords}`
   - вело: `https://routing.openstreetmap.de/routed-bike/route/v1/driving/{coords}`
   - авто: `https://routing.openstreetmap.de/routed-car/route/v1/driving/{coords}`
   - Query: `overview=full&geometries=geojson&steps=true&alternatives=false`
3. **`POST /route-adventure` в этом сценарии не вызывается.** Цепочка UI → ViewModel → `GetOsrmRouteUseCase` → `OsrmSource`. `BuildAdventureRouteUseCase` в `LiveMapViewModel` не инжектится и нигде кроме Koin не используется.
4. Успешный ответ AI-генерации (`POST /route-adventure` HTTP 200) **не подтверждён**. Код клиента для этого вызова существует, но из UI мёртв. Runtime-запрос не наблюдался.
5. Приложение **может показать линию маршрута при полном отказе `/route-adventure`**, если пользователь вручную добавил точки и OSRM доступен.
6. Сконфигурированный адрес Guide API в актуальных исходниках: `http://192.168.3.11:5137` (одна константа, без flavor/BuildConfig). Этот адрес используется для мест, анонимного пользователя, story, ratings — не для кнопки «Построить».

### Предположения (не доказаны runtime)

- Пользователь называет «работающей генерацией» ручной конструктор + OSRM-линию.
- Если у кого-то стоит более старый APK (до выноса auto-adventure с главного экрана), там `/route-adventure` мог вызываться. В текущем дереве исходников этого нет.

---

## B. Архитектура

Независимых сценариев, связанных с «маршрутом», **четыре живых и два мёртвых**.

### Сценарий 1 (живой). Ручной конструктор + OSRM — кнопка «Построить»

Это то, что пользователь видит как построение маршрута.

| Шаг | Что | Файл | Строки |
| --- | --- | --- | --- |
| UI | Кнопка «Построить» | `app/src/main/java/com/example/homework/ui/feature/map/RouteBuilderPanel.kt` | 136 |
| Экран | Нижняя панель на карте, если не выбран пин и нет навигации | `LiveMapScreen.kt` | 170–187 |
| onClick | `onBuild` | `RouteBuilderPanel.kt` | 49, 136, 191–211 |
| Привязка | `onBuild = viewModel::buildRoute` | `LiveMapScreen.kt` | 182 |
| ViewModel | `fun buildRoute() = requestOsrmRoute(fromNavigation = false)` | `LiveMapViewModel.kt` | 161, 272–309 |
| UseCase | `GetOsrmRouteUseCase` | `GetOsrmRouteUseCase.kt` | 7–8 |
| Impl | `GetOsrmRouteUseCaseImpl` → `OsrmRepository.getRoute` | `GetOsrmRouteUseCaseImpl.kt` | 9–13 |
| Repository | `OsrmRepositoryImpl` | `OsrmRepositoryImpl.kt` | 10–14 |
| DataSource | `OsrmSource.getRoute` | `OsrmSource.kt` | 27–43, 129–144 |
| HTTP | `GET` OSRM, **не** Guide API | `OsrmSource.kt` | 56–74, 129–144 |
| Источник точек | GPS/`KazanCenter` + уже выбранные `routePlaces` | `LiveMapViewModel.kt` | 311–315 |
| Отображение | `state.route.geometry` → osmdroid `Polyline` | `LiveMapScreen.kt` 93–94; `OsmMap.kt` 134, 218–241 | |

Точки **не запрашиваются** у backend в момент «Построить». Они должны уже лежать в `LiveMapUiState.routePlaces` после «Добавить в маршрут».

Новые туристические места от backend не запрашиваются. LLM не вызывается. Расписания (`arrivalAt` / `departureAt`) нет.

Работает **без** TatarTouristGuideService, если точки уже есть (в т.ч. с Overpass).

### Сценарий 2 (живой). «Оптимизировать»

| Шаг | Что | Файл | Строки |
| --- | --- | --- | --- |
| UI | Кнопка «Оптимизировать» | `RouteBuilderPanel.kt` | 137 |
| ViewModel | `optimizeAndBuild()` | `LiveMapViewModel.kt` | 163–184 |
| UseCase | `OptimizeRoutePlacesUseCase` | `OptimizeRoutePlacesUseCaseImpl.kt` | 10–24 |
| HTTP | `GET {osrm}/table/v1/driving/{coords}?annotations=duration,distance` | `OsrmSource.kt` | 45–54, 133–134 |
| Затем | тот же `requestOsrmRoute` (сценарий 1) | `LiveMapViewModel.kt` | 172 |

Жадный nearest-neighbor по матрице времени: `RouteResult.kt` 25–51. Не LLM.

### Сценарий 3 (живой). Пешая GPS-навигация

| Действие UI | ViewModel | HTTP |
| --- | --- | --- |
| «Начать навигацию» | `startNavigation()` 186–193 | OSRM `getRoute` |
| GPS update при сходe | `startLocationUpdates` 400–403 | OSRM `getRoute` |
| Retry / следующая точка | `retryNavigation` 203–207, `nextNavigationPlace` 209–213 | OSRM `getRoute` |

Автомат: `WalkingNavigation.kt`. `rebuildRoute` там — флаг перестроить **OSRM**, не `/route-adventure/rebuild`.

Только `TransportMode.Walking`. Transit не строится OSRM (`TransportMode.kt` 10–11, `OsrmSource.kt` 143).

### Сценарий 4 (живой). Яндекс / Поделиться

Кнопки «Яндекс» и «Поделиться» (`RouteBuilderPanel.kt` 141–142) открывают URL `https://yandex.ru/maps/?rtext=...` (`TransportMode.kt` 13–28, `LiveMapUiState.kt` 57–61). HTTP к своему backend и к OSRM нет.

### Сценарий 5 (мёртвый). `POST /route-adventure`

| Слой | Класс | Файл | Строки | Вызывается из UI? |
| --- | --- | --- | --- | --- |
| DataSource | `RouteApiSource.buildRoute` | `RouteApiSource.kt` | 21–53 | нет |
| Repository | `RouteRepository.buildRoute` | `RouteRepository.kt` 7; `RouteRepositoryImpl.kt` 11–12 | нет |
| UseCase | `BuildAdventureRouteUseCase` / `Impl` | `BuildAdventureRouteUseCase.kt`; `BuildAdventureRouteUseCaseImpl.kt` 8–12 | нет |
| DI | `factoryOf(::BuildAdventureRouteUseCaseImpl)` | `KoinModules.kt` | 139 | регистрация, без потребителей |
| ViewModel | отсутствует в конструкторе | `LiveMapViewModel.kt` | 38–52 | **не инжектится** |

Поиск по репозиторию: `BuildAdventureRouteUseCase` встречается только в собственном интерфейсе, impl и `KoinModules.kt`. В `LiveMapViewModel` нет импорта и поля.

Кнопки автоматической AI-генерации культурного маршрута **нет**.

### Сценарий 6 (мёртвый). `POST /route-adventure/rebuild`

| Слой | Класс | Файл | Строки | Вызывается из UI? |
| --- | --- | --- | --- | --- |
| DataSource | `RouteApiSource.rebuildRoute` | `RouteApiSource.kt` | 55–74 | нет |
| UseCase | `ContinueRouteUseCaseImpl` | `ContinueRouteUseCaseImpl.kt` | 9–29 | нет |
| DI | `KoinModules.kt` | 137 | без потребителей |

Кнопка AI-гида **«Дальше к маршруту»** (`AudioGuideContent.kt` 255–259) привязана к `viewModel::goToNextStop` (`LiveMapScreen.kt` 212), а не к `ContinueRouteUseCase`.

`GoToNextStopUseCaseImpl` только сдвигает локальный индекс `TourSource` и сбрасывает аудио (`GoToNextStopUseCaseImpl.kt` 7–14). HTTP нет.

### Загрузка мест (не построение маршрута)

Нужна, чтобы было что добавить в конструктор.

```
LiveMapViewModel.init / GPS
  → EnsureAnonymousUserUseCase  (POST /users/anonymous, ошибки глотаются)
  → GetNearbyPlacesUseCase
      → PlacesRepositoryImpl
          1) PlacesApiSource GET /api/places/in-area
          2) если пусто/ошибка → OverpassSource POST overpass-api.de
```

Файлы: `LiveMapViewModel.kt` 84–87, 409–447; `PlacesRepositoryImpl.kt` 9–27; `PlacesApiSource.kt` 17–27; `OverpassSource.kt` 25–50, 125–129.

### Туристический «прогресс» N из M

`TourSource` — in-memory список остановок (`TourSource.kt` 11–23). `bindStops` вызывается при изменении `routePlaces` или при загрузке nearby (`LiveMapViewModel.kt` 262, 418–420). Это **не** сохранённый adventure-маршрут и не кэш `/route-adventure`.

---

## C. Реальные запросы

**Runtime HTTP к `/route-adventure` и к OSRM в этой сессии не снимались.** Устройств/эмулятора нет (`adb devices` пустой список). В приложении нет `HttpLoggingInterceptor`, Chucker, Timber, `android.util.Log` сетевых вызовов.

Ниже — **реконструкция по коду**, не наблюдаемые логи.

### C1. Что уходит при «Построить» (если бы процесс был запущен)

Метод: `GET`  
Host зависит от `transportMode` (по умолчанию Walking):

```
https://routing.openstreetmap.de/routed-foot/route/v1/driving/{lon1},{lat1};{lon2},{lat2};...
  ?overview=full&geometries=geojson&steps=true&alternatives=false
```

Заголовки (`OsrmSource.kt` 57–61):

- `User-Agent: KazanWalk/1.0 (com.example.homework; Android student homework; Overpass+OSM)`
- `Accept: application/json`

Тела нет.  
Точки: `[user ?: mapCenter] + routePlaces.map { location }` (`LiveMapViewModel.kt` 311–315). Нужно ≥ 2 точки, иначе линия сбрасывается без HTTP (276–278). При одной выбранной точке старт = GPS или `KazanCenter(55.7908, 49.1144)`.

Ответ ожидается OSRM GeoJSON: `routes[0].geometry.coordinates`, `distance`, `duration`, `legs[].steps`. Маппинг: `OsrmSource.kt` 32–42, 77–110. Это **не** `RouteAdventureResponse`.

### C2. Что *было бы* отправлено в `/route-adventure`, если бы UseCase вызвали

Код существует. Из UI не вызывается. Реконструкция тела по `RouteApiSource.buildRoute` при дефолтах (UseCase передаёт только `location`):

```
POST http://192.168.3.11:5137/route-adventure
Accept: application/json
Content-Type: application/json; charset=utf-8
X-User-Id: <VALID_USER_ID>
```

```json
{
  "durationMinutes": 240,
  "interests": [
    "история",
    "татарская культура",
    "архитектура",
    "музеи",
    "мечети",
    "парки"
  ],
  "pace": "обычный",
  "userLocation": { "lat": "<clampToKazan(location.lat)>", "lon": "<clampToKazan(location.lon)>" },
  "optimizeVisitOrder": true
}
```

Не передаются (поля просто не кладутся в JSON, не `null`):

- `aiRequest`
- `startAt`
- `requiredPlaces` (пустой список → блок пропускается)
- `visitedPlaces`

`RouteRepository.buildRoute` принимает только `GeoLocation` (`RouteRepository.kt` 7), поэтому duration/interests/pace из UI изменить нельзя.

Фактический HTTP status/duration/body этого вызова **неизвестны** (вызов мёртв; заказчик уже получил 502 на аналогичном POST снаружи).

### C3. Сопутствующие Guide API-вызовы, которые UI реально делает

При старте карты (`LiveMapViewModel.kt` 84–86, 415–416):

1. `POST http://192.168.3.11:5137/users/anonymous` — тело пустое, `Content-Type` не ставится (`GuideApiClient.kt` 47–48; `AnonymousUserSource.kt` 15–16). Ответ `userId` пишется в SharedPreferences `guide_api` / `user_id`. Ошибка глотается `runCatching`.
2. `GET http://192.168.3.11:5137/api/places/in-area?topLeftLat&topLeftLon&bottomRightLat&bottomRightLon` — bbox ±0.04° вокруг `clampToKazan(location)`.

При открытии карточки: `GET /places/{placeId}/story` (не маршрут).

`GET /health` (`HealthSource.kt`) **не зарегистрирован в Koin и ниоткуда не вызывается**.

`PUT /location-ratings` — UseCase в Koin, в UI нет.

---

## D. Сравнение с React

| | Веб (из задания) | Мобильный «Построить» (код) | Мобильный мёртвый `/route-adventure` (код) |
| --- | --- | --- | --- |
| URL | предположительно тот же backend | `routing.openstreetmap.de` | `http://192.168.3.11:5137/route-adventure` |
| Method / path | `POST /route-adventure` | `GET .../route/v1/driving/...` | `POST /route-adventure` |
| `X-User-Id` | неизвестно (в задании не дан) | нет | да, UUID из prefs |
| `durationMinutes` | 180 | не применимо | 240 |
| `interests` | история, татарская культура, архитектура | не применимо | + музеи, мечети, парки |
| `pace` | обычный | не применимо | обычный |
| `aiRequest` | JSON `null` | нет | поле отсутствует |
| `requiredPlaces` | JSON `null` | нет | поле отсутствует |
| `optimizeVisitOrder` | true | нет (порядок = порядок добавления / nearest-neighbor OSRM) | true |
| `userLocation` | 55.74309562972644, 49.18193230506392 | GPS или 55.7908, 49.1144 | `clampToKazan` той же точки |
| Выбор точек | backend/LLM | пользователь вручную | backend/LLM |
| Расписание | да, в контракте API | нет | парсится частично (`AdventureRoute` без arrival/departure) |
| Обработка 502 | сообщение «Не удалось построить маршрут.» | до `/route-adventure` не доходит | `IOException(parseError)` → если бы вызвали, баннер `errorMessage` |

Различия default-полей **нельзя считать причиной 502**: заказчик уже слал «параметры мобильного клиента» напрямую и тоже получил 502 за ~45 мс.

Веб шлёт явные `null`; мобильный JSONObject их опускает. Для многих ASP.NET-моделей это эквивалентно. Не доказано как причина ошибки.

`userLocation` веба внутри `clampToKazan` (лат 55.55–56.05, лон 48.75–49.45, `Coordinates.kt` 10–18). Кламп эту точку не изменил бы.

---

## E. Кэширование

Может ли линия маршрута появиться без нового HTTP?

| Хранилище | Что хранит | Маршрут? |
| --- | --- | --- |
| Room / SQLite | нет зависимости | нет |
| DataStore | нет | нет |
| SharedPreferences `guide_api` | только `user_id` (`UserIdStore.kt` 8–19) | нет |
| SharedPreferences `osmdroid` | кэш тайлов карты (`HomeworkApp.kt` 44–56) | нет |
| `PlaceStorySource` | in-memory ConcurrentHashMap рассказов | нет |
| `TourSource` | in-memory остановки и visited ids | не геометрия |
| `LiveMapUiState.route` | текущая `RouteResult` в процессе | да, до убийства процесса |
| `LiveMapUiState.routePlaces` | выбранные точки | да, до убийства процесса |
| Mock / offline pack маршрутов | не найдено | нет |
| OkHttp cache | клиент без `Cache()` (`KoinModules.kt` 81–86) | нет |

Вывод: **предзагруженного adventure-маршрута нет.** Линия на карте — либо свежий OSRM, либо in-memory результат того же сеанса. После перезапуска приложения маршрут пустой, пока пользователь снова не добавит точки и не нажмёт «Построить».

`isBuildingRoute` + `routeJob` блокируют параллельный второй OSRM-запрос (`LiveMapViewModel.kt` 280–281), это не кэш ответа.

---

## F. Причина различий

### Подтверждено кодом

Веб вызывает AI-генерацию `POST /route-adventure` и получает 502 от backend.

Мобильная кнопка «Построить» строит **ломаную по дорогам через публичный OSRM** по уже выбранным местам. К `/route-adventure` она не ходит. Поэтому мобильный сценарий **не зависит от исправности AI-генерации**.

Этого достаточно, чтобы объяснить расхождение «на вебе 502, на телефоне маршрут рисуется», без предположения, что мобильный успешно бьёт в тот же endpoint.

### Не подтверждено

- Что `/route-adventure` когда-либо возвращал 200 именно этому APK.
- Какой APK стоит у пользователя (debug/release одинаковы по URL, но старая сборка могла ещё звать adventure).
- Точные заголовки React-клиента.
- Внутренняя причина 502 на backend (ответ за 45 мс похож на быстрый fail, не на таймаут LLM; это наблюдение заказчика, мобильный код его не объясняет).

### Недостающая информация

1. HAR/лог React: полный URL, заголовки, длительность, correlation id.
2. Логи TatarTouristGuideService на `POST /route-adventure` (исключение upstream).
3. Runtime-лог актуального Android (Logcat + OkHttp) при нажатии «Построить» — ожидается URL `routing.openstreetmap.de`, не `:5137/route-adventure`.
4. Версия установленного APK vs этот commit.

---

## G. Рекомендации для веб-клиента

Не подменять AI-генерацию OSRM: это другой продукт (линия по выбранным точкам vs культурное расписание из DataSet).

Имеет смысл проверить в React:

1. Не сравнивать UX с мобильной кнопкой «Построить»: это другой endpoint.
2. Совпадает ли `X-User-Id` с UUID из `POST /users/anonymous` (201), а не случайный id.
3. Не слать лишние `null`, если модель `additionalProperties: false` и nullable-поля на практике валятся (маловероятно как причина уже проверенного 502, но стоит унифицировать).
4. Не считать успех мобильного доказательством, что backend-генерация жива.
5. Смотреть серверные логи 502: сообщение `"Не удалось построить маршрут."` формируется backend, клиенты его только показывают (`parseError` читает JSON `message`, `GuideApiClient.kt` 110–131). Мобильный при живом вызове показал бы тот же текст в баннере.

---

## H. Сообщение backend-разработчику

`POST /route-adventure` на `http://192.168.3.11:5137` стабильно возвращает **HTTP 502** за ~45 мс с телом:

```json
{ "message": "Не удалось построить маршрут." }
```

Воспроизведение (уже сделано заказчиком, повторять не нужно):

1. `GET /health` → 200.
2. `POST /users/anonymous` → 201, взять `userId`.
3. `POST /route-adventure` с `X-User-Id` и телом веб-клиента (duration 180, 3 интереса, pace «обычный», `userLocation` 55.743…/49.181…, `aiRequest`/`requiredPlaces` null, `optimizeVisitOrder` true) → 502.
4. Тот же POST с дефолтами мобильного клиента (duration 240, 6 интересов, без null-полей) → 502.
5. Другая стартовая точка → 502.

Смежные методы живы: anonymous, health. Клиентский таймаут не виноват (45 мс ≪ 180 с у Android `GuideApiClient`).

Android **больше не вызывает этот endpoint из UI**. Рабочее построение линии идёт на `routing.openstreetmap.de`. Исправление 502 нужно вебу и будущей AI-генерации, не текущей кнопке «Построить».

Просьба: stack trace / лог операции `BuildRouteAdventure` на этих запросах, какой upstream упал (LLM, Places, внутренний роутер).

---

## Приложение 1. Цепочка «Построить» с фрагментами

`RouteBuilderPanel.kt`:

```136:137:app/src/main/java/com/example/homework/ui/feature/map/RouteBuilderPanel.kt
            PanelButton("Построить", onBuild, enabled = routePlaces.size >= 1 && transportMode.isRoutable)
            PanelButton("Оптимизировать", onOptimize, filled = false, enabled = routePlaces.size >= 2 && transportMode.isRoutable)
```

`LiveMapScreen.kt`:

```181:183:app/src/main/java/com/example/homework/ui/feature/map/LiveMapScreen.kt
                    onDurationDelta = viewModel::changeVisitDuration,
                    onTransportMode = viewModel::setTransportMode,
                    onBuild = viewModel::buildRoute,
```

`LiveMapViewModel.kt`:

```161:161:app/src/main/java/com/example/homework/ui/feature/map/LiveMapViewModel.kt
    fun buildRoute() = requestOsrmRoute(fromNavigation = false)
```

```272:315:app/src/main/java/com/example/homework/ui/feature/map/LiveMapViewModel.kt
    private fun requestOsrmRoute(fromNavigation: Boolean) {
        val mode = _state.value.transportMode
        if (!mode.isRoutable) return
        val points = buildRoutePoints(fromNavigation)
        // ...
                val result = getOsrmRoute(points, mode)
        // ...
    }

    private fun buildRoutePoints(fromNavigation: Boolean): List<GeoLocation> {
        val start = _state.value.user ?: _state.value.mapCenter
        val stops = if (fromNavigation) navigator.remainingPlaces() else _state.value.routePlaces
        return listOf(start) + stops.map { it.location }
    }
```

Конструктор ViewModel (нет adventure):

```38:52:app/src/main/java/com/example/homework/ui/feature/map/LiveMapViewModel.kt
class LiveMapViewModel(
    private val getNearbyPlaces: GetNearbyPlacesUseCase,
    // ...
    private val ensureAnonymousUser: EnsureAnonymousUserUseCase,
    private val getOsrmRoute: GetOsrmRouteUseCase,
    private val optimizeRoutePlaces: OptimizeRoutePlacesUseCase,
) : ViewModel() {
```

OSRM URL:

```129:144:app/src/main/java/com/example/homework/core/data/source/osrm/OsrmSource.kt
    private fun routeUrl(points: List<GeoLocation>, mode: TransportMode): String =
        "${baseUrl(mode)}/route/v1/driving/${coordsPath(points)}" +
            "?overview=full&geometries=geojson&steps=true&alternatives=false"
    // ...
    private fun baseUrl(mode: TransportMode): String = when (mode) {
        TransportMode.Walking -> "https://routing.openstreetmap.de/routed-foot"
        TransportMode.Cycling -> "https://routing.openstreetmap.de/routed-bike"
        TransportMode.Driving -> "https://routing.openstreetmap.de/routed-car"
        TransportMode.Transit -> error("transit не строится через OSRM")
    }
```

Мёртвый adventure:

```48:52:app/src/main/java/com/example/homework/core/data/source/route/RouteApiSource.kt
        api.postJson(
            path = "/route-adventure",
            body = body,
            headers = mapOf(GuideApiConfig.USER_ID_HEADER to userId),
        ).toAdventureRoute()
```

Ошибка Guide API (если бы вызвали): неуспешный HTTP → `IOException(parseError(payload, code))` (`GuideApiClient.kt` 91–96). ViewModel ловит любой `Exception` и пишет `e.message` в `errorMessage` (`LiveMapViewModel.kt` 300–302). Для 502 с телом `{"message":"Не удалось построить маршрут."}` баннер показал бы этот текст. **Fallback на OSRM при 502 adventure нет** — ветки не связаны. Retry баннера вызывает `retryPlaces`, не повтор маршрута (`LiveMapScreen.kt` 146–147).

---

## Приложение 2. Конфигурация backend

Единственный адрес Guide API:

```4:6:app/src/main/java/com/example/homework/core/data/source/api/GuideApiConfig.kt
object GuideApiConfig {
    const val BASE_URL = "http://192.168.3.11:5137"
    const val USER_ID_HEADER = "X-User-Id"
```

Инъекция: `KoinModules.kt` 89–98, тот же `BASE_URL`.  
`app/build.gradle.kts`: нет `productFlavors`, нет `buildConfigField`, debug/release отличаются только `optimization.enable = false` у release.  
`gradle.properties`: JVM/кэш Gradle, URL нет.  
Эмулятор vs устройство: отдельного `10.0.2.2` больше нет; LAN `192.168.3.11` общий. Cleartext разрешён (`AndroidManifest.xml` `usesCleartextTraffic=true`).  
Переключения сервера в runtime нет.

Актуальная сборка из этих исходников ходит в `http://192.168.3.11:5137` **для мест и userId**, и в `https://routing.openstreetmap.de` **для линии маршрута**.

---

## Приложение 3. Сравнение OSRM vs Route Adventure

| | OSRM (кнопка «Построить») | Route Adventure (мёртвый код) |
| --- | --- | --- |
| Назначение | Геометрия пути по выбранным точкам | Культурный пеший план из DataSet + оценки + LLM |
| Источник мест | Уже выбранные `routePlaces` (сначала `GET /api/places/in-area`, иначе Overpass) | Backend Places после планирования |
| Endpoint | `GET routing.openstreetmap.de/routed-*/route/v1/driving/...` | `POST /route-adventure` |
| Вход | координаты ≥ 2, mode walking/cycling/driving | duration, interests, pace, userLocation, optional required/visited/aiRequest/startAt |
| Ответ | GeoJSON line, distance, duration, steps | startedAt/finishedAt, минуты, places[] с расписанием и imageUrl |
| LLM | нет | да (на backend) |
| Расписание | нет | да |
| Интересы | нет (только локальный фильтр маркеров) | да |
| Построение по выбранным точкам | да | optional `requiredPlaces` |
| Работа при 502 `/route-adventure` | **да** | нет |

**Главный вопрос:** может ли мобильное приложение успешно построить и отобразить маршрут через OSRM, когда AI-генерация на backend не работает?

**Да.** Доказательство: кнопка «Построить» вызывает только `getOsrmRoute`; `BuildAdventureRouteUseCase` не входит в ViewModel; `PlacesRepositoryImpl` при падении Guide API падает на Overpass. Нужны сеть до `routing.openstreetmap.de` и хотя бы одна точка в `routePlaces`. Runtime этого сеанса не запускался; вывод по графу вызовов.

---

## Приложение 4. Параметры мобильного клиента vs веб

Актуальность «240 / 6 интересов / обычный / optimizeVisitOrder true»: **да**, `RouteApiSource.kt` 23–37, 114–122. Но этот JSON **не отправляется из UI**.

Как считалось бы при вызове:

- `durationMinutes` — константа 240, не из UI.
- `interests` — `DEFAULT_INTERESTS`, не связаны с `PlaceFilter`.
- `pace` — `"обычный"`.
- `userLocation` — аргумент UseCase; с карты это был бы GPS или `KazanCenter`. Затем `clampToKazan`.
- `clampToKazan` — `lat.coerceIn(55.55, 56.05)`, `lon.coerceIn(48.75, 49.45)`.
- `requiredPlaces` — по умолчанию empty, в JSON нет.
- `aiRequest`, `startAt` — не передаются.
- Пользователь создаётся при старте карты (`ensureAnonymousUser`), не в момент построения OSRM.

Реальные различия с веб-запросом из задания: другой endpoint у живого UI; у мёртвого кода ещё duration 240 vs 180, 6 vs 3 интересов, omit vs explicit null, другая стартовая точка. **Ни одно различие не доказано как причина 502.**

---

## Приложение 5. Runtime

Попытка: `~/Library/Android/sdk/platform-tools/adb devices` → `List of devices attached` (пусто).

Выводы **только по исходникам**. Свежая генерация OSRM в этом отчёте не прогонялась. Запросы к `/route-adventure` намеренно не повторялись.
