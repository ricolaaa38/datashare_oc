"use client";

import type { Ref } from "react";

import { BLOCKED_UPLOAD_EXTENSIONS, MAX_UPLOAD_SIZE_BYTES } from "@/lib/config";
import { formatFileSize } from "@/lib/format";

type FileUploadProps = {
  ref?: Ref<HTMLInputElement>;
  onFileAccepted: (file: File) => void;
  onError: (message: string) => void;
  blockedExtensions?: readonly string[];
  maxSizeInBytes?: number;
  className?: string;
};

export function FileUpload({
  ref,
  onFileAccepted,
  onError,
  blockedExtensions = BLOCKED_UPLOAD_EXTENSIONS,
  maxSizeInBytes = MAX_UPLOAD_SIZE_BYTES,
  className,
}: FileUploadProps) {
  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];

    if (!file) {
      return;
    }

    const lowerCaseName = file.name.toLowerCase();
    const blockedExtension = blockedExtensions.find((extension) =>
      lowerCaseName.endsWith(extension.toLowerCase()),
    );

    if (blockedExtension) {
      onError(`Les fichiers ${blockedExtension} ne sont pas autorisés.`);
      event.target.value = "";
      return;
    }

    if (file.size > maxSizeInBytes) {
      onError(`La taille du fichier est limitée à ${formatFileSize(maxSizeInBytes)}.`);
      event.target.value = "";
      return;
    }

    onFileAccepted(file);
  };

  return <input ref={ref} type="file" className={className} onChange={handleFileChange} />;
}