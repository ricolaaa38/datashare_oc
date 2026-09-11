import { Button } from "@/components/ui/button";
import { Card, CardTitle } from "@/components/ui/card";
import { FileSummary } from "@/components/ui/file-summary";
import { ShareLinkField } from "@/components/ui/share-link-field";
import { RETENTION_OPTIONS } from "@/lib/config";
import type { FileResource } from "@/lib/types";

function retentionLabelFor(retentionDays: number): string {
  const option = RETENTION_OPTIONS.find((candidate) => candidate.days === retentionDays);
  return (option?.label ?? `${retentionDays} jours`).toLowerCase();
}

type UploadSuccessProps = {
  uploadedFile: FileResource;
  shareLink: string;
  retentionDays: number;
  onUploadAnother: () => void;
};

export function UploadSuccess({
  uploadedFile,
  shareLink,
  retentionDays,
  onUploadAnother,
}: UploadSuccessProps) {
  return (
    <Card>
      <CardTitle>Ajouter un fichier</CardTitle>

      <FileSummary
        name={uploadedFile.originalName}
        mimeType={uploadedFile.mimeType}
        sizeBytes={uploadedFile.sizeBytes}
      />

      <p className="w-full text-base leading-6 text-black">
        Félicitations, ton fichier sera conservé chez nous pendant {retentionLabelFor(retentionDays)} !
      </p>

      <ShareLinkField link={shareLink} />

      <Button variant="tertiary" onClick={onUploadAnother}>
        Envoyer un autre fichier
      </Button>
    </Card>
  );
}
