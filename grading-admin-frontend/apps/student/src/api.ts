import { UserManager, WebStorageStateStore } from "oidc-client-ts";
import createFetchClient, { Middleware } from "openapi-fetch";
import createClient from "openapi-react-query";
import type { paths } from "./lib/api/v1";

export const AUTH_CONFIG = {
  authority:
    import.meta.env.VITE_OAUTH_URL ||
    "https://localhost.dev/auth/application/o/grading-admin/",
  client_id: import.meta.env.VITE_CLIENT_ID || "grading_admin_provider",
  redirect_uri: import.meta.env.VITE_REDIRECT_URI || "https://localhost.dev/",
  response_type: "code",
  scope:
    import.meta.env.VITE_AUTH_SCOPES ||
    "openid is_admin cwid email profile offline_access",
  post_logout_redirect_uri:
    import.meta.env.VITE_LOGOUT_REDIRECT_URI || "https://localhost.dev/",
  code_challenge_method: "S256",
  onSigninCallback: () => {
    window.history.replaceState({}, document.title, window.location.origin);
  },
  userStore: new WebStorageStateStore({ store: window.localStorage }),
};

// export const AUTH_CONFIG = createAuthConfig(
//   import.meta.env.VITE_OAUTH_URL ||
//     "https://localhost.dev/auth/application/o/grading-admin/",
//   import.meta.env.VITE_CLIENT_ID || "grading_admin_provider",
//   import.meta.env.VITE_REDIRECT_URI || "https://localhost.dev/",
//   import.meta.env.VITE_AUTH_SCOPES ||
//     "openid is_admin cwid email profile offline_access",
//   import.meta.env.VITE_LOGOUT_REDIRECT_URI || "https://localhost.dev/"
// );

export const userManager = new UserManager(AUTH_CONFIG);

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
