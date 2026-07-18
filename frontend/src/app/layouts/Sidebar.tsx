import { NavLink } from "react-router-dom";
import { LayoutDashboard, Users, ShieldCheck, Building2, Coins, Percent, MapPin, Hash, Package } from "lucide-react";
import { cn } from "@/shared/utils/cn";
import { usePermission } from "@/shared/hooks/usePermission";
import { PERMISSIONS } from "@/shared/constants/permissionCodes";
import { ROUTES } from "@/shared/constants/routePaths";

/**
 * Persistent sidebar (ARCHITECTURE.md §3.2 — mounts once, never remounts on
 * navigation). Menu items are permission-gated (§3.2 permission-driven menu):
 * a user never sees links to modules they can't access.
 */
interface NavItem {
  to: string;
  label: string;
  icon: typeof Users;
  group: string;
  permission?: string;
}

const ITEMS: NavItem[] = [
  { to: ROUTES.DASHBOARD, label: "Dashboard", icon: LayoutDashboard, group: "Overview" },
  { to: ROUTES.PRODUCTS, label: "Products", icon: Package, group: "Catalog", permission: PERMISSIONS.PRODUCT_READ },
  { to: ROUTES.USERS, label: "Users", icon: Users, group: "Access", permission: PERMISSIONS.USER_READ },
  { to: ROUTES.ROLES, label: "Roles", icon: ShieldCheck, group: "Access", permission: PERMISSIONS.ROLE_READ },
  { to: ROUTES.COMPANY, label: "Company", icon: Building2, group: "Settings", permission: PERMISSIONS.COMPANY_READ },
  { to: ROUTES.CURRENCIES, label: "Currencies", icon: Coins, group: "Settings", permission: PERMISSIONS.CURRENCY_READ },
  { to: ROUTES.TAX_CODES, label: "Tax codes", icon: Percent, group: "Settings", permission: PERMISSIONS.TAXCODE_READ },
  { to: ROUTES.LOCATIONS, label: "Locations", icon: MapPin, group: "Settings", permission: PERMISSIONS.LOCATION_READ },
  { to: ROUTES.NUMBERING, label: "Numbering", icon: Hash, group: "Settings", permission: PERMISSIONS.NUMBERING_READ },
];

export function Sidebar() {
  const { has } = usePermission();
  const visible = ITEMS.filter((i) => !i.permission || has(i.permission));

  const groups = visible.reduce<Record<string, NavItem[]>>((acc, item) => {
    (acc[item.group] ??= []).push(item);
    return acc;
  }, {});

  return (
    <nav className="flex w-56 flex-col gap-1 border-r border-border bg-surface p-3">
      <div className="px-2 pb-3 text-body-strong font-bold tracking-tight text-fg">Guru ERP</div>
      {Object.entries(groups).map(([group, items]) => (
        <div key={group} className="flex flex-col gap-0.5">
          <span className="px-2 pt-3 text-[11px] font-semibold uppercase tracking-wider text-fg-muted">
            {group}
          </span>
          {items.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                cn(
                  "flex items-center gap-2.5 rounded-md px-2.5 py-2 text-body font-medium transition-colors",
                  isActive
                    ? "bg-primary text-neutral-0"
                    : "text-fg-muted hover:bg-surface-muted hover:text-fg",
                )
              }
            >
              <Icon className="h-4 w-4" />
              {label}
            </NavLink>
          ))}
        </div>
      ))}
    </nav>
  );
}
