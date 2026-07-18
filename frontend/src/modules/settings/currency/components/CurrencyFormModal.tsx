import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { Modal } from "@/shared/components/Modal";
import { Field } from "@/shared/components/Field";
import { Input } from "@/shared/components/Input";
import { Button } from "@/shared/components/Button";
import { Switch } from "@/shared/components/Switch";
import { createCurrency, updateCurrency } from "../slice/currencySlice";
import type { Currency } from "../api/currencyApi";

const schema = z.object({
  code: z.string().length(3, "3 letters").regex(/^[A-Za-z]{3}$/, "Letters only"),
  name: z.string().min(1).max(100),
  shortName: z.string().min(1).max(20),
  country: z.string().min(1).max(100),
  symbol: z.string().min(1).max(8),
  decimalPlaces: z.coerce.number().int().min(0).max(4),
  isActive: z.boolean(),
});
type FormValues = z.infer<typeof schema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  editing: Currency | null;
}

/** Create/edit currency (ARCHITECTURE.md §6 — component ≤120, logic via thunk/RHF). */
export function CurrencyFormModal({ open, onOpenChange, editing }: Props) {
  const dispatch = useAppDispatch();
  const saving = useAppSelector((s) => s.currency.saving);
  const isEdit = Boolean(editing);

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { decimalPlaces: 2, isActive: true },
  });

  useEffect(() => {
    if (open) {
      reset(
        editing
          ? {
              code: editing.code,
              name: editing.name,
              shortName: editing.shortName,
              country: editing.country,
              symbol: editing.symbol,
              decimalPlaces: editing.decimalPlaces,
              isActive: editing.isActive,
            }
          : { code: "", name: "", shortName: "", country: "", symbol: "", decimalPlaces: 2, isActive: true },
      );
    }
  }, [open, editing, reset]);

  const onSubmit = async (values: FormValues) => {
    const result = editing
      ? await dispatch(updateCurrency({ id: editing.id, body: { ...values, version: editing.version } }))
      : await dispatch(createCurrency(values));
    if (!("error" in result)) onOpenChange(false);
  };

  const active = watch("isActive");

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title={isEdit ? `Edit ${editing?.code}` : "New currency"}
      footer={
        <>
          <Button variant="secondary" onClick={() => onOpenChange(false)} type="button">
            Cancel
          </Button>
          <Button type="submit" form="currency-form" loading={saving}>
            {isEdit ? "Save" : "Create"}
          </Button>
        </>
      }
    >
      <form id="currency-form" onSubmit={handleSubmit(onSubmit)} className="grid grid-cols-2 gap-4" noValidate>
        <Field label="Code" error={errors.code?.message} required className="col-span-1">
          {({ id, invalid }) => (
            <Input id={id} invalid={invalid} disabled={isEdit} maxLength={3} {...register("code")} />
          )}
        </Field>
        <Field label="Symbol" error={errors.symbol?.message} required className="col-span-1">
          {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("symbol")} />}
        </Field>
        <Field label="Name" error={errors.name?.message} required className="col-span-2">
          {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("name")} />}
        </Field>
        <Field label="Short name" error={errors.shortName?.message} required className="col-span-1">
          {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("shortName")} />}
        </Field>
        <Field label="Country" error={errors.country?.message} required className="col-span-1">
          {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("country")} />}
        </Field>
        <Field label="Decimal places" error={errors.decimalPlaces?.message} required className="col-span-1">
          {({ id, invalid }) => (
            <Input id={id} type="number" min={0} max={4} invalid={invalid} {...register("decimalPlaces")} />
          )}
        </Field>
        <div className="col-span-1 flex items-center gap-2 pt-6">
          <Switch checked={active} onCheckedChange={(v) => setValue("isActive", v)} label="Active" />
          <span className="text-body text-fg">Active</span>
        </div>
      </form>
    </Modal>
  );
}
