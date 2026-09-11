export const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export const ROUTES = {
  home: "/",
  login: "/login",
  register: "/register",
  myFiles: "/files",
  download: (token: string) => `/download/${token}`,
} as const;

export const RETENTION_OPTIONS = [
  { days: 1, label: "Une journée" },
  { days: 3, label: "3 jours" },
  { days: 7, label: "Une semaine" },
] as const;

export const DEFAULT_RETENTION_DAYS = 7;

/** Backend rejects anything above 1 GB, mirrored here to fail fast before uploading. */
export const MAX_UPLOAD_SIZE_BYTES = 1024 ** 3;

/** Backend forbidden extensions, mirrored here to reject them before uploading. */
export const BLOCKED_UPLOAD_EXTENSIONS = [
  ".exe",
  ".bat",
  ".cmd",
  ".sh",
  ".msi",
  ".dll",
  ".com",
  ".scr",
  ".jar",
  ".vbs",
  ".ps1",
] as const;

export const MIN_PASSWORD_LENGTH = 8;
