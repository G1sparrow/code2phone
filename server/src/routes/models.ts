import { Router, Request, Response } from "express";
import { opencodeClient } from "../opencode/client";
import { logger } from "../utils/logger";

const router = Router();

router.get("/models", async (_req: Request, res: Response) => {
  try {
    const models = await opencodeClient.listModels();
    res.json({ models });
  } catch (error) {
    logger.error("Failed to list models", { error: String(error) });
    res.status(500).json({ error: "Failed to list models" });
  }
});

export default router;