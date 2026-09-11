import { Button } from "@/components/ui/button";
import { FileTypeIcon } from "@/components/ui/file-type-icon";
import { ArrowRightIcon, LockIcon, TrashIcon } from "@/components/ui/icons";
import { formatExpiryLabel, isExpired } from "@/lib/format";
import type { FileResource } from "@/lib/types";

const EXPIRED_HINT = "Ce fichier a expiré, il n'est plus stocké chez nous";

type FileRowProps = {
  file: FileResource;
  isBusy: boolean;
  onDelete: (fileId: number) => void;
  onOpenShareLink: (file: FileResource) => void;
};

export function FileRow({ file, isBusy, onDelete, onOpenShareLink }: FileRowProps) {
  const expired = file.status === "EXPIRED" || isExpired(file.expiresAt);

  return (
    <li className="flex w-full items-center gap-4 rounded-lg border border-border-warm bg-surface-row px-4 py-2">
      <FileTypeIcon mimeType={file.mimeType} className="size-6 text-black" />

      <div className="flex min-w-0 flex-1 flex-col justify-center">
        <p className="truncate text-base leading-6 font-semibold text-black">{file.originalName}</p>
        <p className={`text-sm leading-4 ${expired ? "text-text-expired" : "text-black"}`}>
          {formatExpiryLabel(file.expiresAt)}
        </p>
      </div>

      {expired ? (
        <p className="hidden text-sm leading-4 text-black/50 sm:block">{EXPIRED_HINT}</p>
      ) : (
        <div className="flex shrink-0 items-center gap-2">
          {file.hasPassword && (
            <LockIcon className="size-4 text-black" aria-label="Protégé par mot de passe" />
          )}
          <Button
            variant="secondary"
            size="small"
            disabled={isBusy}
            onClick={() => onDelete(file.fileId)}
          >
            <TrashIcon />
            <span className="hidden sm:inline">Supprimer</span>
          </Button>
          <Button
            variant="secondary"
            size="small"
            disabled={isBusy}
            onClick={() => onOpenShareLink(file)}
          >
            <span className="hidden sm:inline">Accéder</span>
            <ArrowRightIcon />
          </Button>
        </div>
      )}
    </li>
  );
}
