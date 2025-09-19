import { OpenAPIClientAxios } from "openapi-client-axios";
import qs from "qs";
import { Client } from "./openapi";
import openApi from "./openapi.json";

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

const api = new OpenAPIClientAxios({
  // @ts-ignore
  definition: openApi,
  axiosConfigDefaults: {
    baseURL: window.__ENV__?.VITE_API_URL || "https://localhost.dev/api",
  },
});
api.init();

let userManager: { getUser: () => Promise<{ access_token: string } | null> };

export const configureApiClient = (options: {
  userManager: typeof userManager;
}) => {
  userManager = options.userManager;
};

export const getApiClient = async () => {
  const client = await api.getClient<Client>();

  // axios doesn't serialize array params properly as Spring wants it:
  // https://github.com/axios/axios/issues/5058#issuecomment-1272107602
  client.defaults.paramsSerializer = (params) =>
    qs.stringify(params, { arrayFormat: "repeat" });

  client.interceptors.response.use(
    (response) => response,
    (error) => {
      return Promise.reject(error);
    }
  );

  const u = await userManager.getUser();
  if (u == null) {
    throw new Error("Failed to get user");
  }
  client.defaults.headers["authorization"] = `Bearer ${u.access_token}`;

  return client;
};
