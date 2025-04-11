import { UserManager } from "oidc-client-ts";
import createFetchClient, { Middleware } from "openapi-fetch";
import createClient from "openapi-react-query";
import type { paths } from "./lib/api/v1";
import { AUTH_CONFIG } from "./main";

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
