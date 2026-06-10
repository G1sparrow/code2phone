export interface OpencodeModel {
  id: string;
  providerID: string;
}

export interface OpencodeSession {
  id: string;
  title: string;
  updated: number;
  created: number;
  projectId: string;
  directory: string;
}

export type SessionPart =
  | { type: "text"; text: string; id: string; sessionID: string; messageID: string; time?: { start: number; end: number } }
  | { type: "reasoning"; text: string; id: string; sessionID: string; messageID: string; time: { start: number; end: number } }
  | { type: "step-start"; id: string; sessionID: string; messageID: string }
  | { type: "step-finish"; id: string; sessionID: string; messageID: string; reason: string; tokens: { total: number; input: number; output: number; reasoning: number; cache: { write: number; read: number } }; cost: number }
  | { type: "tool-call"; id: string; sessionID: string; messageID: string; name: string; input: unknown }
  | { type: "tool-result"; id: string; sessionID: string; messageID: string; name: string; output: unknown; error?: string }
  | { type: "error"; id: string; sessionID: string; messageID: string; error: string };

export interface StreamEvent {
  type: "step_start" | "text" | "reasoning" | "tool_call" | "tool_result" | "step_finish" | "error";
  timestamp: number;
  sessionID: string;
  part: SessionPart;
}

export interface ChatRequest {
  message: string;
  model?: string;
  agent?: "plan" | "build";
  sessionId?: string;
  files?: string[];
}

export interface ChatResponse {
  sessionId: string;
  messageId: string;
  content: string;
}