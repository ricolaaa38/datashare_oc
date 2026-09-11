import { AuthCard } from "@/components/auth/auth-card";
import { PublicPage } from "@/components/layout/public-page";

export default function RegisterPage() {
  return (
    <PublicPage>
      <AuthCard mode="register" />
    </PublicPage>
  );
}
