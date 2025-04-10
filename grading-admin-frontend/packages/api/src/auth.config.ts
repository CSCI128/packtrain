import { WebStorageStateStore } from "oidc-client-ts";

// TODO fix imports from env
export const AUTH_CONFIG = {
  authority: "https://localhost.dev/auth/application/o/grading-admin/",
  client_id: "grading_admin_provider",
  redirect_uri: "https://localhost.dev/callback",
  response_type: "code",
  scope: "openid is_admin cwid email profile offline_access",
  post_logout_redirect_uri: "https://localhost.dev",
  code_challenge_method: "S256",
  onSigninCallback: () => {
    window.history.replaceState({}, document.title, window.location.origin);
  },
  userStore: new WebStorageStateStore({ store: window.localStorage }),
};

// export const AUTH_CONFIG = {
//   authority:
//     import.meta.VITE_OAUTH_URL ||
//     "https://localhost.dev/auth/application/o/grading-admin/",
//   client_id: import.meta.VITE_CLIENT_ID || "grading_admin_provider",
//   redirect_uri: import.meta.VITE_REDIRECT_URI || "https://localhost.dev/",
//   response_type: "code",
//   scope:
//     import.meta.VITE_AUTH_SCOPES ||
//     "openid is_admin cwid email profile offline_access",
//   post_logout_redirect_uri:
//     import.meta.VITE_LOGOUT_REDIRECT_URI || "https://localhost.dev/",
//   code_challenge_method: "S256",
//   onSigninCallback: () => {
//     window.history.replaceState({}, document.title, window.location.origin);
//   },
//   userStore: new WebStorageStateStore({ store: window.localStorage }),
// };
