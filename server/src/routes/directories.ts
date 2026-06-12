import { Router, Request, Response } from "express";
import { opencodeClient } from "../opencode/client";
import { logger } from "../utils/logger";

const router = Router();

router.get("/directories", async (_req: Request, res: Response) => {
  try {
    const [globalSessions, projectSessions] = await Promise.all([
      opencodeClient.listSessionsGlobal(),
      opencodeClient.listSessions(),
    ]);
    const dirs = [...new Set([
      ...globalSessions.map(s => s.directory).filter(Boolean),
      ...projectSessions.map(s => s.directory).filter(Boolean),
    ])].sort();
    res.json({ directories: dirs });
  } catch (error) {
    logger.error("Failed to list directories", { error: String(error) });
    res.status(500).json({ error: "Failed to list directories", detail: String(error).slice(0, 200) });
  }
});

export default router;
