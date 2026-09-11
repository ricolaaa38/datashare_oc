"use client";

import type { ReactNode } from "react";

import { WorkspaceSidebar } from "@/components/layout/workspace-sidebar";
import { WorkspaceTopbar } from "@/components/layout/workspace-topbar";

export function WorkspacePage({ children, onSignOut }: { children: ReactNode; onSignOut: () => void }) {
  return (
    <div className="flex min-h-screen bg-surface-page">
      <WorkspaceSidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <WorkspaceTopbar onSignOut={onSignOut} />
        <main className="flex flex-1 flex-col gap-6 p-6">{children}</main>
      </div>
    </div>
  );
}
