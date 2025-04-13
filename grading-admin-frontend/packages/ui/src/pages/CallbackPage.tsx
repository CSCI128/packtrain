import "@mantine/core/styles.css";
import { store$ } from "@repo/api/store";
import { useEffect } from "react";
import { useAuth } from "react-oidc-context";
import { useNavigate } from "react-router-dom";

export const CallbackPage = () => {
  const auth = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (auth.isAuthenticated && !store$.id.get()) {
      navigate("/select");
      // if (auth.user?.profile.is_admin && !store$.id.get()) {
      //   console.log("redirect select");
      //   navigate("/select");
      // } else if (!auth.user?.profile.is_admin && !store$.id.get()) {
      //   navigate("/requests");
      //   console.log("redirect requests");
      // }
    }

    // TODO handle auth redirect in select; navigate to select (for admin/instructor) OR requests (for students)
  }, [auth.isAuthenticated]);

  return null;
};
