import {
  Box,
  Button,
  Center,
  Container,
  Divider,
  Group,
  Modal,
  Select,
  Stack,
  Text,
  TextInput,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { useDisclosure } from "@mantine/hooks";
import React, { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { $api } from "../api";
import { components } from "../lib/api/v1";

export function ProfilePage() {
  const auth = useAuth();
  const [selectedCredential, setSelectedCredential] = useState<
    components["schemas"]["Credential"] | null
  >(null);
  const [deleteOpened, { open: openDelete, close: closeDelete }] =
    useDisclosure(false);
  const [editUserOpened, { open: openEditUser, close: closeEditUser }] =
    useDisclosure(false);
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
        value != "canvas" &&
        value != "gradescope" &&
        value != "prairielearn" &&
        value != "runestone"
          ? "Please select a valid service!"
          : null,
    },
  });

  const {
    data,
    error,
    isLoading,
    refetch: refetchUser,
  } = $api.useQuery("get", "/user");

  const editUserForm = useForm({
    mode: "controlled",
    initialValues: {
      name: "",
      email: "",
      cwid: "",
    },
    validate: {
      name: (value) =>
        value && value.length < 1
          ? "Name must have at least 1 character"
          : null,
      email: (value) => (/^\S+@\S+$/.test(value) ? null : "Invalid email"),
    },
  });

  const {
    data: credentialData,
    error: credentialError,
    isLoading: credentialIsLoading,
    refetch,
  } = $api.useQuery("get", "/user/credentials");

  const mutation = $api.useMutation("post", "/user/credentials");

  const deleteCredentialMutation = $api.useMutation(
    "delete",
    "/user/credentials/{credential_id}/delete"
  );

  const updateUserMutation = $api.useMutation("put", "/user");

  useEffect(() => {
    editUserForm.setValues({
      name: data?.name,
      email: data?.email,
      cwid: data?.cwid,
    });
  }, [data]);

  if (isLoading || !data) return "Loading...";

  if (credentialIsLoading || !credentialData) return "Credentials loading..";

  if (error || credentialError) return `An error occured: ${error}`;

  const editUser = (values: typeof editUserForm.values) => {
    updateUserMutation.mutate(
      {
        body: {
          cwid: data.cwid,
          email: values.email,
          name: values.name,
          admin: auth.user?.profile.is_admin as boolean,
          enabled: true,
        },
      },
      {
        onSuccess: () => {
          refetchUser();
        },
      }
    );
    closeEditUser();
  };

  const addCredential = (values: typeof form.values) => {
    mutation.mutate(
      {
        body: {
          owning_user: {
            email: data.email,
            cwid: data.cwid,
            name: data.name,
            admin: data.admin,
          },
          service: values.service as "canvas" | "gradescope", // still cursed
          api_key: values.apiKey,
          name: values.credentialName,
          private: true,
        },
      },
      {
        onSuccess: () => {
          refetch();
        },
      }
    );
    close();
  };

  const deleteCredential = (credential_id: string) => {
    deleteCredentialMutation.mutate(
      {
        params: {
          path: { credential_id: credential_id },
        },
      },
      {
        onSuccess: () => {
          refetch();
        },
      }
    );
    closeDelete();
  };

  const handleDelete = (credential: components["schemas"]["Credential"]) => {
    setSelectedCredential(credential);
    openDelete();
  };

  return (
    <>
      <Modal
        opened={deleteOpened}
        onClose={closeDelete}
        title="Delete Credential"
        centered
      >
        <Center>
          <Stack>
            <Text size="md">
              Are you sure you want to delete the specified credential?
            </Text>

            <Button color="gray" onClick={closeDelete}>
              Cancel
            </Button>

            <Button
              color="red"
              onClick={() => deleteCredential(selectedCredential?.id as string)}
            >
              Delete
            </Button>
          </Stack>
        </Center>
      </Modal>

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
            data={[
              { value: "canvas", label: "Canvas" },
              { value: "gradescope", label: "Gradescope" },
              { value: "prairielearn", label: "PrairieLearn" },
              { value: "runestone", label: "Runestone" },
            ]}
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

      <Modal opened={editUserOpened} onClose={closeEditUser} title="Edit User">
        <form onSubmit={editUserForm.onSubmit(editUser)}>
          <TextInput
            label="Name"
            key={editUserForm.key("name")}
            {...editUserForm.getInputProps("name")}
          />

          <TextInput
            label="Email"
            key={editUserForm.key("email")}
            {...editUserForm.getInputProps("email")}
          />

          <TextInput
            disabled
            label="CWID"
            key={editUserForm.key("cwid")}
            {...editUserForm.getInputProps("cwid")}
          />

          <br />

          <Group gap="xs" justify="flex-end">
            <Button color="gray" onClick={closeEditUser}>
              Cancel
            </Button>
            <Button color="blue" type="submit">
              Save
            </Button>
          </Group>
        </form>
      </Modal>

      <Container size="sm">
        <Group justify="space-between">
          <Text size="xl" fw={700}>
            Profile
          </Text>
          <Button justify="flex-end" variant="filled" onClick={openEditUser}>
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
                {credential.name}
              </Text>

              <Text size="md">Service: {credential.service}</Text>

              <Button color="red" onClick={() => handleDelete(credential)}>
                Delete
              </Button>
            </Box>
          </React.Fragment>
        ))}
      </Container>
    </>
  );
}
