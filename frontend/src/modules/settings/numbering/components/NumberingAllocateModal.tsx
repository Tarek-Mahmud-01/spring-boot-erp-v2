import { useEffect, useState } from "react";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { Modal } from "@/shared/components/Modal";
import { Field } from "@/shared/components/Field";
import { Input } from "@/shared/components/Input";
import { Button } from "@/shared/components/Button";
import { allocateNumber } from "../slice/numberingSlice";
import type { NumberingRule } from "../api/numberingApi";

const today = () => new Date().toISOString().slice(0, 10);

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  rule: NumberingRule | null;
}

/** Allocate the next sequence number for a rule at a given document date. */
export function NumberingAllocateModal({ open, onOpenChange, rule }: Props) {
  const dispatch = useAppDispatch();
  const saving = useAppSelector((s) => s.numbering.saving);
  const [documentDate, setDocumentDate] = useState(today());

  useEffect(() => {
    if (open) setDocumentDate(today());
  }, [open]);

  const onAllocate = async () => {
    if (!rule) return;
    const result = await dispatch(allocateNumber({ id: rule.id, documentDate }));
    if (!("error" in result)) onOpenChange(false);
  };

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title={rule ? `Allocate ${rule.documentType} number` : "Allocate number"}
      description="Consumes the next sequence number for the selected document date."
      footer={
        <>
          <Button variant="secondary" onClick={() => onOpenChange(false)} type="button">
            Cancel
          </Button>
          <Button type="button" onClick={onAllocate} loading={saving}>
            Allocate
          </Button>
        </>
      }
    >
      <Field label="Document date" required>
        {({ id, invalid }) => (
          <Input
            id={id}
            type="date"
            invalid={invalid}
            value={documentDate}
            onChange={(e) => setDocumentDate(e.target.value)}
          />
        )}
      </Field>
    </Modal>
  );
}
