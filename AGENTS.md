# AGENTS.md – Coding-Agent Guide

> **Project:** AAU SE2 – WebSocket Broker Demo (Android Frontend)
> **Course:** 621.250 (25S) Software Engineering II
> **Language / Platform:** Kotlin · Android (minSdk 30, targetSdk 36) · Jetpack Compose + View-based UI

---

## 1. Repository Overview

This is an Android client application that communicates with a Spring Boot backend via **WebSockets / STOMP** using the [Krossbow](https://github.com/joffrey-bion/krossbow) library. The app implements a simplified **Monopoly-style board game** debug client.

```
app/src/main/java/
├── MyStomp.kt                          # Legacy demo STOMP client (hello / JSON topics)
└── at/aau/serg/websocketbrokerdemo/
    ├── Callbacks.kt                    # Interface: onResponse(String)
    ├── GameCallbacks.kt                # Interface: onStatus / onGameEvent
    ├── GameStompClient.kt              # Primary STOMP client for game actions
    ├── GameActivity.kt                 # Main launcher Activity (Monopoly debug UI)
    ├── MainActivity.kt                 # Legacy demo Activity
    ├── messaging/
    │   ├── GameAction.kt               # Data class sent TO the server
    │   └── GameEvent.kt                # Data class received FROM the server
    ├── model/
    │   ├── DiceRoll.kt                 # die1, die2, total, isDouble
    │   ├── GameState.kt                # Full game state (fields, players, phase, …)
    │   ├── Player.kt                   # Player state + isBankrupt()
    │   ├── card/
    │   │   ├── Card.kt                 # Abstract base for deck cards
    │   │   ├── ChanceCard.kt
    │   │   └── CommunityChestCard.kt
    │   ├── enums/
    │   │   ├── CardAction.kt           # COLLECT_MONEY, PAY_MONEY, MOVE_TO, …
    │   │   ├── FieldType.kt            # GO, PROPERTY, TAX, RAILROAD, CHANCE, …
    │   │   ├── GamePhase.kt            # WAITING, ROLLING, BUYING, …, FINISHED
    │   │   └── PropertyColor.kt        # BROWN, LIGHT_BLUE, …, DARK_BLUE
    │   └── field/
    │       ├── Field.kt                # Abstract base for board fields
    │       ├── PropertyField.kt        # id, color, price, rent[], houses, hotel
    │       ├── RailroadField.kt
    │       ├── UtilityField.kt
    │       ├── TaxField.kt
    │       ├── ChanceField.kt
    │       ├── CommunityChestField.kt
    │       ├── GoField.kt
    │       ├── GoToJailField.kt
    │       ├── JailField.kt
    │       └── FreeParkingField.kt
    └── ui/theme/
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
| `androidx.compose` BOM | 2026.03.00 | Jetpack Compose UI toolkit |
| `androidx.activity:activity-compose` | 1.13.0 | Compose entry point |
| `androidx.constraintlayout` | 2.2.1 | Legacy XML layout support |
| `org.junit.jupiter` | 6.0.3 | Unit testing (JUnit 5) |
| Kotlin | 2.3.10 | Primary language |
| AGP | 8.13.2 | Android Gradle Plugin |

Build tool: **Gradle (Kotlin DSL)** – `build.gradle.kts`

---

## 3. Architecture

### 3.1 STOMP Communication

```
GameActivity  ──uses──►  GameStompClient  ──STOMP/WS──►  Backend (ws://10.0.2.2:8080/ws)
     ▲                          │
     │ GameCallbacks             │  subscribeText("/topic/game/{gameId}")
     └──────────────────────────┘  sendText("/app/game/{create|join|start|action|state}")
```

- **`GameStompClient`** manages a single `StompSession` on a `CoroutineScope(Dispatchers.IO)`.
- All callbacks are posted back to the main thread via `Handler(Looper.getMainLooper())`.
- A unique `currentPlayerId` (UUID) is generated at `GameStompClient` instantiation time.

### 3.2 Message Protocol

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

When a `GAME_CREATED` event is received, `GameActivity` automatically fills the game-ID field and calls `stomp.setGameId(gameId)`.

### 3.3 Game Phases (`GamePhase`)

`WAITING → ROLLING → BUYING / AUCTIONING → TURN_END → (loop) → FINISHED`

---

## 4. STOMP Endpoints

| Direction | Destination | Triggered by |
|---|---|---|
| Send | `/app/game/create` | `GameStompClient.createGame(playerName)` |
| Send | `/app/game/join` | `GameStompClient.joinGame(gameId, playerName)` |
| Send | `/app/game/start` | `GameStompClient.startGame()` |
| Send | `/app/game/action` | `rollDice()` / `endTurn()` |
| Send | `/app/game/state` | `GameStompClient.requestState()` |
| Subscribe | `/topic/game/{gameId}` | `GameStompClient.subscribeToGame(gameId)` |

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

### `PropertyField`
Extends `Field`. Notable fields: `color` (`PropertyColor`), `price`, `rent: List<Int>` (index 0 = no houses … 5 = hotel), `houses`, `hasHotel`, `isMortgaged`, `ownerId`.

---

## 6. Build & Run

### Requirements
- Android Studio Meerkat (or later)
- JDK 17
- Android SDK 36 (API level 36.1)
- A running backend on `localhost:8080` (or adjust `WEBSOCKET_URI` in `GameStompClient.kt`)

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
# Project key: AAU-SE2_WebSocketBrokerDemo-App
# Organisation:  aau-se2
```

---

## 7. Coding Guidelines for Agents

1. **Kotlin only.** Do not add Java source files.
2. **Coroutines for async work.** All STOMP calls go through `CoroutineScope(Dispatchers.IO)`. Never block the main thread.
3. **Post to main thread** via `Handler(Looper.getMainLooper()).post { … }` (see `GameStompClient.postToMain`).
4. **JSON serialisation** is done manually with `org.json.JSONObject` / `org.json.JSONArray`. Do not add a separate serialisation library without team approval.
5. **Package convention:** `at.aau.serg.websocketbrokerdemo.<subpackage>`. New classes must follow this scheme.
6. **Models mirror the backend.** Package names inside model files intentionally use `at.aau.serg.websocketdemoserver.model.*` to stay in sync with the backend. Do not rename them without updating the backend simultaneously.
7. **UI:** `GameActivity` is the launcher Activity. `MainActivity` is a legacy demo and should not be extended.
8. **Tests:** Use JUnit 5 (`@Test` from `org.junit.jupiter.api`). Place unit tests under `app/src/test/java/`.
9. **No hardcoded secrets.** Keep `WEBSOCKET_URI` values in the respective client files; externalise to `local.properties` / build config if needed.
10. **Min SDK is 30.** Do not use APIs that require a higher SDK level without a runtime check.

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

