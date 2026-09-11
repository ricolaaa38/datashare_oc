import { AuthCard } from "@/components/auth/auth-card";
import { PublicPage } from "@/components/layout/public-page";

export default function LoginPage() {
  return (
    <PublicPage>
      <AuthCard mode="login" />
    </PublicPage>
  );
}
