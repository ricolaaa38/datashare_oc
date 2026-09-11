export function SiteFooter({ className = "text-white" }: { className?: string }) {
  return (
    <footer className="flex w-full justify-center">
      <div className={`w-full max-w-[1280px] p-4 text-base leading-6 ${className}`}>
        Copyright DataShare© 2025
      </div>
    </footer>
  );
}
