import Link from "next/link";

import { ROUTES } from "@/lib/config";

export function WorkspaceSidebar() {
  return (
    <aside className="gradient-warm-diagonal hidden w-[259px] shrink-0 flex-col border-r-2 border-border-sidebar lg:flex">
      <div className="flex h-[72px] items-center px-8">
        <Link href={ROUTES.home} className="text-[32px] leading-10 font-bold text-white">
          DataShare
        </Link>
      </div>

      <nav className="flex flex-1 flex-col gap-2 p-6">
        <Link
          href={ROUTES.myFiles}
          aria-current="page"
          className="rounded-xl bg-white/40 px-4 py-2 text-base leading-6 font-semibold text-text-nav-active"
        >
          Mes fichiers
        </Link>
      </nav>

      <p className="px-6 py-4 text-base leading-6 whitespace-nowrap text-text-on-gradient-muted">
        Copyright DataShare© 2025
      </p>
    </aside>
  );
}
