import "@mantine/core/styles.css";
import { store$ } from "@repo/api/store";
import { useEffect } from "react";
import { useAuth } from "react-oidc-context";
import { useNavigate } from "react-router-dom";

export const CallbackPage = () => {
  const auth = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    // user should be authed and store should be empty, but safety check
    if (auth.isAuthenticated && !store$.id.get()) {
      navigate("/select");
    }
  }, [auth.isAuthenticated]);

  return null;
};
