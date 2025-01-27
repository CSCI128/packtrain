import { User } from "oidc-client-ts";
import { useEffect, useState } from "react";
import { $api, userManager } from "../api";

export function HomePage() {
  const [user, setUser] = useState<User | null>(null);
  useEffect(() => {
    const fetchUser = async () => {
      try {
        const result = await userManager.getUser();
        console.log(result);
        setUser(result);
      } catch (error) {
        console.error("Error fetching user:", error);
      }
    };
    fetchUser();
  }, []);

  const { data, error, isLoading } = $api.useQuery(
    "get",
    "/admin/course/{course_id}",
    {
      headers: {
        Authorization: "Bearer " + user?.access_token,
      },
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

  if (error) return `An error occured: ${error.error_message}`;

  return (
    <>
      <div>{data.canvas_id}</div>
    </>
  );
}
