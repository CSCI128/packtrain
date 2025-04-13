import { createAuthConfig } from "@repo/api/auth.config";
import { UserManager } from "oidc-client-ts";
import createFetchClient, { Middleware } from "openapi-fetch";
import createClient from "openapi-react-query";
import type { paths } from "./lib/api/v1";

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

// TODO delete this once everything migrated
const middleware: Middleware = {
  async onRequest({ request }) {
    const user = await userManager.getUser();
    request.headers.set("Authorization", "Bearer " + user?.access_token);
    return request;
  },
};

const fetchClient = createFetchClient<paths>({
  baseUrl: "https://localhost.dev/api/",
});
fetchClient.use(middleware);

export const $api = createClient(fetchClient);
