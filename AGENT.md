# Озвучка описания точки: `POST /speech/synthesize`

AI-гид на карте озвучивает **описание места** (`OsmPlace.description`). Кнопка **«Слушать AI-гид»** открывает `AudioGuideContent` и отправляет этот текст в TTS бэкенда. Это не отдельный рассказ и не story API.

База: значение `GuideApiConfig.BASE_URL` (сейчас `http://192.168.0.101:5137`)  
Swagger: `{BASE_URL}/swagger/index.html`

```text
точка на карте           ->  OsmPlace.description
кнопка «Слушать AI-гид»  ->  LiveMapViewModel.openGuide()
POST /speech/synthesize  ->  WAV (audio/wav)
MediaPlayer              ->  экран AudioGuideContent
```

## Что отправлять

Только поле `description` выбранной точки. Источники описания:

| Откуда точка | Откуда берётся `description` |
| --- | --- |
| Маршрут / DataSet / `POST /route-adventure` | Поле `description` в ответе, мапится в `OsmPlace.description` |
| Историческое место | После `GET /historical-places/{placeId}` story кладётся в `OsmPlace.description` |
| Демо-точки / точки у коридора маршрута | Уже заполненный `description`, иначе короткий mock (`withMockSightDescription`) |

На экране гида показывается тот же текст: `guideNarration.text` == `place.description`.

## Чего не использовать

- **`GET /places/{placeId}/story`** — не вызывать и не подключать к гиду, карточке и озвучке.
- **`POST /voice-reading`** — старый TTS, заменён на `/speech/synthesize`.
- Не выдумывать текст гида на клиенте, если `description` пустой: синтез должен упасть с понятной ошибкой.

## Запрос `SpeechSynthesisRequest`

`POST /speech/synthesize`

`additionalProperties` запрещены. Язык, голос и прочие поля не отправлять.

| Поле | Тип | Обязательно | Описание |
| --- | --- | --- | --- |
| `text` | string | да | Первые 1000 символов `description`. Пустая строка недопустима. |

Заголовки:

| Заголовок | Обязателен | Описание |
| --- | --- | --- |
| `Content-Type` | да | `application/json` |
| `Accept` | да | `audio/wav` |

`X-User-Id` для этого эндпоинта не нужен.

### Пример запроса

```json
{
  "text": "Сәлам, Казан. Мин сезнең шәһәрдә иң мәдәни урыннарны карарга телим."
}
```

## Ответ

| HTTP | Тело |
| --- | --- |
| 200 | Бинарный WAV (`audio/wav`). PCM, mono. Swagger UI предлагает сохранить файл. |
| 400, 429, 502, 504 | JSON `{ "message": "..." }` — публичный текст без внутренних деталей. |

Клиент читает успешный ответ как `ByteArray` (`GuideApiClient.postBytes`) и отдаёт в `MediaPlayer`.

## Поток в приложении

1. Пользователь открывает карточку места → выбирает **«Слушать AI-гид»**.
2. `PlaceDetailsBottomSheet` вызывает `LiveMapViewModel.openGuide()`.
3. Открывается `AudioGuideContent`, `isPreparingGuideAudio = true` (спиннер на play, строка «Готовим озвучку…»).
4. `GetAiGuideUseCase.prepareAudio(place)` → `AiGuideRepositoryImpl`:
   - текст = `place.description.trim()`;
   - если тот же `placeId` + тот же текст уже в плеере — повторный запрос не шлётся;
   - иначе `VoiceReadingSource.synthesize(text)` → `POST /speech/synthesize`.
5. WAV пишется в cache (`speech-synthesize.wav`), `AiGuideSource` готовит `MediaPlayer` и запускает воспроизведение.
6. Ошибка синтеза попадает в `LiveMapUiState.errorMessage` (`guide_audio_error` или `message` с бэкенда).

Тот же `playPlaceNarration` используется при переходе к следующей точке гида и при включённом AI-гиде в навигации.

## Клиент в коде

| Слой | Класс |
| --- | --- |
| UI | `AudioGuideContent`, `PlaceDetailsBottomSheet` (кнопка `place_listen_guide`) |
| VM | `LiveMapViewModel.openGuide()` / `playPlaceNarration()` |
| Use case | `GetAiGuideUseCase.prepareAudio` |
| Domain text | `AiGuideSource.getNarration` ← только `OsmPlace.description` |
| HTTP | `VoiceReadingSource` → `GuideApiClient.postBytes("/speech/synthesize")` |
| Плеер | `AiGuideSource.prepareAudio(bytes)` |

Таймаут HTTP-клиента гида: connect 10s, read/call 180s (`KoinModules`). Синтез длинного описания может занять десятки секунд.

## Ограничения

- На синтез уходят только первые **1000** символов `description` (`VoiceReadingSource.MAX_TEXT_LENGTH`). Полный текст на экране гида не обрезается.
- Кнопка play во время синтеза заблокирована.
- Без готового WAV `togglePlayback()` ничего не делает (нет «фейкового» прогресса).
- Повторное открытие той же точки с тем же описанием переиспользует уже синтезированный файл.

# Summary маршрута: `POST /route-adventure`

После успешного `POST /route-adventure` и `POST /route-adventure/rebuild` бэкенд возвращает готовый вступительный рассказ:

```json
{ "summary": "..." }
```

Клиент кладёт его в `AdventureRoute.summary` (`RouteApiSource`) и показывает popup на карте (`RouteSummaryOverlay`). Текст не генерируется на клиенте.

## Правила UI

- Popup внутри области карты, слева снизу, над панелью маршрута / HUD навигации.
- Пустой `summary` → popup и TTS нет.
- На фильтре «История» popup скрывается, `summary` и аудио не сбрасываются.
- Закрытие popup не удаляет маршрут и не стирает `summary`; маленькая кнопка «Рассказ» открывает его снова без нового TTS.
- Полный текст можно раскрыть («Читать полностью») с ограничением высоты.

## TTS summary

- Сразу после получения маршрута идёт фоновый `POST /speech/synthesize`, автоплея нет.
- Пользователь сам нажимает «Прослушать рассказ».
- Текст длиннее 1000 символов режется по предложениям, максимум 4 фрагмента, WAV склеивается.
- Новый запрос только если изменился сам `summary` (rebuild). Старый синтез отменяется, плеер сбрасывается.
- Ошибка TTS не ломает маршрут: текст остаётся, кнопка «Повторить».
