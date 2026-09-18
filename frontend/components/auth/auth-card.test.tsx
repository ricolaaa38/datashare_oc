import { fireEvent, render, screen, waitFor } from "@testing-library/react";

import { AuthCard } from "@/components/auth/auth-card";
import { loginUser } from "@/lib/api/auth-api";

const push = jest.fn();

jest.mock("next/navigation", () => ({
  useRouter: () => ({ push }),
}));

jest.mock("@/lib/api/auth-api", () => ({
  loginUser: jest.fn(),
  registerUser: jest.fn(),
}));

describe("AuthCard", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("submits valid login credentials and opens the file area", async () => {
    jest.mocked(loginUser).mockResolvedValue({} as never);
    render(<AuthCard mode="login" />);

    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "sam@datashare.test" } });
    fireEvent.change(screen.getByLabelText("Mot de passe"), { target: { value: "password123" } });
    fireEvent.click(screen.getByRole("button", { name: "Connexion" }));

    await waitFor(() => expect(loginUser).toHaveBeenCalledWith("sam@datashare.test", "password123"));
    expect(push).toHaveBeenCalledWith("/files");
  });

  it("renders the password confirmation field during registration", () => {
    render(<AuthCard mode="register" />);

    expect(screen.getByRole("heading", { name: "Créer un compte" })).toBeInTheDocument();
    expect(screen.getByLabelText("Vérification du mot de passe")).toBeRequired();
  });
});