"use client";

import { useCallback, useSyncExternalStore } from "react";
import { useRouter } from "next/navigation";

import { ROUTES } from "@/lib/config";
import {
  endSession,
  getServerSessionSnapshot,
  getSessionSnapshot,
  subscribeToSession,
} from "@/lib/session";
import type { User } from "@/lib/types";

type SessionState = {
  /** False until the browser has read localStorage, so guards do not redirect too early. */
  isReady: boolean;
  isAuthenticated: boolean;
  user: User | null;
  signOut: () => void;
};

export function useSession(): SessionState {
  const router = useRouter();
  const session = useSyncExternalStore(
    subscribeToSession,
    getSessionSnapshot,
    getServerSessionSnapshot,
  );

  const signOut = useCallback(() => {
    endSession();
    router.replace(ROUTES.home);
  }, [router]);

  return {
    isReady: session.isHydrated,
    isAuthenticated: session.accessToken.length > 0,
    user: session.user,
    signOut,
  };
}
