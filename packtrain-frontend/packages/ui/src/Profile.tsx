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
import { getApiClient } from "@repo/api/index";
import { Credential, User } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { useMutation, useQuery } from "@tanstack/react-query";
import React, { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";

export function ProfilePage() {
  const auth = useAuth();
  const [selectedCredential, setSelectedCredential] =
    useState<Credential | null>(null);
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
  } = useQuery({
    queryKey: ["getUser"],
    queryFn: () =>
      getApiClient()
        .then((client) => client.get_user())
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
    enabled: !!auth.isAuthenticated,
  });

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
  } = useQuery<Credential[] | null>({
    queryKey: ["getCredentials"],
    queryFn: () =>
      getApiClient()
        .then((client) => client.get_credentials())
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });

  const { data: enrollmentInfo } = useQuery({
    queryKey: ["getEnrollments"],
    queryFn: () =>
      getApiClient()
        .then((client) => client.get_enrollments())
        .then((res) => res.data)
        .catch((err) => console.log(err)),
    enabled: !!auth.isAuthenticated,
  });

  const mutation = useMutation({
    mutationKey: ["newCredential"],
    mutationFn: ({ body }: { body: Credential }) =>
      getApiClient()
        .then((client) => client.new_credential({}, body))
        .then((res) => res.data)
        .catch((err) => console.log(err)),
  });

  const deleteCredentialMutation = useMutation({
    mutationKey: ["deleteCredential"],
    mutationFn: ({ credential_id }: { credential_id: string }) =>
      getApiClient()
        .then((client) => client.delete_credential(credential_id))
        .then((res) => res.data)
        .catch((err) => console.log(err)),
  });

  const updateUserMutation = useMutation({
    mutationKey: ["updateUser"],
    mutationFn: ({ body }: { body: User }) =>
      getApiClient()
        .then((client) => client.update_user({}, body))
        .then((res) => res.data)
        .catch((err) => console.log(err)),
  });

  useEffect(() => {
    editUserForm.setValues({
      name: data?.name,
      email: data?.email,
      cwid: data?.cwid,
    });
  }, [data]);

  if (isLoading || !data) return "Loading...";

  if (credentialIsLoading || !credentialData) return "Credentials loading..";

  if (error || credentialError) return `An error occurred: ${error}`;

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
        credential_id: credential_id,
      },
      {
        onSuccess: () => {
          refetch();
        },
      }
    );
    closeDelete();
  };

  const handleDelete = (credential: Credential) => {
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

            <Button color="gray" variant="light" onClick={closeDelete}>
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
            <Button color="gray" variant="light" onClick={close}>
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
            <Button color="gray" variant="light" onClick={closeEditUser}>
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

        {enrollmentInfo &&
        enrollmentInfo.filter((c) => c.id === store$.id.get()).at(0)
          ?.course_role !== "student" ? (
          <>
            <Group mt={25} justify="space-between">
              <Text size="xl" fw={700}>
                Credentials
              </Text>
              <Button justify="flex-end" variant="filled" onClick={open}>
                Add Credential
              </Button>
            </Group>

            <Divider my="sm" />

            {credentialData.map((credential: Credential) => (
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
          </>
        ) : (
          <></>
        )}
      </Container>
    </>
  );
}
