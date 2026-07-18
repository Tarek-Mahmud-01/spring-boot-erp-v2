import { useEffect } from "react";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { DataTable, type Column } from "@/shared/components/DataTable";
import { StatusPill } from "@/shared/components/StatusPill";
import { formatDate } from "@/shared/utils/format";
import { fetchUsers, setQuery } from "../slice/usersSlice";
import type { UserRow } from "../api/usersApi";

/**
 * Users list (composition only — ARCHITECTURE.md §6 cap ≤150). Server-driven:
 * one fetch on mount / query change; the DataTable is presentational (§3.2).
 */
const columns: Column<UserRow>[] = [
  { key: "username", header: "Username", render: (u) => <span className="font-semibold">{u.username}</span> },
  { key: "fullName", header: "Full name", render: (u) => u.fullName },
  { key: "email", header: "Email", render: (u) => u.email },
  { key: "roles", header: "Roles", render: (u) => u.roles.join(", ") || "—" },
  {
    key: "status",
    header: "Status",
    render: (u) => <StatusPill label={u.active ? "Active" : "Inactive"} tone={u.active ? "success" : "neutral"} />,
  },
  { key: "createdAt", header: "Created", align: "right", render: (u) => formatDate(u.createdAt) },
];

export default function UsersPage() {
  const dispatch = useAppDispatch();
  const { page, query, loading } = useAppSelector((s) => s.accessUsers);

  useEffect(() => {
    void dispatch(fetchUsers(query));
  }, [dispatch, query]);

  const onPageChange = (next: number) => {
    dispatch(setQuery({ page: next }));
  };

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h1 className="text-h1 text-fg">Users</h1>
        <p className="mt-1 text-body text-fg-muted">Manage user accounts and role assignments.</p>
      </div>
      <DataTable
        columns={columns}
        page={page}
        loading={loading}
        rowKey={(u) => u.publicId}
        onPageChange={onPageChange}
        emptyMessage="No users yet."
      />
    </div>
  );
}
