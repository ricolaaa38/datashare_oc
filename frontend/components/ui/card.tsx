import type { ReactNode } from "react";

/** White rounded panel used by every public screen of the design. */
export function Card({ children, className = "" }: { children: ReactNode; className?: string }) {
  return (
    <div
      className={`flex w-full max-w-[640px] flex-col items-center gap-6 rounded-2xl bg-white p-6 shadow-card ${className}`}
    >
      {children}
    </div>
  );
}

export function CardTitle({ children }: { children: ReactNode }) {
  return <h1 className="w-full text-center text-[28px] leading-10 font-bold text-black">{children}</h1>;
}
