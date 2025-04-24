import ReactDOM from "react-dom/client";
import { AuthProvider } from "react-oidc-context";
import App from "./StudentApp.tsx";
import { AUTH_CONFIG } from "./auth.ts";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <AuthProvider {...AUTH_CONFIG}>
    <App />
  </AuthProvider>
);
