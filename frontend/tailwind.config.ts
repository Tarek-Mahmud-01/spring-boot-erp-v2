import type { Config } from "tailwindcss";

/**
 * Tokens are driven by CSS variables defined in src/styles/globals.css.
 * Semantic surface/content tokens (bg, fg, border, muted) flip via
 * [data-theme="dark"] on <html>. Brand scales (primary/accent/etc.) are
 * fixed hues. NEVER hardcode hex in components — use these token classes.
 *
 * Brand: primary #714B67 (mauve), accent #00A09D (teal), Inter, 14px body.
 */
const config: Config = {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  darkMode: ["class", '[data-theme="dark"]'],
  theme: {
    extend: {
      colors: {
        // Semantic surface/content tokens (theme-aware via CSS vars)
        bg: "rgb(var(--bg) / <alpha-value>)",
        surface: "rgb(var(--surface) / <alpha-value>)",
        "surface-muted": "rgb(var(--surface-muted) / <alpha-value>)",
        fg: "rgb(var(--fg) / <alpha-value>)",
        "fg-muted": "rgb(var(--fg-muted) / <alpha-value>)",
        border: "rgb(var(--border) / <alpha-value>)",

        // Brand scales (fixed hues)
        primary: {
          50: "#F5F0F4",
          100: "#E8DCE4",
          200: "#D1B8C9",
          300: "#B395A8",
          400: "#956F86",
          500: "#825A73",
          600: "#714B67",
          700: "#5C3D54",
          800: "#472F40",
          900: "#2E1F2A",
          DEFAULT: "#714B67",
        },
        accent: {
          50: "#E0F5F4",
          100: "#B3E6E5",
          300: "#4DBFBD",
          500: "#00A09D",
          600: "#017F7D",
          700: "#015F5D",
          DEFAULT: "#00A09D",
        },
        neutral: {
          0: "#FFFFFF",
          50: "#F9FAFB",
          100: "#F3F4F6",
          200: "#E5E7EB",
          300: "#D1D5DB",
          400: "#9CA3AF",
          500: "#6B7280",
          600: "#4B5563",
          700: "#374151",
          800: "#1F2937",
          900: "#111827",
        },
        success: { 50: "#F0FDF4", 500: "#16A34A", 700: "#15803D", DEFAULT: "#16A34A" },
        warning: { 50: "#FFFBEB", 500: "#F59E0B", 700: "#B45309", DEFAULT: "#F59E0B" },
        danger: { 50: "#FEF2F2", 500: "#DC2626", 700: "#B91C1C", DEFAULT: "#DC2626" },
        info: { 50: "#F0F9FF", 500: "#0284C7", 700: "#0369A1", DEFAULT: "#0284C7" },
      },
      fontFamily: {
        sans: ["Inter", "system-ui", "-apple-system", "Segoe UI", "Roboto", "sans-serif"],
        mono: ["JetBrains Mono", "ui-monospace", "SF Mono", "Menlo", "monospace"],
      },
      fontSize: {
        display: ["36px", { lineHeight: "44px", fontWeight: "700" }],
        h1: ["24px", { lineHeight: "32px", fontWeight: "600" }],
        h2: ["20px", { lineHeight: "28px", fontWeight: "600" }],
        h3: ["18px", { lineHeight: "24px", fontWeight: "600" }],
        h4: ["16px", { lineHeight: "24px", fontWeight: "600" }],
        body: ["14px", { lineHeight: "20px", fontWeight: "400" }],
        "body-strong": ["14px", { lineHeight: "20px", fontWeight: "600" }],
        small: ["13px", { lineHeight: "18px", fontWeight: "400" }],
      },
      borderRadius: { sm: "4px", md: "6px", lg: "8px", full: "9999px" },
      boxShadow: {
        sm: "0 1px 2px rgba(17,24,39,0.06)",
        md: "0 4px 8px rgba(17,24,39,0.08), 0 2px 4px rgba(17,24,39,0.04)",
        lg: "0 12px 24px rgba(17,24,39,0.12), 0 4px 8px rgba(17,24,39,0.06)",
      },
    },
  },
  plugins: [],
};

export default config;
