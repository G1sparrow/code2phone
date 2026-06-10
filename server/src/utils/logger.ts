export enum LogLevel {
  DEBUG = 0,
  INFO = 1,
  WARN = 2,
  ERROR = 3,
}

const logLevel = process.env.LOG_LEVEL 
  ? LogLevel[process.env.LOG_LEVEL as keyof typeof LogLevel] 
  : LogLevel.INFO;

function formatMessage(level: string, message: string, meta?: Record<string, unknown>): string {
  const timestamp = new Date().toISOString();
  const metaStr = meta ? ` ${JSON.stringify(meta)}` : "";
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