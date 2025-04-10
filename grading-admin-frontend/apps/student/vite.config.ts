import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

function manualChunks(id: string) {
  if (id.includes("node_modules")) {
    return "vendor";
  }
  return null;
}

export default defineConfig({
  base: "/",
  plugins: [react()],
  resolve: {
    alias: {
      // https://github.com/tabler/tabler-icons/issues/1233#issuecomment-2428245119
      // /esm/icons/index.mjs only exports the icons statically, so no separate chunks are created
      "@tabler/icons-react": "@tabler/icons-react/dist/esm/icons/index.mjs",
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        api: "modern-compiler",
      },
    },
  },
  server: {
    host: "0.0.0.0",
    port: 5175,
    origin: "http://host.docker.internal:5175",
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks,
      },
    },
    chunkSizeWarningLimit: 750,
  },
});
