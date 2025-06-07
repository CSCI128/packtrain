import {
  Button,
  Center,
  Checkbox,
  Container,
  Divider,
  Group,
  InputWrapper,
  Modal,
  ScrollArea,
  Stack,
  Table,
  Text,
  TextInput,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { useDisclosure } from "@mantine/hooks";
import { getApiClient } from "@repo/api/index";
import { Course, User } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { sortData, TableHeader } from "@repo/ui/table/Table";
import { IconSearch } from "@tabler/icons-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import React, { useEffect, useState } from "react";
import { BsPencilSquare } from "react-icons/bs";
import { useAuth } from "react-oidc-context";

export function UsersPage() {
  const {
    data: courseData,
    error: courseError,
    isLoading: courseIsLoading,
  } = useQuery<Course>({
    queryKey: ["getCourse"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_course_information_instructor({
            course_id: store$.id.get() as string,
          })
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });

  const { data, error, isLoading, refetch } = useQuery<User[]>({
    queryKey: ["getUsers"],
    queryFn: () =>
      getApiClient()
        .then((client) => client.get_all_users())
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });

  const auth = useAuth();
  const [opened, { open, close }] = useDisclosure(false);
  const [addUserOpened, { open: openAddUser, close: closeAddUser }] =
    useDisclosure(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const addUserForm = useForm({
    mode: "uncontrolled",
    initialValues: {
      name: "",
      email: "",
      cwid: "",
      admin: false,
    },
    validate: {
      name: (value) =>
        value.length < 1 ? "Name must have at least 1 character" : null,
      email: (value) => (/^\S+@\S+$/.test(value) ? null : "Invalid email"),
      cwid: (value) =>
        value.length !== 8 ? "CWID must be 8 characters long" : null,
      admin: () => null,
    },
  });

  const editUserForm = useForm({
    mode: "uncontrolled",
    initialValues: {
      name: "",
      email: "",
      cwid: "",
      admin: false,
      enabled: true,
    },
    validate: {
      name: (value) =>
        value && value.length < 1
          ? "Name must have at least 1 character"
          : null,
      admin: () => null,
      enabled: () => null,
    },
  });

  const handleEditOpen = (row: User) => {
    setSelectedUser(row);
    editUserForm.setValues({
      name: row.name,
      email: row.email,
      cwid: row.cwid,
      admin: row.admin,
      enabled: row.enabled,
    });
    open();
  };

  const addUserMutation = useMutation({
    mutationFn: (userData: {
      cwid: string;
      email: string;
      admin: boolean;
      name: string;
      enabled: boolean;
    }) =>
      getApiClient()
        .then((client) => client.create_user({}, userData))
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });

  const editUserMutation = useMutation({
    mutationFn: (userData: {
      cwid: string;
      email: string;
      admin: boolean;
      name: string;
      enabled: boolean;
    }) =>
      getApiClient()
        .then((client) => client.admin_update_user({}, userData))
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });

  const addUser = (values: typeof addUserForm.values) => {
    addUserMutation.mutate(
      {
        cwid: values.cwid,
        email: values.email,
        admin: values.admin,
        name: values.name,
        enabled: true,
      },
      {
        onSuccess: () => {
          addUserForm.reset();
          refetch();
        },
      }
    );
    closeAddUser();
  };

  const editUser = (values: typeof editUserForm.values) => {
    editUserMutation.mutate(
      {
        cwid: selectedUser?.cwid as string,
        email: selectedUser?.email as string,
        admin: values.admin,
        name: values.name,
        enabled: values.enabled,
      },
      {
        onSuccess: () => {
          refetch();
        },
      }
    );
    close();
  };

  const [search, setSearch] = useState("");
  const [sortedData, setSortedData] = useState(data || []);
  const [sortBy, setSortBy] = useState<keyof User | null>(null);
  const [reverseSortDirection, setReverseSortDirection] = useState(false);

  // sync sortedData with data
  useEffect(() => {
    if (data) {
      setSortedData(data);
    }
  }, [data]);

  if (isLoading || !data) return "Loading...";

  if (error) return `An error occured: ${error}`;

  const setSorting = (field: keyof User) => {
    const reversed = field === sortBy ? !reverseSortDirection : false;
    setReverseSortDirection(reversed);
    setSortBy(field);
    setSortedData(sortData<User>(data, { sortBy: field, reversed, search }));
  };

  const handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { value } = event.currentTarget;
    setSearch(value);
    setSortedData(
      sortData<User>(data, {
        sortBy,
        reversed: reverseSortDirection,
        search: value,
      })
    );
  };

  const rows = sortedData.map((row) => (
    <Table.Tr key={row.cwid}>
      <Table.Td>
        {row.name} {row.admin && <>(admin)</>} {!row.enabled && <>(disabled)</>}
      </Table.Td>
      <Table.Td>{row.email}</Table.Td>
      <Table.Td>{row.cwid}</Table.Td>
      <Table.Td onClick={() => handleEditOpen(row)}>
        {courseData && courseData.enabled && (
          <Center>
            <Text size="sm" pr={5}>
              Edit
            </Text>
            <BsPencilSquare />
          </Center>
        )}
      </Table.Td>
    </Table.Tr>
  ));

  return (
    <>
      <Modal opened={opened} onClose={close} title="Edit User">
        <form onSubmit={editUserForm.onSubmit(editUser)}>
          <TextInput
            withAsterisk
            label="Name"
            key={editUserForm.key("name")}
            {...editUserForm.getInputProps("name")}
          />
          <TextInput
            disabled
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

          <InputWrapper withAsterisk label="Admin User">
            <Checkbox
              disabled={
                !editUserForm.getValues().enabled ||
                editUserForm.getValues().cwid === auth.user?.profile.cwid
              }
              defaultChecked={editUserForm.values.admin}
              key={editUserForm.key("admin")}
              {...editUserForm.getInputProps("admin", { type: "checkbox" })}
            />
          </InputWrapper>

          <InputWrapper withAsterisk label="Enabled">
            <Checkbox
              disabled={
                editUserForm.getValues().admin ||
                editUserForm.getValues().cwid === auth.user?.profile.cwid
              }
              defaultChecked={editUserForm.values.enabled}
              key={editUserForm.key("enabled")}
              {...editUserForm.getInputProps("enabled", { type: "checkbox" })}
            />
          </InputWrapper>

          <br />

          <Group gap="xs" justify="flex-end">
            <Button color="gray" variant="light" onClick={close}>
              Cancel
            </Button>
            <Button color="blue" type="submit">
              Save
            </Button>
          </Group>
        </form>
      </Modal>
      <Modal opened={addUserOpened} onClose={closeAddUser} title="Add User">
        <form onSubmit={addUserForm.onSubmit(addUser)}>
          <TextInput
            withAsterisk
            label="Name"
            placeholder="Jane Doe"
            key={addUserForm.key("name")}
            {...addUserForm.getInputProps("name")}
          />

          <TextInput
            withAsterisk
            label="Email"
            placeholder="user@email.com"
            key={addUserForm.key("email")}
            {...addUserForm.getInputProps("email")}
          />

          <TextInput
            withAsterisk
            label="CWID"
            placeholder="xxxxxxxx"
            key={addUserForm.key("cwid")}
            {...addUserForm.getInputProps("cwid")}
          />

          <InputWrapper withAsterisk label="Admin User">
            <Checkbox
              key={addUserForm.key("admin")}
              {...addUserForm.getInputProps("admin", { type: "checkbox" })}
            />
          </InputWrapper>

          <br />

          <Group gap="xs" justify="flex-end">
            <Button color="gray" variant="light" onClick={closeAddUser}>
              Cancel
            </Button>
            <Button color="green" type="submit">
              Add
            </Button>
          </Group>
        </form>
      </Modal>

      <Container size="md">
        <Group justify="space-between">
          <Text size="xl" fw={700}>
            Users
          </Text>
          {courseData && courseData.enabled && (
            <Button justify="flex-end" variant="filled" onClick={openAddUser}>
              Add User
            </Button>
          )}
        </Group>
        <Stack mt={10}>
          <TextInput
            placeholder="Search by any field"
            mb="md"
            leftSection={<IconSearch size={16} stroke={1.5} />}
            value={search}
            onChange={handleSearchChange}
          />
        </Stack>

        <Divider my="sm" />

        <ScrollArea h={750}>
          <Table horizontalSpacing="md" verticalSpacing="xs" miw={700}>
            <Table.Tbody>
              <Table.Tr>
                <TableHeader
                  sorted={sortBy === "name"}
                  reversed={reverseSortDirection}
                  onSort={() => setSorting("name")}
                >
                  Name
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "email"}
                  reversed={reverseSortDirection}
                  onSort={() => setSorting("email")}
                >
                  Email
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "cwid"}
                  reversed={reverseSortDirection}
                  onSort={() => setSorting("cwid")}
                >
                  CWID
                </TableHeader>
                <TableHeader sorted={false} reversed={false} onSort={undefined}>
                  Actions
                </TableHeader>
              </Table.Tr>
            </Table.Tbody>
            <Table.Tbody>
              {rows.length > 0 ? (
                rows
              ) : (
                <Table.Tr>
                  <Table.Td colSpan={Object.keys(data[0]).length}>
                    <Text fw={500} ta="center">
                      No users found
                    </Text>
                  </Table.Td>
                </Table.Tr>
              )}
            </Table.Tbody>
          </Table>
        </ScrollArea>
      </Container>
    </>
  );
}
