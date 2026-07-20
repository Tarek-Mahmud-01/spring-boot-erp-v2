import * as Dialog from "@radix-ui/react-dialog";
import { Button } from "./Button";

interface ConfirmDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  destructive?: boolean;
  loading?: boolean;
  onConfirm: () => void;
}

/** Centered confirmation dialog (Radix Dialog). */
export function ConfirmDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
  destructive,
  loading,
  onConfirm,
}: ConfirmDialogProps) {
  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-40 bg-neutral-900/50 backdrop-blur-sm data-[state=open]:animate-in data-[state=open]:fade-in" />
        <Dialog.Content className="fixed left-1/2 top-1/2 z-50 w-full max-w-sm -translate-x-1/2 -translate-y-1/2 rounded-lg border border-border bg-surface p-5 shadow-lg focus:outline-none">
          <Dialog.Title className="text-h4 text-fg">{title}</Dialog.Title>
          {description && <Dialog.Description className="mt-2 text-body text-fg-muted">{description}</Dialog.Description>}
          <div className="mt-5 flex justify-end gap-2">
            <Button variant="secondary" type="button" onClick={() => onOpenChange(false)}>
              {cancelLabel}
            </Button>
            <Button variant={destructive ? "danger" : "primary"} type="button" loading={loading} onClick={onConfirm}>
              {confirmLabel}
            </Button>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
