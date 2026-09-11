"use client";

import { useCallback, useEffect, useState } from "react";

import { FILE_FILTER_OPTIONS, filterFiles, type FileFilter } from "@/components/files/file-filters";
import { FileRow } from "@/components/files/file-row";
import { Callout } from "@/components/ui/callout";
import { SegmentedControl } from "@/components/ui/segmented-control";
import { ShareLinkField } from "@/components/ui/share-link-field";
import { createDownloadToken, deleteOwnedFile, listOwnedFiles } from "@/lib/api/files-api";
import { buildShareLink } from "@/lib/format";
import type { FileResource } from "@/lib/types";

export function MyFilesPanel() {
  const [files, setFiles] = useState<FileResource[]>([]);
  const [filter, setFilter] = useState<FileFilter>("all");
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");
  const [activeShareLink, setActiveShareLink] = useState("");
  const [busyFileId, setBusyFileId] = useState<number | null>(null);
  const [reloadCount, setReloadCount] = useState(0);

  const reloadFiles = useCallback(() => setReloadCount((count) => count + 1), []);

  useEffect(() => {
    let isCurrentRequest = true;

    listOwnedFiles()
      .then((page) => {
        if (isCurrentRequest) {
          setFiles(page.items);
          setErrorMessage("");
        }
      })
      .catch((error: unknown) => {
        if (isCurrentRequest) {
          setErrorMessage(
            error instanceof Error ? error.message : "Impossible de charger vos fichiers.",
          );
        }
      })
      .finally(() => {
        if (isCurrentRequest) {
          setIsLoading(false);
        }
      });

    return () => {
      isCurrentRequest = false;
    };
  }, [reloadCount]);

  const handleDelete = async (fileId: number) => {
    setBusyFileId(fileId);
    try {
      await deleteOwnedFile(fileId);
      setActiveShareLink("");
      reloadFiles();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "La suppression a échoué.");
    } finally {
      setBusyFileId(null);
    }
  };

  // A file only keeps the hash of its token, so a fresh one is issued each time the link is opened.
  const handleOpenShareLink = async (file: FileResource) => {
    setBusyFileId(file.fileId);
    try {
      const issuedToken = await createDownloadToken(file.fileId);
      setActiveShareLink(buildShareLink(issuedToken.token));
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Le lien n'a pas pu être généré.");
    } finally {
      setBusyFileId(null);
    }
  };

  const visibleFiles = filterFiles(files, filter);

  return (
    <section className="flex flex-col gap-6">
      <h1 className="text-[28px] leading-10 font-bold text-black">Mes fichiers</h1>

      <SegmentedControl
        ariaLabel="Filtrer les fichiers"
        options={FILE_FILTER_OPTIONS}
        value={filter}
        onChange={setFilter}
      />

      {errorMessage && <Callout tone="danger">{errorMessage}</Callout>}
      {activeShareLink && <ShareLinkField link={activeShareLink} />}

      {isLoading ? (
        <p className="text-base leading-6 text-black">Chargement de vos fichiers…</p>
      ) : visibleFiles.length === 0 ? (
        <p className="text-base leading-6 text-black/60">Aucun fichier pour le moment.</p>
      ) : (
        <ul className="flex flex-col gap-4">
          {visibleFiles.map((file) => (
            <FileRow
              key={file.fileId}
              file={file}
              isBusy={busyFileId === file.fileId}
              onDelete={handleDelete}
              onOpenShareLink={handleOpenShareLink}
            />
          ))}
        </ul>
      )}
    </section>
  );
}
