#!/bin/sh

cat <<EOF > /usr/share/nginx/html/env.js
window.__ENV__ = {
  VITE_OAUTH_URL: "${VITE_OAUTH_URL}",
  VITE_CLIENT_ID: "${VITE_CLIENT_ID}",
  VITE_REDIRECT_URI: "${VITE_REDIRECT_URI}",
  VITE_AUTH_SCOPES: "${VITE_AUTH_SCOPES}",
  VITE_LOGOUT_REDIRECT_URI: "${VITE_LOGOUT_REDIRECT_URI}",
  API_URL: "https://packtrain.gregory-bell.com/api"
};
EOF

exec "$@"
