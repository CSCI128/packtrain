import {
  Box,
  Button,
  Container,
  Divider,
  Group,
  Modal,
  Select,
  Text,
  TextInput,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { useDisclosure } from "@mantine/hooks";
import React from "react";
import { $api } from "../../api";

export function ProfilePage() {
  const [opened, { open, close }] = useDisclosure(false);
  const form = useForm({
    mode: "uncontrolled",
    initialValues: {
      credentialName: "",
      apiKey: "",
      service: "Canvas",
    },
    validate: {
      credentialName: (value) =>
        value.length < 1 ? "Name must have at least 1 character" : null,
      apiKey: (value) =>
        value.length < 1 ? "API key must have at least 1 character" : null,
      service: (value) =>
        value != "Canvas" &&
        value != "Gradescope" &&
        value != "PrairieLearn" &&
        value != "Runestone"
          ? "Please select a valid service!"
          : null,
    },
  });

  const { data, error, isLoading } = $api.useQuery("get", "/user");

  const {
    data: credentialData,
    refetch,
    error: credentialError,
    isLoading: credentialIsLoading,
  } = $api.useQuery("get", "/user/credential");

  const mutation = $api.useMutation("post", "/user/credential");

  if (isLoading || !data) return "Loading...";

  if (credentialIsLoading || !credentialData) return "Credentials loading..";

  if (error || credentialError) return `An error occured: ${error}`;

  const addCredential = (values: typeof form.values) => {
    mutation.mutate({
      body: {
        owning_user: {
          email: data.email,
          cwid: data.cwid,
          name: data.name,
          admin: data.admin,
        },
        // I know this is cursed but I'm making TypeScript happy
        service: values.service == "Canvas" ? "canvas" : "gradescope",
        active: true,
        api_key: values.apiKey,
        name: values.credentialName,
        private: true,
      },
    });
    refetch();
    close();
  };

  return (
    <>
      <Modal opened={opened} onClose={close} title="Add Credential">
        <form onSubmit={form.onSubmit(addCredential)}>
          <TextInput
            withAsterisk
            label="Name"
            placeholder="Credential 1"
            key={form.key("credentialName")}
            {...form.getInputProps("credentialName")}
          />

          <Select
            withAsterisk
            label="Service:"
            placeholder="Pick value"
            data={["Canvas", "Gradescope", "PrairieLearn", "Runestone"]}
            key={form.key("service")}
            {...form.getInputProps("service")}
          />

          <TextInput
            withAsterisk
            label="API Key"
            placeholder="xxxx-xxxx"
            key={form.key("apiKey")}
            {...form.getInputProps("apiKey")}
          />

          <br />

          <Group gap="xs" justify="flex-end">
            <Button color="gray" onClick={close}>
              Cancel
            </Button>
            <Button color="green" type="submit">
              Add
            </Button>
          </Group>
        </form>
      </Modal>

      <Container size="sm">
        <Group justify="space-between">
          <Text size="xl" fw={700}>
            Profile
          </Text>
          <Button justify="flex-end" variant="filled">
            Edit
          </Button>
        </Group>

        <Divider my="sm" />

        <Text size="md" fw={700}>
          {data.name} {data.admin ? "(Admin)" : ""}
        </Text>

        <Text size="md">{data.email}</Text>

        <Text size="md">CWID: {data.cwid}</Text>

        <Group mt={25} justify="space-between">
          <Text size="xl" fw={700}>
            Credentials
          </Text>
          <Button justify="flex-end" variant="filled" onClick={open}>
            Add Credential
          </Button>
        </Group>

        <Divider my="sm" />

        {credentialData.map((credential) => (
          <React.Fragment key={credential.id}>
            <Box size="sm" mt={15}>
              <Text size="md" fw={700}>
                {credential.name} ({credential.active ? "Active" : "Inactive"})
              </Text>

              <Text size="md">Service: {credential.service}</Text>

              <Text size="md">API Key: {credential.api_key}</Text>

              {credential.active ? (
                <>
                  <Button color="red">Disable</Button>
                </>
              ) : (
                <>
                  <Button color="green">Enable</Button>
                </>
              )}
            </Box>
          </React.Fragment>
        ))}
      </Container>
    </>
  );
}
