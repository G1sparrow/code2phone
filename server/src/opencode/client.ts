import { spawn, ChildProcess, SpawnOptions } from "child_process";
import { EventEmitter } from "events";
import * as os from "os";
import { getConfig } from "../config";
import { logger } from "../utils/logger";
import { StreamEvent, OpencodeModel, OpencodeSession, ChatRequest } from "./types";

function createSpawnOptions(cwd?: string): SpawnOptions {
  const opts: SpawnOptions = {
    stdio: ["ignore", "pipe", "pipe"],
    env: { ...process.env, OPENCODE_SERVER_PASSWORD: process.env.OPENCODE_SERVER_PASSWORD || "" },
    windowsHide: true,
    shell: false,
  };
  if (cwd) opts.cwd = cwd;
  return opts;
}

export class OpencodeClient extends EventEmitter {
  private process: ChildProcess | null = null;
  private buffer = "";
  private currentSessionId: string | null = null;
  private isRunning = false;

  async listModels(): Promise<OpencodeModel[]> {
    return this.runCommand<OpencodeModel[]>(["models"], (output) => {
      return output.trim().split("\n").filter(Boolean).map(line => {
        const clean = line.replace(/\r$/, "");
        const [provider, model] = clean.split("/");
        return { id: model, providerID: provider };
      });
    });
  }

  async listSessions(): Promise<OpencodeSession[]> {
    return this.runCommand<OpencodeSession[]>(["session", "list", "--format", "json"], (output) => {
      return JSON.parse(output);
    });
  }

  async listSessionsGlobal(): Promise<OpencodeSession[]> {
    return this.runCommand<OpencodeSession[]>(["session", "list", "--format", "json"], (output) => {
      return JSON.parse(output);
    }, os.tmpdir());
  }

  async exportSession(sessionId: string): Promise<unknown> {
    return this.runCommand<unknown>(["export", sessionId], (output) => {
      return JSON.parse(output);
    });
  }

  startStreamingSession(request: ChatRequest): void {
    const args = ["run", request.message, "--format", "json"];
    
    if (request.model) args.push("--model", request.model);
    if (request.agent) args.push("--agent", request.agent);
    if (request.sessionId) {
      args.push("--session", request.sessionId);
      this.currentSessionId = request.sessionId;
    }

    if (this.isRunning) {
      this.stop();
    }

    const config = getConfig();
    const cwd = request.sessionDir || process.cwd();
    this.process = spawn(config.opencodePath, args, createSpawnOptions(cwd));

    this.isRunning = true;
    this.buffer = "";

    this.process.stdout!.on("data", (data) => {
      this.buffer += data.toString();
      this.processBuffer();
    });

    this.process.stderr!.on("data", (data) => {
      logger.debug("opencode stderr", { data: data.toString() });
    });

    this.process.on("close", (code) => {
      this.isRunning = false;
      this.process = null;
      if (code !== 0) {
        this.emit("error", new Error(`opencode exited with code ${code}`));
      }
      this.emit("close");
    });

    this.process.on("error", (err) => {
      this.isRunning = false;
      this.process = null;
      this.emit("error", err);
    });
  }

  private processBuffer(): void {
    const lines = this.buffer.split("\n");
    this.buffer = lines.pop() || "";

    for (const line of lines) {
      if (!line.trim()) continue;
      try {
        const event = JSON.parse(line) as StreamEvent;
        this.emit("event", event);
      } catch (e) {
        logger.warn("Failed to parse opencode event", { line, error: String(e) });
      }
    }
  }

  stop(): void {
    if (this.process) {
      this.process.kill("SIGTERM");
      this.process = null;
    }
    this.isRunning = false;
    this.currentSessionId = null;
    this.removeAllListeners();
  }

  get running(): boolean {
    return this.isRunning;
  }

  private async runCommand<T>(args: string[], parser: (output: string) => T, cwd?: string): Promise<T> {
    return new Promise((resolve, reject) => {
      const config = getConfig();
      const proc = spawn(config.opencodePath, args, createSpawnOptions(cwd));

      let output = "";
      let errorOutput = "";

      proc.stdout!.on("data", (data) => {
        output += data.toString();
      });

      proc.stderr!.on("data", (data) => {
        errorOutput += data.toString();
      });

      let timedOut = false;
      const timeout = setTimeout(() => {
        timedOut = true;
        proc.kill();
        reject(new Error(`Command timed out after ${config.requestTimeout}ms`));
      }, config.requestTimeout);

      proc.on("close", (code) => {
        clearTimeout(timeout);
        if (timedOut) return;
        if (code === 0) {
          try {
            resolve(parser(output));
          } catch (e) {
            reject(new Error(`Failed to parse output: ${e}`));
          }
        } else {
          const detail = errorOutput.length > 2000 ? errorOutput.slice(0, 2000) + "..." : errorOutput;
          reject(new Error(`opencode exited with code ${code}: ${detail}`));
        }
      });

      proc.on("error", (err) => {
        clearTimeout(timeout);
        reject(err);
      });
    });
  }
}

export const opencodeClient = new OpencodeClient();