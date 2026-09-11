import { UploadCloudIcon } from "@/components/ui/icons";

/** Empty state of the landing page: a big call to action opening the file picker. */
export function UploadHero({ onSelectFile }: { onSelectFile: () => void }) {
  return (
    <button
      type="button"
      onClick={onSelectFile}
      className="flex w-full cursor-pointer flex-col items-center gap-6 px-6"
    >
      <span className="max-w-[600px] text-center text-[30px] leading-10 font-light text-black">
        Tu veux partager un fichier ?
      </span>
      <span className="rounded-full bg-hero-halo p-6">
        <span className="flex rounded-full bg-hero-badge p-6">
          <UploadCloudIcon className="size-12 text-white" />
        </span>
      </span>
    </button>
  );
}
