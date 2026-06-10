import { z } from "zod";

const configSchema = z.object({
  port: z.number().default(3001),
  hostname: z.string().default("0.0.0.0"),
  opencodePath: z.string(),
  corsOrigins: z.array(z.string()).default(["*"]),
  requestTimeout: z.number().default(30000),
});

export type Config = z.infer<typeof configSchema>;

function findOpencodePath(): string {
  const candidates = [
    process.env.OPENCODE_PATH,
    "C:\\Users\\GGBond\\AppData\\Roaming\\npm\\node_modules\\opencode-ai\\bin\\opencode.exe",
    "C:\\Users\\GGBond\\AppData\\Roaming\\npm\\opencode-ai",
    "opencode-ai",
    "npx opencode-ai",
  ];
  return candidates.find(Boolean) || "opencode-ai";
}

let cachedConfig: Config | null = null;

export function loadConfig(): Config {
  if (cachedConfig) return cachedConfig;

  const port = process.env.PORT ? parseInt(process.env.PORT, 10) : 3001;
  const hostname = process.env.HOSTNAME || "0.0.0.0";
  const opencodePath = findOpencodePath();
  const corsOrigins = process.env.CORS_ORIGINS?.split(",") || ["*"];
  const requestTimeout = process.env.REQUEST_TIMEOUT ? parseInt(process.env.REQUEST_TIMEOUT, 10) : 60000;

  cachedConfig = configSchema.parse({ port, hostname, opencodePath, corsOrigins, requestTimeout });
  return cachedConfig;
}

export function getConfig(): Config {
  return cachedConfig || loadConfig();
}