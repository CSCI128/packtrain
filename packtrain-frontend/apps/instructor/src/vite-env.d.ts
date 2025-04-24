/// <reference types="vite/client" />

declare global {
  interface Window {
    __ENV__: {
      VITE_OAUTH_URL: string;
      VITE_CLIENT_ID: string;
      VITE_REDIRECT_URI: string;
      VITE_AUTH_SCOPES: string;
      VITE_LOGOUT_REDIRECT_URI: string;
      VITE_API_URL: string;
    };
  }
}
