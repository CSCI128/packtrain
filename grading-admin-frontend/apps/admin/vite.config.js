import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";
function manualChunks(id) {
    if (id.includes("node_modules")) {
        return "vendor";
    }
    return null;
}
export default defineConfig({
    base: "/admin",
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
        host: true,
        port: 5173,
        origin: "http://host.docker.internal:5173",
    },
    build: {
        rollupOptions: {
            output: {
                manualChunks: manualChunks,
            },
        },
        chunkSizeWarningLimit: 750,
    },
});
