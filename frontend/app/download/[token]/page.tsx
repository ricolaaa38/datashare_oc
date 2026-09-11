import { DownloadFileCard } from "@/components/download/download-file-card";
import { PublicPage } from "@/components/layout/public-page";

export default async function DownloadPage({ params }: PageProps<"/download/[token]">) {
  const { token } = await params;

  return (
    <PublicPage>
      <DownloadFileCard token={token} />
    </PublicPage>
  );
}
