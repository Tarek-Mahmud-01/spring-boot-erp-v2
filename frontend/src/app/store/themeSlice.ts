import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

/**
 * Theme slice (ARCHITECTURE.md §3 global slices). Dark mode via `data-theme`
 * on <html>. "system" follows the OS; an explicit choice overrides it.
 */
export type Theme = "light" | "dark";
export type ThemePreference = Theme | "system";

const STORAGE_KEY = "guru-erp.theme";

function systemTheme(): Theme {
  return window.matchMedia?.("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

function readStored(): ThemePreference {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw === "light" || raw === "dark" || raw === "system") return raw;
  } catch {
    /* ignore */
  }
  return "system";
}

function persist(pref: ThemePreference): void {
  try {
    localStorage.setItem(STORAGE_KEY, pref);
  } catch {
    /* ignore */
  }
}

function resolve(pref: ThemePreference): Theme {
  return pref === "system" ? systemTheme() : pref;
}

interface ThemeState {
  preference: ThemePreference;
  resolved: Theme;
}

const initialPref = readStored();
const initialState: ThemeState = {
  preference: initialPref,
  resolved: resolve(initialPref),
};

const themeSlice = createSlice({
  name: "theme",
  initialState,
  reducers: {
    setPreference(state, action: PayloadAction<ThemePreference>) {
      state.preference = action.payload;
      state.resolved = resolve(action.payload);
      persist(action.payload);
    },
    toggleTheme(state) {
      const next: Theme = state.resolved === "dark" ? "light" : "dark";
      state.preference = next;
      state.resolved = next;
      persist(next);
    },
    systemChanged(state) {
      if (state.preference === "system") {
        state.resolved = systemTheme();
      }
    },
  },
});

export const { setPreference, toggleTheme, systemChanged } = themeSlice.actions;
export default themeSlice.reducer;
export type { ThemeState };
