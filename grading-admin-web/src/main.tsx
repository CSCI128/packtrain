import ReactDOM from "react-dom/client";
import { AuthProvider } from "react-oidc-context";
import App from "./App.tsx";
import { AUTH_CONFIG } from "./config/auth.config.ts";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <AuthProvider {...AUTH_CONFIG}>
    <App />
  </AuthProvider>
);
