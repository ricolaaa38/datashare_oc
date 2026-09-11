export type SegmentedOption<TValue extends string> = {
  value: TValue;
  label: string;
};

type SegmentedControlProps<TValue extends string> = {
  options: readonly SegmentedOption<TValue>[];
  value: TValue;
  onChange: (value: TValue) => void;
  ariaLabel: string;
};

export function SegmentedControl<TValue extends string>({
  options,
  value,
  onChange,
  ariaLabel,
}: SegmentedControlProps<TValue>) {
  return (
    <div
      role="tablist"
      aria-label={ariaLabel}
      className="flex self-start overflow-hidden rounded-3xl border border-border-warm bg-segment-surface"
    >
      {options.map((option) => {
        const isSelected = option.value === value;

        return (
          <button
            key={option.value}
            type="button"
            role="tab"
            aria-selected={isSelected}
            onClick={() => onChange(option.value)}
            className={`cursor-pointer px-4 py-2 text-base leading-4 transition ${
              isSelected ? "bg-segment-active text-white" : "text-black hover:bg-white/40"
            }`}
          >
            {option.label}
          </button>
        );
      })}
    </div>
  );
}
