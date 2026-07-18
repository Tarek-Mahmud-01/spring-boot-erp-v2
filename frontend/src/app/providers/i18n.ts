import i18n from "i18next";
import { initReactI18next } from "react-i18next";

/**
 * i18n bootstrap (ARCHITECTURE.md §3 providers). English baseline; module
 * namespaces are added via i18n.addResourceBundle as modules load. Kept minimal
 * — no backend HTTP loader so startup stays lean (§3.2 startup loads).
 */
const resources = {
  en: {
    common: {
      appName: "Guru ERP",
      signIn: "Sign in",
      signOut: "Sign out",
      username: "Username",
      password: "Password",
      loading: "Loading…",
      search: "Search",
      save: "Save",
      cancel: "Cancel",
    },
  },
};

void i18n.use(initReactI18next).init({
  resources,
  lng: "en",
  fallbackLng: "en",
  defaultNS: "common",
  interpolation: { escapeValue: false },
});

export default i18n;
