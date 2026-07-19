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
import { createTransfer } from "../slice/transferSlice";

const lineSchema = z.object({
  productId: z.string().length(26, "26-char ID"),
  qtySent: z.coerce.number().positive(),
});

const schema = z.object({
  sourceLocationId: z.string().length(26, "26-char ID"),
  destinationLocationId: z.string().length(26, "26-char ID"),
  transferDate: z.string().or(z.literal("")),
  notes: z.string().max(2000).or(z.literal("")),
  lines: z.array(lineSchema).min(1, "At least one line required"),
});
type FormValues = z.infer<typeof schema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

/** Create stock transfer with lines (ARCHITECTURE.md §6 — logic via thunk/RHF). */
export function TransferFormModal({ open, onOpenChange }: Props) {
  const dispatch = useAppDispatch();
  const saving = useAppSelector((s) => s.transfer.saving);

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      transferDate: new Date().toISOString().slice(0, 10),
      notes: "",
      lines: [{ productId: "", qtySent: 1 }],
    },
  });
  const { fields, append, remove } = useFieldArray({ control, name: "lines" });

  useEffect(() => {
    if (!open) return;
    reset({
      sourceLocationId: "",
      destinationLocationId: "",
      transferDate: new Date().toISOString().slice(0, 10),
      notes: "",
      lines: [{ productId: "", qtySent: 1 }],
    });
  }, [open, reset]);

  const onSubmit = async (v: FormValues) => {
    const result = await dispatch(
      createTransfer({
        sourceLocationId: v.sourceLocationId,
        destinationLocationId: v.destinationLocationId,
        transferDate: v.transferDate || undefined,
        notes: v.notes || undefined,
        autoComplete: false,
        lines: v.lines.map((l) => ({ productId: l.productId, qtySent: l.qtySent })),
      }),
    );
    if (!("error" in result)) onOpenChange(false);
  };

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title="New stock transfer"
      className="max-w-3xl"
      footer={
        <>
          <Button variant="secondary" type="button" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button type="submit" form="transfer-form" loading={saving}>
            Create
          </Button>
        </>
      }
    >
      <form id="transfer-form" onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
        <div className="grid grid-cols-2 gap-4">
          <Field label="Source location ID" error={errors.sourceLocationId?.message} required>
            {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("sourceLocationId")} />}
          </Field>
          <Field label="Destination location ID" error={errors.destinationLocationId?.message} required>
            {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("destinationLocationId")} />}
          </Field>
          <Field label="Transfer date" error={errors.transferDate?.message}>
            {({ id, invalid }) => <Input id={id} type="date" invalid={invalid} {...register("transferDate")} />}
          </Field>
          <Field label="Notes" error={errors.notes?.message}>
            {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("notes")} />}
          </Field>
        </div>

        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <span className="text-small font-semibold text-fg">Lines</span>
            <Button type="button" variant="ghost" size="sm" onClick={() => append({ productId: "", qtySent: 1 })}>
              <Plus className="h-4 w-4" />
              Add line
            </Button>
          </div>
          {errors.lines?.message && <p className="text-small text-danger">{errors.lines.message}</p>}
          {fields.map((f, idx) => (
            <div key={f.id} className="grid grid-cols-[2fr_1fr_auto] items-end gap-2">
              <Field label="Product ID" error={errors.lines?.[idx]?.productId?.message}>
                {({ id, invalid }) => <Input id={id} invalid={invalid} {...register(`lines.${idx}.productId`)} />}
              </Field>
              <Field label="Qty sent" error={errors.lines?.[idx]?.qtySent?.message}>
                {({ id, invalid }) => (
                  <Input id={id} type="number" step="0.001" invalid={invalid} {...register(`lines.${idx}.qtySent`)} />
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
