import { requestBlob, requestJson } from "@/lib/api/http-client";
import type { DownloadMetadata } from "@/lib/types";

export async function fetchDownloadMetadata(token: string): Promise<DownloadMetadata> {
  return requestJson<DownloadMetadata>(`/downloads/${encodeURIComponent(token)}/metadata`, {
    authenticated: false,
  });
}

export async function fetchDownloadContent(token: string, password?: string): Promise<Blob> {
  return requestBlob(`/downloads/${encodeURIComponent(token)}`, {
    authenticated: false,
    headers: password ? { "X-File-Password": password } : undefined,
  });
}

/** Browsers cannot send the password header through a plain link, so the blob is saved manually. */
export function saveBlobAs(blob: Blob, fileName: string) {
  const objectUrl = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = objectUrl;
  anchor.download = fileName;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(objectUrl);
}
