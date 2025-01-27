import { $api } from "../api";

export function HomePage() {
  const { data, error, isLoading } = $api.useQuery(
    "get",
    "/admin/course/{course_id}",
    {
      params: {
        path: { course_id: "5" },
      },
    }
  );

  // const { data, error, isLoading } = $api.useQuery(
  //   "put",
  //   "/admin/course/new/{canvas_id}",
  //   {
  //     params: {
  //       path: { canvas_id: "5" },
  //     }
  //   }
  // );

  if (isLoading || !data) return "Loading...";

  if (error) return `An error occured: ${error}`;

  return (
    <>
      <div>{data.canvas_id}</div>
    </>
  );
}
