import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { Field } from "@/shared/components/Field";
import { Input } from "@/shared/components/Input";
import { Select } from "@/shared/components/Select";
import { Switch } from "@/shared/components/Switch";
import { Button } from "@/shared/components/Button";
import { updateCompany } from "../slice/companySlice";
import type { Company } from "../api/companyApi";

const COMPLIANCE_OPTIONS = [
  { value: "NONE", label: "None" },
  { value: "AU", label: "Australia (AU)" },
];

const BAS_PERIOD_OPTIONS = [
  { value: "", label: "—" },
  { value: "MONTHLY", label: "Monthly" },
  { value: "QUARTERLY", label: "Quarterly" },
  { value: "ANNUAL", label: "Annual" },
];

/** Update contract (PATCH /api/companies/{id}). baseCurrency is immutable — read-only. */
const schema = z.object({
  legalName: z.string().min(1, "Required").max(200),
  tradingName: z.string().max(200).optional(),
  country: z.string().length(2, "2-letter ISO code"),
  taxRegistrationNo: z.string().max(30).optional(),
  taxRegistered: z.boolean(),
  taxRegistrationDate: z.string().optional(),
  fiscalYearStart: z.string().regex(/^\d{2}-\d{2}$/, "MM-DD"),
  complianceProfile: z.enum(["NONE", "AU"]),
  abn: z.string().max(14).optional(),
  acn: z.string().max(12).optional(),
  gstRegistrationDate: z.string().optional(),
  basPeriod: z.enum(["", "MONTHLY", "QUARTERLY", "ANNUAL"]),
});
type FormValues = z.infer<typeof schema>;

function toDefaults(c: Company): FormValues {
  return {
    legalName: c.legalName,
    tradingName: c.tradingName ?? "",
    country: c.country,
    taxRegistrationNo: c.taxRegistrationNo ?? "",
    taxRegistered: c.taxRegistered,
    taxRegistrationDate: c.taxRegistrationDate ?? "",
    fiscalYearStart: c.fiscalYearStart,
    complianceProfile: c.complianceProfile,
    abn: c.abn ?? "",
    acn: c.acn ?? "",
    gstRegistrationDate: c.gstRegistrationDate ?? "",
    basPeriod: c.basPeriod ?? "",
  };
}

interface Props {
  company: Company;
  canWrite: boolean;
}

/** Company settings edit-in-place form (ARCHITECTURE.md §6 — logic via thunk/RHF). */
export function CompanyForm({ company, canWrite }: Props) {
  const dispatch = useAppDispatch();
  const saving = useAppSelector((s) => s.company.saving);
  const ro = !canWrite;

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors, isDirty },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: toDefaults(company),
  });

  // Re-seed the form whenever a fresh record arrives (fetch / post-save version bump).
  useEffect(() => {
    reset(toDefaults(company));
  }, [company, reset]);

  const isAu = watch("complianceProfile") === "AU";
  const taxRegistered = watch("taxRegistered");

  const onSubmit = (values: FormValues) => {
    void dispatch(
      updateCompany({
        id: company.id,
        body: {
          legalName: values.legalName,
          tradingName: values.tradingName || undefined,
          country: values.country,
          taxRegistrationNo: values.taxRegistrationNo || undefined,
          taxRegistered: values.taxRegistered,
          taxRegistrationDate: values.taxRegistrationDate || null,
          fiscalYearStart: values.fiscalYearStart,
          complianceProfile: values.complianceProfile,
          abn: values.abn || undefined,
          acn: values.acn || undefined,
          gstRegistrationDate: values.gstRegistrationDate || null,
          basPeriod: values.basPeriod || undefined,
          version: company.version,
        },
      }),
    );
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-6" noValidate>
      {/* Identity */}
      <section className="grid grid-cols-2 gap-4">
        <Field label="Company code" className="col-span-1">
          {({ id }) => <Input id={id} value={company.code} disabled readOnly />}
        </Field>
        <Field label="Base currency" hint="Immutable after creation" className="col-span-1">
          {({ id }) => <Input id={id} value={company.baseCurrency} disabled readOnly />}
        </Field>
        <Field label="Legal name" error={errors.legalName?.message} required className="col-span-2">
          {({ id, invalid }) => <Input id={id} invalid={invalid} disabled={ro} {...register("legalName")} />}
        </Field>
        <Field label="Trading name" error={errors.tradingName?.message} className="col-span-2">
          {({ id, invalid }) => <Input id={id} invalid={invalid} disabled={ro} {...register("tradingName")} />}
        </Field>
        <Field label="Country" error={errors.country?.message} hint="ISO 3166-1 alpha-2" required className="col-span-1">
          {({ id, invalid }) => (
            <Input id={id} invalid={invalid} disabled={ro} maxLength={2} {...register("country")} />
          )}
        </Field>
        <Field label="Fiscal year start" error={errors.fiscalYearStart?.message} hint="MM-DD" required className="col-span-1">
          {({ id, invalid }) => (
            <Input id={id} invalid={invalid} disabled={ro} placeholder="07-01" {...register("fiscalYearStart")} />
          )}
        </Field>
      </section>

      {/* Tax */}
      <section className="grid grid-cols-2 gap-4">
        <div className="col-span-2 flex items-center gap-2">
          <Switch
            checked={taxRegistered}
            onCheckedChange={(v) => setValue("taxRegistered", v, { shouldDirty: true })}
            disabled={ro}
            label="Tax registered"
          />
          <span className="text-body text-fg">Tax registered</span>
        </div>
        <Field label="Tax registration no." error={errors.taxRegistrationNo?.message} className="col-span-1">
          {({ id, invalid }) => (
            <Input id={id} invalid={invalid} disabled={ro} {...register("taxRegistrationNo")} />
          )}
        </Field>
        <Field label="Tax registration date" error={errors.taxRegistrationDate?.message} className="col-span-1">
          {({ id, invalid }) => (
            <Input id={id} type="date" invalid={invalid} disabled={ro} {...register("taxRegistrationDate")} />
          )}
        </Field>
      </section>

      {/* Compliance */}
      <section className="grid grid-cols-2 gap-4">
        <Field label="Compliance profile" error={errors.complianceProfile?.message} className="col-span-1">
          {({ id, invalid }) => (
            <Select id={id} invalid={invalid} disabled={ro} options={COMPLIANCE_OPTIONS} {...register("complianceProfile")} />
          )}
        </Field>
        {isAu && (
          <>
            <Field label="BAS period" error={errors.basPeriod?.message} className="col-span-1">
              {({ id, invalid }) => (
                <Select id={id} invalid={invalid} disabled={ro} options={BAS_PERIOD_OPTIONS} {...register("basPeriod")} />
              )}
            </Field>
            <Field label="ABN" error={errors.abn?.message} hint="Australian Business Number" className="col-span-1">
              {({ id, invalid }) => <Input id={id} invalid={invalid} disabled={ro} maxLength={14} {...register("abn")} />}
            </Field>
            <Field label="ACN" error={errors.acn?.message} hint="Australian Company Number" className="col-span-1">
              {({ id, invalid }) => <Input id={id} invalid={invalid} disabled={ro} maxLength={12} {...register("acn")} />}
            </Field>
            <Field label="GST registration date" error={errors.gstRegistrationDate?.message} className="col-span-1">
              {({ id, invalid }) => (
                <Input id={id} type="date" invalid={invalid} disabled={ro} {...register("gstRegistrationDate")} />
              )}
            </Field>
          </>
        )}
      </section>

      {canWrite && (
        <div className="flex justify-end gap-2 border-t border-border pt-4">
          <Button type="button" variant="secondary" onClick={() => reset(toDefaults(company))} disabled={saving || !isDirty}>
            Reset
          </Button>
          <Button type="submit" loading={saving} disabled={!isDirty}>
            Save changes
          </Button>
        </div>
      )}
    </form>
  );
}
