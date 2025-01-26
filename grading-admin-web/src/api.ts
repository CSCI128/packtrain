import createFetchClient from "openapi-fetch";
import createClient from "openapi-react-query";
import type { paths } from "./lib/api/v1";

const fetchClient = createFetchClient<paths>({
    baseUrl: "https://localhost.dev/api/",
});
export const $api = createClient(fetchClient);
