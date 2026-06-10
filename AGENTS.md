# Opencode2Phone — AGENTS.md

## Project structure

```
opencode2phone/
├── server/          Node.js bridge (Express + WS)
└── android/         Android app (Kotlin/Compose, MVVM + Clean Architecture)
```

## Server (`server/`)

### Commands
- `npm run dev` — hot-reload dev mode (uses `tsx watch`)
- `npm run build` — TypeScript compile (`tsc`)
- `npm start` — run compiled JS from `dist/`

### Critical gotchas
- **Stdin must be `"ignore"`** when spawning opencode CLI (`client.ts`). `opencode run` hangs if stdin is a `"pipe"` (detects non-TTY and waits). Use `stdio: ["ignore", "pipe", "pipe"]`.
- **opencode path**: Auto-detected from `$env:OPENCODE_PATH`, then hardcoded fallback (`C:\Users\GGBond\AppData\Roaming\npm\node_modules\opencode-ai\bin\opencode.exe`), then `"opencode-ai"`.
- **`shell: true`** is required on Windows to resolve PATH-based commands. Used in `createSpawnOptions()`.
- **WS protocol**: First message must be `{"type":"init"}`. Server responds `{"type":"init","sessionID":"..."}`. User messages are `{"type":"user","message":"..."}`. Stream events are emitted as flat JSON (one per line), NOT wrapped.
- **opencode `--format json`** outputs newline-delimited JSON events: `step_start`, `text`, `reasoning`, `tool_call`, `tool_result`, `step_finish`, `error`. Each event has `type`, `timestamp`, `sessionID`, `part`.
- **Port 3001** may be in use from stale processes. Kill with `netstat -ano | findstr :3001` then `taskkill /PID <pid> /F`.

### Endpoints
| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | API info |
| GET | `/api/models` | List models (via `opencode-ai models`) |
| GET | `/api/sessions` | List sessions (supports `?directory=xxx`) |
| GET | `/api/sessions/:id` | Session detail + messages |
| GET | `/api/directories` | Unique project directories from sessions |
| WS | `/api/chat` | Streaming chat |

### Data flow
1. Phone connects via WebSocket → sends `{"type":"init"}`
2. Server spawns `opencode-ai run "<message>" --format json`
3. stdout parsed line-by-line as JSON events → forwarded to WS client
4. On `step_finish` or `error` → process exits, WS connection remains open for next message
5. Each WS message spawns a new opencode process (opencode singleton manages one at a time)

## Android (`android/`)

### Build
- `gradlew assembleDebug` — requires `ANDROID_HOME` pointing to SDK (currently `E:\project\222\Sdk`)
- Uses `local.properties` or env var for SDK path
- APK output: `app/build/outputs/apk/debug/app-debug.apk`

### Dependencies
- Kotlin 1.9.22, AGP 8.2.2, Compose BOM 2024.02, Gradle 8.5
- Room 2.6.1 (KSP), Hilt 2.50 (KSP), Retrofit 2.9 + Moshi 1.15 (KSP codegen), OkHttp 4.12

### Architecture
- **MVVM + Clean Architecture**: `data/` → `domain/` → `ui/`
- **DI**: Hilt (`@HiltAndroidApp`, `@HiltViewModel`, `@Singleton`)
- **Dynamic base URL**: `DynamicBaseUrlInterceptor` intercepts Retrofit requests and rewrites host/port from `ServerConfig` (SharedPreferences-backed)
- **WebSocket**: `OpencodeWebSocket` wraps OkHttp WS in a `callbackFlow`. Sends init message on `onOpen`, sends first user message inside `onMessage` after receiving init response (avoids race condition).
- **Room**: Caches sessions/messages locally. `SessionDao` and `MessageDao` with `Flow`-based observation.

### UI navigation
- `HomeScreen` → folder picker → filtered session list (per directory)
- `ChatScreen` → message bubbles + streaming input + model selector (dropdown in top bar menu)
- `SettingsScreen` → host/port config + connection test + model list display

### Key screens
- **HomeScreen**: Shows directory picker initially. After selecting a folder, shows sessions filtered by that directory. Folder name displayed in top bar.
- **ChatScreen**: Message bubbles (user right-aligned, assistant left-aligned). Streaming content shown as animated bubble. Model picker in overflow menu. Stop button replaces send during streaming.
- **SettingsScreen**: Configure Tailscale IP + port. Test connection loads models.

### Known quirks
- `ArrowBack` and `Send` icons should use `Icons.AutoMirrored.Filled.*` (not `Icons.Default.*`)
- `enableEdgeToEdge()` requires `.navigationBarsPadding()` on content containers
- Moshi codegen via KSP (`@JsonClass(generateAdapter = true)`)
- `android:usesCleartextTraffic="true"` in manifest (HTTP over Tailscale)
