import type { ReactNode } from "react";
import * as Dialog from "@radix-ui/react-dialog";
import { X } from "lucide-react";
import { cn } from "@/shared/utils/cn";

type Size = "md" | "lg" | "xl";

interface DrawerProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description?: string;
  children: ReactNode;
  footer?: ReactNode;
  size?: Size;
}

const WIDTH: Record<Size, string> = {
  md: "max-w-md",
  lg: "max-w-2xl",
  xl: "max-w-4xl",
};

/** Right-side sheet for rich create/edit forms (Radix Dialog, token-styled). */
export function Drawer({ open, onOpenChange, title, description, children, footer, size = "lg" }: DrawerProps) {
  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-40 bg-neutral-900/50 backdrop-blur-sm data-[state=open]:animate-in data-[state=open]:fade-in" />
        <Dialog.Content
          className={cn(
            "fixed inset-y-0 right-0 z-50 flex w-full flex-col border-l border-border bg-surface shadow-lg focus:outline-none",
            "data-[state=open]:animate-in data-[state=open]:slide-in-from-right",
            WIDTH[size],
          )}
        >
          <div className="flex items-start justify-between border-b border-border px-5 py-4">
            <div>
              <Dialog.Title className="text-h4 text-fg">{title}</Dialog.Title>
              {description && (
                <Dialog.Description className="mt-1 text-small text-fg-muted">{description}</Dialog.Description>
              )}
            </div>
            <Dialog.Close
              className="rounded-md p-1 text-fg-muted transition-colors hover:bg-surface-muted hover:text-fg focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
              aria-label="Close"
            >
              <X className="h-4 w-4" />
            </Dialog.Close>
          </div>
          <div className="flex-1 overflow-y-auto px-5 py-4">{children}</div>
          {footer && <div className="flex justify-end gap-2 border-t border-border px-5 py-3">{footer}</div>}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
