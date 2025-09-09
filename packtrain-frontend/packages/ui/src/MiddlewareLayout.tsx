import { store$ } from "@repo/api/store";
import { useEffect } from "react";
import { useAuth } from "react-oidc-context";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { useGetUser } from "./hooks";

export const MiddlewareLayout = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const auth = useAuth();
  const currentPage = location.pathname;
  const { data, error, status } = useGetUser(true);

  useEffect(() => {
    const shouldSkip =
      status === "pending" || status === "error" || auth.isLoading;
    if (shouldSkip) return;

    const fetchData = async () => {
      if (
        !data &&
        !error &&
        (currentPage.includes("/admin") || currentPage.includes("/instructor"))
      ) {
        navigate("/");
        window.location.href = "/";
      } else if (auth.isAuthenticated && error) {
        navigate("/disabled");
      } else if (auth.isAuthenticated && store$.id.get() === undefined) {
        navigate("/select");
      } else if (
        data !== undefined &&
        !data?.admin &&
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
  }, [auth, data, error, navigate, currentPage, status]);

  return <Outlet />;
};
