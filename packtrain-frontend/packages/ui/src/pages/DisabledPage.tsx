import { Text } from "@mantine/core";
import { useAuth } from "react-oidc-context";

export const DisabledPage = () => {
  const auth = useAuth();
  const email = auth.user?.profile.email || "None";
  return (
    email != "None" && (
      <>
        <Text>
          User '{email}' has been disabled! Contact an administrator for
          assistance!
        </Text>
      </>
    )
  );
};
