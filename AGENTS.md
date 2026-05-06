# AGENTS.md – Coding-Agent Guide

> **Project:** AAU SE2 – Monopoly Klagenfurt (Android Frontend)
> **Course:** 621.250 (25S) Software Engineering II
> **Language / Platform:** Kotlin · Android (minSdk 30, targetSdk 36) · Jetpack Compose

---

## 1. Repository Overview

This is an Android client application that communicates with a Spring Boot backend via **WebSockets / STOMP** using the [Krossbow](https://github.com/joffrey-bion/krossbow) library. The app implements a **Monopoly-style board game** client.

```
app/src/main/java/at/aau/monopoly/klagenfurt/
├── CreditsActivity.kt
├── JoinActivity.kt
├── LobbyActivity.kt                    # Launcher Activity (lobby → join → gameboard)
├── MainMenuActivity.kt
├── ServiceLocator.kt                   # Simple DI / service registry
├── SettingsActivity.kt
├── messaging/
│   ├── GameAction.kt                   # Data class sent TO the server
│   └── GameEvent.kt                    # Data class received FROM the server
├── model/
│   ├── DiceRoll.kt                     # die1, die2, total, isDouble
│   ├── GameState.kt                    # Full game state (fields, players, phase, …)
│   ├── Player.kt                       # Player state + isBankrupt()
│   ├── card/
│   │   ├── Card.kt                     # Abstract base for deck cards
│   │   ├── ChanceCard.kt
│   │   └── CommunityChestCard.kt
│   ├── enums/
│   │   ├── CardAction.kt
│   │   ├── FieldType.kt
│   │   ├── GamePhase.kt
│   │   └── PropertyColor.kt
│   └── field/
│       ├── Field.kt                    # Abstract base + OwnableField interface
│       ├── PropertyField.kt            # implements OwnableField
│       ├── RailroadField.kt            # implements OwnableField
│       ├── UtilityField.kt             # implements OwnableField
│       ├── TaxField.kt
│       ├── ChanceField.kt
│       ├── CommunityChestField.kt
│       ├── GoField.kt
│       ├── GoToJailField.kt
│       ├── JailField.kt
│       └── FreeParkingField.kt
├── networking/
│   ├── GameService.kt                  # High-level game service (flows + actions)
│   ├── GameStompClient.kt              # STOMP session management
│   ├── JacksonProvider.kt              # Shared ObjectMapper
│   └── ServerConfig.kt                 # WebSocket URI configuration
├── sensors/                            # Device sensor integrations
└── ui/
    ├── GameViewModel.kt                # Primary game ViewModel
    ├── LobbyViewModel.kt               # Lobby ViewModel
    ├── GameServiceViewModelFactory.kt  # Generic reusable ViewModel factory
    ├── GameboardUI.kt                  # Gameboard orchestrator composable
    ├── FieldCardUI.kt                  # Field detail card (title deed style)
    ├── PlayerInfoPanel.kt              # Player info sidebar
    ├── PlayerPropertyOverlayUI.kt      # Player property overlay
    ├── CardUI.kt                       # Chance / Community Chest card UI
    ├── DiceRollOverlay.kt              # Dice roll animation overlay
    ├── PropertyCardUI.kt               # Property card composable
    ├── board/
    │   ├── BoardFieldRendering.kt      # FieldItem, FieldBounds, PropertyColorBar, etc.
    │   └── PlayerTokens.kt            # Player token rendering
    ├── chat/                           # In-game chat UI components
    ├── components/                     # Shared UI components
    ├── zoom/
    │   └── ZoomableWrapper.kt          # ZoomState + ZoomableWrapper composable
    ├── util/
    │   ├── GameEventParser.kt          # parseGameEvent(json): GameEvent?
    │   └── UiMappers.kt               # getPlayerTokenResource(), PropertyColor.toComposeColor()
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

---

## 2. Tech Stack & Key Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| `org.hildan.krossbow:krossbow-stomp-core` | 9.3.0 | STOMP protocol over WebSocket |
| `org.hildan.krossbow:krossbow-websocket-okhttp` | 9.3.0 | OkHttp WebSocket transport |
| `org.hildan.krossbow:krossbow-websocket-builtin` | 9.3.0 | Built-in WebSocket transport |
| `com.fasterxml.jackson` | — | JSON serialization (Jackson) |
| `androidx.compose` BOM | 2026.03.00 | Jetpack Compose UI toolkit |
| `androidx.activity:activity-compose` | 1.13.0 | Compose entry point |
| `org.junit.jupiter` | 6.0.3 | Unit testing (JUnit 5) |
| Kotlin | 2.3.10 | Primary language |
| AGP | 8.13.2 | Android Gradle Plugin |

Build tool: **Gradle (Kotlin DSL)** – `build.gradle.kts`

---

## 3. Architecture

### 3.1 Communication

```
LobbyActivity ──► GameService ──► GameStompClient ──STOMP/WS──► Backend (ws://10.0.2.2:8080/ws)
     ▲                  │
     │ flows            │  subscribeText("/topic/game/{gameId}")
     └──────────────────┘  sendText("/app/game/{create|join|start|action|state}")
```

- **`GameService`** exposes `SharedFlow<String>` for events/status; ViewModels observe these.
- **`GameStompClient`** manages a single `StompSession` on `Dispatchers.IO`.
- JSON is parsed via Jackson (`JacksonProvider.objectMapper`).
- A shared `parseGameEvent()` utility in `ui/util/GameEventParser.kt` avoids duplicated parsing.

### 3.2 OwnableField Interface

`PropertyField`, `RailroadField`, and `UtilityField` implement `OwnableField`:
```kotlin
interface OwnableField {
    val ownerId: String?
    val price: Int
    val isMortgaged: Boolean
}
```
Use `field is OwnableField` instead of multi-branch `when` for ownership checks.

### 3.3 Message Protocol

**Outgoing – `GameAction` (JSON)**
```json
{
  "gameId":   "<uuid>",
  "playerId": "<uuid>",
  "action":   "ROLL_DICE | END_TURN | ...",
  "payload":  { "name": "Alice" }
}
```

**Incoming – `GameEvent` (JSON)**
```json
{
  "gameId":    "<uuid>",
  "event":     "GAME_CREATED | GAME_STARTED | DICE_ROLLED | ...",
  "gameState": { ... },
  "message":   "optional human-readable string"
}
```

### 3.4 Game Phases (`GamePhase`)

`WAITING → ROLLING → BUYING / AUCTIONING → TURN_END → (loop) → FINISHED`

---

## 4. STOMP Endpoints

| Direction | Destination | Triggered by |
|---|---|---|
| Send | `/app/game/create` | `GameService.createGame(playerName)` |
| Send | `/app/game/join` | `GameService.joinGame(gameId, playerName)` |
| Send | `/app/game/start` | `GameService.startGame()` |
| Send | `/app/game/action` | `rollDice()` / `endTurn()` |
| Send | `/app/game/state` | `GameService.requestState()` |
| Subscribe | `/topic/game/{gameId}` | auto on join/create |

> **Note:** The emulator backend address is `ws://10.0.2.2:8080/ws`. For a real device, replace `10.0.2.2` with the host machine's LAN IP.

---

## 5. Data Model Reference

### `Player`
| Field | Type | Default |
|---|---|---|
| `id` | `String` | required |
| `name` | `String` | required |
| `position` | `Int` | `0` |
| `money` | `Int` | `1500` |
| `inJail` | `Boolean` | `false` |
| `jailTurns` | `Int` | `0` |
| `getOutOfJailCards` | `Int` | `0` |
| `ownedPropertyIds` | `MutableList<Int>` | `[]` |

### `DiceRoll`
| Field | Type | Notes |
|---|---|---|
| `die1` | `Int` | 1–6 |
| `die2` | `Int` | 1–6 |
| `total` | `Int` | computed |
| `isDouble` | `Boolean` | computed |

### `OwnableField` (interface)
Implemented by `PropertyField`, `RailroadField`, `UtilityField`.
| Field | Type |
|---|---|
| `ownerId` | `String?` |
| `price` | `Int` |
| `isMortgaged` | `Boolean` |

### `PropertyField`
Extends `Field`, implements `OwnableField`. Additional: `color` (`PropertyColor`), `rent: List<Int>`, `houses`, `hasHotel`, `houseCost`, `hotelCost`.

---

## 6. Build & Run

### Requirements
- Android Studio Meerkat (or later)
- JDK 17
- Android SDK 36 (API level 36.1)
- A running backend on `localhost:8080` (or adjust `ServerConfig.kt`)

### Build
```bash
./gradlew assembleDebug
```

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Coverage Report (JaCoCo)
```bash
./gradlew jacocoTestReport
# Output: app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml
```

### SonarCloud Analysis
```bash
./gradlew sonar \
  -Dsonar.login=<token>
# Project key: AAU-SE2-Monopoly_aau-se2-frontend
# Organisation:  aau-se2-monopoly
```

---

## 7. Coding Guidelines for Agents

1. **Kotlin only.** Do not add Java source files.
2. **Coroutines for async work.** All STOMP calls go through `Dispatchers.IO`. Never block the main thread.
3. **JSON serialisation** uses Jackson via `JacksonProvider.objectMapper`. Use the shared `parseGameEvent()` utility for event parsing.
4. **Package convention:** `at.aau.monopoly.klagenfurt.<subpackage>`. New classes must follow this scheme.
5. **OwnableField:** Use `field is OwnableField` for ownership/price/mortgage checks. Do not duplicate `when` branches per field type.
6. **ViewModel factories:** Use `GameServiceViewModelFactory<T>` for any ViewModel that takes a `GameService`. Do not duplicate `Factory` inner classes.
7. **UI structure:** `GameboardUI.kt` is the orchestrator. Field rendering goes in `ui/board/`, zoom in `ui/zoom/`, shared mappers in `ui/util/`.
8. **No hardcoded test data in production UI.** Preview data belongs in `@Preview` functions only.
9. **Tests:** Use JUnit 5 (`@Test` from `org.junit.jupiter.api`). Place unit tests under `app/src/test/java/`.
10. **No hardcoded secrets.** WebSocket URI lives in `ServerConfig.kt`.
11. **Min SDK is 30.** Do not use APIs that require a higher SDK level without a runtime check.
12. **LobbyActivity** is the launcher. `GameActivity` has been removed (was dead code).

---

## 8. CI / Quality Gates

- **SonarCloud:** Code quality and coverage are reported to `https://sonarcloud.io` (org `aau-se2`).
- **JaCoCo XML** coverage report is required by SonarCloud (`sonar.coverage.jacoco.xmlReportPaths`).
- The `jacocoTestReport` task is automatically finalised after `testDebugUnitTest`.

---

## 9. File Exclusions (JaCoCo / SonarCloud)

The following patterns are excluded from coverage analysis:
- `**/R.class`, `**/R$*.class`
- `**/BuildConfig.*`
- `**/Manifest*.*`
- `**/*Test*.*`
- `android/**/*.*`
