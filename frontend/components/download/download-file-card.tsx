"use client";

import { useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import { Callout } from "@/components/ui/callout";
import { Card, CardTitle } from "@/components/ui/card";
import { FileSummary } from "@/components/ui/file-summary";
import { DownloadCloudIcon } from "@/components/ui/icons";
import { TextField } from "@/components/ui/text-field";
import { ApiError } from "@/lib/api/http-client";
import { fetchDownloadContent, fetchDownloadMetadata, saveBlobAs } from "@/lib/api/downloads-api";
import { formatExpirySentence, getDaysUntilExpiry } from "@/lib/format";
import type { DownloadMetadata } from "@/lib/types";

const EXPIRED_MESSAGE = "Ce fichier n'est plus disponible en téléchargement car il a expiré.";
const UNKNOWN_LINK_MESSAGE = "Ce lien de téléchargement est introuvable.";

/** Warn a day before expiry, stay informative otherwise. */
function expiryTone(expiresAt: string) {
  return getDaysUntilExpiry(expiresAt) <= 1 ? ("warning" as const) : ("info" as const);
}

export function DownloadFileCard({ token }: { token: string }) {
  const [metadata, setMetadata] = useState<DownloadMetadata | null>(null);
  const [linkErrorMessage, setLinkErrorMessage] = useState("");
  const [password, setPassword] = useState("");
  const [downloadErrorMessage, setDownloadErrorMessage] = useState("");
  const [isLoadingMetadata, setIsLoadingMetadata] = useState(true);
  const [isDownloading, setIsDownloading] = useState(false);

  useEffect(() => {
    let isCurrentRequest = true;

    fetchDownloadMetadata(token)
      .then((result) => {
        if (isCurrentRequest) {
          setMetadata(result);
        }
      })
      .catch((error: unknown) => {
        if (!isCurrentRequest) {
          return;
        }
        const status = error instanceof ApiError ? error.status : 0;
        setLinkErrorMessage(status === 410 ? EXPIRED_MESSAGE : UNKNOWN_LINK_MESSAGE);
      })
      .finally(() => {
        if (isCurrentRequest) {
          setIsLoadingMetadata(false);
        }
      });

    return () => {
      isCurrentRequest = false;
    };
  }, [token]);

  const handleDownload = async () => {
    if (!metadata) {
      return;
    }

    setIsDownloading(true);
    setDownloadErrorMessage("");

    try {
      const content = await fetchDownloadContent(token, password || undefined);
      saveBlobAs(content, metadata.originalName);
    } catch (error) {
      const status = error instanceof ApiError ? error.status : 0;
      if (status === 401) {
        setDownloadErrorMessage("Mot de passe incorrect.");
      } else if (status === 410) {
        setDownloadErrorMessage(EXPIRED_MESSAGE);
      } else {
        setDownloadErrorMessage("Le téléchargement a échoué, réessayez.");
      }
    } finally {
      setIsDownloading(false);
    }
  };

  if (isLoadingMetadata) {
    return (
      <Card>
        <CardTitle>Télécharger un fichier</CardTitle>
        <p className="text-base leading-6 text-black">Chargement du fichier…</p>
      </Card>
    );
  }

  if (!metadata) {
    return (
      <Card>
        <CardTitle>Télécharger un fichier</CardTitle>
        <Callout tone="danger">{linkErrorMessage}</Callout>
      </Card>
    );
  }

  const isPasswordMissing = metadata.hasPassword && password.length === 0;

  return (
    <Card>
      <CardTitle>Télécharger un fichier</CardTitle>

      <div className="flex w-full flex-col gap-4">
        <FileSummary
          name={metadata.originalName}
          mimeType={metadata.mimeType}
          sizeBytes={metadata.sizeBytes}
        />

        <Callout tone={expiryTone(metadata.expiresAt)}>{formatExpirySentence(metadata.expiresAt)}</Callout>

        {metadata.hasPassword && (
          <TextField
            label="Mot de passe"
            type="password"
            value={password}
            placeholder="Saisissez le mot de passe..."
            autoComplete="off"
            onChange={(event) => setPassword(event.target.value)}
          />
        )}

        {downloadErrorMessage && <Callout tone="danger">{downloadErrorMessage}</Callout>}
      </div>

      <Button fullWidth onClick={handleDownload} disabled={isPasswordMissing || isDownloading}>
        <DownloadCloudIcon />
        {isDownloading ? "Téléchargement..." : "Télécharger"}
      </Button>
    </Card>
  );
}
