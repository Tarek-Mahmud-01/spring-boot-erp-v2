import { useEffect } from "react";
import { useFieldArray, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Plus, Trash2 } from "lucide-react";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { Modal } from "@/shared/components/Modal";
import { Field } from "@/shared/components/Field";
import { Input } from "@/shared/components/Input";
import { Button } from "@/shared/components/Button";
import { createAdjustment } from "../slice/adjustmentSlice";

const lineSchema = z.object({
  productId: z.string().length(26, "26-char ID"),
  qtyDelta: z.coerce.number().refine((v) => v !== 0, "Must be non-zero"),
  writeOffReason: z.string().max(200).or(z.literal("")),
});

const schema = z.object({
  locationId: z.string().length(26, "26-char ID"),
  reason: z.string().min(1).max(200),
  notes: z.string().max(2000).or(z.literal("")),
  lines: z.array(lineSchema).min(1, "At least one line required"),
});
type FormValues = z.infer<typeof schema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

/** Create stock adjustment with variance lines (ARCHITECTURE.md §6 — logic via thunk/RHF). */
export function AdjustmentFormModal({ open, onOpenChange }: Props) {
  const dispatch = useAppDispatch();
  const saving = useAppSelector((s) => s.adjustment.saving);

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      reason: "",
      notes: "",
      lines: [{ productId: "", qtyDelta: 0, writeOffReason: "" }],
    },
  });
  const { fields, append, remove } = useFieldArray({ control, name: "lines" });

  useEffect(() => {
    if (!open) return;
    reset({
      locationId: "",
      reason: "",
      notes: "",
      lines: [{ productId: "", qtyDelta: 0, writeOffReason: "" }],
    });
  }, [open, reset]);

  const onSubmit = async (v: FormValues) => {
    const result = await dispatch(
      createAdjustment({
        locationId: v.locationId,
        reason: v.reason,
        notes: v.notes || undefined,
        autoComplete: false,
        lines: v.lines.map((l) => ({
          productId: l.productId,
          qtyDelta: l.qtyDelta,
          writeOffReason: l.writeOffReason || undefined,
        })),
      }),
    );
    if (!("error" in result)) onOpenChange(false);
  };

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title="New stock adjustment"
      className="max-w-3xl"
      footer={
        <>
          <Button variant="secondary" type="button" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button type="submit" form="adj-form" loading={saving}>
            Create
          </Button>
        </>
      }
    >
      <form id="adj-form" onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
        <div className="grid grid-cols-2 gap-4">
          <Field label="Location ID" error={errors.locationId?.message} required>
            {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("locationId")} />}
          </Field>
          <Field label="Reason" error={errors.reason?.message} required>
            {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("reason")} />}
          </Field>
          <Field label="Notes" error={errors.notes?.message} className="col-span-2">
            {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("notes")} />}
          </Field>
        </div>

        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <span className="text-small font-semibold text-fg">Lines (qty delta: negative removes, positive adds)</span>
            <Button type="button" variant="ghost" size="sm" onClick={() => append({ productId: "", qtyDelta: 0, writeOffReason: "" })}>
              <Plus className="h-4 w-4" />
              Add line
            </Button>
          </div>
          {errors.lines?.message && <p className="text-small text-danger">{errors.lines.message}</p>}
          {fields.map((f, idx) => (
            <div key={f.id} className="grid grid-cols-[2fr_1fr_2fr_auto] items-end gap-2">
              <Field label="Product ID" error={errors.lines?.[idx]?.productId?.message}>
                {({ id, invalid }) => <Input id={id} invalid={invalid} {...register(`lines.${idx}.productId`)} />}
              </Field>
              <Field label="Qty delta" error={errors.lines?.[idx]?.qtyDelta?.message}>
                {({ id, invalid }) => (
                  <Input id={id} type="number" step="0.001" invalid={invalid} {...register(`lines.${idx}.qtyDelta`)} />
                )}
              </Field>
              <Field label="Write-off reason" error={errors.lines?.[idx]?.writeOffReason?.message}>
                {({ id, invalid }) => <Input id={id} invalid={invalid} {...register(`lines.${idx}.writeOffReason`)} />}
              </Field>
              <Button type="button" variant="ghost" size="sm" onClick={() => remove(idx)} aria-label="Remove line">
                <Trash2 className="h-4 w-4" />
              </Button>
            </div>
          ))}
        </div>
      </form>
    </Modal>
  );
}
