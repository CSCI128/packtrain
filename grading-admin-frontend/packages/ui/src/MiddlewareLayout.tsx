import "@mantine/core/styles.css";
import { getApiClient } from "@repo/api/index";
import { store$ } from "@repo/api/store";
import { useQuery } from "@tanstack/react-query";
import { UserManager } from "oidc-client-ts";
import { useEffect } from "react";
import { useAuth } from "react-oidc-context";
import { Outlet, useLocation, useNavigate } from "react-router-dom";

export const MiddlewareLayout = ({
  userManager,
}: {
  userManager: UserManager;
}) => {
  const navigate = useNavigate();
  const location = useLocation();
  const auth = useAuth();
  const currentPage = location.pathname;

  const { data: enrollmentInfo } = useQuery({
    queryKey: ["getEnrollments"],
    queryFn: () =>
      getApiClient()
        .then((client) => client.get_enrollments())
        .then((res) => res.data)
        .catch((err) => console.log(err)),
    enabled: !!store$.id.get(),
  });

  const { data: userInfo, error: userError } = useQuery({
    queryKey: ["getUser"],
    queryFn: () =>
      getApiClient()
        .then((client) => client.get_user())
        .then((res) => res.data)
        .catch((err) => console.log(err)),
    enabled: !!auth.isAuthenticated,
  });

  useEffect(() => {
    const fetchData = async () => {
      try {
        if (enrollmentInfo) {
          let enrollment = enrollmentInfo
            .filter((c) => c.id === store$.id.get())
            .at(0);
          if (enrollment !== undefined) {
            console.log(enrollment);
          }
        }

        if (auth.isAuthenticated && userError) {
          navigate("/disabled");
        }

        const user = await userManager.getUser();
        if (store$.id.get() === undefined) {
          navigate("/select");
        }
      } catch (error) {
        console.error("An error occurred:", error);
      }
    };

    if (currentPage !== "/profile" && currentPage !== "/users") {
      fetchData();
    }
  }, [userInfo, navigate, enrollmentInfo, currentPage]);

  return <Outlet />;
};
