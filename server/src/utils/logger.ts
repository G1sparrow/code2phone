export enum LogLevel {
  DEBUG = 0,
  INFO = 1,
  WARN = 2,
  ERROR = 3,
}

const logLevel = process.env.LOG_LEVEL 
  ? LogLevel[process.env.LOG_LEVEL as keyof typeof LogLevel] 
  : LogLevel.INFO;

function safeStringify(obj: unknown): string {
  const seen = new WeakSet<object>();
  return JSON.stringify(obj, (_key, value) => {
    if (typeof value === "object" && value !== null) {
      if (seen.has(value)) return "[Circular]";
      seen.add(value);
    }
    return value;
  });
}

function formatMessage(level: string, message: string, meta?: Record<string, unknown>): string {
  const timestamp = new Date().toISOString();
  const metaStr = meta ? ` ${safeStringify(meta)}` : "";
  return `[${timestamp}] [${level}] ${message}${metaStr}`;
}

export const logger = {
  debug: (message: string, meta?: Record<string, unknown>) => {
    if (logLevel <= LogLevel.DEBUG) console.debug(formatMessage("DEBUG", message, meta));
  },
  info: (message: string, meta?: Record<string, unknown>) => {
    if (logLevel <= LogLevel.INFO) console.info(formatMessage("INFO", message, meta));
  },
  warn: (message: string, meta?: Record<string, unknown>) => {
    if (logLevel <= LogLevel.WARN) console.warn(formatMessage("WARN", message, meta));
  },
  error: (message: string, meta?: Record<string, unknown>) => {
    if (logLevel <= LogLevel.ERROR) console.error(formatMessage("ERROR", message, meta));
  },
};