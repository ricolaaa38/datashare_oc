"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

import { Button } from "@/components/ui/button";
import { ButtonLink } from "@/components/ui/button-link";
import { Callout } from "@/components/ui/callout";
import { Card, CardTitle } from "@/components/ui/card";
import { TextField } from "@/components/ui/text-field";
import { loginUser, registerUser } from "@/lib/api/auth-api";
import { MIN_PASSWORD_LENGTH, ROUTES } from "@/lib/config";

export type AuthMode = "login" | "register";

const CONTENT = {
  login: {
    title: "Connexion",
    submitLabel: "Connexion",
    pendingLabel: "Connexion...",
    alternativeLabel: "Créer un compte",
    alternativeHref: ROUTES.register,
  },
  register: {
    title: "Créer un compte",
    submitLabel: "Créer mon compte",
    pendingLabel: "Création...",
    alternativeLabel: "J'ai déjà un compte",
    alternativeHref: ROUTES.login,
  },
} as const;

export function AuthCard({ mode }: { mode: AuthMode }) {
  const router = useRouter();
  const content = CONTENT[mode];
  const isRegistration = mode === "register";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirmation, setPasswordConfirmation] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage("");

    if (isRegistration && password !== passwordConfirmation) {
      setErrorMessage("Les deux mots de passe ne correspondent pas.");
      return;
    }

    setIsSubmitting(true);

    try {
      if (isRegistration) {
        await registerUser(email, password);
        await loginUser(email, password);
      } else {
        await loginUser(email, password);
      }
      router.push(ROUTES.myFiles);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Une erreur est survenue.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Card>
      <CardTitle>{content.title}</CardTitle>

      <form className="flex w-full flex-col gap-6" onSubmit={handleSubmit}>
        <div className="flex w-full flex-col gap-4">
          <TextField
            label="Email"
            type="email"
            value={email}
            required
            autoComplete="email"
            placeholder="Saisissez votre email..."
            onChange={(event) => setEmail(event.target.value)}
          />
          <TextField
            label="Mot de passe"
            type="password"
            value={password}
            required
            minLength={MIN_PASSWORD_LENGTH}
            autoComplete={isRegistration ? "new-password" : "current-password"}
            placeholder="Saisissez votre mot de passe..."
            onChange={(event) => setPassword(event.target.value)}
          />
          {isRegistration && (
            <TextField
              label="Vérification du mot de passe"
              type="password"
              value={passwordConfirmation}
              required
              minLength={MIN_PASSWORD_LENGTH}
              autoComplete="new-password"
              placeholder="Saisissez-le à nouveau"
              onChange={(event) => setPasswordConfirmation(event.target.value)}
            />
          )}
        </div>

        {errorMessage && <Callout tone="danger">{errorMessage}</Callout>}

        <div className="flex w-full flex-col gap-2">
          <ButtonLink href={content.alternativeHref} variant="tertiary" fullWidth>
            {content.alternativeLabel}
          </ButtonLink>
          <Button type="submit" fullWidth disabled={isSubmitting}>
            {isSubmitting ? content.pendingLabel : content.submitLabel}
          </Button>
        </div>
      </form>
    </Card>
  );
}
