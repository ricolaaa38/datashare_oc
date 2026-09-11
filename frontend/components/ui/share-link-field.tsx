import { CopyIcon } from "@/components/ui/icons";
import { Button } from "@/components/ui/button";
import { useState } from "react";

/** Read-only share link with a copy shortcut, as shown on the upload success screen. */
export function ShareLinkField({ link }: { link: string }) {
  const [isCopied, setIsCopied] = useState(false);

  const copyLink = async () => {
    await navigator.clipboard?.writeText(link);
    setIsCopied(true);
  };

  return (
    <div className="flex w-full flex-col items-center gap-4">
      <div className="w-full rounded-lg bg-surface-muted px-4 py-2">
        <a
          href={link}
          className="block break-all text-base leading-6 text-text-link underline"
          target="_blank"
          rel="noreferrer"
        >
          {link}
        </a>
      </div>
      <Button onClick={copyLink}>
        <CopyIcon />
        {isCopied ? "Lien copié" : "Copier le lien"}
      </Button>
    </div>
  );
}
