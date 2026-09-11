import type { ButtonHTMLAttributes } from "react";

export type ButtonVariant = "primary" | "secondary" | "tertiary" | "dark";
export type ButtonSize = "small" | "medium";

const SIZE_CLASSES: Record<ButtonSize, string> = {
  small: "px-3 py-2",
  medium: "p-3",
};

const ENABLED_VARIANT_CLASSES: Record<ButtonVariant, string> = {
  primary:
    "bg-action-primary-surface border border-action-primary-border text-action-primary-foreground hover:brightness-95",
  secondary:
    "border border-action-secondary-border text-action-secondary-foreground hover:bg-action-primary-surface",
  tertiary: "text-action-secondary-foreground hover:bg-action-primary-surface",
  dark: "bg-action-dark text-action-dark-foreground hover:brightness-110",
};

const DISABLED_VARIANT_CLASSES: Record<ButtonVariant, string> = {
  primary:
    "bg-action-disabled-surface border border-action-disabled-border text-action-disabled-foreground",
  secondary: "border border-action-disabled-foreground text-action-disabled-foreground",
  tertiary: "text-action-disabled-foreground",
  dark: "bg-action-dark-disabled text-action-disabled-foreground",
};

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  size?: ButtonSize;
  fullWidth?: boolean;
};

export function Button({
  variant = "primary",
  size = "medium",
  fullWidth = false,
  disabled = false,
  className = "",
  type = "button",
  children,
  ...buttonProps
}: ButtonProps) {
  const variantClasses = disabled ? DISABLED_VARIANT_CLASSES[variant] : ENABLED_VARIANT_CLASSES[variant];

  return (
    <button
      type={type}
      disabled={disabled}
      className={`inline-flex items-center justify-center gap-2 overflow-hidden rounded-lg text-base leading-4 transition ${
        SIZE_CLASSES[size]
      } ${variantClasses} ${fullWidth ? "w-full" : ""} ${
        disabled ? "cursor-not-allowed" : "cursor-pointer"
      } ${className}`}
      {...buttonProps}
    >
      {children}
    </button>
  );
}
