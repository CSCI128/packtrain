import {
  Center,
  Container,
  Divider,
  ScrollArea,
  Table,
  Text,
  TextInput,
} from "@mantine/core";
import { getApiClient } from "@repo/api/index";
import { Extension, LateRequest } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { calculateNewDueDate, formattedDate } from "@repo/ui/DateUtil";
import { sortData, TableHeader } from "@repo/ui/table/Table";
import { IconSearch } from "@tabler/icons-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";

interface RequestRowData {
  id?: string;
  date_submitted: string;
  request_type: "extension" | "late_pass";
  num_days_requested: number;
  assignment_id?: string;
  assignment_name?: string;
  extension?: Extension;
  user_requester_id?: string;
  status: "pending" | "approved" | "rejected";
}

export function ApprovalPage() {
  const { data, error, isLoading, refetch } = useQuery<LateRequest[] | null>({
    queryKey: ["getExtensions"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_all_extensions_for_course({
            course_id: store$.id.get() as string,
          })
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });

  const approveExtensionMutation = useMutation({
    mutationKey: ["approveExtension"],
    mutationFn: ({
      assignment_id,
      user_id,
      extension_id,
    }: {
      assignment_id: string;
      user_id: string;
      extension_id: string;
    }) =>
      getApiClient()
        .then((client) =>
          client.approve_extension({
            course_id: store$.id.get() as string,
            assignment_id: assignment_id,
            user_id: user_id,
            extension_id: extension_id,
          })
        )
        .then((res) => res.data)
        .catch((err) => console.log(err)),
  });

  const denyExtensionMutation = useMutation({
    mutationKey: ["denyExtension"],
    mutationFn: ({
      assignment_id,
      user_id,
      extension_id,
    }: {
      assignment_id: string;
      user_id: string;
      extension_id: string;
    }) =>
      getApiClient()
        .then((client) =>
          client.deny_extension({
            course_id: store$.id.get() as string,
            assignment_id: assignment_id,
            user_id: user_id,
            extension_id: extension_id,
          })
        )
        .then((res) => res.data)
        .catch((err) => console.log(err)),
  });

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
    setSortedData(
      sortData<RequestRowData>(data, { sortBy: field, reversed, search })
    );
  };

  const handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { value } = event.currentTarget;
    setSearch(value);
    setSortedData(
      sortData<RequestRowData>(data, {
        sortBy,
        reversed: reverseSortDirection,
        search: value,
      })
    );
  };

  const approveExtension = (request: RequestRowData) => {
    approveExtensionMutation.mutate(
      {
        assignment_id: request.assignment_id as string,
        user_id: request.user_requester_id as string,
        extension_id: request.id as string,
      },
      {
        onSuccess: () => {
          refetch();
        },
      }
    );
  };

  const denyExtension = (request: RequestRowData) => {
    denyExtensionMutation.mutate(
      {
        assignment_id: request.assignment_id as string,
        user_id: request.user_requester_id as string,
        extension_id: request.id as string,
      },
      {
        onSuccess: () => {
          refetch();
        },
      }
    );
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
      <Table.Td>
        <Center>
          {row.status !== "approved" && (
            <Text
              size="sm"
              pr={5}
              c="green"
              onClick={() => approveExtension(row)}
            >
              Approve
            </Text>
          )}
          {row.status !== "rejected" && (
            <Text size="sm" pr={5} c="red" onClick={() => denyExtension(row)}>
              Deny
            </Text>
          )}
        </Center>
      </Table.Td>
    </Table.Tr>
  ));

  return (
    <>
      <Container size="lg">
        <Text size="xl" fw={700}>
          Manage Requests
        </Text>

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
      </Container>
    </>
  );
}
