import { observable } from "@legendapp/state";
import { ObservablePersistLocalStorage } from "@legendapp/state/persist-plugins/local-storage";
import { syncObservable } from "@legendapp/state/sync";
import { UserManager } from "oidc-client-ts";
import createFetchClient, { Middleware } from "openapi-fetch";
import createClient from "openapi-react-query";
import { AUTH_CONFIG } from "./config/auth.config";
import type { paths } from "./lib/api/v1";

interface CourseStore {
  id: string;
  name: string;
}

export const store$ = observable<CourseStore>();

syncObservable(store$, {
  persist: {
    name: "activeCourse",
    plugin: ObservablePersistLocalStorage,
  },
});
// todo - need a better way to grab the user access_token

export const userManager = new UserManager(AUTH_CONFIG);

const middleware: Middleware = {
  async onRequest({ request }) {
    const user = await userManager.getUser();
    request.headers.set("Authorization", "Bearer " + user?.access_token);
    return request;
  },
};

const fetchClient = createFetchClient<paths>({
  baseUrl: import.meta.env.VITE_API_URL || "https://localhost.dev/api/",
});
fetchClient.use(middleware);

export const $api = createClient(fetchClient);
