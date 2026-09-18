import {
  buildShareLink,
  formatExpiryLabel,
  formatExpirySentence,
  formatFileSize,
  isExpired,
} from "@/lib/format";

describe("format", () => {
  it("formats file sizes using the French units", () => {
    expect(formatFileSize(0)).toBe("0 o");
    expect(formatFileSize(1024)).toBe("1 Ko");
    expect(formatFileSize(1024 * 1024 * 2.5)).toBe("2,5 Mo");
  });

  it("formats expiry information for available and expired files", () => {
    const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString();
    const yesterday = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();

    expect(formatExpiryLabel(tomorrow)).toBe("Expire demain");
    expect(formatExpirySentence(tomorrow)).toBe("Ce fichier expirera demain.");
    expect(isExpired(yesterday)).toBe(true);
    expect(formatExpiryLabel(yesterday)).toBe("Expiré");
  });

  it("builds a share link to the frontend download route", () => {
    expect(buildShareLink("token-123")).toBe("http://localhost/download/token-123");
  });
});