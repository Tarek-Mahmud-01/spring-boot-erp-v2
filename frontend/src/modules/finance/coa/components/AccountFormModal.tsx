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
import { createAccount, updateAccount } from "../slice/accountSlice";
import type { Account } from "../api/accountApi";

const schema = z.object({
  code: z.string().min(1).max(20),
  name: z.string().min(1).max(200),
  type: z.enum(["ASSET", "LIABILITY", "EQUITY", "INCOME", "EXPENSE"]),
  postingType: z.enum(["POSTING", "HEADER"]),
  currency: z.string().length(3).or(z.literal("")),
  parentId: z.string().length(26).or(z.literal("")),
});
type FormValues = z.infer<typeof schema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  editing: Account | null;
  companyId: string;
}

/** Create/edit chart-of-accounts node (ARCHITECTURE.md §6 — logic via thunk/RHF). */
export function AccountFormModal({ open, onOpenChange, editing, companyId }: Props) {
  const dispatch = useAppDispatch();
  const saving = useAppSelector((s) => s.account.saving);
  const isEdit = Boolean(editing);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { type: "ASSET", postingType: "POSTING", currency: "SAR", parentId: "" },
  });

  useEffect(() => {
    if (!open) return;
    reset(
      editing
        ? {
            code: editing.code,
            name: editing.name,
            type: editing.type,
            postingType: editing.postingType,
            currency: editing.currency ?? "SAR",
            parentId: editing.parentId ?? "",
          }
        : { code: "", name: "", type: "ASSET", postingType: "POSTING", currency: "SAR", parentId: "" },
    );
  }, [open, editing, reset]);

  const onSubmit = async (v: FormValues) => {
    const result = editing
      ? await dispatch(
          updateAccount({
            id: editing.id,
            companyId,
            body: {
              name: v.name,
              postingType: v.postingType,
              currency: v.currency || undefined,
              version: editing.version,
            },
          }),
        )
      : await dispatch(
          createAccount({
            companyId,
            code: v.code,
            name: v.name,
            type: v.type,
            postingType: v.postingType,
            currency: v.currency || undefined,
            parentId: v.parentId || undefined,
          }),
        );
    if (!("error" in result)) onOpenChange(false);
  };

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title={isEdit ? `Edit ${editing?.code}` : "New account"}
      className="max-w-2xl"
      footer={
        <>
          <Button variant="secondary" type="button" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button type="submit" form="account-form" loading={saving}>
            {isEdit ? "Save" : "Create"}
          </Button>
        </>
      }
    >
      <form id="account-form" onSubmit={handleSubmit(onSubmit)} className="grid grid-cols-2 gap-4" noValidate>
        <Field label="Code" error={errors.code?.message} required>
          {({ id, invalid }) => <Input id={id} invalid={invalid} disabled={isEdit} {...register("code")} />}
        </Field>
        <Field label="Name" error={errors.name?.message} required>
          {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("name")} />}
        </Field>
        <Field label="Type" error={errors.type?.message} required>
          {({ id, invalid }) => (
            <Select
              id={id}
              invalid={invalid}
              disabled={isEdit}
              options={[
                { value: "ASSET", label: "Asset" },
                { value: "LIABILITY", label: "Liability" },
                { value: "EQUITY", label: "Equity" },
                { value: "INCOME", label: "Income" },
                { value: "EXPENSE", label: "Expense" },
              ]}
              {...register("type")}
            />
          )}
        </Field>
        <Field label="Posting type" error={errors.postingType?.message} required>
          {({ id, invalid }) => (
            <Select
              id={id}
              invalid={invalid}
              options={[
                { value: "POSTING", label: "Posting (leaf)" },
                { value: "HEADER", label: "Header (group)" },
              ]}
              {...register("postingType")}
            />
          )}
        </Field>
        <Field label="Currency" error={errors.currency?.message}>
          {({ id, invalid }) => <Input id={id} invalid={invalid} maxLength={3} {...register("currency")} />}
        </Field>
        {!isEdit && (
          <Field label="Parent account ID" error={errors.parentId?.message}>
            {({ id, invalid }) => <Input id={id} invalid={invalid} {...register("parentId")} />}
          </Field>
        )}
      </form>
    </Modal>
  );
}
