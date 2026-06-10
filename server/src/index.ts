import express from "express";
import { createServer } from "http";
import cors from "cors";
import { WebSocketServer } from "ws";
import { getConfig } from "./config";
import { logger } from "./utils/logger";
import healthRouter from "./routes/health";
import modelsRouter from "./routes/models";
import sessionsRouter from "./routes/sessions";
import directoriesRouter from "./routes/directories";
import chatRouter from "./routes/chat";
import { setupChatWebSocket } from "./ws/chat";

const app = express();
const config = getConfig();

app.use(cors({ origin: config.corsOrigins }));
app.use(express.json({ limit: "10mb" }));
app.use(express.urlencoded({ extended: true }));

app.use("/health", healthRouter);
app.use("/api", modelsRouter);
app.use("/api", sessionsRouter);
app.use("/api", directoriesRouter);
app.use("/api", chatRouter);

app.get("/", (_req, res) => {
  res.json({
    name: "opencode-bridge",
    version: "1.0.0",
    endpoints: {
      health: "/health",
      models: "/api/models",
      sessions: "/api/sessions",
      directories: "/api/directories",
      chat: "/api/chat",
      websocket: "/api/chat/:sessionId",
    },
  });
});

const server = createServer(app);

const wss = new WebSocketServer({ server, path: "/api/chat" });
setupChatWebSocket(wss);

server.listen(config.port, config.hostname, () => {
  logger.info("Server started", {
    port: config.port,
    hostname: config.hostname,
    opencodePath: config.opencodePath,
  });
  console.log(`
╔══════════════════════════════════════════════════════════════╗
║  opencode-bridge server                                      ║
║  HTTP:  http://${config.hostname}:${config.port}                                    ║
║  WS:    ws://${config.hostname}:${config.port}/api/chat                           ║
║                                                              ║
║  Endpoints:                                                  ║
║  - GET  /health                                              ║
║  - GET  /api/models                                          ║
║  - GET  /api/sessions                                        ║
║  - GET  /api/sessions/:id                                    ║
║  - GET  /api/directories                                     ║
║  - POST /api/chat                                            ║
║  - WS   /api/chat (streaming)                                ║
╚══════════════════════════════════════════════════════════════╝
  `);
});

process.on("SIGTERM", () => {
  logger.info("SIGTERM received, shutting down...");
  server.close(() => {
    logger.info("Server closed");
    process.exit(0);
  });
});

process.on("SIGINT", () => {
  logger.info("SIGINT received, shutting down...");
  server.close(() => {
    logger.info("Server closed");
    process.exit(0);
  });
});