"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

import { MyFilesPanel } from "@/components/files/my-files-panel";
import { WorkspacePage } from "@/components/layout/workspace-page";
import { useSession } from "@/hooks/use-session";
import { ROUTES } from "@/lib/config";

/** Members-only space listing the files owned by the signed-in user. */
export default function MyFilesPage() {
  const router = useRouter();
  const { isReady, isAuthenticated, signOut } = useSession();

  useEffect(() => {
    if (isReady && !isAuthenticated) {
      router.replace(ROUTES.login);
    }
  }, [isReady, isAuthenticated, router]);

  if (!isReady || !isAuthenticated) {
    return null;
  }

  return (
    <WorkspacePage onSignOut={signOut}>
      <MyFilesPanel />
    </WorkspacePage>
  );
}
