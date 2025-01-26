import createFetchClient from "openapi-fetch";
import createClient from "openapi-react-query";
import type { paths } from "../lib/api/v1";
import { useEffect, useState } from 'react';
import { UserManager, User } from 'oidc-client-ts';
import { AUTH_CONFIG } from "../config/auth.config";

const userManager = new UserManager(AUTH_CONFIG);
const fetchClient = createFetchClient<paths>({
  baseUrl: "https://localhost.dev/api/",
});
const $api = createClient(fetchClient);

// export function HomePage() {
//   const { data, error, isLoading } = $api.useQuery(
//     "put",
//     "/admin/course/new/{canvas_id}",
//     {
//       params: {
//         path: { canvas_id: "5" },
//       },
//     },
//   );

//   if (isLoading || !data) return "Loading...";

//   if (error) return `An error occured: ${error.error_message}`;

//   return <div>{data.canvas_id}</div>;
// };

// set global state for classes

export function HomePage() {
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    const handleCallback = async () => {
      try {
        // Check if the URL contains an authorization response (e.g., ?code=)
        const url = new URL(window.location.href);
        if (url.searchParams.has('code') || url.searchParams.has('state')) {
          await userManager.signinRedirectCallback();
          // Remove query parameters from the URL after processing
          window.history.replaceState({}, document.title, url.pathname);
        }
        const user = await userManager.getUser();
        setUser(user);
        console.log(user);
      } catch (error) {
        console.error('Error handling callback:', error);
      }
    };

    handleCallback();
  }, []);

  const handleLogin = async () => {
    await userManager.signinRedirect();
  };

  const handleLogout = async () => {
    await userManager.signoutRedirect();
  };

  return (
    <>
      <div>
        {user ? (
          <div>
            <p>Welcome, {user.profile.sub}!</p>
            <button onClick={handleLogout}>Logout</button>
          </div>
        ) : (
          <button onClick={handleLogin}>Login</button>
        )}
      </div>
    </>
  );
};
