import {
  Button,
  Container,
  Divider,
  Group,
  Modal,
  Text,
  TextInput,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { useDisclosure } from "@mantine/hooks";
import { getApiClient } from "@repo/api/index";
import { User } from "@repo/api/openapi";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useEffect } from "react";
import { useAuth } from "react-oidc-context";

export function ProfilePage() {
  const auth = useAuth();
  const [editUserOpened, { open: openEditUser, close: closeEditUser }] =
    useDisclosure(false);

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

  if (error) return `An error occurred: ${error}`;

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

  return (
    <>
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
      </Container>
    </>
  );
}
