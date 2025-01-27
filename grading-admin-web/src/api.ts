import { UserManager } from "oidc-client-ts";
import createFetchClient from "openapi-fetch";
import createClient from "openapi-react-query";
import { AUTH_CONFIG } from "./config/auth.config";
import type { paths } from "./lib/api/v1";

const fetchClient = createFetchClient<paths>({
  baseUrl: "https://localhost.dev/api/",
});
export const $api = createClient(fetchClient);

export const userManager = new UserManager(AUTH_CONFIG);
