import { ROUTES } from "@/lib/config";

const BYTES_PER_UNIT = 1024;
const SIZE_UNITS = ["o", "Ko", "Mo", "Go"];
const MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000;

export function formatFileSize(sizeBytes: number): string {
  if (!Number.isFinite(sizeBytes) || sizeBytes <= 0) {
    return "0 o";
  }

  const unitIndex = Math.min(
    Math.floor(Math.log(sizeBytes) / Math.log(BYTES_PER_UNIT)),
    SIZE_UNITS.length - 1,
  );
  const size = sizeBytes / BYTES_PER_UNIT ** unitIndex;
  const formattedSize = size.toLocaleString("fr-FR", {
    maximumFractionDigits: size >= 10 || unitIndex === 0 ? 0 : 1,
  });

  return `${formattedSize} ${SIZE_UNITS[unitIndex]}`;
}

export function getDaysUntilExpiry(expiresAt: string): number {
  const remainingMilliseconds = new Date(expiresAt).getTime() - Date.now();
  return Math.ceil(remainingMilliseconds / MILLISECONDS_PER_DAY);
}

export function isExpired(expiresAt: string): boolean {
  return getDaysUntilExpiry(expiresAt) <= 0;
}

/** Short label used in the file list, e.g. "Expire dans 2 jours". */
export function formatExpiryLabel(expiresAt: string): string {
  const remainingDays = getDaysUntilExpiry(expiresAt);

  if (remainingDays <= 0) {
    return "Expiré";
  }

  return remainingDays === 1 ? "Expire demain" : `Expire dans ${remainingDays} jours`;
}

/** Full sentence used in the download callout, e.g. "Ce fichier expirera dans 3 jours." */
export function formatExpirySentence(expiresAt: string): string {
  const remainingDays = getDaysUntilExpiry(expiresAt);

  if (remainingDays <= 0) {
    return "Ce fichier n'est plus disponible en téléchargement car il a expiré.";
  }

  return remainingDays === 1
    ? "Ce fichier expirera demain."
    : `Ce fichier expirera dans ${remainingDays} jours.`;
}

/** Shareable link pointing at the DataShare download page rather than the raw API endpoint. */
export function buildShareLink(downloadToken: string): string {
  const origin = typeof window === "undefined" ? "" : window.location.origin;
  return `${origin}${ROUTES.download(downloadToken)}`;
}
