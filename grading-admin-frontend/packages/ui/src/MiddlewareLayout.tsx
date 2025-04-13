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
        if (auth.isAuthenticated && userError) {
          navigate("/disabled");
        }

        if (auth.isAuthenticated && store$.id.get() === undefined) {
          navigate("/select");
        }
      } catch (error) {
        console.error("An error occurred:", error);
      }
    };

    // TODO better solution/whitelist for this
    if (
      currentPage !== "/profile" &&
      currentPage !== "/instructor/profile" &&
      currentPage !== "/admin/profile" &&
      currentPage !== "/admin/users" &&
      currentPage !== "/admin/create" &&
      currentPage !== "/instructor/users"
    ) {
      fetchData();
    }
  }, [userInfo, navigate, currentPage]);

  return <Outlet />;
};
