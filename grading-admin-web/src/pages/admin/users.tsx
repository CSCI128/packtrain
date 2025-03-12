import {
  Button,
  Center,
  Checkbox,
  Container,
  Divider,
  Group,
  InputWrapper,
  keys,
  Modal,
  ScrollArea,
  Table,
  Text,
  TextInput,
  UnstyledButton,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { useDisclosure } from "@mantine/hooks";
import {
  IconChevronDown,
  IconChevronUp,
  IconSearch,
  IconSelector,
} from "@tabler/icons-react";
import React, { useEffect, useState } from "react";
import { BsPencilSquare } from "react-icons/bs";
import { $api } from "../../api";
import classes from "./Table.module.scss";

// adapted from https://ui.mantine.dev/category/tables/#table-sort
interface User {
  email: string;
  cwid: string;
  name: string;
  admin: boolean;
  enabled: boolean;
}

interface UserRowData {
  email: string;
  cwid: string;
  name: string;
  admin: boolean;
}

// TODO can have null reversed/sorted/onsort; if null display nothing in that header
interface TableHeaderProps {
  children: React.ReactNode;
  reversed: boolean;
  sorted: boolean;
  onSort: () => void;
}

function TableHeader({ children, reversed, sorted, onSort }: TableHeaderProps) {
  const Icon = sorted
    ? reversed
      ? IconChevronUp
      : IconChevronDown
    : IconSelector;
  return (
    <Table.Th className={classes.th}>
      <UnstyledButton onClick={onSort} className={classes.control}>
        <Group justify="space-between">
          <Text fw={500} fz="sm">
            {children}
          </Text>
          <Center className={classes.icon}>
            <Icon size={16} stroke={1.5} />
          </Center>
        </Group>
      </UnstyledButton>
    </Table.Th>
  );
}

function filterData(data: UserRowData[], search: string) {
  const query = search.toLowerCase().trim();
  return data.filter((item) =>
    keys(data[0]).some((key) => String(item[key]).toLowerCase().includes(query))
  );
}

function sortData(
  data: UserRowData[],
  payload: {
    sortBy: keyof UserRowData | null;
    reversed: boolean;
    search: string;
  }
) {
  const { sortBy } = payload;

  if (!sortBy) {
    return filterData(data, payload.search);
  }

  return filterData(
    [...data].sort((a, b) => {
      if (payload.reversed) {
        return String(b[sortBy]).localeCompare(String(a[sortBy]));
      }

      return String(a[sortBy]).localeCompare(String(b[sortBy]));
    }),
    payload.search
  );
}

export function UsersPage() {
  const { data, error, isLoading, refetch } = $api.useQuery(
    "get",
    "/admin/users"
  );

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
  const form = useForm({
    mode: "controlled",
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

  const handleEditOpen = (row: any) => {
    setSelectedUser(row);
    form.setValues({
      name: row.name,
      email: row.email,
      cwid: row.cwid,
    });
    open();
  };

  const addUserMutation = $api.useMutation("post", "/admin/users");
  const editUserMutation = $api.useMutation("put", "/user");

  const addUser = (values: typeof addUserForm.values) => {
    addUserMutation.mutate(
      {
        body: {
          cwid: values.cwid,
          email: values.email,
          admin: values.admin,
          name: values.name,
          enabled: true,
        },
      },
      {
        onSuccess: () => {
          refetch();
        },
      }
    );
    closeAddUser();
  };

  const editUser = (values: typeof form.values) => {
    editUserMutation.mutate(
      {
        body: {
          cwid: selectedUser?.cwid!,
          email: selectedUser?.email!,
          admin: values.admin,
          name: values.name,
          enabled: values.enabled,
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

  const [search, setSearch] = useState("");
  const [sortedData, setSortedData] = useState(data || []);
  const [sortBy, setSortBy] = useState<keyof UserRowData | null>(null);
  const [reverseSortDirection, setReverseSortDirection] = useState(false);

  // sync sortedData with data - TODO see if this is necessary
  useEffect(() => {
    if (data) {
      setSortedData(data);
    }
  }, [data]);

  if (isLoading || !data) return "Loading...";

  if (error) return `An error occured: ${error}`;

  const setSorting = (field: keyof UserRowData) => {
    const reversed = field === sortBy ? !reverseSortDirection : false;
    setReverseSortDirection(reversed);
    setSortBy(field);
    setSortedData(sortData(data, { sortBy: field, reversed, search }));
  };

  const handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { value } = event.currentTarget;
    setSearch(value);
    setSortedData(
      sortData(data, { sortBy, reversed: reverseSortDirection, search: value })
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
        <Center>
          <Text size="sm" pr={5}>
            Edit
          </Text>
          <BsPencilSquare />
        </Center>
      </Table.Td>
    </Table.Tr>
  ));

  return (
    <>
      {/* TODO should export these modals to a separate component file */}
      <Modal opened={opened} onClose={close} title="Edit User">
        <form onSubmit={form.onSubmit(editUser)}>
          <TextInput
            withAsterisk
            label="Name"
            key={form.key("name")}
            {...form.getInputProps("name")}
          />

          <TextInput
            disabled
            label="Email"
            key={form.key("email")}
            {...form.getInputProps("email")}
          />

          <TextInput
            disabled
            label="CWID"
            key={form.key("cwid")}
            {...form.getInputProps("cwid")}
          />

          <InputWrapper
            withAsterisk
            label="Admin User"
            key={form.key("admin")}
            {...form.getInputProps("admin")}
          >
            <Checkbox defaultChecked={selectedUser?.admin ? true : undefined} />
          </InputWrapper>

          <InputWrapper
            withAsterisk
            label="Enabled"
            key={form.key("enabled")}
            {...form.getInputProps("enabled")}
          >
            <Checkbox defaultChecked={selectedUser?.enabled ? true : false} />
          </InputWrapper>

          <br />

          <Group gap="xs" justify="flex-end">
            <Button color="gray" onClick={close}>
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

          <InputWrapper
            withAsterisk
            label="Admin User"
            key={addUserForm.key("admin")}
            {...addUserForm.getInputProps("admin")}
          >
            <Checkbox />
          </InputWrapper>

          <br />

          <Group gap="xs" justify="flex-end">
            <Button color="gray" onClick={closeAddUser}>
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
          <Button justify="flex-end" variant="filled" onClick={openAddUser}>
            Add User
          </Button>
        </Group>

        <Divider my="sm" />

        <ScrollArea h={750}>
          <TextInput
            placeholder="Search by any field"
            mb="md"
            leftSection={<IconSearch size={16} stroke={1.5} />}
            value={search}
            onChange={handleSearchChange}
          />
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
                <TableHeader sorted={false} reversed={false} onSort={() => {}}>
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
