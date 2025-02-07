import { Button, Container, Text } from "@mantine/core";
import { $api } from "../../api";

export function Profile() {
  const { data, error, isLoading } = $api.useQuery("get", "/user");

  const {
    data: credentialData,
    error: credentialError,
    isLoading: credentialIsLoading,
  } = $api.useQuery("get", "/user/credential");

  if (isLoading || !data) return "Loading...";

  if (credentialIsLoading || !credentialData) return "Credentials loading..";

  if (error || credentialError) return `An error occured: ${error}`;

  return (
    <>
      <Container size="md">
        <Text size="xl" fw={700}>
          Profile
        </Text>

        <p>
          {data.name} {data.admin ? "(Admin)" : ""}
        </p>
        <p>{data.email}</p>
        <p>{data.cwid}</p>

        <Button variant="filled">Edit</Button>

        <Text size="xl" fw={700}>
          Credentials
        </Text>
        {credentialData.map((credential) => (
          <p>{credential.name}</p>
        ))}
        <Button variant="filled">Add Credential</Button>
      </Container>
    </>
  );
}
