import { Router, Request, Response } from "express";
import { logger } from "../utils/logger";

const router = Router();

router.get("/health", (_req: Request, res: Response) => {
  res.json({ 
    status: "ok", 
    timestamp: Date.now(),
    service: "opencode-bridge"
  });
});

router.get("/ready", (_req: Request, res: Response) => {
  res.json({ ready: true });
});

export default router;