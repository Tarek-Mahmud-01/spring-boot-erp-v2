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
import { createProduct, updateProduct } from "../slice/productSlice";
import type { Product } from "../api/productApi";

// Money entered in major units; converted to minor units (×100) on submit.
const schema = z.object({
  sku: z.string().min(1).max(50),
  name: z.string().min(1).max(200),
  categoryId: z.string().length(26, "26-char ID"),
  uomId: z.string().length(26, "26-char ID"),
  taxCodeId: z.string().length(26).or(z.literal("")),
  brand: z.string().max(100).or(z.literal("")),
  sellMajor: z.coerce.number().min(0),
  costMajor: z.coerce.number().min(0),
  currency: z.string().length(3),
  soldByWeight: z.boolean(),
});
type FormValues = z.infer<typeof schema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  editing: Product | null;
}

/** Create/edit product (ARCHITECTURE.md §6 — logic via thunk/RHF, money display-only). */
export function ProductFormModal({ open, onOpenChange, editing }: Props) {
  const dispatch = useAppDispatch();
  const saving = useAppSelector((s) => s.product.saving);
  const isEdit = Boolean(editing);

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { currency: "SAR", soldByWeight: false, sellMajor: 0, costMajor: 0 },
  });

  useEffect(() => {
    if (!open) return;
    reset(
      editing
        ? {
            sku: editing.sku,
            name: editing.name,
            categoryId: editing.categoryId,
            uomId: editing.uomId,
            taxCodeId: editing.taxCodeId ?? "",
            brand: editing.brand ?? "",
            sellMajor: editing.sellAmount / 100,
            costMajor: editing.costAmount / 100,
            currency: editing.sellCurrency,
            soldByWeight: editing.soldByWeight,
          }
        : { sku: "", name: "", categoryId: "", uomId: "", taxCodeId: "", brand: "", sellMajor: 0, costMajor: 0, currency: "SAR", soldByWeight: false },
    );
  }, [open, editing, reset]);

  const onSubmit = async (v: FormValues) => {
    const sellAmount = Math.round(v.sellMajor * 100);
    const costAmount = Math.round(v.costMajor * 100);
    const result = editing
      ? await dispatch(
          updateProduct({
            id: editing.id,
            body: {
              name: v.name,
              brand: v.brand || undefined,
              taxCodeId: v.taxCodeId || undefined,
              categoryId: v.categoryId,
              uomId: v.uomId,
              sellAmount,
              costAmount,
              sellCurrency: v.currency,
              costCurrency: v.currency,
              soldByWeight: v.soldByWeight,
              version: editing.version,
            },
          }),
        )
      : await dispatch(
          createProduct({
            sku: v.sku,
            name: v.name,
            categoryId: v.categoryId,
            uomId: v.uomId,
            taxCodeId: v.taxCodeId || undefined,
            brand: v.brand || undefined,
            sellAmount,
            costAmount,
            sellCurrency: v.currency,
            costCurrency: v.currency,
            soldByWeight: v.soldByWeight,
          }),
        );
    if (!("error" in result)) onOpenChange(false);
  };

  const soldByWeight = watch("soldByWeight");

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title={isEdit ? `Edit ${editing?.sku}` : "New product"}
      className="max-w-2xl"
      footer={
        <>
          <Button variant="secondary" type="button" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button type="submit" form="product-form" loading={saving}>
            {isEdit ? "Save" : "Create"}
          </Button>
        </>
      }
    >
      <form id="product-form" onSubmit={handleSubmit(onSubmit)} className="grid grid-cols-2 gap-4" noValidate>
        <Field label="SKU" error={errors.sku?.message} required>
          {({ id, invalid }) => <Input id={id} invalid={invalid} disabled={isEdit} {...register("sku")} />}
        </Field>
        <Field label="Brand" error={errors.brand?.message}>
          {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("brand")} />}
        </Field>
        <Field label="Name" error={errors.name?.message} required className="col-span-2">
          {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("name")} />}
        </Field>
        <Field label="Category ID" error={errors.categoryId?.message} required>
          {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("categoryId")} />}
        </Field>
        <Field label="Unit of measure ID" error={errors.uomId?.message} required>
          {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("uomId")} />}
        </Field>
        <Field label="Tax code ID" error={errors.taxCodeId?.message}>
          {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("taxCodeId")} />}
        </Field>
        <Field label="Currency" error={errors.currency?.message} required>
          {({ id, invalid }) => <Input id={id} invalid={invalid} maxLength={3} {...register("currency")} />}
        </Field>
        <Field label="Sell price" error={errors.sellMajor?.message} required>
          {({ id, invalid }) => <Input id={id} type="number" step="0.01" invalid={invalid} {...register("sellMajor")} />}
        </Field>
        <Field label="Cost price" error={errors.costMajor?.message} required>
          {({ id, invalid }) => <Input id={id} type="number" step="0.01" invalid={invalid} {...register("costMajor")} />}
        </Field>
        <div className="col-span-2 flex items-center gap-2">
          <Switch checked={soldByWeight} onCheckedChange={(val) => setValue("soldByWeight", val)} label="Sold by weight" />
          <span className="text-body text-fg">Sold by weight</span>
        </div>
      </form>
    </Modal>
  );
}
