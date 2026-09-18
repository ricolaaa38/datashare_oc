import { fireEvent, render, screen, waitFor } from "@testing-library/react";

import { DownloadFileCard } from "@/components/download/download-file-card";
import { ApiError } from "@/lib/api/http-client";
import { fetchDownloadContent, fetchDownloadMetadata, saveBlobAs } from "@/lib/api/downloads-api";
import type { DownloadMetadata } from "@/lib/types";

jest.mock("@/lib/api/downloads-api", () => ({
  fetchDownloadMetadata: jest.fn(),
  fetchDownloadContent: jest.fn(),
  saveBlobAs: jest.fn(),
}));

const protectedMetadata: DownloadMetadata = {
  originalName: "report.pdf",
  mimeType: "application/pdf",
  sizeBytes: 42,
  expiresAt: "2099-01-01T00:00:00Z",
  hasPassword: true,
};

describe("DownloadFileCard", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("requires a password before it lets the user download a protected file", async () => {
    jest.mocked(fetchDownloadMetadata).mockResolvedValue(protectedMetadata);
    render(<DownloadFileCard token="a-token" />);

    expect(await screen.findByRole("button", { name: "Télécharger" })).toBeDisabled();
  });

  it("downloads and saves the file once the correct password is entered", async () => {
    jest.mocked(fetchDownloadMetadata).mockResolvedValue(protectedMetadata);
    const blob = new Blob(["content"]);
    jest.mocked(fetchDownloadContent).mockResolvedValue(blob);
    render(<DownloadFileCard token="a-token" />);

    fireEvent.change(await screen.findByLabelText("Mot de passe"), { target: { value: "secret1" } });
    fireEvent.click(screen.getByRole("button", { name: "Télécharger" }));

    await waitFor(() => expect(fetchDownloadContent).toHaveBeenCalledWith("a-token", "secret1"));
    expect(saveBlobAs).toHaveBeenCalledWith(blob, "report.pdf");
  });

  it("shows a wrong-password message on a 401 response", async () => {
    jest.mocked(fetchDownloadMetadata).mockResolvedValue(protectedMetadata);
    jest.mocked(fetchDownloadContent).mockRejectedValue(new ApiError("Unauthorized", 401));
    render(<DownloadFileCard token="a-token" />);

    fireEvent.change(await screen.findByLabelText("Mot de passe"), { target: { value: "wrong" } });
    fireEvent.click(screen.getByRole("button", { name: "Télécharger" }));

    expect(await screen.findByText("Mot de passe incorrect.")).toBeInTheDocument();
  });

  it("reports an unknown download link", async () => {
    jest.mocked(fetchDownloadMetadata).mockRejectedValue(new ApiError("Not found", 404));
    render(<DownloadFileCard token="unknown" />);

    expect(await screen.findByText("Ce lien de téléchargement est introuvable.")).toBeInTheDocument();
  });
});
