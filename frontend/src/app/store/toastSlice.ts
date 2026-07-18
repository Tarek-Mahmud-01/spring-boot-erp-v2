import { createSlice, nanoid, type PayloadAction } from "@reduxjs/toolkit";

/** A transient user notification (ARCHITECTURE.md §3 global notification slice). */
export interface Toast {
  id: string;
  tone: "success" | "error" | "info";
  message: string;
}

interface ToastState {
  items: Toast[];
}

const initialState: ToastState = { items: [] };

const toastSlice = createSlice({
  name: "toast",
  initialState,
  reducers: {
    pushToast: {
      reducer(state, action: PayloadAction<Toast>) {
        state.items.push(action.payload);
      },
      prepare(input: { tone: Toast["tone"]; message: string }) {
        return { payload: { id: nanoid(), ...input } };
      },
    },
    dismissToast(state, action: PayloadAction<string>) {
      state.items = state.items.filter((t) => t.id !== action.payload);
    },
  },
});

export const { pushToast, dismissToast } = toastSlice.actions;
export default toastSlice.reducer;
export type { ToastState };
