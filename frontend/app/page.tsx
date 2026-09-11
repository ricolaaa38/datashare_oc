import { PublicPage } from "@/components/layout/public-page";
import { ShareFileFlow } from "@/components/upload/share-file-flow";

/** Public landing page: sharing a file never requires an account. */
export default function HomePage() {
  return (
    <PublicPage>
      <ShareFileFlow />
    </PublicPage>
  );
}
