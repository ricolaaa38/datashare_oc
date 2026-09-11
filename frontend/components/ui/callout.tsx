import { AlertOctagonIcon, AlertTriangleIcon, InfoIcon } from "@/components/ui/icons";

export type CalloutTone = "info" | "warning" | "danger";

const TONE_CLASSES: Record<CalloutTone, string> = {
  info: "bg-callout-info-surface border-callout-info-border text-callout-info-foreground",
  warning: "bg-callout-warning-surface border-callout-warning-border text-callout-warning-foreground",
  danger: "bg-callout-danger-surface border-callout-danger-border text-callout-danger-foreground",
};

const TONE_ICONS: Record<CalloutTone, typeof InfoIcon> = {
  info: InfoIcon,
  warning: AlertTriangleIcon,
  danger: AlertOctagonIcon,
};

export function Callout({ tone, children }: { tone: CalloutTone; children: React.ReactNode }) {
  const ToneIcon = TONE_ICONS[tone];

  return (
    <div
      role={tone === "danger" ? "alert" : "status"}
      className={`flex w-full items-center gap-2 rounded-lg border p-2 text-sm leading-4 ${TONE_CLASSES[tone]}`}
    >
      <ToneIcon />
      <p className="min-w-0 flex-1">{children}</p>
    </div>
  );
}
