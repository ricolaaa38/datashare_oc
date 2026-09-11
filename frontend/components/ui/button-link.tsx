import Link from "next/link";
import type { ComponentProps } from "react";

import type { ButtonSize, ButtonVariant } from "@/components/ui/button";

const SIZE_CLASSES: Record<ButtonSize, string> = {
  small: "px-3 py-2",
  medium: "p-3",
};

const VARIANT_CLASSES: Record<ButtonVariant, string> = {
  primary:
    "bg-action-primary-surface border border-action-primary-border text-action-primary-foreground hover:brightness-95",
  secondary:
    "border border-action-secondary-border text-action-secondary-foreground hover:bg-action-primary-surface",
  tertiary: "text-action-secondary-foreground hover:bg-action-primary-surface",
  dark: "bg-action-dark text-action-dark-foreground hover:brightness-110",
};

type ButtonLinkProps = ComponentProps<typeof Link> & {
  variant?: ButtonVariant;
  size?: ButtonSize;
  fullWidth?: boolean;
};

export function ButtonLink({
  variant = "primary",
  size = "medium",
  fullWidth = false,
  className = "",
  children,
  ...linkProps
}: ButtonLinkProps) {
  return (
    <Link
      className={`inline-flex items-center justify-center gap-2 overflow-hidden rounded-lg text-base leading-4 transition ${
        SIZE_CLASSES[size]
      } ${VARIANT_CLASSES[variant]} ${fullWidth ? "w-full" : ""} ${className}`}
      {...linkProps}
    >
      {children}
    </Link>
  );
}
