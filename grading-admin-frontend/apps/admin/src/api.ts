import { createAuthConfig } from "@repo/api/auth.config";
import { UserManager } from "oidc-client-ts";

export const AUTH_CONFIG = createAuthConfig(
  import.meta.env.VITE_OAUTH_URL ||
    "https://localhost.dev/auth/application/o/grading-admin/",
  import.meta.env.VITE_CLIENT_ID || "grading_admin_provider",
  import.meta.env.VITE_REDIRECT_URI || "https://localhost.dev/",
  import.meta.env.VITE_AUTH_SCOPES ||
    "openid is_admin cwid email profile offline_access",
  import.meta.env.VITE_LOGOUT_REDIRECT_URI || "https://localhost.dev/"
);

export const userManager = new UserManager(AUTH_CONFIG);
