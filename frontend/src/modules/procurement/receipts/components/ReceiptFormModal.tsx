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
import { Switch } from "@/shared/components/Switch";
import { createReceipt } from "../slice/receiptSlice";

const lineSchema = z.object({
  poLineId: z.string().length(26, "26-char ID").or(z.literal("")),
  qtyReceived: z.coerce.number().min(0),
  batchNo: z.string().max(100).or(z.literal("")),
});

const schema = z.object({
  poId: z.string().length(26, "26-char ID"),
  locationId: z.string().length(26, "26-char ID"),
  receivedAt: z.string().min(1),
  deliveryNoteNo: z.string().max(100).or(z.literal("")),
  notes: z.string().max(2000).or(z.literal("")),
  confirm: z.boolean(),
  lines: z.array(lineSchema).min(1, "At least one line required"),
});
type FormValues = z.infer<typeof schema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

/** Create goods receipt (GRN) with lines (ARCHITECTURE.md §6 — logic via thunk/RHF). */
export function ReceiptFormModal({ open, onOpenChange }: Props) {
  const dispatch = useAppDispatch();
  const saving = useAppSelector((s) => s.receipt.saving);

  const {
    register,
    handleSubmit,
    reset,
    control,
    watch,
    setValue,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      receivedAt: new Date().toISOString().slice(0, 10),
      deliveryNoteNo: "",
      notes: "",
      confirm: false,
      lines: [{ poLineId: "", qtyReceived: 1, batchNo: "" }],
    },
  });
  const { fields, append, remove } = useFieldArray({ control, name: "lines" });
  const confirm = watch("confirm");

  useEffect(() => {
    if (!open) return;
    reset({
      poId: "",
      locationId: "",
      receivedAt: new Date().toISOString().slice(0, 10),
      deliveryNoteNo: "",
      notes: "",
      confirm: false,
      lines: [{ poLineId: "", qtyReceived: 1, batchNo: "" }],
    });
  }, [open, reset]);

  const onSubmit = async (v: FormValues) => {
    const result = await dispatch(
      createReceipt({
        poId: v.poId,
        locationId: v.locationId,
        receivedAt: new Date(v.receivedAt).toISOString(),
        autoReceipt: false,
        confirm: v.confirm,
        deliveryNoteNo: v.deliveryNoteNo || undefined,
        notes: v.notes || undefined,
        lines: v.lines.map((l) => ({
          poLineId: l.poLineId || undefined,
          qtyReceived: l.qtyReceived,
          batchNo: l.batchNo || undefined,
        })),
      }),
    );
    if (!("error" in result)) onOpenChange(false);
  };

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title="New goods receipt"
      className="max-w-3xl"
      footer={
        <>
          <Button variant="secondary" type="button" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button type="submit" form="grn-form" loading={saving}>
            Create
          </Button>
        </>
      }
    >
      <form id="grn-form" onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
        <div className="grid grid-cols-2 gap-4">
          <Field label="PO ID" error={errors.poId?.message} required>
            {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("poId")} />}
          </Field>
          <Field label="Location ID" error={errors.locationId?.message} required>
            {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("locationId")} />}
          </Field>
          <Field label="Received at" error={errors.receivedAt?.message} required>
            {({ id, invalid }) => <Input id={id} type="date" invalid={invalid} {...register("receivedAt")} />}
          </Field>
          <Field label="Delivery note #" error={errors.deliveryNoteNo?.message}>
            {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("deliveryNoteNo")} />}
          </Field>
          <Field label="Notes" error={errors.notes?.message} className="col-span-2">
            {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("notes")} />}
          </Field>
        </div>
        <div className="flex items-center gap-2">
          <Switch checked={confirm} onCheckedChange={(val) => setValue("confirm", val)} label="Confirm immediately" />
          <span className="text-body text-fg">Confirm immediately (posts stock now)</span>
        </div>

        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <span className="text-small font-semibold text-fg">Lines</span>
            <Button type="button" variant="ghost" size="sm" onClick={() => append({ poLineId: "", qtyReceived: 1, batchNo: "" })}>
              <Plus className="h-4 w-4" />
              Add line
            </Button>
          </div>
          {errors.lines?.message && <p className="text-small text-danger">{errors.lines.message}</p>}
          {fields.map((f, idx) => (
            <div key={f.id} className="grid grid-cols-[2fr_1fr_1fr_auto] items-end gap-2">
              <Field label="PO line ID" error={errors.lines?.[idx]?.poLineId?.message}>
                {({ id, invalid }) => <Input id={id} invalid={invalid} {...register(`lines.${idx}.poLineId`)} />}
              </Field>
              <Field label="Qty received" error={errors.lines?.[idx]?.qtyReceived?.message}>
                {({ id, invalid }) => (
                  <Input id={id} type="number" step="0.001" invalid={invalid} {...register(`lines.${idx}.qtyReceived`)} />
                )}
              </Field>
              <Field label="Batch no." error={errors.lines?.[idx]?.batchNo?.message}>
                {({ id, invalid }) => <Input id={id} invalid={invalid} {...register(`lines.${idx}.batchNo`)} />}
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
