import { AudioFileIcon, FileIcon, ImageFileIcon, VideoFileIcon } from "@/components/ui/icons";

/** The design uses a different glyph per media family (image, audio, video, other). */
export function FileTypeIcon({ mimeType, className }: { mimeType: string; className?: string }) {
  if (mimeType.startsWith("image/")) {
    return <ImageFileIcon className={className} />;
  }

  if (mimeType.startsWith("audio/")) {
    return <AudioFileIcon className={className} />;
  }

  if (mimeType.startsWith("video/")) {
    return <VideoFileIcon className={className} />;
  }

  return <FileIcon className={className} />;
}
