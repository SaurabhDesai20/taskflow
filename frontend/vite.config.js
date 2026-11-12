import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// The browser only talks to ONE origin. In dev, Vite forwards /api and /reports to the services.
// In production, Nginx will do the exact same job (that is your P1 task).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": "http://localhost:8080",
      "/reports": "http://localhost:8000",
    },
  },
});
