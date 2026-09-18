import { fireEvent, render, screen, waitFor } from "@testing-library/react";

import { ShareFileFlow } from "@/components/upload/share-file-flow";
import { uploadAnonymousFile, uploadOwnedFile } from "@/lib/api/files-api";
import { hasActiveSession } from "@/lib/session";
import type { FileResource } from "@/lib/types";

jest.mock("@/lib/api/files-api", () => ({
  uploadAnonymousFile: jest.fn(),
  uploadOwnedFile: jest.fn(),
}));

jest.mock("@/lib/session", () => ({
  hasActiveSession: jest.fn(),
}));

const uploadedFile: FileResource = {
  fileId: 1,
  originalName: "report.pdf",
  sizeBytes: 42,
  mimeType: "application/pdf",
  createdAt: "2026-09-01T00:00:00Z",
  expiresAt: "2026-09-08T00:00:00Z",
  hasPassword: false,
  status: "VALID",
  downloadToken: "abc123",
  downloadUrl: "http://localhost:8080/downloads/abc123",
};

function selectFile(file: File) {
  const input = document.querySelector('input[type="file"]') as HTMLInputElement;
  Object.defineProperty(input, "files", { value: [file] });
  fireEvent.change(input);
}

describe("ShareFileFlow", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("uploads anonymously when there is no active session and shows the share link", async () => {
    jest.mocked(hasActiveSession).mockReturnValue(false);
    jest.mocked(uploadAnonymousFile).mockResolvedValue(uploadedFile);
    render(<ShareFileFlow />);

    selectFile(new File(["content"], "report.pdf", { type: "application/pdf" }));
    fireEvent.click(await screen.findByRole("button", { name: "Téléverser" }));

    await waitFor(() => expect(uploadAnonymousFile).toHaveBeenCalled());
    expect(uploadOwnedFile).not.toHaveBeenCalled();
    expect(screen.getByText("http://localhost/download/abc123")).toBeInTheDocument();
  });

  it("uploads to the owned endpoint when a session is active", async () => {
    jest.mocked(hasActiveSession).mockReturnValue(true);
    jest.mocked(uploadOwnedFile).mockResolvedValue(uploadedFile);
    render(<ShareFileFlow />);

    selectFile(new File(["content"], "report.pdf", { type: "application/pdf" }));
    fireEvent.click(await screen.findByRole("button", { name: "Téléverser" }));

    await waitFor(() => expect(uploadOwnedFile).toHaveBeenCalled());
    expect(uploadAnonymousFile).not.toHaveBeenCalled();
  });

  it("shows the backend error message when the upload fails", async () => {
    jest.mocked(hasActiveSession).mockReturnValue(false);
    jest.mocked(uploadAnonymousFile).mockRejectedValue(new Error("Fichier trop volumineux"));
    render(<ShareFileFlow />);

    selectFile(new File(["content"], "report.pdf", { type: "application/pdf" }));
    fireEvent.click(await screen.findByRole("button", { name: "Téléverser" }));

    expect(await screen.findByText("Fichier trop volumineux")).toBeInTheDocument();
  });
});
