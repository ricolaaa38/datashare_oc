"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import { ButtonLink } from "@/components/ui/button-link";
import { LogOutIcon, MenuIcon } from "@/components/ui/icons";
import { useSession } from "@/hooks/use-session";
import { ROUTES } from "@/lib/config";
import { formatDisplayName } from "@/lib/format";

export function WorkspaceTopbar({ onSignOut }: { onSignOut: () => void }) {
  const { user } = useSession();
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const displayName = user ? formatDisplayName(user.login) : "";

  const closeMenu = () => setIsMenuOpen(false);

  // Escape closes the drawer like the backdrop click does.
  useEffect(() => {
    if (!isMenuOpen) {
      return;
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        closeMenu();
      }
    };

    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [isMenuOpen]);

  return (
    <div className="flex w-full justify-center border-b border-border-topbar bg-surface-topbar px-4 lg:px-8">
      <div className="flex w-full max-w-[1280px] items-center justify-between gap-2.5 p-4 lg:justify-end">
        {/* Mobile-only: the sidebar nav is hidden below lg, so this opens it instead. */}
        <button
          type="button"
          aria-label="Ouvrir le menu"
          aria-expanded={isMenuOpen}
          onClick={() => setIsMenuOpen((open) => !open)}
          className="cursor-pointer text-black lg:hidden"
        >
          <MenuIcon className="size-6" />
        </button>

        <div className="hidden items-center gap-2.5 lg:flex">
          <ButtonLink href={ROUTES.home} variant="dark" size="small">
            Ajouter des fichiers
          </ButtonLink>
          <Button variant="tertiary" size="small" onClick={onSignOut}>
            <LogOutIcon />
            Déconnexion
          </Button>
        </div>

        <div className="flex min-w-0 items-center gap-2 lg:hidden">
          <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-action-dark text-base font-semibold text-action-dark-foreground">
            {displayName.charAt(0).toUpperCase() || "?"}
          </span>
          <span className="max-w-[120px] truncate text-base leading-6 font-semibold text-black">
            {displayName}
          </span>
        </div>
      </div>

      {isMenuOpen && (
        <>
          <button
            type="button"
            aria-label="Fermer le menu"
            onClick={closeMenu}
            className="fixed inset-0 z-40 cursor-default bg-black/40 lg:hidden"
          />
          <div
            role="dialog"
            aria-modal="true"
            className="gradient-warm-diagonal fixed inset-y-0 left-0 z-50 flex w-3/4 max-w-[320px] flex-col lg:hidden"
          >
            <div className="flex h-[72px] items-center px-8">
              <Link
                href={ROUTES.home}
                onClick={closeMenu}
                className="text-[32px] leading-10 font-bold text-white"
              >
                DataShare
              </Link>
            </div>

            <nav className="flex flex-1 flex-col gap-2 p-6">
              <Link
                href={ROUTES.myFiles}
                onClick={closeMenu}
                aria-current="page"
                className="rounded-xl bg-white/40 px-4 py-2 text-base leading-6 font-semibold text-text-nav-active"
              >
                Mes fichiers
              </Link>
              <Link
                href={ROUTES.home}
                onClick={closeMenu}
                className="rounded-xl px-4 py-2 text-base leading-6 font-semibold text-white"
              >
                Ajouter des fichiers
              </Link>
              <button
                type="button"
                onClick={() => {
                  closeMenu();
                  onSignOut();
                }}
                className="flex cursor-pointer items-center gap-2 rounded-xl px-4 py-2 text-left text-base leading-6 font-semibold text-white"
              >
                <LogOutIcon />
                Déconnexion
              </button>
            </nav>

            <p className="px-6 py-4 text-base leading-6 whitespace-nowrap text-text-on-gradient-muted">
              Copyright DataShare© 2025
            </p>
          </div>
        </>
      )}
    </div>
  );
}
