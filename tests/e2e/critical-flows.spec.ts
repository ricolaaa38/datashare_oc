import { expect, test } from "@playwright/test";

function uniqueEmail(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 10000)}@datashare.test`;
}

test.describe("Fonctionnalités critiques", () => {
  test("un visiteur peut partager un fichier sans compte et le télécharger", async ({ page }) => {
    await page.goto("/");

    await page.locator('input[type="file"]').setInputFiles({
      name: "rapport.pdf",
      mimeType: "application/pdf",
      buffer: Buffer.from("contenu de test"),
    });
    await expect(page.getByRole("heading", { name: "Ajouter un fichier" })).toBeVisible();
    await page.getByRole("button", { name: "Téléverser" }).click();

    const shareLink = page.locator('a[href*="/download/"]');
    await expect(shareLink).toBeVisible();
    const href = await shareLink.getAttribute("href");
    expect(href).toBeTruthy();

    await page.goto(href!);
    await expect(page.getByText("rapport.pdf")).toBeVisible();

    const downloadPromise = page.waitForEvent("download");
    await page.getByRole("button", { name: "Télécharger" }).click();
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toBe("rapport.pdf");
  });

  test("un utilisateur peut créer un compte, se connecter, ajouter un fichier et le retrouver dans son espace", async ({
    page,
  }) => {
    const email = uniqueEmail("membre");
    const password = "longenoughpassword";

    await page.goto("/register");
    await page.getByLabel("Email").fill(email);
    await page.getByLabel("Mot de passe", { exact: true }).fill(password);
    await page.getByLabel("Vérification du mot de passe").fill(password);
    await page.getByRole("button", { name: "Créer mon compte" }).click();
    await expect(page).toHaveURL(/\/files$/);

    await page.goto("/");
    await page.locator('input[type="file"]').setInputFiles({
      name: "contrat.pdf",
      mimeType: "application/pdf",
      buffer: Buffer.from("contenu du contrat"),
    });
    await page.getByRole("button", { name: "Téléverser" }).click();
    await expect(page.locator('a[href*="/download/"]')).toBeVisible();

    await page.goto("/files");
    await expect(page.getByText("contrat.pdf")).toBeVisible();

    await page.getByRole("button", { name: "Supprimer" }).click();
    await expect(page.getByText("contrat.pdf")).not.toBeVisible();
  });

  test("une mauvaise combinaison identifiant/mot de passe est rejetée avec un message clair", async ({ page }) => {
    const email = uniqueEmail("erreur");

    await page.goto("/register");
    await page.getByLabel("Email").fill(email);
    await page.getByLabel("Mot de passe", { exact: true }).fill("longenoughpassword");
    await page.getByLabel("Vérification du mot de passe").fill("longenoughpassword");
    await page.getByRole("button", { name: "Créer mon compte" }).click();
    await expect(page).toHaveURL(/\/files$/);

    await page.goto("/login");
    await page.getByLabel("Email").fill(email);
    await page.getByLabel("Mot de passe").fill("wrongpassword");
    await page.getByRole("button", { name: "Connexion" }).click();

    await expect(page.getByText("Invalid credentials")).toBeVisible();
  });
});
