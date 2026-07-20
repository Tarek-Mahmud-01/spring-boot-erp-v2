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
import { createJournalEntry } from "../slice/journalEntrySlice";

const lineSchema = z.object({
  accountId: z.string().length(26, "26-char ID"),
  debitMajor: z.coerce.number().min(0),
  creditMajor: z.coerce.number().min(0),
});

const schema = z.object({
  voucherType: z.string().min(1).max(10),
  entryDate: z.string().min(1),
  currency: z.string().length(3),
  reference: z.string().max(200).or(z.literal("")),
  narration: z.string().max(1000).or(z.literal("")),
  lines: z.array(lineSchema).min(2, "At least two lines required"),
});
type FormValues = z.infer<typeof schema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  companyId: string;
}

/** Create a balanced journal entry (ARCHITECTURE.md §6 — logic via thunk/RHF). */
export function JournalEntryFormModal({ open, onOpenChange, companyId }: Props) {
  const dispatch = useAppDispatch();
  const saving = useAppSelector((s) => s.journalEntry.saving);

  const {
    register,
    handleSubmit,
    reset,
    control,
    watch,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      voucherType: "JV",
      entryDate: new Date().toISOString().slice(0, 10),
      currency: "SAR",
      reference: "",
      narration: "",
      lines: [
        { accountId: "", debitMajor: 0, creditMajor: 0 },
        { accountId: "", debitMajor: 0, creditMajor: 0 },
      ],
    },
  });
  const { fields, append, remove } = useFieldArray({ control, name: "lines" });
  const lines = watch("lines");
  const totalDebit = lines.reduce((sum, l) => sum + (Number(l.debitMajor) || 0), 0);
  const totalCredit = lines.reduce((sum, l) => sum + (Number(l.creditMajor) || 0), 0);
  const balanced = Math.abs(totalDebit - totalCredit) < 0.005;

  useEffect(() => {
    if (!open) return;
    reset({
      voucherType: "JV",
      entryDate: new Date().toISOString().slice(0, 10),
      currency: "SAR",
      reference: "",
      narration: "",
      lines: [
        { accountId: "", debitMajor: 0, creditMajor: 0 },
        { accountId: "", debitMajor: 0, creditMajor: 0 },
      ],
    });
  }, [open, reset]);

  const onSubmit = async (v: FormValues) => {
    const result = await dispatch(
      createJournalEntry({
        companyId,
        voucherType: v.voucherType,
        entryDate: v.entryDate,
        reference: v.reference || undefined,
        narration: v.narration || undefined,
        lines: v.lines.map((l) => ({
          accountId: l.accountId,
          debit: Math.round(l.debitMajor * 100),
          credit: Math.round(l.creditMajor * 100),
          currency: v.currency,
        })),
      }),
    );
    if (!("error" in result)) onOpenChange(false);
  };

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title="New journal entry"
      className="max-w-3xl"
      footer={
        <>
          <Button variant="secondary" type="button" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button type="submit" form="je-form" loading={saving} disabled={!balanced}>
            Create
          </Button>
        </>
      }
    >
      <form id="je-form" onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
        <div className="grid grid-cols-2 gap-4">
          <Field label="Voucher type" error={errors.voucherType?.message} required>
            {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("voucherType")} />}
          </Field>
          <Field label="Entry date" error={errors.entryDate?.message} required>
            {({ id, invalid }) => <Input id={id} type="date" invalid={invalid} {...register("entryDate")} />}
          </Field>
          <Field label="Currency" error={errors.currency?.message} required>
            {({ id, invalid }) => <Input id={id} invalid={invalid} maxLength={3} {...register("currency")} />}
          </Field>
          <Field label="Reference" error={errors.reference?.message}>
            {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("reference")} />}
          </Field>
          <Field label="Narration" error={errors.narration?.message} className="col-span-2">
            {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("narration")} />}
          </Field>
        </div>

        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <span className="text-small font-semibold text-fg">
              Lines — debit {totalDebit.toFixed(2)} / credit {totalCredit.toFixed(2)}{" "}
              {!balanced && <span className="text-danger">(must balance)</span>}
            </span>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => append({ accountId: "", debitMajor: 0, creditMajor: 0 })}
            >
              <Plus className="h-4 w-4" />
              Add line
            </Button>
          </div>
          {errors.lines?.message && <p className="text-small text-danger">{errors.lines.message}</p>}
          {fields.map((f, idx) => (
            <div key={f.id} className="grid grid-cols-[2fr_1fr_1fr_auto] items-end gap-2">
              <Field label="Account ID" error={errors.lines?.[idx]?.accountId?.message}>
                {({ id, invalid }) => <Input id={id} invalid={invalid} {...register(`lines.${idx}.accountId`)} />}
              </Field>
              <Field label="Debit" error={errors.lines?.[idx]?.debitMajor?.message}>
                {({ id, invalid }) => (
                  <Input id={id} type="number" step="0.01" invalid={invalid} {...register(`lines.${idx}.debitMajor`)} />
                )}
              </Field>
              <Field label="Credit" error={errors.lines?.[idx]?.creditMajor?.message}>
                {({ id, invalid }) => (
                  <Input id={id} type="number" step="0.01" invalid={invalid} {...register(`lines.${idx}.creditMajor`)} />
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
