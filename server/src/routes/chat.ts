import { Router, Request, Response } from "express";
import { z } from "zod";
import { logger } from "../utils/logger";

const router = Router();

const chatSchema = z.object({
  message: z.string().min(1),
  model: z.string().optional(),
  agent: z.enum(["plan", "build"]).optional(),
  sessionId: z.string().optional(),
  files: z.array(z.string()).optional(),
});

router.post("/chat", async (req: Request, res: Response) => {
  const parseResult = chatSchema.safeParse(req.body);
  if (!parseResult.success) {
    res.status(400).json({ error: "Invalid request", details: parseResult.error.flatten() });
    return;
  }

  logger.info("Chat request received (use WebSocket for streaming)", { model: parseResult.data.model });
  res.json({ message: "Use WebSocket at ws://host/api/chat for streaming chat", wsEndpoint: "/api/chat" });
});

export default router;