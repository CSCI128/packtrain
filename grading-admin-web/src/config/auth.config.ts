export const AUTH_CONFIG = {
  authority:
    import.meta.env.VITE_OAUTH_URL ||
    "https://localhost.dev/auth/application/o/grading-admin/",
  client_id: "grading_admin_provider",
  redirect_uri: import.meta.env.VITE_REDIRECT_URI || "https://localhost.dev/",
  response_type: "code",
  scope: "openid is_admin cwid email profile offline_access",
  post_logout_redirect_uri:
    import.meta.env.VITE_LOGOUT_REDIRECT_URI || "https://localhost.dev/",
  code_challenge_method: "S256",
};
