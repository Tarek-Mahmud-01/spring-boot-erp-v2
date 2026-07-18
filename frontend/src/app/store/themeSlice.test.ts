import { beforeEach, describe, expect, it } from "vitest";
import reducer, { setPreference, toggleTheme, systemChanged } from "./themeSlice";
import type { ThemeState } from "./themeSlice";

const base = (over: Partial<ThemeState> = {}): ThemeState => ({
  preference: "light",
  resolved: "light",
  ...over,
});

beforeEach(() => {
  localStorage.clear();
});

describe("themeSlice", () => {
  it("toggleTheme flips light → dark", () => {
    const next = reducer(base(), toggleTheme());
    expect(next.resolved).toBe("dark");
    expect(next.preference).toBe("dark");
  });

  it("setPreference persists to localStorage", () => {
    reducer(base(), setPreference("dark"));
    expect(localStorage.getItem("guru-erp.theme")).toBe("dark");
  });

  it("systemChanged is a no-op when preference is explicit", () => {
    const next = reducer(base({ preference: "light", resolved: "light" }), systemChanged());
    expect(next.resolved).toBe("light");
  });
});
