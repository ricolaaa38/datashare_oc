import type { ReactNode } from "react";

import { FileTypeIcon } from "@/components/ui/file-type-icon";
import { formatFileSize } from "@/lib/format";

type FileSummaryProps = {
  name: string;
  mimeType: string;
  sizeBytes: number;
  action?: ReactNode;
};

/** Icon + filename + weight block shared by the upload, download and success screens. */
export function FileSummary({ name, mimeType, sizeBytes, action }: FileSummaryProps) {
  return (
    <div className="flex w-full items-center gap-4">
      <div className="flex min-w-0 flex-1 items-center gap-4 p-2">
        <FileTypeIcon mimeType={mimeType} className="size-6 text-black" />
        <div className="flex min-w-0 flex-1 flex-col justify-center text-black">
          <p className="truncate text-base leading-6">{name}</p>
          <p className="text-sm leading-4">{formatFileSize(sizeBytes)}</p>
        </div>
      </div>
      {action}
    </div>
  );
}
