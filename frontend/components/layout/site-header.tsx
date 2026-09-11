"use client";

import Link from "next/link";

import { ButtonLink } from "@/components/ui/button-link";
import { useSession } from "@/hooks/use-session";
import { ROUTES } from "@/lib/config";

export function SiteHeader() {
  const { isAuthenticated } = useSession();

  return (
    <header className="flex w-full justify-center">
      <div className="flex w-full max-w-[1280px] items-center justify-end gap-2.5 p-4">
        <Link href={ROUTES.home} className="flex-1 text-[32px] leading-10 font-bold text-black">
          DataShare
        </Link>
        <ButtonLink href={isAuthenticated ? ROUTES.myFiles : ROUTES.login} variant="dark">
          {isAuthenticated ? "Mon espace" : "Se connecter"}
        </ButtonLink>
      </div>
    </header>
  );
}
