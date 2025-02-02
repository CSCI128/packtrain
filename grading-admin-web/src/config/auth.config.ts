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
};
