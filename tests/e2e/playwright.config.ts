import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./",
  reporter: [
    ["html", { outputFolder: "../reports/e2e", open: "never" }],
    ["junit", { outputFile: "../reports/e2e/junit.xml" }],
  ],
  use: {
    baseURL: process.env.FRONTEND_URL ?? "http://localhost:3000",
    // The default headless-shell build fails to navigate to plain hostnames
    // (e.g. "frontend-test") inside the Docker test network; the full
    // Chromium build handles it correctly.
    channel: "chromium",
  },
});
