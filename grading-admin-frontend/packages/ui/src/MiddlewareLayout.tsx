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
  const isAuthenticated = store$.id.get();
  const auth = useAuth();
  const currentPage = location.pathname;

  const { data: userInfo } = useQuery({
    queryKey: ["getUser"],
    queryFn: () =>
      getApiClient()
        .then((client) => client.get_user())
        .then((res) => res.data)
        .catch((err) => console.log(err)),
  });

  useEffect(() => {
    const fetchData = async () => {
      try {
        if (userInfo) {
          console.log(userInfo); // TODO handle error if caught

          if (auth.isAuthenticated && userInfo === undefined) {
            navigate("/disabled");
          }
        }

        const user = await userManager.getUser();
        if (isAuthenticated && !store$.id.get()) {
          navigate("/select");
        } else if (store$.id.get() && user?.profile.is_admin) {
          navigate("/admin");
        }
      } catch (error) {
        console.error("An error occurred:", error);
      }
    };

    if (currentPage !== "/profile" && currentPage !== "/users") {
      fetchData();
    }
  }, [userInfo, isAuthenticated, navigate]);

  return <Outlet />;
};
