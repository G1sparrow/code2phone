import { Router, Request, Response } from "express";
import { opencodeClient } from "../opencode/client";
import { logger } from "../utils/logger";
import { OpencodeExport, OpencodeExportMessage } from "../opencode/types";

const router = Router();

router.get("/sessions", async (req: Request, res: Response) => {
  try {
    const [globalSessions, projectSessions] = await Promise.all([
      opencodeClient.listSessionsGlobal(),
      opencodeClient.listSessions(),
    ]);
    const seen = new Set<string>();
    const sessions = [...globalSessions, ...projectSessions].filter(s => {
      if (seen.has(s.id)) return false;
      seen.add(s.id);
      return true;
    });
    const directory = req.query.directory as string | undefined;
    const filtered = directory ? sessions.filter(s => s.directory === directory) : sessions;
    filtered.sort((a, b) => b.updated - a.updated);
    res.json({ sessions: filtered });
  } catch (error) {
    logger.error("Failed to list sessions", { error: String(error) });
    res.status(500).json({ error: "Failed to list sessions" });
  }
});

router.get("/sessions/:id", async (req: Request, res: Response) => {
  try {
    const raw = await opencodeClient.exportSession(req.params.id) as OpencodeExport;

    const session = {
      id: raw.info.id,
      title: raw.info.title,
      created: raw.info.time.created,
      updated: raw.info.time.updated,
      projectId: raw.info.projectID,
      directory: raw.info.directory,
      messageCount: raw.messages?.length ?? 0,
    };

    const messages = (raw.messages || []).map((msg: OpencodeExportMessage) => {
      const textContent = (msg.parts || [])
        .filter(p => p.type === "text")
        .map(p => p.text)
        .join("");

      const reasoningContent = (msg.parts || [])
        .filter(p => p.type === "reasoning")
        .map(p => p.text)
        .join("\n");

      const toolCalls = (msg.parts || [])
        .filter(p => p.type === "tool-call")
        .map(p => ({
          name: p.name ?? "",
          input: typeof p.input === "string" ? p.input : JSON.stringify(p.input ?? {}),
        }));

      const toolResults = (msg.parts || [])
        .filter(p => p.type === "tool-result")
        .map(p => ({
          name: p.name ?? "",
          output: typeof p.result === "string" ? p.result : JSON.stringify(p.result ?? ""),
        }));

      return {
        id: msg.info.id,
        role: msg.info.role,
        content: textContent,
        reasoning: reasoningContent || null,
        toolCalls: toolCalls.length > 0 ? toolCalls : undefined,
        toolResults: toolResults.length > 0 ? toolResults : undefined,
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
