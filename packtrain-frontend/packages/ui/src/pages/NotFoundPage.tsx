import { Container, Stack, Text } from "@mantine/core";

export const NotFoundPage = () => {
  return (
    <Container size="xs">
      <Stack>
        <Text size="lg" fw={500} ta="center">
          404: Page Not Found
        </Text>
        <Text size="lg" ta="center">
          The specified page could not be found!
        </Text>
      </Stack>
    </Container>
  );
};
