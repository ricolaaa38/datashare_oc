import { requestJson } from "@/lib/api/http-client";
import { startSession } from "@/lib/session";
import type { LoginResponse, User } from "@/lib/types";

export async function registerUser(login: string, password: string): Promise<User> {
  return requestJson<User>("/users", {
    method: "POST",
    body: JSON.stringify({ login, password }),
    authenticated: false,
  });
}

export async function loginUser(login: string, password: string): Promise<LoginResponse> {
  const result = await requestJson<LoginResponse>("/auth/login", {
    method: "POST",
    body: JSON.stringify({ login, password }),
    authenticated: false,
  });

  startSession(result.accessToken, result.user);
  return result;
}
