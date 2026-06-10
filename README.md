# Opencode2Phone

Android app for chatting with [opencode-ai](https://opencode.ai) from your phone via Tailscale. A lightweight Node.js bridge server wraps the opencode CLI and exposes it over REST + WebSocket.

## Architecture

```
┌──────────────────────┐     Tailscale      ┌──────────────────────┐
│   Android App        │◄──────────────────►│   Bridge Server      │
│   (Kotlin/Compose)   │   Tailscale IP     │   (Node.js)          │
│                      │                    │                      │
│  ┌────────────────┐  │   REST + WS        │  ┌────────────────┐  │
│  │ Jetpack Compose │  │                    │  │ Express/WS     │  │
│  │ UI             │  │                    │  │ Port 3001      │  │
│  ├────────────────┤  │                    │  ├────────────────┤  │
│  │ ViewModels     │  │                    │  │ OpencodeClient │  │
│  ├────────────────┤  │                    │  │ (spawn CLI)    │  │
│  │ Repositories   │  │                    │  ├────────────────┤  │
│  ├────────────────┤  │                    │  │ opencode-ai    │  │
│  │ Room (SQLite)  │  │                    │  │ (CLI)          │  │
│  └────────────────┘  │                    │  └────────────────┘  │
└──────────────────────┘                    └──────────────────────┘
```

- **Bridge Server**: Express + WebSocket server that spawns `opencode-ai` as a child process, parses its `--format json` stream output, and forwards events to the Android client.
- **Android App**: MVVM + Clean Architecture with Jetpack Compose, Room for local history caching, Retrofit for REST calls, OkHttp WebSocket for streaming.

## Setup

### Prerequisites

- **Windows** computer (where opencode-ai is installed)
- [opencode-ai](https://opencode.ai) installed (`npm install -g opencode-ai`)
- [Node.js](https://nodejs.org/) 18+ on the computer
- [Tailscale](https://tailscale.com/) installed on both computer and Android phone
- [Android Studio](https://developer.android.com/studio) (for building the app)

### 1. Bridge Server

```bash
cd server
npm install
npm run build
npm start
```

The server starts on `http://0.0.0.0:3001` and auto-detects the opencode CLI path.

**Environment variables:**
- `PORT` - Server port (default: 3001)
- `HOSTNAME` - Bind address (default: 0.0.0.0)
- `OPENCODE_PATH` - Path to opencode executable

**API endpoints:**

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | API info |
| `/health` | GET | Health check |
| `/api/models` | GET | List available models |
| `/api/sessions` | GET | List chat sessions |
| `/api/sessions/:id` | GET | Session details + messages |
| `/api/chat` | POST | Redirects to WebSocket |
| `/api/chat` | WS | Streaming chat (WebSocket) |

### 2. Android App

Open `android/` in Android Studio, sync Gradle, and build.

**Key gradle dependencies:**
- Kotlin 1.9.22, AGP 8.2.2, Compose BOM 2024.02
- Room 2.6.1, Hilt 2.50, Retrofit 2.9, OkHttp 4.12, Moshi 1.15

### 3. Connect

1. Make sure both devices are on your Tailscale network
2. Start the bridge server on your computer
3. On your Android phone, find your computer's Tailscale IP (`tailscale status`)
4. Open app Settings → enter Tailscale IP:3001 → tap "Test Connection"
5. Start chatting

## Project Structure

```
opencode2phone/
├── server/                          # Node.js bridge server
│   └── src/
│       ├── config.ts                # Server configuration
│       ├── index.ts                 # Express + WebSocket setup
│       ├── routes/
│       │   ├── health.ts            # GET /health
│       │   ├── models.ts            # GET /api/models
│       │   ├── sessions.ts          # GET /api/sessions, GET /api/sessions/:id
│       │   └── chat.ts              # POST /api/chat
│       ├── ws/
│       │   └── chat.ts              # WS /api/chat (streaming)
│       ├── opencode/
│       │   ├── client.ts            # Opencode CLI wrapper (spawn)
│       │   └── types.ts             # TypeScript interfaces
│       └── utils/
│           └── logger.ts            # Logging utility
│
└── android/                         # Android app
    └── app/src/main/java/com/opencode2phone/
        ├── OpencodeApp.kt           # Hilt Application
        ├── MainActivity.kt          # Main activity
        ├── di/
        │   ├── AppModule.kt         # Hilt DI module
        │   ├── ServerConfig.kt      # Server address (SharedPreferences)
        │   └── DynamicBaseUrlInterceptor.kt
        ├── data/
        │   ├── local/               # Room database
        │   │   ├── AppDatabase.kt
        │   │   ├── dao/             # SessionDao, MessageDao
        │   │   └── entity/          # SessionEntity, MessageEntity
        │   ├── remote/              # Network layer
        │   │   ├── OpencodeApi.kt   # Retrofit interface
        │   │   ├── OpencodeWebSocket.kt
        │   │   └── dto/             # ModelDto, SessionDto, StreamEventDto
        │   └── repository/          # SessionRepository, ModelRepository, ChatRepository
        ├── domain/model/            # Session, Message, OpencodeModel
        └── ui/
            ├── navigation/NavGraph.kt
            ├── theme/               # Color, Type, Theme
            ├── home/                # HomeScreen, HomeViewModel
            ├── chat/                # ChatScreen, ChatViewModel
            └── settings/            # SettingsScreen, SettingsViewModel
```

## Security

- **No app-level authentication**: The app trusts the Tailscale network. Only devices on your Tailscale network can reach the server.
- **Cleartext traffic**: The bridge server uses plain HTTP. Tailscale encrypts traffic at the network layer.
- `android:usesCleartextTraffic="true"` is enabled in the manifest to allow HTTP connections over Tailscale.

## Development

```bash
# Run server in dev mode (auto-reload)
cd server
npm run dev

# Build server
npm run build
```
