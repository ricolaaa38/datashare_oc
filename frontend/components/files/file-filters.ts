import type { FileResource } from "@/lib/types";
import { isExpired } from "@/lib/format";

export type FileFilter = "all" | "active" | "expired";

export const FILE_FILTER_OPTIONS = [
  { value: "all", label: "Tous" },
  { value: "active", label: "Actifs" },
  { value: "expired", label: "Expiré" },
] as const satisfies readonly { value: FileFilter; label: string }[];

export function filterFiles(files: FileResource[], filter: FileFilter): FileResource[] {
  if (filter === "all") {
    return files;
  }

  const wantExpired = filter === "expired";
  return files.filter((file) => (file.status === "EXPIRED" || isExpired(file.expiresAt)) === wantExpired);
}
