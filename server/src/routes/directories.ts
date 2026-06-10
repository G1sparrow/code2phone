import { Router, Request, Response } from "express";
import { opencodeClient } from "../opencode/client";
import { logger } from "../utils/logger";

const router = Router();

router.get("/directories", async (_req: Request, res: Response) => {
  try {
    const sessions = await opencodeClient.listSessions();
    const dirs = [...new Set(sessions.map(s => s.directory).filter(Boolean))].sort();
    res.json({ directories: dirs });
  } catch (error) {
    logger.error("Failed to list directories", { error: String(error) });
    res.status(500).json({ error: "Failed to list directories" });
  }
});

export default router;
