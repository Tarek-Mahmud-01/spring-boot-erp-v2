import { useRef, useState, type DragEvent } from "react";
import { UploadCloud, File as FileIcon, X } from "lucide-react";
import { cn } from "@/shared/utils/cn";

interface FileUploadProps {
  value: File[];
  onChange: (files: File[]) => void;
  accept?: string;
  multiple?: boolean;
  disabled?: boolean;
}

/** Click-or-drop file field. Captures File objects and lists them (upload wired by the caller). */
export function FileUpload({ value, onChange, accept, multiple = true, disabled }: FileUploadProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);

  const add = (files: FileList | null) => {
    if (!files || files.length === 0) return;
    const next = multiple ? [...value, ...Array.from(files)] : [Array.from(files)[0]];
    onChange(next);
  };

  const onDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setDragging(false);
    if (!disabled) add(e.dataTransfer.files);
  };

  return (
    <div className="flex flex-col gap-2">
      <div
        role="button"
        tabIndex={0}
        onClick={() => !disabled && inputRef.current?.click()}
        onKeyDown={(e) => (e.key === "Enter" || e.key === " ") && !disabled && inputRef.current?.click()}
        onDragOver={(e) => {
          e.preventDefault();
          if (!disabled) setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
        className={cn(
          "flex cursor-pointer flex-col items-center justify-center gap-1 rounded-lg border border-dashed px-4 py-6 text-center transition-colors",
          dragging ? "border-primary bg-surface-muted" : "border-border hover:bg-surface-muted",
          disabled && "cursor-not-allowed opacity-60",
        )}
      >
        <UploadCloud className="h-5 w-5 text-fg-muted" aria-hidden />
        <span className="text-body text-fg">Click to upload or drag &amp; drop</span>
        {accept && <span className="text-small text-fg-muted">{accept}</span>}
        <input
          ref={inputRef}
          type="file"
          accept={accept}
          multiple={multiple}
          disabled={disabled}
          className="hidden"
          onChange={(e) => add(e.target.files)}
        />
      </div>
      {value.length > 0 && (
        <ul className="flex flex-col gap-1">
          {value.map((f, i) => (
            <li
              key={`${f.name}-${i}`}
              className="flex items-center justify-between gap-2 rounded-md border border-border bg-surface px-3 py-1.5 text-small text-fg"
            >
              <span className="flex items-center gap-2 truncate">
                <FileIcon className="h-4 w-4 shrink-0 text-fg-muted" aria-hidden />
                <span className="truncate">{f.name}</span>
                <span className="shrink-0 text-fg-muted">{(f.size / 1024).toFixed(0)} KB</span>
              </span>
              <button
                type="button"
                aria-label={`Remove ${f.name}`}
                onClick={() => onChange(value.filter((_, idx) => idx !== i))}
                className="text-fg-muted hover:text-danger"
              >
                <X className="h-4 w-4" />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
