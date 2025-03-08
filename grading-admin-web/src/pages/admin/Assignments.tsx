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
import React, { useState } from "react";
import { BsPencilSquare } from "react-icons/bs";
import classes from "./Table.module.scss";

interface AssignmentRowData {
  id: string;
  name: string;
  category: string;
  points: number;
  externalService: string;
  externalPoints: number;
  unlock_date: string;
  due_date: string;
  enabled: boolean;
  status: string;
  group_assignment: boolean;
  attention_required: boolean;
  locked: boolean;
}

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

function filterData(data: AssignmentRowData[], search: string) {
  const query = search.toLowerCase().trim();
  return data.filter((item) =>
    keys(data[0]).some((key) => String(item[key]).toLowerCase().includes(query))
  );
}

function sortData(
  data: AssignmentRowData[],
  payload: {
    sortBy: keyof AssignmentRowData | null;
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

export function AssignmentsPage() {
  const data: AssignmentRowData[] = [
    {
      id: "1",
      name: "Assessment 1",
      category: "Assessments",
      points: 14,
      externalService: "Gradescope",
      externalPoints: 28,
      unlock_date: "May 1",
      due_date: "May 8",
      enabled: true,
      status: "Migrated",
      group_assignment: false,
      attention_required: false,
      locked: false,
    },
  ];
  // const { data, error, isLoading } = $api.useQuery(
  //   "get",
  //   "/admin/courses/{course_id}",
  //   {
  //     params: {
  //       path: { course_id: store$.id.get() as string },
  //       query: { include: ["assignments"] },
  //     },
  //   }
  // );

  const [opened, { open, close }] = useDisclosure(false);
  const [selectedAssignment, setSelectedAssignment] =
    useState<AssignmentRowData | null>(null);

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

  const handleAssignmentEdit = (row: any) => {
    setSelectedAssignment(row);
    open();
  };

  const [search, setSearch] = useState("");
  const [sortedData, setSortedData] = useState(data || []);
  const [sortBy, setSortBy] = useState<keyof AssignmentRowData | null>(null);
  const [reverseSortDirection, setReverseSortDirection] = useState(false);

  const setSorting = (field: keyof AssignmentRowData) => {
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

  // if (isLoading || !data) return "Loading...";

  // if (error) return `An error occurred: ${error}`;

  // need to add external service stuff - but that needs to be setup
  // the attention required field means that the grades are coming from an external source in canvas
  // and need to be setup
  const rows = sortedData.map((element) => (
    <Table.Tr key={element.id}>
      <Table.Td>{element.name}</Table.Td>
      <Table.Td>{element.category}</Table.Td>
      <Table.Td>{element.points}</Table.Td>
      <Table.Td>{element.externalService}</Table.Td>
      <Table.Td>{element.externalPoints}</Table.Td>
      <Table.Td>{element.unlock_date}</Table.Td>
      <Table.Td>{element.due_date}</Table.Td>
      <Table.Td>{element.enabled ? "Yes" : "No"}</Table.Td>
      <Table.Td>{element.group_assignment ? "Yes" : "No"}</Table.Td>
      <Table.Td>{element.attention_required ? "Yes" : "No"}</Table.Td>
      <Table.Td>{element.locked ? "Yes" : "No"}</Table.Td>
      <Table.Td>{element.status}</Table.Td>
      <Table.Td onClick={() => handleAssignmentEdit(element)}>
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
      <Modal opened={opened} onClose={close} title="Edit Assignment">
        <form onSubmit={form.onSubmit(() => {})}>
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
            <Checkbox defaultChecked={false} />
          </InputWrapper>

          <InputWrapper
            withAsterisk
            label="Enabled"
            key={form.key("enabled")}
            {...form.getInputProps("enabled")}
          >
            <Checkbox defaultChecked={false} />
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

      <Container size="lg">
        <Text size="xl" fw={700}>
          Assignments
        </Text>

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
                  sorted={sortBy === "category"}
                  reversed={reverseSortDirection}
                  onSort={() => setSorting("category")}
                >
                  Category
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "points"}
                  reversed={reverseSortDirection}
                  onSort={() => setSorting("points")}
                >
                  Points
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "externalService"}
                  reversed={reverseSortDirection}
                  onSort={() => setSorting("externalService")}
                >
                  External Service
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "externalPoints"}
                  reversed={reverseSortDirection}
                  onSort={() => setSorting("externalPoints")}
                >
                  External Points
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "unlock_date"}
                  reversed={reverseSortDirection}
                  onSort={() => setSorting("unlock_date")}
                >
                  Unlock Date
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "due_date"}
                  reversed={reverseSortDirection}
                  onSort={() => setSorting("due_date")}
                >
                  Due Date
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "enabled"}
                  reversed={reverseSortDirection}
                  onSort={() => setSorting("enabled")}
                >
                  Enabled
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "group_assignment"}
                  reversed={reverseSortDirection}
                  onSort={() => setSorting("group_assignment")}
                >
                  Group Assignment
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "attention_required"}
                  reversed={reverseSortDirection}
                  onSort={() => setSorting("attention_required")}
                >
                  Attention Required
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "locked"}
                  reversed={reverseSortDirection}
                  onSort={() => setSorting("locked")}
                >
                  Locked
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "status"}
                  reversed={reverseSortDirection}
                  onSort={() => setSorting("status")}
                >
                  Status
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
                      No assignments found
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
