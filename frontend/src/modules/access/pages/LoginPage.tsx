import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { login } from "@/app/store/authSlice";
import { ROUTES } from "@/shared/constants/routePaths";
import { Button } from "@/shared/components/Button";
import { Input } from "@/shared/components/Input";
import { Field } from "@/shared/components/Field";
import { Card, CardBody } from "@/shared/components/Card";

const schema = z.object({
  username: z.string().min(1, "Username is required"),
  password: z.string().min(1, "Password is required"),
});
type LoginForm = z.infer<typeof schema>;

/** Login page (composition only — ARCHITECTURE.md §6). Logic via thunk + RHF/zod. */
export default function LoginPage() {
  const { t } = useTranslation();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { user, status, error } = useAppSelector((s) => s.auth);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginForm>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (user) navigate(ROUTES.DASHBOARD, { replace: true });
  }, [user, navigate]);

  const onSubmit = (data: LoginForm) => {
    void dispatch(login(data));
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg p-4">
      <Card className="w-full max-w-sm">
        <CardBody>
          <div className="mb-6 text-center">
            <h1 className="text-h2 text-fg">{t("appName")}</h1>
            <p className="mt-1 text-small text-fg-muted">{t("signIn")}</p>
          </div>
          <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
            <Field label={t("username")} error={errors.username?.message} required>
              {({ id, invalid }) => (
                <Input id={id} invalid={invalid} autoComplete="username" {...register("username")} />
              )}
            </Field>
            <Field label={t("password")} error={errors.password?.message} required>
              {({ id, invalid }) => (
                <Input
                  id={id}
                  type="password"
                  invalid={invalid}
                  autoComplete="current-password"
                  {...register("password")}
                />
              )}
            </Field>
            {error && <p className="text-small text-danger">{error}</p>}
            <Button type="submit" loading={status === "loading"} className="mt-2">
              {t("signIn")}
            </Button>
          </form>
        </CardBody>
      </Card>
    </div>
  );
}
