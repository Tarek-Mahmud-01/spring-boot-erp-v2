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
import { createSupplier, updateSupplier } from "../slice/supplierSlice";
import type { Supplier } from "../api/supplierApi";

const schema = z.object({
  name: z.string().min(1).max(200),
  type: z.enum(["GOODS", "SERVICES", "BOTH"]),
  paymentTerms: z.string().max(100).or(z.literal("")),
  defaultCurrency: z.string().length(3).or(z.literal("")),
  taxRegistrationNo: z.string().max(100).or(z.literal("")),
  creditLimitMajor: z.coerce.number().min(0),
});
type FormValues = z.infer<typeof schema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  editing: Supplier | null;
}

/** Create/edit supplier (ARCHITECTURE.md §6 — logic via thunk/RHF). */
export function SupplierFormModal({ open, onOpenChange, editing }: Props) {
  const dispatch = useAppDispatch();
  const saving = useAppSelector((s) => s.supplier.saving);
  const isEdit = Boolean(editing);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { type: "GOODS", defaultCurrency: "SAR", paymentTerms: "", taxRegistrationNo: "", creditLimitMajor: 0 },
  });

  useEffect(() => {
    if (!open) return;
    reset(
      editing
        ? {
            name: editing.name,
            type: editing.type,
            paymentTerms: editing.paymentTerms ?? "",
            defaultCurrency: editing.defaultCurrency ?? "SAR",
            taxRegistrationNo: editing.taxRegistrationNo ?? "",
            creditLimitMajor: editing.creditLimitAmount / 100,
          }
        : { name: "", type: "GOODS", paymentTerms: "", defaultCurrency: "SAR", taxRegistrationNo: "", creditLimitMajor: 0 },
    );
  }, [open, editing, reset]);

  const onSubmit = async (v: FormValues) => {
    const creditLimitAmount = Math.round(v.creditLimitMajor * 100);
    const result = editing
      ? await dispatch(
          updateSupplier({
            id: editing.id,
            body: {
              name: v.name,
              type: v.type,
              paymentTerms: v.paymentTerms || undefined,
              defaultCurrency: v.defaultCurrency || undefined,
              taxRegistrationNo: v.taxRegistrationNo || undefined,
              creditLimitAmount,
              creditLimitCurrency: v.defaultCurrency || undefined,
              version: editing.version,
            },
          }),
        )
      : await dispatch(
          createSupplier({
            name: v.name,
            type: v.type,
            paymentTerms: v.paymentTerms || undefined,
            defaultCurrency: v.defaultCurrency || undefined,
            taxRegistrationNo: v.taxRegistrationNo || undefined,
            creditLimitAmount,
            creditLimitCurrency: v.defaultCurrency || undefined,
          }),
        );
    if (!("error" in result)) onOpenChange(false);
  };

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title={isEdit ? `Edit ${editing?.name}` : "New supplier"}
      className="max-w-2xl"
      footer={
        <>
          <Button variant="secondary" type="button" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button type="submit" form="supplier-form" loading={saving}>
            {isEdit ? "Save" : "Create"}
          </Button>
        </>
      }
    >
      <form id="supplier-form" onSubmit={handleSubmit(onSubmit)} className="grid grid-cols-2 gap-4" noValidate>
        <Field label="Name" error={errors.name?.message} required className="col-span-2">
          {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("name")} />}
        </Field>
        <Field label="Type" error={errors.type?.message} required>
          {({ id, invalid }) => (
            <Select
              id={id}
              invalid={invalid}
              options={[
                { value: "GOODS", label: "Goods" },
                { value: "SERVICES", label: "Services" },
                { value: "BOTH", label: "Both" },
              ]}
              {...register("type")}
            />
          )}
        </Field>
        <Field label="Payment terms" error={errors.paymentTerms?.message}>
          {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("paymentTerms")} />}
        </Field>
        <Field label="Currency" error={errors.defaultCurrency?.message}>
          {({ id, invalid }) => <Input id={id} invalid={invalid} maxLength={3} {...register("defaultCurrency")} />}
        </Field>
        <Field label="Tax registration no." error={errors.taxRegistrationNo?.message}>
          {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("taxRegistrationNo")} />}
        </Field>
        <Field label="Credit limit" error={errors.creditLimitMajor?.message}>
          {({ id, invalid }) => <Input id={id} type="number" step="0.01" invalid={invalid} {...register("creditLimitMajor")} />}
        </Field>
      </form>
    </Modal>
  );
}
