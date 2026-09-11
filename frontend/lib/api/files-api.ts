import { requestJson } from "@/lib/api/http-client";
import type { DownloadTokenResponse, FileResource, PagedFiles, UploadRequest } from "@/lib/types";

function toUploadFormData(request: UploadRequest, includeTags: boolean): FormData {
  const formData = new FormData();
  formData.append("file", request.file);
  formData.append("originalName", request.originalName || request.file.name);
  formData.append("mimeType", request.mimeType || request.file.type || "application/octet-stream");
  formData.append("expiresInDays", String(request.expiresInDays));

  if (request.password?.trim()) {
    formData.append("password", request.password.trim());
  }

  if (includeTags) {
    request.tags?.forEach((tag) => formData.append("tags", tag));
  }

  return formData;
}

/** Upload owned by the authenticated user: the file shows up in "Mes fichiers". */
export async function uploadOwnedFile(request: UploadRequest): Promise<FileResource> {
  return requestJson<FileResource>("/files", {
    method: "POST",
    body: toUploadFormData(request, true),
  });
}

/** Upload without an account: the backend refuses this endpoint when a bearer token is sent. */
export async function uploadAnonymousFile(request: UploadRequest): Promise<FileResource> {
  return requestJson<FileResource>("/anonymous/files", {
    method: "POST",
    body: toUploadFormData(request, false),
    authenticated: false,
  });
}

export async function listOwnedFiles(page = 0, size = 20): Promise<PagedFiles> {
  return requestJson<PagedFiles>(`/files?page=${page}&size=${size}`);
}

export async function createDownloadToken(fileId: number): Promise<DownloadTokenResponse> {
  return requestJson<DownloadTokenResponse>(`/files/${fileId}/download-tokens`, { method: "POST" });
}

export async function deleteOwnedFile(fileId: number): Promise<void> {
  return requestJson<void>(`/files/${fileId}`, { method: "DELETE" });
}
