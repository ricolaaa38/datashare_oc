import type { ReactNode } from "react";

import { SiteFooter } from "@/components/layout/site-footer";
import { SiteHeader } from "@/components/layout/site-header";

/** Gradient shell shared by the landing, auth and download screens. */
export function PublicPage({ children }: { children: ReactNode }) {
  return (
    <div className="gradient-warm-vertical flex min-h-screen flex-col">
      <SiteHeader />
      <main className="flex flex-1 flex-col items-center justify-center px-6 py-10">{children}</main>
      <SiteFooter />
    </div>
  );
}
