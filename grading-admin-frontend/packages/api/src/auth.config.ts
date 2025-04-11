import { WebStorageStateStore } from "oidc-client-ts";

export function createAuthConfig(
  authority: string,
  client_id: string,
  redirect_uri: string,
  scope: string,
  post_logout_redirect_uri: string
) {
  return {
    authority: authority,
    client_id: client_id,
    redirect_uri: redirect_uri,
    response_type: "code",
    scope: scope,
    post_logout_redirect_uri: post_logout_redirect_uri,
    code_challenge_method: "S256",
    onSigninCallback: () => {
      window.history.replaceState({}, document.title, window.location.origin);
    },
    userStore: new WebStorageStateStore({ store: window.localStorage }),
  };
}

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
