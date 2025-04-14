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
