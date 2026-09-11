import type { User } from "@/lib/types";

const ACCESS_TOKEN_STORAGE_KEY = "datashare.accessToken";
const USER_STORAGE_KEY = "datashare.user";

export type SessionSnapshot = {
  /** False while rendering on the server, where localStorage is unreachable. */
  isHydrated: boolean;
  accessToken: string;
  user: User | null;
};

const SERVER_SNAPSHOT: SessionSnapshot = { isHydrated: false, accessToken: "", user: null };

const listeners = new Set<() => void>();
let cachedSnapshot: SessionSnapshot | null = null;

function parseUser(serializedUser: string): User | null {
  try {
    return JSON.parse(serializedUser) as User;
  } catch {
    return null;
  }
}

function readSnapshot(): SessionSnapshot {
  const accessToken = window.localStorage.getItem(ACCESS_TOKEN_STORAGE_KEY) ?? "";
  const serializedUser = window.localStorage.getItem(USER_STORAGE_KEY) ?? "";

  return {
    isHydrated: true,
    accessToken,
    user: serializedUser ? parseUser(serializedUser) : null,
  };
}

function notifySessionChanged() {
  cachedSnapshot = null;
  listeners.forEach((listener) => listener());
}

/** useSyncExternalStore needs a stable reference, so the snapshot is memoised until it changes. */
export function getSessionSnapshot(): SessionSnapshot {
  cachedSnapshot ??= readSnapshot();
  return cachedSnapshot;
}

export function getServerSessionSnapshot(): SessionSnapshot {
  return SERVER_SNAPSHOT;
}

export function subscribeToSession(listener: () => void): () => void {
  listeners.add(listener);
  window.addEventListener("storage", notifySessionChanged);

  return () => {
    listeners.delete(listener);
    window.removeEventListener("storage", notifySessionChanged);
  };
}

export function getAccessToken(): string {
  if (typeof window === "undefined") {
    return "";
  }

  return getSessionSnapshot().accessToken;
}

export function hasActiveSession(): boolean {
  return getAccessToken().length > 0;
}

export function startSession(accessToken: string, user: User) {
  window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, accessToken);
  window.localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
  notifySessionChanged();
}

export function endSession() {
  window.localStorage.removeItem(ACCESS_TOKEN_STORAGE_KEY);
  window.localStorage.removeItem(USER_STORAGE_KEY);
  notifySessionChanged();
}
