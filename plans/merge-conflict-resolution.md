# Merge Conflict Resolution: GameStompClientTest.kt

## File
`app/src/test/java/at/aau/monopoly/klagenfurt/GameStompClientTest.kt`

## Conflict Location
Lines 458–464 in `subscribeToGame_error` test method

## Context
- **HEAD**: Old log format (`"Subscription timed out for test-id"`) + unnecessary `disconnect()`/`runCurrent()` cleanup
- **main**: SonarQube fix format (`"Subscription timed out for test-id (attempt 2/2)"`) matching production code in `GameStompClient.kt` line 521

## Resolution
Keep `main`'s line only:
```kotlin
        verify { Log.e("GameStomp", "Subscription timed out for test-id (attempt 2/2)") }
```

Drop HEAD's old format and the unnecessary `gameStompClient.disconnect()` / `runCurrent()` calls.

## Rationale
1. Production code at `GameStompClient.kt:521` logs with `(attempt X/2)` suffix (SonarQube fix commit `81bd0fa`)
2. All other similar tests in the file do NOT include manual disconnect/runCurrent cleanup — `statusJob.cancel()` on the next line is sufficient
3. The test must verify the actual log message the production code emits
