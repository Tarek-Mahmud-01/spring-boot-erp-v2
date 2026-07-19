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
import { createRequisition } from "../slice/requisitionSlice";

const lineSchema = z.object({
  productId: z.string().length(26, "26-char ID"),
  qty: z.coerce.number().positive(),
  unitPriceMajor: z.coerce.number().min(0),
  discountPercent: z.coerce.number().min(0),
});

const schema = z.object({
  locationId: z.string().length(26, "26-char ID"),
  currency: z.string().length(3),
  neededByDate: z.string().or(z.literal("")),
  notes: z.string().max(2000).or(z.literal("")),
  lines: z.array(lineSchema).min(1, "At least one line required"),
});
type FormValues = z.infer<typeof schema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

/** Create purchase requisition with lines (ARCHITECTURE.md §6 — logic via thunk/RHF). */
export function RequisitionFormModal({ open, onOpenChange }: Props) {
  const dispatch = useAppDispatch();
  const saving = useAppSelector((s) => s.requisition.saving);

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      currency: "SAR",
      neededByDate: "",
      notes: "",
      lines: [{ productId: "", qty: 1, unitPriceMajor: 0, discountPercent: 0 }],
    },
  });
  const { fields, append, remove } = useFieldArray({ control, name: "lines" });

  useEffect(() => {
    if (!open) return;
    reset({
      locationId: "",
      currency: "SAR",
      neededByDate: "",
      notes: "",
      lines: [{ productId: "", qty: 1, unitPriceMajor: 0, discountPercent: 0 }],
    });
  }, [open, reset]);

  const onSubmit = async (v: FormValues) => {
    const lines = v.lines.map((l) => {
      const unitPriceAmount = Math.round(l.unitPriceMajor * 100);
      const lineTotalAmount = Math.round(unitPriceAmount * l.qty * (1 - l.discountPercent / 100));
      return {
        productId: l.productId,
        qty: l.qty,
        unitPriceAmount,
        unitPriceCurrency: v.currency,
        discountPercent: l.discountPercent,
        lineTotalAmount,
      };
    });
    const totalAmount = lines.reduce((sum, l) => sum + l.lineTotalAmount, 0);
    const result = await dispatch(
      createRequisition({
        locationId: v.locationId,
        currency: v.currency,
        neededByDate: v.neededByDate ? new Date(v.neededByDate).toISOString() : undefined,
        notes: v.notes || undefined,
        totalAmount,
        lines,
      }),
    );
    if (!("error" in result)) onOpenChange(false);
  };

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title="New requisition"
      className="max-w-3xl"
      footer={
        <>
          <Button variant="secondary" type="button" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button type="submit" form="pr-form" loading={saving}>
            Create
          </Button>
        </>
      }
    >
      <form id="pr-form" onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
        <div className="grid grid-cols-2 gap-4">
          <Field label="Location ID" error={errors.locationId?.message} required>
            {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("locationId")} />}
          </Field>
          <Field label="Currency" error={errors.currency?.message} required>
            {({ id, invalid }) => <Input id={id} invalid={invalid} maxLength={3} {...register("currency")} />}
          </Field>
          <Field label="Needed by" error={errors.neededByDate?.message}>
            {({ id, invalid }) => <Input id={id} type="date" invalid={invalid} {...register("neededByDate")} />}
          </Field>
          <Field label="Notes" error={errors.notes?.message}>
            {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("notes")} />}
          </Field>
        </div>

        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <span className="text-small font-semibold text-fg">Lines</span>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => append({ productId: "", qty: 1, unitPriceMajor: 0, discountPercent: 0 })}
            >
              <Plus className="h-4 w-4" />
              Add line
            </Button>
          </div>
          {errors.lines?.message && <p className="text-small text-danger">{errors.lines.message}</p>}
          {fields.map((f, idx) => (
            <div key={f.id} className="grid grid-cols-[2fr_1fr_1fr_1fr_auto] items-end gap-2">
              <Field label="Product ID" error={errors.lines?.[idx]?.productId?.message}>
                {({ id, invalid }) => <Input id={id} invalid={invalid} {...register(`lines.${idx}.productId`)} />}
              </Field>
              <Field label="Qty" error={errors.lines?.[idx]?.qty?.message}>
                {({ id, invalid }) => (
                  <Input id={id} type="number" step="0.001" invalid={invalid} {...register(`lines.${idx}.qty`)} />
                )}
              </Field>
              <Field label="Unit price" error={errors.lines?.[idx]?.unitPriceMajor?.message}>
                {({ id, invalid }) => (
                  <Input id={id} type="number" step="0.01" invalid={invalid} {...register(`lines.${idx}.unitPriceMajor`)} />
                )}
              </Field>
              <Field label="Disc %" error={errors.lines?.[idx]?.discountPercent?.message}>
                {({ id, invalid }) => (
                  <Input id={id} type="number" step="0.01" invalid={invalid} {...register(`lines.${idx}.discountPercent`)} />
                )}
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
