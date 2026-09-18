import { test, expect } from "@playwright/test";

test("la page d'accueil se charge", async ({ page }) => {
  await page.goto("/");
  await expect(page).toHaveTitle(/./);
});

test("les pages publiques d'authentification sont accessibles", async ({ page }) => {
  await page.goto("/login");
  await expect(page.getByRole("heading", { name: "Connexion" })).toBeVisible();
  await expect(page.getByRole("link", { name: "Créer un compte" })).toHaveAttribute("href", "/register");

  await page.goto("/register");
  await expect(page.getByRole("heading", { name: "Créer un compte" })).toBeVisible();
  await expect(page.getByLabel("Vérification du mot de passe")).toBeVisible();
});
