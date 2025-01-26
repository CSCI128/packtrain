import createFetchClient from "openapi-fetch";
import createClient from "openapi-react-query";
import type { paths } from "../lib/api/v1";

const fetchClient = createFetchClient<paths>({
  baseUrl: "https://localhost.dev/api/",
});
const $api = createClient(fetchClient);

// export function HomePage() {
//   const { data, error, isLoading } = $api.useQuery(
//     "put",
//     "/admin/course/new/{canvas_id}",
//     {
//       params: {
//         path: { canvas_id: "5" },
//       },
//     },
//   );

//   if (isLoading || !data) return "Loading...";

//   if (error) return `An error occured: ${error.error_message}`;

//   return <div>{data.canvas_id}</div>;
// };

// set global state for classes

export function HomePage() {
  return (
    <>
      <p>Hello world!</p>
    </>
  );
};
