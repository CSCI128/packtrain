import ReactDOM from "react-dom/client";
import { AuthProvider } from "react-oidc-context";
import { AUTH_CONFIG } from "./auth.ts";
import App from "./InstructorApp.tsx";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <AuthProvider {...AUTH_CONFIG}>
    <App />
  </AuthProvider>
);
