import { configureStore } from "@reduxjs/toolkit";
import { masterDataApi } from "@/shared/services/masterDataApi";
import accessUsersReducer from "@/modules/access/slice/usersSlice";
import authReducer from "./authSlice";
import themeReducer from "./themeSlice";

/**
 * Root store (ARCHITECTURE.md §3). Global slices (auth, theme, …) + the
 * master-data RTK Query reducer. Feature modules inject their own slices via
 * their module registration — kept out of here so modules stay self-contained.
 */
export const store = configureStore({
  reducer: {
    auth: authReducer,
    theme: themeReducer,
    accessUsers: accessUsersReducer,
    [masterDataApi.reducerPath]: masterDataApi.reducer,
  },
  middleware: (getDefault) => getDefault().concat(masterDataApi.middleware),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
