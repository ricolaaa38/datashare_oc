import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  reporter: [
    ["html", { outputFolder: "./reports/e2e", open: "never" }],
    ["junit", { outputFile: "./reports/e2e/junit.xml" }],
  ],
  use: {
    baseURL: process.env.FRONTEND_URL ?? "http://localhost:3000",
  },
});
