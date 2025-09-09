import { Container, Stack, Text } from "@mantine/core";
import { useAuth } from "react-oidc-context";

export const DisabledPage = () => {
  const auth = useAuth();
  return (
    auth.user?.profile.email != "None" && (
      <Container size="xs">
        <Stack>
          <Text size="lg" fw={500} ta="center">
            User Disabled
          </Text>
          <Text size="lg" ta="center">
            User by email '{auth.user?.profile.email}' has been disabled!
            Contact an administrator for assistance!
          </Text>
        </Stack>
      </Container>
    )
  );
};
