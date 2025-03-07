import {
  Button,
  Center,
  Container,
  Divider,
  Group,
  keys,
  ScrollArea,
  Stack,
  Table,
  Text,
  TextInput,
  UnstyledButton,
} from "@mantine/core";
import {
  IconChevronDown,
  IconChevronUp,
  IconSearch,
  IconSelector,
} from "@tabler/icons-react";
import { useState } from "react";
import { Link } from "react-router-dom";
import classes from "../../components/table/Table.module.scss";

interface RequestRowData {
  id: string;
  requestDate: string;
  type: string;
  assignments: string;
  newDueDate: string;
  status: string;
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

function filterData(data: RequestRowData[], search: string) {
  const query = search.toLowerCase().trim();
  return data.filter((item) =>
    keys(data[0]).some((key) => String(item[key]).toLowerCase().includes(query))
  );
}

function sortData(
  data: RequestRowData[],
  payload: {
    sortBy: keyof RequestRowData | null;
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

export function Requests() {
  const data: RequestRowData[] = [
    {
      id: "1",
      requestDate: "3/7/25",
      type: "Extension",
      assignments: "Studio 1",
      newDueDate: "3/8/25",
      status: "Pending",
    },
  ]; // TODO replace w/ query

  const [search, setSearch] = useState("");
  const [sortedData, setSortedData] = useState(data || []);
  const [sortBy, setSortBy] = useState<keyof RequestRowData | null>(null);
  const [reverseSortDirection, setReverseSortDirection] = useState(false);

  const setSorting = (field: keyof RequestRowData) => {
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
    <Table.Tr key={row.id}>
      <Table.Td>{row.requestDate}</Table.Td>
      <Table.Td>{row.type}</Table.Td>
      <Table.Td>{row.assignments}</Table.Td>
      <Table.Td>{row.newDueDate}</Table.Td>
      <Table.Td>{row.status}</Table.Td>
    </Table.Tr>
  ));

  return (
    <Container>
      <Group justify="space-between">
        <Text size="xl" fw={700}>
          All Requests
        </Text>
        <Button
          justify="flex-end"
          component={Link}
          to="/extension"
          variant="filled"
        >
          New Request
        </Button>
      </Group>

      <Divider my="sm" />

      <ScrollArea>
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
                sorted={sortBy === "requestDate"}
                reversed={reverseSortDirection}
                onSort={() => setSorting("requestDate")}
              >
                Request Date
              </TableHeader>
              <TableHeader
                sorted={sortBy === "type"}
                reversed={reverseSortDirection}
                onSort={() => setSorting("type")}
              >
                Type
              </TableHeader>
              <TableHeader
                sorted={sortBy === "assignments"}
                reversed={reverseSortDirection}
                onSort={() => setSorting("assignments")}
              >
                Assignment(s)
              </TableHeader>
              <TableHeader
                sorted={sortBy === "newDueDate"}
                reversed={reverseSortDirection}
                onSort={() => setSorting("newDueDate")}
              >
                New Due Date
              </TableHeader>
              <TableHeader
                sorted={sortBy === "status"}
                reversed={reverseSortDirection}
                onSort={() => setSorting("status")}
              >
                Status
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

      <Group grow mt={25}>
        <Stack>
          <Text size="md" ta="center">
            You have <strong>X</strong> late passes remaining.
          </Text>
          <Button component={Link} to="/extension" variant="filled">
            Request Late Pass/Extension
          </Button>
        </Stack>
      </Group>
    </Container>
  );
}
