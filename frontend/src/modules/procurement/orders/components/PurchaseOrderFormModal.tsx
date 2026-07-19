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
import { createPurchaseOrder } from "../slice/purchaseOrderSlice";

const lineSchema = z.object({
  productId: z.string().length(26, "26-char ID"),
  qtyOrdered: z.coerce.number().positive(),
  unitPriceMajor: z.coerce.number().min(0),
  discountPercent: z.coerce.number().min(0).max(100),
});

const schema = z.object({
  supplierId: z.string().length(26, "26-char ID"),
  locationId: z.string().length(26, "26-char ID"),
  poDate: z.string().min(1),
  currency: z.string().length(3),
  notes: z.string().max(2000).or(z.literal("")),
  lines: z.array(lineSchema).min(1, "At least one line required"),
});
type FormValues = z.infer<typeof schema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

/** Create purchase order with lines (ARCHITECTURE.md §6 — logic via thunk/RHF). */
export function PurchaseOrderFormModal({ open, onOpenChange }: Props) {
  const dispatch = useAppDispatch();
  const saving = useAppSelector((s) => s.purchaseOrder.saving);

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
      poDate: new Date().toISOString().slice(0, 10),
      notes: "",
      lines: [{ productId: "", qtyOrdered: 1, unitPriceMajor: 0, discountPercent: 0 }],
    },
  });
  const { fields, append, remove } = useFieldArray({ control, name: "lines" });

  useEffect(() => {
    if (!open) return;
    reset({
      supplierId: "",
      locationId: "",
      poDate: new Date().toISOString().slice(0, 10),
      currency: "SAR",
      notes: "",
      lines: [{ productId: "", qtyOrdered: 1, unitPriceMajor: 0, discountPercent: 0 }],
    });
  }, [open, reset]);

  const onSubmit = async (v: FormValues) => {
    const result = await dispatch(
      createPurchaseOrder({
        supplierId: v.supplierId,
        locationId: v.locationId,
        poDate: new Date(v.poDate).toISOString(),
        currency: v.currency,
        notes: v.notes || undefined,
        isDirect: false,
        lines: v.lines.map((l) => ({
          productId: l.productId,
          qtyOrdered: l.qtyOrdered,
          unitPriceAmount: Math.round(l.unitPriceMajor * 100),
          unitPriceCurrency: v.currency,
          discountPercent: l.discountPercent,
        })),
      }),
    );
    if (!("error" in result)) onOpenChange(false);
  };

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title="New purchase order"
      className="max-w-3xl"
      footer={
        <>
          <Button variant="secondary" type="button" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button type="submit" form="po-form" loading={saving}>
            Create
          </Button>
        </>
      }
    >
      <form id="po-form" onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
        <div className="grid grid-cols-2 gap-4">
          <Field label="Supplier ID" error={errors.supplierId?.message} required>
            {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("supplierId")} />}
          </Field>
          <Field label="Location ID" error={errors.locationId?.message} required>
            {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("locationId")} />}
          </Field>
          <Field label="PO date" error={errors.poDate?.message} required>
            {({ id, invalid }) => <Input id={id} type="date" invalid={invalid} {...register("poDate")} />}
          </Field>
          <Field label="Currency" error={errors.currency?.message} required>
            {({ id, invalid }) => <Input id={id} invalid={invalid} maxLength={3} {...register("currency")} />}
          </Field>
          <Field label="Notes" error={errors.notes?.message} className="col-span-2">
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
              onClick={() => append({ productId: "", qtyOrdered: 1, unitPriceMajor: 0, discountPercent: 0 })}
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
              <Field label="Qty" error={errors.lines?.[idx]?.qtyOrdered?.message}>
                {({ id, invalid }) => (
                  <Input id={id} type="number" step="0.001" invalid={invalid} {...register(`lines.${idx}.qtyOrdered`)} />
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
