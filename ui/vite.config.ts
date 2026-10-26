/// <reference types="vitest/config" />
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// In development the API runs on :8080; the proxy keeps the UI and API on one origin (no CORS).
export default defineConfig({
  plugins: [react(), tailwindcss()],
  build: {
    // One chunk of about 178 kB gzipped: React 19, framer-motion, the router and TanStack Query are used on
    // every page, so splitting them buys little. Unused icons are tree-shaken (checked).
    chunkSizeWarningLimit: 650,
  },
  server: {
    port: 5173,
    proxy: { "/api": process.env.RETROFLOW_API ?? "http://localhost:8080" },
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./src/test/setup.ts"],
    css: false,
  },
});
