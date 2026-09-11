import { Button } from "@/components/ui/button";
import { Callout } from "@/components/ui/callout";
import { Card, CardTitle } from "@/components/ui/card";
import { FileSummary } from "@/components/ui/file-summary";
import { UploadCloudIcon } from "@/components/ui/icons";
import { SelectField } from "@/components/ui/select-field";
import { TextField } from "@/components/ui/text-field";
import { MAX_UPLOAD_SIZE_BYTES, MIN_PASSWORD_LENGTH, RETENTION_OPTIONS } from "@/lib/config";
import { formatFileSize } from "@/lib/format";

const RETENTION_SELECT_OPTIONS = RETENTION_OPTIONS.map((option) => ({
  value: option.days,
  label: option.label,
}));

type UploadFormProps = {
  file: File;
  password: string;
  retentionDays: number;
  isUploading: boolean;
  errorMessage: string;
  onPasswordChange: (password: string) => void;
  onRetentionChange: (retentionDays: number) => void;
  onReplaceFile: () => void;
  onSubmit: () => void;
};

export function UploadForm({
  file,
  password,
  retentionDays,
  isUploading,
  errorMessage,
  onPasswordChange,
  onRetentionChange,
  onReplaceFile,
  onSubmit,
}: UploadFormProps) {
  const isTooLarge = file.size > MAX_UPLOAD_SIZE_BYTES;
  const isPasswordTooShort = password.length > 0 && password.length < MIN_PASSWORD_LENGTH;
  const canSubmit = !isTooLarge && !isPasswordTooShort && !isUploading;

  return (
    <Card>
      <CardTitle>Ajouter un fichier</CardTitle>

      <div className="flex w-full flex-col gap-2">
        <FileSummary
          name={file.name}
          mimeType={file.type || "application/octet-stream"}
          sizeBytes={file.size}
          action={
            <Button variant="secondary" size="small" onClick={onReplaceFile}>
              Changer
            </Button>
          }
        />
        {isTooLarge && (
          <p className="px-2 text-sm leading-6 text-callout-danger-foreground">
            La taille des fichiers est limitée à {formatFileSize(MAX_UPLOAD_SIZE_BYTES)}
          </p>
        )}
      </div>

      <div className="flex w-full flex-col gap-4">
        <TextField
          label="Mot de passe"
          type="password"
          value={password}
          placeholder="Optionnel"
          autoComplete="new-password"
          onChange={(event) => onPasswordChange(event.target.value)}
          error={isPasswordTooShort ? `Au moins ${MIN_PASSWORD_LENGTH} caractères.` : undefined}
        />
        <SelectField
          label="Expiration"
          value={retentionDays}
          options={RETENTION_SELECT_OPTIONS}
          onChange={(event) => onRetentionChange(Number(event.target.value))}
        />
      </div>

      {errorMessage && <Callout tone="danger">{errorMessage}</Callout>}

      <Button onClick={onSubmit} disabled={!canSubmit}>
        <UploadCloudIcon />
        {isUploading ? "Téléversement..." : "Téléverser"}
      </Button>
    </Card>
  );
}
