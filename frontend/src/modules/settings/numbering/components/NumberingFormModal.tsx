import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { Modal } from "@/shared/components/Modal";
import { Field } from "@/shared/components/Field";
import { Input } from "@/shared/components/Input";
import { Select } from "@/shared/components/Select";
import { Button } from "@/shared/components/Button";
import { createNumberingRule, updateNumberingRule } from "../slice/numberingSlice";
import {
  DOCUMENT_TYPES,
  RESET_CADENCES,
  type NumberingRule,
} from "../api/numberingApi";

const schema = z.object({
  companyId: z.string().length(26, "Must be a 26-char ULID"),
  documentType: z.enum(DOCUMENT_TYPES),
  prefix: z.string().max(10, "Max 10 chars"),
  padding: z.coerce.number().int().min(4).max(10),
  resetCadence: z.enum(RESET_CADENCES),
  startValue: z.coerce.number().int().min(1),
});
type FormValues = z.infer<typeof schema>;

const DOC_OPTIONS = DOCUMENT_TYPES.map((v) => ({ value: v, label: v }));
const CADENCE_OPTIONS = RESET_CADENCES.map((v) => ({ value: v, label: v }));

const emptyValues: FormValues = {
  companyId: "",
  documentType: "INVOICE",
  prefix: "",
  padding: 4,
  resetCadence: "NEVER",
  startValue: 1,
};

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  editing: NumberingRule | null;
}

/** Create/edit numbering rule (ARCHITECTURE.md §6 — component ≤120, logic via thunk/RHF). */
export function NumberingFormModal({ open, onOpenChange, editing }: Props) {
  const dispatch = useAppDispatch();
  const saving = useAppSelector((s) => s.numbering.saving);
  const isEdit = Boolean(editing);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: emptyValues,
  });

  useEffect(() => {
    if (open) {
      reset(
        editing
          ? {
              companyId: editing.companyId,
              documentType: editing.documentType,
              prefix: editing.prefix,
              padding: editing.padding,
              resetCadence: editing.resetCadence,
              startValue: editing.startValue,
            }
          : emptyValues,
      );
    }
  }, [open, editing, reset]);

  const onSubmit = async (values: FormValues) => {
    if (editing) {
      const result = await dispatch(
        updateNumberingRule({
          id: editing.id,
          body: {
            prefix: values.prefix,
            padding: values.padding,
            resetCadence: values.resetCadence,
            startValue: values.startValue,
            version: editing.version,
          },
        }),
      );
      if (!("error" in result)) onOpenChange(false);
      return;
    }
    const result = await dispatch(createNumberingRule(values));
    if (!("error" in result)) onOpenChange(false);
  };

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title={isEdit ? `Edit ${editing?.documentType} rule` : "New numbering rule"}
      footer={
        <>
          <Button variant="secondary" onClick={() => onOpenChange(false)} type="button">
            Cancel
          </Button>
          <Button type="submit" form="numbering-form" loading={saving}>
            {isEdit ? "Save" : "Create"}
          </Button>
        </>
      }
    >
      <form id="numbering-form" onSubmit={handleSubmit(onSubmit)} className="grid grid-cols-2 gap-4" noValidate>
        <Field label="Company ID" error={errors.companyId?.message} required className="col-span-2">
          {({ id, invalid }) => (
            <Input id={id} invalid={invalid} disabled={isEdit} maxLength={26} {...register("companyId")} />
          )}
        </Field>
        <Field label="Document type" error={errors.documentType?.message} required className="col-span-1">
          {({ id, invalid }) => (
            <Select id={id} invalid={invalid} disabled={isEdit} options={DOC_OPTIONS} {...register("documentType")} />
          )}
        </Field>
        <Field label="Reset cadence" error={errors.resetCadence?.message} required className="col-span-1">
          {({ id, invalid }) => (
            <Select id={id} invalid={invalid} options={CADENCE_OPTIONS} {...register("resetCadence")} />
          )}
        </Field>
        <Field label="Prefix" error={errors.prefix?.message} className="col-span-2">
          {({ id, invalid }) => <Input id={id} invalid={invalid} maxLength={10} {...register("prefix")} />}
        </Field>
        <Field label="Padding" error={errors.padding?.message} required className="col-span-1">
          {({ id, invalid }) => (
            <Input id={id} type="number" min={4} max={10} invalid={invalid} {...register("padding")} />
          )}
        </Field>
        <Field label="Start value" error={errors.startValue?.message} required className="col-span-1">
          {({ id, invalid }) => (
            <Input id={id} type="number" min={1} invalid={invalid} {...register("startValue")} />
          )}
        </Field>
      </form>
    </Modal>
  );
}
