import { userManager } from "@repo/api/api";
import { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";

const ProtectedRoute = ({ children }: { children: JSX.Element }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const location = useLocation();

  // redirect depending on user role

  useEffect(() => {
    const fetchUser = async () => {
      const currentUser = await userManager.getUser();
      setUser(currentUser);
      setLoading(false);
    };
    fetchUser();
  }, []);

  if (loading) return <div>Loading...</div>;

  if (!user) {
    userManager.signinRedirect({ state: { from: location.pathname } });
    return null;
  }

  return children;
};

export default ProtectedRoute;
