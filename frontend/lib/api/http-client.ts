import { API_BASE_URL } from "@/lib/config";
import { getAccessToken } from "@/lib/session";

export class ApiError extends Error {
  readonly status: number;
  readonly code?: string;

  constructor(message: string, status: number, code?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

type RequestOptions = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: BodyInit;
  headers?: Record<string, string>;
  /** Attach the bearer token; disabled for public endpoints so the backend keeps treating them as anonymous. */
  authenticated?: boolean;
};

function buildHeaders({ headers = {}, body, authenticated = true }: RequestOptions): Headers {
  const requestHeaders = new Headers(headers);

  if (body !== undefined && !(body instanceof FormData)) {
    requestHeaders.set("Content-Type", "application/json");
  }

  if (authenticated) {
    const accessToken = getAccessToken();
    if (accessToken) {
      requestHeaders.set("Authorization", `Bearer ${accessToken}`);
    }
  }

  return requestHeaders;
}

async function toApiError(response: Response): Promise<ApiError> {
  const payload = await response.json().catch(() => null);
  const message =
    typeof payload?.message === "string" ? payload.message : "Une erreur est survenue, réessayez.";

  return new ApiError(message, response.status, payload?.code);
}

async function sendRequest(path: string, options: RequestOptions): Promise<Response> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: options.method ?? "GET",
    body: options.body,
    headers: buildHeaders(options),
  });

  if (!response.ok) {
    throw await toApiError(response);
  }

  return response;
}

export async function requestJson<TResponse>(
  path: string,
  options: RequestOptions = {},
): Promise<TResponse> {
  const response = await sendRequest(path, options);

  if (response.status === 204) {
    return undefined as TResponse;
  }

  return (await response.json()) as TResponse;
}

export async function requestBlob(path: string, options: RequestOptions = {}): Promise<Blob> {
  const response = await sendRequest(path, options);
  return response.blob();
}
