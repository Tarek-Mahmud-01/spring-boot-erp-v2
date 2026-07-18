import { useEffect, useState } from "react";
import { Plus, Pencil, Trash2, Power, PowerOff } from "lucide-react";
import { useAppDispatch, useAppSelector } from "@/shared/hooks/redux";
import { usePermission } from "@/shared/hooks/usePermission";
import { PERMISSIONS } from "@/shared/constants/permissionCodes";
import { DataTable, type Column } from "@/shared/components/DataTable";
import { StatusPill } from "@/shared/components/StatusPill";
import { Button } from "@/shared/components/Button";
import {
  fetchLocations,
  setQuery,
  deleteLocation,
  activateLocation,
  deactivateLocation,
} from "../slice/locationSlice";
import { LocationFormModal } from "../components/LocationFormModal";
import type { Location } from "../api/locationApi";

const isActive = (l: Location) => l.status?.toUpperCase() === "ACTIVE";

/** Locations list + CRUD (composition only — ARCHITECTURE.md §6 ≤150). */
export default function LocationsPage() {
  const dispatch = useAppDispatch();
  const { has } = usePermission();
  const canWrite = has(PERMISSIONS.LOCATION_WRITE);
  const { page, query, loading } = useAppSelector((s) => s.location);
  const [editing, setEditing] = useState<Location | null>(null);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    void dispatch(fetchLocations(query));
  }, [dispatch, query]);

  const openCreate = () => {
    setEditing(null);
    setOpen(true);
  };
  const openEdit = (l: Location) => {
    setEditing(l);
    setOpen(true);
  };

  const columns: Column<Location>[] = [
    { key: "code", header: "Code", render: (l) => <span className="font-semibold">{l.code}</span> },
    { key: "name", header: "Name", render: (l) => l.name },
    { key: "type", header: "Type", render: (l) => l.type },
    { key: "city", header: "City", render: (l) => l.address?.city ?? "" },
    {
      key: "status",
      header: "Status",
      render: (l) => <StatusPill label={l.status} tone={isActive(l) ? "success" : "neutral"} />,
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (l) =>
        canWrite ? (
          <div className="flex justify-end gap-1">
            {isActive(l) ? (
              <Button variant="ghost" size="sm" onClick={() => dispatch(deactivateLocation(l.id))} aria-label="Deactivate">
                <PowerOff className="h-4 w-4" />
              </Button>
            ) : (
              <Button variant="ghost" size="sm" onClick={() => dispatch(activateLocation(l.id))} aria-label="Activate">
                <Power className="h-4 w-4" />
              </Button>
            )}
            <Button variant="ghost" size="sm" onClick={() => openEdit(l)} aria-label="Edit">
              <Pencil className="h-4 w-4" />
            </Button>
            <Button variant="ghost" size="sm" onClick={() => dispatch(deleteLocation(l.id))} aria-label="Delete">
              <Trash2 className="h-4 w-4" />
            </Button>
          </div>
        ) : null,
    },
  ];

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-h1 text-fg">Locations</h1>
          <p className="mt-1 text-body text-fg-muted">Stores, warehouses, and outlets in your company.</p>
        </div>
        {canWrite && (
          <Button onClick={openCreate}>
            <Plus className="h-4 w-4" />
            New location
          </Button>
        )}
      </div>
      <DataTable
        columns={columns}
        page={page}
        loading={loading}
        rowKey={(l) => l.id}
        onPageChange={(p) => dispatch(setQuery({ page: p }))}
        emptyMessage="No locations yet."
      />
      <LocationFormModal open={open} onOpenChange={setOpen} editing={editing} />
    </div>
  );
}
