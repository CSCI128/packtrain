import { OpenAPIClientAxios } from "openapi-client-axios";
import { Client } from "./openapi";
import openApi from "./openapi.json";

const api = new OpenAPIClientAxios({
  // @ts-ignore
  definition: openApi,
  axiosConfigDefaults: {
    baseURL: "https://localhost.dev/api",
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

  const u = await userManager.getUser();
  if (u == null) {
    throw new Error("Failed to get user");
  }

  client.defaults.headers["authorization"] = `Bearer ${u.access_token}`;

  return client;
};
