import { createAuthConfig } from "@repo/api/auth.config";
import { UserManager } from "oidc-client-ts";

declare global {
  interface Window {
    __ENV__: {
      VITE_OAUTH_URL: string;
      VITE_CLIENT_ID: string;
      VITE_REDIRECT_URI: string;
      VITE_AUTH_SCOPES: string;
      VITE_LOGOUT_REDIRECT_URI: string;
      API_URL: string;
    };
  }
}

export const AUTH_CONFIG = createAuthConfig(
  window.__ENV__?.VITE_OAUTH_URL ||
    "https://localhost.dev/auth/application/o/grading-admin/",
  window.__ENV__?.VITE_CLIENT_ID || "grading_admin_provider",
  window.__ENV__?.VITE_REDIRECT_URI || "https://localhost.dev/",
  window.__ENV__?.VITE_AUTH_SCOPES ||
    "openid is_admin cwid email profile offline_access",
  window.__ENV__?.VITE_LOGOUT_REDIRECT_URI || "https://localhost.dev/"
);

export const userManager = new UserManager(AUTH_CONFIG);
