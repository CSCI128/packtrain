import {
  Button,
  Center,
  Container,
  Divider,
  Group,
  keys,
  Modal,
  ScrollArea,
  Stack,
  Table,
  Text,
  TextInput,
  UnstyledButton,
} from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import {
  IconChevronDown,
  IconChevronUp,
  IconSearch,
  IconSelector,
} from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { MdDelete } from "react-icons/md";
import { Link } from "react-router-dom";
import { $api, store$ } from "../../api";
import classes from "../../components/table/Table.module.scss";
import { components } from "../../lib/api/v1";
import { calculateNewDueDate, formattedDate } from "../../utils/DateUtil";

interface RequestRowData {
  id?: string;
  date_submitted: string;
  request_type: "extension" | "late_pass";
  num_days_requested: number;
  assignment_id?: string;
  assignment_name?: string;
  extension?: components["schemas"]["Extension"];
  user_requester_id?: string;
  status: "pending" | "approved" | "rejected";
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
  const { data, error, isLoading, refetch } = $api.useQuery(
    "get",
    "/student/courses/{course_id}/extensions",
    {
      params: {
        path: { course_id: store$.id.get() as string },
      },
    }
  );

  const withdrawExtension = $api.useMutation(
    "delete",
    "/student/courses/{course_id}/extensions/{extension_id}/withdraw"
  );

  const [selectedExtension, setSelectedExtension] = useState<
    components["schemas"]["LateRequest"] | null
  >(null);
  const [deleteOpened, { open: openDelete, close: closeDelete }] =
    useDisclosure(false);
  const [search, setSearch] = useState("");
  const [sortedData, setSortedData] = useState(data || []);
  const [sortBy, setSortBy] = useState<keyof RequestRowData | null>(null);
  const [reverseSortDirection, setReverseSortDirection] = useState(false);

  // sync sortedData with data
  useEffect(() => {
    if (data) {
      setSortedData(data);
    }
  }, [data]);

  if (isLoading || !data) return "Loading...";

  if (error) return `An error occured: ${error}`;

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

  const handleWithdraw = (row: RequestRowData) => {
    setSelectedExtension(row);
    openDelete();
  };

  const deleteExtension = (extension_id: string) => {
    withdrawExtension.mutate(
      {
        params: {
          path: {
            course_id: store$.id.get() as string,
            extension_id: extension_id,
          },
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

  const rows = sortedData.map((row) => (
    <Table.Tr key={row.id}>
      <Table.Td>{formattedDate(new Date(row.date_submitted))}</Table.Td>
      <Table.Td>
        {formattedDate(
          new Date(
            calculateNewDueDate(
              new Date(Date.parse(row.date_submitted)),
              row.num_days_requested
            )
          )
        )}
      </Table.Td>
      <Table.Td>
        {row.request_type === "late_pass" ? "Late Pass" : "Extension"}
      </Table.Td>
      <Table.Td>{row.assignment_name}</Table.Td>
      <Table.Td>{row.status}</Table.Td>
      <Table.Td onClick={() => handleWithdraw(row)}>
        <Center>
          <Text size="sm" pr={5}>
            Withdraw
          </Text>
          <MdDelete />
        </Center>
      </Table.Td>
    </Table.Tr>
  ));

  return (
    <>
      <Modal
        opened={deleteOpened}
        onClose={closeDelete}
        title="Withdraw Extension"
        centered
      >
        <Center>
          <Stack>
            <Text size="md">
              Are you sure you want to withdraw the specified late request? This
              action <strong>cannot</strong> be undone!
            </Text>

            <Button color="gray" onClick={closeDelete}>
              Cancel
            </Button>

            <Button
              color="red"
              onClick={() => {
                deleteExtension(selectedExtension?.id as string);
              }}
            >
              Delete
            </Button>
          </Stack>
        </Center>
      </Modal>

      <Container size="lg">
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
                  sorted={sortBy === "date_submitted"}
                  reversed={reverseSortDirection}
                  onSort={() => setSorting("date_submitted")}
                >
                  Request Date
                </TableHeader>
                <TableHeader
                  sorted={false}
                  // sorted={sortBy === "new_due_date"}
                  reversed={reverseSortDirection}
                  // onSort={() => setSorting("new_due_date")}
                  onSort={() => {}}
                >
                  New Due Date
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "request_type"}
                  reversed={reverseSortDirection}
                  onSort={() => setSorting("request_type")}
                >
                  Type
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "assignment_name"}
                  reversed={reverseSortDirection}
                  onSort={() => setSorting("assignment_name")}
                >
                  Assignment(s)
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
                  <Table.Td colSpan={data.length}>
                    <Text fw={400} ta="center">
                      No late requests found
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
    </>
  );
}
