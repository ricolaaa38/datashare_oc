import { useId } from "react";
import type { SelectHTMLAttributes } from "react";

import { ChevronDownIcon } from "@/components/ui/icons";

export type SelectOption = {
  value: string | number;
  label: string;
};

type SelectFieldProps = SelectHTMLAttributes<HTMLSelectElement> & {
  label: string;
  options: readonly SelectOption[];
};

export function SelectField({ label, options, className = "", ...selectProps }: SelectFieldProps) {
  const selectId = useId();

  return (
    <div className="flex w-full flex-col gap-2">
      <label htmlFor={selectId} className="text-base leading-6 text-black">
        {label}
      </label>
      <div className="relative w-full">
        <select
          id={selectId}
          className={`w-full appearance-none rounded-lg border border-border-field bg-white px-3 py-2 pr-10 text-base leading-6 text-black outline-none transition focus:border-action-secondary-border ${className}`}
          {...selectProps}
        >
          {options.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <ChevronDownIcon className="pointer-events-none absolute top-1/2 right-3 size-4 -translate-y-1/2 text-black" />
      </div>
    </div>
  );
}
