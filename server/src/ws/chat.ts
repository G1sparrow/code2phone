import { WebSocket, WebSocketServer } from "ws";
import { IncomingMessage } from "http";
import { z } from "zod";
import { opencodeClient } from "../opencode/client";
import { logger } from "../utils/logger";
import { StreamEvent, ChatRequest } from "../opencode/types";

interface WSMessage {
  type: "init" | "user" | "ping";
  sessionId?: string;
  sessionDir?: string;
  message?: string;
  model?: string;
  agent?: "plan" | "build";
  files?: string[];
}

const initSchema = z.object({
  type: z.literal("init"),
  sessionId: z.string().optional(),
});

const userSchema = z.object({
  type: z.literal("user"),
  message: z.string().min(1),
  model: z.string().optional(),
  agent: z.enum(["plan", "build"]).optional(),
  sessionId: z.string().optional(),
  sessionDir: z.string().optional(),
  files: z.array(z.string()).optional(),
});

const pingSchema = z.object({
  type: z.literal("ping"),
});

export function setupChatWebSocket(wss: WebSocketServer): void {
  wss.on("connection", (ws: WebSocket, req: IncomingMessage) => {
    const clientIp = req.socket.remoteAddress;
    logger.info("WebSocket connected", { ip: clientIp });

    let currentSessionId: string | null = null;
    let isInitialized = false;

    const send = (data: unknown) => {
      if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify(data));
      }
    };

    const sendError = (errorMsg: string) => {
      send({
        type: "error",
        part: { type: "error", error: errorMsg },
      });
    };

    ws.on("message", (data: Buffer) => {
      try {
        const message = JSON.parse(data.toString()) as WSMessage;

        if (!isInitialized) {
          const initResult = initSchema.safeParse(message);
          if (!initResult.success) {
            sendError("First message must be init");
            ws.close(1008, "Invalid init");
            return;
          }
          isInitialized = true;
          currentSessionId = initResult.data.sessionId || null;
          send({ type: "init", timestamp: Date.now(), sessionID: currentSessionId, part: { type: "init" } });
          return;
        }

        const pingResult = pingSchema.safeParse(message);
        if (pingResult.success) {
          send({ type: "pong" });
          return;
        }

        const userResult = userSchema.safeParse(message);
        if (userResult.success) {
          const request: ChatRequest = {
            message: userResult.data.message,
            model: userResult.data.model,
            agent: userResult.data.agent,
            sessionId: currentSessionId || userResult.data.sessionId,
            sessionDir: userResult.data.sessionDir,
            files: userResult.data.files,
          };

          const handleEvent = (event: StreamEvent) => {
            send(event);
          };

          const handleError = (error: Error) => {
            logger.error("opencode stream error", { error: error.message });
            sendError(error.message);
          };

          const handleClose = () => {
            logger.info("opencode stream closed");
            opencodeClient.off("event", handleEvent);
            opencodeClient.off("error", handleError);
            opencodeClient.off("close", handleClose);
          };

          opencodeClient.on("event", handleEvent);
          opencodeClient.on("error", handleError);
          opencodeClient.on("close", handleClose);

          opencodeClient.startStreamingSession(request);
          return;
        }

        sendError("Unknown message type");
      } catch (error) {
        logger.warn("Invalid WebSocket message", { error: String(error) });
        sendError("Invalid message format");
      }
    });

    ws.on("close", (code, reason) => {
      logger.info("WebSocket disconnected", { code, reason: reason.toString() });
      opencodeClient.removeAllListeners("event");
      opencodeClient.removeAllListeners("error");
      opencodeClient.removeAllListeners("close");
      opencodeClient.stop();
    });

    ws.on("error", (error) => {
      logger.error("WebSocket error", { error: error.message });
    });

    const pingInterval = setInterval(() => {
      if (ws.readyState === WebSocket.OPEN) {
        ws.ping();
      } else {
        clearInterval(pingInterval);
      }
    }, 30000);

    ws.on("close", () => clearInterval(pingInterval));
  });
}
