"use client";

import { useRef, useState } from "react";

import { UploadForm } from "@/components/upload/upload-form";
import { UploadHero } from "@/components/upload/upload-hero";
import { UploadSuccess } from "@/components/upload/upload-success";
import { uploadAnonymousFile, uploadOwnedFile } from "@/lib/api/files-api";
import { DEFAULT_RETENTION_DAYS } from "@/lib/config";
import { buildShareLink } from "@/lib/format";
import { hasActiveSession } from "@/lib/session";
import type { FileResource } from "@/lib/types";

/**
 * Landing flow shared by visitors and members: pick a file, configure it, get a share link.
 * Members upload through /files so the file lands in their space, visitors through /anonymous/files.
 */
export function ShareFileFlow() {
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [password, setPassword] = useState("");
  const [retentionDays, setRetentionDays] = useState<number>(DEFAULT_RETENTION_DAYS);
  const [uploadedFile, setUploadedFile] = useState<FileResource | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const openFilePicker = () => fileInputRef.current?.click();

  const resetFlow = () => {
    setSelectedFile(null);
    setPassword("");
    setRetentionDays(DEFAULT_RETENTION_DAYS);
    setUploadedFile(null);
    setErrorMessage("");
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  const handleFileSelected = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      setSelectedFile(file);
      setErrorMessage("");
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      return;
    }

    setIsUploading(true);
    setErrorMessage("");

    const uploadRequest = {
      file: selectedFile,
      originalName: selectedFile.name,
      mimeType: selectedFile.type || "application/octet-stream",
      expiresInDays: retentionDays,
      password: password || undefined,
    };

    try {
      const result = hasActiveSession()
        ? await uploadOwnedFile(uploadRequest)
        : await uploadAnonymousFile(uploadRequest);
      setUploadedFile(result);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Le téléversement a échoué.");
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <>
      {uploadedFile ? (
        <UploadSuccess
          uploadedFile={uploadedFile}
          shareLink={buildShareLink(uploadedFile.downloadToken ?? "")}
          retentionDays={retentionDays}
          onUploadAnother={resetFlow}
        />
      ) : selectedFile ? (
        <UploadForm
          file={selectedFile}
          password={password}
          retentionDays={retentionDays}
          isUploading={isUploading}
          errorMessage={errorMessage}
          onPasswordChange={setPassword}
          onRetentionChange={setRetentionDays}
          onReplaceFile={openFilePicker}
          onSubmit={handleUpload}
        />
      ) : (
        <UploadHero onSelectFile={openFilePicker} />
      )}

      <input ref={fileInputRef} type="file" className="hidden" onChange={handleFileSelected} />
    </>
  );
}
