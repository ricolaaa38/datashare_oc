import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./",
  use: {
    baseURL: process.env.FRONTEND_URL ?? "http://localhost:3000",
  },
});
