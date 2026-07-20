import type { ReactNode } from "react";
import * as RTabs from "@radix-ui/react-tabs";
import { cn } from "@/shared/utils/cn";

export interface TabItem {
  value: string;
  label: string;
  content: ReactNode;
}

interface TabsProps {
  items: TabItem[];
  defaultValue?: string;
  className?: string;
}

/** Token-styled tabs (Radix). */
export function Tabs({ items, defaultValue, className }: TabsProps) {
  return (
    <RTabs.Root defaultValue={defaultValue ?? items[0]?.value} className={className}>
      <RTabs.List className="flex gap-1 border-b border-border">
        {items.map((t) => (
          <RTabs.Trigger
            key={t.value}
            value={t.value}
            className={cn(
              "-mb-px border-b-2 border-transparent px-3 py-2 text-body font-medium text-fg-muted transition-colors",
              "hover:text-fg data-[state=active]:border-primary data-[state=active]:text-fg",
              "focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary",
            )}
          >
            {t.label}
          </RTabs.Trigger>
        ))}
      </RTabs.List>
      {items.map((t) => (
        <RTabs.Content key={t.value} value={t.value} className="pt-4 focus:outline-none">
          {t.content}
        </RTabs.Content>
      ))}
    </RTabs.Root>
  );
}
