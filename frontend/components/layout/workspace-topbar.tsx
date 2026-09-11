import { Button } from "@/components/ui/button";
import { ButtonLink } from "@/components/ui/button-link";
import { LogOutIcon } from "@/components/ui/icons";
import { ROUTES } from "@/lib/config";

export function WorkspaceTopbar({ onSignOut }: { onSignOut: () => void }) {
  return (
    <div className="flex w-full justify-center border-b border-border-topbar bg-surface-topbar px-4 lg:px-8">
      <div className="flex w-full max-w-[1280px] items-center justify-end gap-2.5 p-4">
        <ButtonLink href={ROUTES.home} variant="dark" size="small">
          Ajouter des fichiers
        </ButtonLink>
        <Button variant="tertiary" size="small" onClick={onSignOut}>
          <LogOutIcon />
          Déconnexion
        </Button>
      </div>
    </div>
  );
}
