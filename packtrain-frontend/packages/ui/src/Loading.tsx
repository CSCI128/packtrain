import { Center, Loader, Text } from "@mantine/core";

export function Loading() {
  return (
    <Center mt={20}>
      <Loader color="blue" mr={15} />
      <Text size="lg">Loading..</Text>
    </Center>
  );
}
