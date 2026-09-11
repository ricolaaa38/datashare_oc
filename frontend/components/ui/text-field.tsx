import { useId } from "react";
import type { InputHTMLAttributes } from "react";

type TextFieldProps = InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  description?: string;
  error?: string;
};

export function TextField({ label, description, error, className = "", ...inputProps }: TextFieldProps) {
  const inputId = useId();
  const messageId = `${inputId}-message`;

  return (
    <div className="flex w-full flex-col gap-2">
      <label htmlFor={inputId} className="text-base leading-6 text-black">
        {label}
      </label>
      <input
        id={inputId}
        aria-describedby={description || error ? messageId : undefined}
        aria-invalid={error ? true : undefined}
        className={`w-full rounded-lg border bg-white px-3 py-2 text-base leading-6 text-black outline-none transition placeholder:text-text-placeholder focus:border-action-secondary-border ${
          error ? "border-callout-danger-border" : "border-border-field"
        } ${className}`}
        {...inputProps}
      />
      {(error || description) && (
        <p
          id={messageId}
          className={`text-sm leading-4 ${error ? "text-callout-danger-foreground" : "text-black/60"}`}
        >
          {error || description}
        </p>
      )}
    </div>
  );
}
