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
import { createBill } from "../slice/billSlice";

const lineSchema = z.object({
  productId: z.string().length(26, "26-char ID"),
  qty: z.coerce.number().positive(),
  unitPriceMajor: z.coerce.number().min(0),
});

const schema = z.object({
  supplierId: z.string().length(26, "26-char ID"),
  poId: z.string().length(26).or(z.literal("")),
  supplierBillNo: z.string().max(100).or(z.literal("")),
  billDate: z.string().min(1),
  dueDate: z.string().or(z.literal("")),
  currency: z.string().length(3),
  notes: z.string().max(2000).or(z.literal("")),
  lines: z.array(lineSchema).min(1, "At least one line required"),
});
type FormValues = z.infer<typeof schema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

/** Create supplier bill with lines (ARCHITECTURE.md §6 — logic via thunk/RHF). */
export function BillFormModal({ open, onOpenChange }: Props) {
  const dispatch = useAppDispatch();
  const saving = useAppSelector((s) => s.bill.saving);

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
      billDate: new Date().toISOString().slice(0, 10),
      dueDate: "",
      supplierBillNo: "",
      poId: "",
      notes: "",
      lines: [{ productId: "", qty: 1, unitPriceMajor: 0 }],
    },
  });
  const { fields, append, remove } = useFieldArray({ control, name: "lines" });

  useEffect(() => {
    if (!open) return;
    reset({
      supplierId: "",
      poId: "",
      supplierBillNo: "",
      billDate: new Date().toISOString().slice(0, 10),
      dueDate: "",
      currency: "SAR",
      notes: "",
      lines: [{ productId: "", qty: 1, unitPriceMajor: 0 }],
    });
  }, [open, reset]);

  const onSubmit = async (v: FormValues) => {
    const result = await dispatch(
      createBill({
        supplierId: v.supplierId,
        poId: v.poId || undefined,
        supplierBillNo: v.supplierBillNo || undefined,
        billDate: new Date(v.billDate).toISOString(),
        dueDate: v.dueDate ? new Date(v.dueDate).toISOString() : undefined,
        currency: v.currency,
        notes: v.notes || undefined,
        lines: v.lines.map((l) => ({
          productId: l.productId,
          qty: l.qty,
          unitPriceAmount: Math.round(l.unitPriceMajor * 100),
          isCapitalItem: false,
        })),
      }),
    );
    if (!("error" in result)) onOpenChange(false);
  };

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title="New supplier bill"
      className="max-w-3xl"
      footer={
        <>
          <Button variant="secondary" type="button" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button type="submit" form="bill-form" loading={saving}>
            Create
          </Button>
        </>
      }
    >
      <form id="bill-form" onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
        <div className="grid grid-cols-2 gap-4">
          <Field label="Supplier ID" error={errors.supplierId?.message} required>
            {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("supplierId")} />}
          </Field>
          <Field label="PO ID" error={errors.poId?.message}>
            {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("poId")} />}
          </Field>
          <Field label="Supplier bill #" error={errors.supplierBillNo?.message}>
            {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("supplierBillNo")} />}
          </Field>
          <Field label="Currency" error={errors.currency?.message} required>
            {({ id, invalid }) => <Input id={id} invalid={invalid} maxLength={3} {...register("currency")} />}
          </Field>
          <Field label="Bill date" error={errors.billDate?.message} required>
            {({ id, invalid }) => <Input id={id} type="date" invalid={invalid} {...register("billDate")} />}
          </Field>
          <Field label="Due date" error={errors.dueDate?.message}>
            {({ id, invalid }) => <Input id={id} type="date" invalid={invalid} {...register("dueDate")} />}
          </Field>
          <Field label="Notes" error={errors.notes?.message} className="col-span-2">
            {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("notes")} />}
          </Field>
        </div>

        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <span className="text-small font-semibold text-fg">Lines</span>
            <Button type="button" variant="ghost" size="sm" onClick={() => append({ productId: "", qty: 1, unitPriceMajor: 0 })}>
              <Plus className="h-4 w-4" />
              Add line
            </Button>
          </div>
          {errors.lines?.message && <p className="text-small text-danger">{errors.lines.message}</p>}
          {fields.map((f, idx) => (
            <div key={f.id} className="grid grid-cols-[2fr_1fr_1fr_auto] items-end gap-2">
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
