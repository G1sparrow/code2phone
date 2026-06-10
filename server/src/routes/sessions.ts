import { Router, Request, Response } from "express";
import { opencodeClient } from "../opencode/client";
import { logger } from "../utils/logger";

const router = Router();

router.get("/sessions", async (req: Request, res: Response) => {
  try {
    let sessions = await opencodeClient.listSessions();
    const directory = req.query.directory as string | undefined;
    if (directory) {
      sessions = sessions.filter(s => s.directory === directory);
    }
    res.json({ sessions });
  } catch (error) {
    logger.error("Failed to list sessions", { error: String(error) });
    res.status(500).json({ error: "Failed to list sessions" });
  }
});

router.get("/sessions/:id", async (req: Request, res: Response) => {
  try {
    const raw = await opencodeClient.exportSession(req.params.id);

    const session = {
      id: raw.info.id,
      title: raw.info.title,
      created: raw.info.time.created,
      updated: raw.info.time.updated,
      projectId: raw.info.projectID,
      directory: raw.info.directory,
      messageCount: raw.messages?.length ?? 0,
    };

    const messages = (raw.messages || []).map((msg: any) => {
      const textContent = (msg.parts || [])
        .filter((p: any) => p.type === "text")
        .map((p: any) => p.text)
        .join("");

      return {
        id: msg.info.id,
        role: msg.info.role,
        content: textContent,
        createdAt: msg.info.time.created,
      };
    });

    res.json({ session, messages });
  } catch (error) {
    logger.error("Failed to export session", { error: String(error), id: req.params.id });
    if (String(error).includes("not found")) {
      res.status(404).json({ error: "Session not found" });
    } else {
      res.status(500).json({ error: "Failed to export session" });
    }
  }
});

export default router;
