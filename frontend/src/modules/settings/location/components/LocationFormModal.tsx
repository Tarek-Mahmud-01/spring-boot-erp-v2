import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { Modal } from "@/shared/components/Modal";
import { Field } from "@/shared/components/Field";
import { Input } from "@/shared/components/Input";
import { Button } from "@/shared/components/Button";
import { createLocation, updateLocation } from "../slice/locationSlice";
import type { Location } from "../api/locationApi";

const addressSchema = z.object({
  street: z.string().max(200),
  city: z.string().max(120),
  region: z.string().max(120),
  postcode: z.string().max(20),
  country: z.string().max(120),
});

const schema = z.object({
  companyId: z.string().length(26, "26 characters"),
  code: z.string().min(1).max(10),
  name: z.string().min(1).max(200),
  type: z.string().min(1).max(32),
  timezone: z.string().min(1).max(50),
  address: addressSchema,
  phone: z.string().max(30).optional().or(z.literal("")),
  publicEmail: z.string().email("Invalid email").max(200).optional().or(z.literal("")),
  defaultPriceListId: z.string().max(26).optional().or(z.literal("")),
  defaultTaxCodeId: z.string().max(26).optional().or(z.literal("")),
  priceDisplayMode: z.string().max(16).optional().or(z.literal("")),
});
type FormValues = z.infer<typeof schema>;

const EMPTY: FormValues = {
  companyId: "",
  code: "",
  name: "",
  type: "",
  timezone: "",
  address: { street: "", city: "", region: "", postcode: "", country: "" },
  phone: "",
  publicEmail: "",
  defaultPriceListId: "",
  defaultTaxCodeId: "",
  priceDisplayMode: "",
};

/** Trim empty optional strings to undefined so they are omitted from the payload. */
const opt = (v?: string) => (v && v.length > 0 ? v : undefined);

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  editing: Location | null;
}

/** Create/edit location (ARCHITECTURE.md §6 — logic via thunk/RHF). */
export function LocationFormModal({ open, onOpenChange, editing }: Props) {
  const dispatch = useAppDispatch();
  const saving = useAppSelector((s) => s.location.saving);
  const isEdit = Boolean(editing);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: EMPTY,
  });

  useEffect(() => {
    if (open) {
      reset(
        editing
          ? {
              companyId: editing.companyId,
              code: editing.code,
              name: editing.name,
              type: editing.type,
              timezone: editing.timezone,
              address: {
                street: editing.address?.street ?? "",
                city: editing.address?.city ?? "",
                region: editing.address?.region ?? "",
                postcode: editing.address?.postcode ?? "",
                country: editing.address?.country ?? "",
              },
              phone: editing.phone ?? "",
              publicEmail: editing.publicEmail ?? "",
              defaultPriceListId: editing.defaultPriceListId ?? "",
              defaultTaxCodeId: editing.defaultTaxCodeId ?? "",
              priceDisplayMode: editing.priceDisplayMode ?? "",
            }
          : EMPTY,
      );
    }
  }, [open, editing, reset]);

  const onSubmit = async (values: FormValues) => {
    if (editing) {
      const result = await dispatch(
        updateLocation({
          id: editing.id,
          body: {
            code: values.code,
            name: values.name,
            type: values.type,
            timezone: values.timezone,
            address: values.address,
            phone: opt(values.phone),
            publicEmail: opt(values.publicEmail),
            defaultPriceListId: opt(values.defaultPriceListId),
            defaultTaxCodeId: opt(values.defaultTaxCodeId),
            priceDisplayMode: opt(values.priceDisplayMode),
            version: editing.version,
          },
        }),
      );
      if (!("error" in result)) onOpenChange(false);
    } else {
      const result = await dispatch(
        createLocation({
          companyId: values.companyId,
          code: values.code,
          name: values.name,
          type: values.type,
          timezone: values.timezone,
          address: values.address,
          phone: opt(values.phone),
          publicEmail: opt(values.publicEmail),
          defaultPriceListId: opt(values.defaultPriceListId),
          defaultTaxCodeId: opt(values.defaultTaxCodeId),
          priceDisplayMode: opt(values.priceDisplayMode),
        }),
      );
      if (!("error" in result)) onOpenChange(false);
    }
  };

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title={isEdit ? `Edit ${editing?.code}` : "New location"}
      footer={
        <>
          <Button variant="secondary" onClick={() => onOpenChange(false)} type="button">
            Cancel
          </Button>
          <Button type="submit" form="location-form" loading={saving}>
            {isEdit ? "Save" : "Create"}
          </Button>
        </>
      }
    >
      <form id="location-form" onSubmit={handleSubmit(onSubmit)} className="grid grid-cols-2 gap-4" noValidate>
        {!isEdit && (
          <Field label="Company ID" error={errors.companyId?.message} required className="col-span-2">
            {({ id, invalid }) => (
              <Input id={id} invalid={invalid} maxLength={26} {...register("companyId")} />
            )}
          </Field>
        )}
        <Field label="Code" error={errors.code?.message} required className="col-span-1">
          {({ id, invalid }) => <Input id={id} invalid={invalid} maxLength={10} {...register("code")} />}
        </Field>
        <Field label="Type" error={errors.type?.message} required className="col-span-1">
          {({ id, invalid }) => <Input id={id} invalid={invalid} maxLength={32} {...register("type")} />}
        </Field>
        <Field label="Name" error={errors.name?.message} required className="col-span-2">
          {({ id, invalid }) => <Input id={id} invalid={invalid} maxLength={200} {...register("name")} />}
        </Field>
        <Field label="Timezone" error={errors.timezone?.message} required className="col-span-1">
          {({ id, invalid }) => <Input id={id} invalid={invalid} maxLength={50} {...register("timezone")} />}
        </Field>
        <Field label="Price display mode" error={errors.priceDisplayMode?.message} className="col-span-1">
          {({ id, invalid }) => (
            <Input id={id} invalid={invalid} maxLength={16} {...register("priceDisplayMode")} />
          )}
        </Field>

        <p className="col-span-2 mt-1 text-small font-semibold text-fg-muted">Address</p>
        <Field label="Street" error={errors.address?.street?.message} className="col-span-2">
          {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("address.street")} />}
        </Field>
        <Field label="City" error={errors.address?.city?.message} className="col-span-1">
          {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("address.city")} />}
        </Field>
        <Field label="Region" error={errors.address?.region?.message} className="col-span-1">
          {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("address.region")} />}
        </Field>
        <Field label="Postcode" error={errors.address?.postcode?.message} className="col-span-1">
          {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("address.postcode")} />}
        </Field>
        <Field label="Country" error={errors.address?.country?.message} className="col-span-1">
          {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("address.country")} />}
        </Field>

        <p className="col-span-2 mt-1 text-small font-semibold text-fg-muted">Contact</p>
        <Field label="Phone" error={errors.phone?.message} className="col-span-1">
          {({ id, invalid }) => <Input id={id} invalid={invalid} maxLength={30} {...register("phone")} />}
        </Field>
        <Field label="Public email" error={errors.publicEmail?.message} className="col-span-1">
          {({ id, invalid }) => (
            <Input id={id} type="email" invalid={invalid} maxLength={200} {...register("publicEmail")} />
          )}
        </Field>

        <p className="col-span-2 mt-1 text-small font-semibold text-fg-muted">Defaults</p>
        <Field label="Default price list ID" error={errors.defaultPriceListId?.message} className="col-span-1">
          {({ id, invalid }) => (
            <Input id={id} invalid={invalid} maxLength={26} {...register("defaultPriceListId")} />
          )}
        </Field>
        <Field label="Default tax code ID" error={errors.defaultTaxCodeId?.message} className="col-span-1">
          {({ id, invalid }) => (
            <Input id={id} invalid={invalid} maxLength={26} {...register("defaultTaxCodeId")} />
          )}
        </Field>
      </form>
    </Modal>
  );
}
