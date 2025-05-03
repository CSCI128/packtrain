import { getApiClient } from "@repo/api/index";
import { store$ } from "@repo/api/store";
import { useQuery } from "@tanstack/react-query";
import { useEffect } from "react";
import { useAuth } from "react-oidc-context";
import { Outlet, useLocation, useNavigate } from "react-router-dom";

export const MiddlewareLayout = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const auth = useAuth();
  const currentPage = location.pathname;

  const {
    data: userInfo,
    error: userError,
    status,
  } = useQuery({
    queryKey: ["getUser"],
    queryFn: () =>
      getApiClient()
        .then((client) => client.get_user())
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });

  useEffect(() => {
    const shouldSkip =
      status === "pending" || status === "error" || auth.isLoading;
    if (shouldSkip) return;

    const fetchData = async () => {
      if (
        !userInfo &&
        !userError &&
        (currentPage.includes("/admin") || currentPage.includes("/instructor"))
      ) {
        navigate("/");
        window.location.href = "/";
      } else if (auth.isAuthenticated && userError) {
        navigate("/disabled");
      } else if (auth.isAuthenticated && store$.id.get() === undefined) {
        navigate("/select");
      } else if (
        userInfo !== undefined &&
        !userInfo?.admin &&
        currentPage.includes("/admin")
      ) {
        window.location.href = "/";
      }
    };

    if (
      currentPage !== "/profile" &&
      currentPage !== "/instructor/profile" &&
      currentPage !== "/admin/profile" &&
      currentPage !== "/admin/users" &&
      currentPage !== "/admin/create" &&
      currentPage !== "/instructor/users" &&
      currentPage !== "/" &&
      currentPage !== "/select"
    ) {
      fetchData();
    }
  }, [auth, userInfo, userError, navigate, currentPage, status]);

  return <Outlet />;
};
