import { type ReactNode, useEffect } from "react";
import { Provider } from "react-redux";
import { I18nextProvider } from "react-i18next";
import { store } from "@/app/store";
import { fetchCurrentUser } from "@/app/store/authSlice";
import { configureHttp } from "@/shared/services/http";
import { ThemeProvider } from "./ThemeProvider";
import i18n from "./i18n";

/**
 * Composes all global providers (ARCHITECTURE.md §3 providers): Redux, i18n,
 * Theme. Also performs the lean startup work (§3.2): wire the http error/session
 * handlers and rehydrate the current user from any persisted token.
 */
function StartupBootstrap({ children }: { children: ReactNode }) {
  useEffect(() => {
    configureHttp({
      notifyError: (err) => {
        // Minimal surface for now; a Toast provider replaces this later.
        console.error(`[${err.code}] ${err.detail}`);
      },
      onSessionExpired: () => {
        window.location.assign("/login");
      },
    });
    void store.dispatch(fetchCurrentUser());
  }, []);

  return <>{children}</>;
}

export function AppProviders({ children }: { children: ReactNode }) {
  return (
    <Provider store={store}>
      <I18nextProvider i18n={i18n}>
        <ThemeProvider>
          <StartupBootstrap>{children}</StartupBootstrap>
        </ThemeProvider>
      </I18nextProvider>
    </Provider>
  );
}
