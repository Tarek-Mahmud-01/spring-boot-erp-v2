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
import { Switch } from "@/shared/components/Switch";
import { createTaxCode, updateTaxCode } from "../slice/taxcodeSlice";
import { GST_TREATMENTS, type TaxCode } from "../api/taxcodeApi";

const schema = z.object({
  companyId: z.string().length(26, "26-char company ULID"),
  code: z.string().min(1).max(20),
  description: z.string().min(1).max(200),
  ratePercent: z.coerce.number().min(0).max(100),
  inclusive: z.boolean(),
  exempt: z.boolean(),
  gstTreatment: z.enum(GST_TREATMENTS),
  effectiveFrom: z.string().min(1, "Required"),
  effectiveTo: z.string().optional().or(z.literal("")),
});
type FormValues = z.infer<typeof schema>;

const GST_OPTIONS = GST_TREATMENTS.map((v) => ({ value: v, label: v.replace(/_/g, " ") }));

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  editing: TaxCode | null;
}

/** Create/edit tax code (ARCHITECTURE.md §6 — component ≤120, logic via thunk/RHF). */
export function TaxCodeFormModal({ open, onOpenChange, editing }: Props) {
  const dispatch = useAppDispatch();
  const saving = useAppSelector((s) => s.taxcode.saving);
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
    defaultValues: {
      companyId: "",
      code: "",
      description: "",
      ratePercent: 0,
      inclusive: false,
      exempt: false,
      gstTreatment: "STANDARD",
      effectiveFrom: "",
      effectiveTo: "",
    },
  });

  useEffect(() => {
    if (open) {
      reset(
        editing
          ? {
              companyId: editing.companyId,
              code: editing.code,
              description: editing.description,
              ratePercent: editing.ratePercent,
              inclusive: editing.inclusive,
              exempt: editing.exempt,
              gstTreatment: editing.gstTreatment as FormValues["gstTreatment"],
              effectiveFrom: editing.effectiveFrom,
              effectiveTo: editing.effectiveTo ?? "",
            }
          : {
              companyId: "",
              code: "",
              description: "",
              ratePercent: 0,
              inclusive: false,
              exempt: false,
              gstTreatment: "STANDARD",
              effectiveFrom: "",
              effectiveTo: "",
            },
      );
    }
  }, [open, editing, reset]);

  const onSubmit = async (values: FormValues) => {
    const effectiveTo = values.effectiveTo ? values.effectiveTo : undefined;
    if (editing) {
      const result = await dispatch(
        updateTaxCode({
          id: editing.id,
          body: {
            description: values.description,
            ratePercent: values.ratePercent,
            inclusive: values.inclusive,
            exempt: values.exempt,
            gstTreatment: values.gstTreatment,
            effectiveFrom: values.effectiveFrom,
            effectiveTo,
            version: editing.version,
          },
        }),
      );
      if (!("error" in result)) onOpenChange(false);
    } else {
      const result = await dispatch(
        createTaxCode({
          companyId: values.companyId,
          code: values.code,
          description: values.description,
          ratePercent: values.ratePercent,
          inclusive: values.inclusive,
          exempt: values.exempt,
          gstTreatment: values.gstTreatment,
          effectiveFrom: values.effectiveFrom,
          effectiveTo,
        }),
      );
      if (!("error" in result)) onOpenChange(false);
    }
  };

  const inclusive = watch("inclusive");
  const exempt = watch("exempt");

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title={isEdit ? `Edit ${editing?.code}` : "New tax code"}
      footer={
        <>
          <Button variant="secondary" onClick={() => onOpenChange(false)} type="button">
            Cancel
          </Button>
          <Button type="submit" form="taxcode-form" loading={saving}>
            {isEdit ? "Save" : "Create"}
          </Button>
        </>
      }
    >
      <form id="taxcode-form" onSubmit={handleSubmit(onSubmit)} className="grid grid-cols-2 gap-4" noValidate>
        <Field label="Company ID" error={errors.companyId?.message} required className="col-span-2">
          {({ id, invalid }) => (
            <Input id={id} invalid={invalid} disabled={isEdit} minLength={26} maxLength={26} {...register("companyId")} />
          )}
        </Field>
        <Field label="Code" error={errors.code?.message} required className="col-span-1">
          {({ id, invalid }) => (
            <Input id={id} invalid={invalid} disabled={isEdit} maxLength={20} {...register("code")} />
          )}
        </Field>
        <Field label="Rate %" error={errors.ratePercent?.message} required className="col-span-1">
          {({ id, invalid }) => (
            <Input id={id} type="number" step="0.01" min={0} max={100} invalid={invalid} {...register("ratePercent")} />
          )}
        </Field>
        <Field label="Description" error={errors.description?.message} required className="col-span-2">
          {({ id, invalid }) => <Input id={id} invalid={invalid} maxLength={200} {...register("description")} />}
        </Field>
        <Field label="GST treatment" error={errors.gstTreatment?.message} required className="col-span-2">
          {({ id, invalid }) => <Select id={id} invalid={invalid} options={GST_OPTIONS} {...register("gstTreatment")} />}
        </Field>
        <Field label="Effective from" error={errors.effectiveFrom?.message} required className="col-span-1">
          {({ id, invalid }) => <Input id={id} type="date" invalid={invalid} {...register("effectiveFrom")} />}
        </Field>
        <Field label="Effective to" error={errors.effectiveTo?.message} className="col-span-1">
          {({ id, invalid }) => <Input id={id} type="date" invalid={invalid} {...register("effectiveTo")} />}
        </Field>
        <div className="col-span-1 flex items-center gap-2 pt-2">
          <Switch checked={inclusive} onCheckedChange={(v) => setValue("inclusive", v)} label="Inclusive" />
          <span className="text-body text-fg">Inclusive</span>
        </div>
        <div className="col-span-1 flex items-center gap-2 pt-2">
          <Switch checked={exempt} onCheckedChange={(v) => setValue("exempt", v)} label="Exempt" />
          <span className="text-body text-fg">Exempt</span>
        </div>
      </form>
    </Modal>
  );
}
