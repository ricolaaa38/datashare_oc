/** Shapes exposed by the DataShare OpenAPI contract (backend/src/main/resources/static/openapi.yaml). */

export type User = {
  userId: number;
  login: string;
  createdAt: string;
  lastLogin?: string | null;
};

export type LoginResponse = {
  accessToken: string;
  user: User;
};

export type FileStatus = "VALID" | "EXPIRED";

export type FileResource = {
  fileId: number;
  ownerId?: number | null;
  originalName: string;
  sizeBytes: number;
  mimeType: string;
  createdAt: string;
  expiresAt: string;
  hasPassword: boolean;
  status: FileStatus;
  tags?: string[];
  downloadToken?: string | null;
  downloadUrl?: string | null;
};

export type PagedFiles = {
  total: number;
  page: number;
  size: number;
  items: FileResource[];
};

export type DownloadTokenResponse = {
  token: string;
  downloadUrl: string;
  createdAt: string;
};

export type DownloadMetadata = {
  originalName: string;
  mimeType: string;
  sizeBytes: number;
  expiresAt: string;
  hasPassword: boolean;
};

export type UploadRequest = {
  file: File;
  originalName: string;
  mimeType: string;
  expiresInDays: number;
  password?: string;
  tags?: string[];
};
