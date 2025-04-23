import {
  Button,
  Center,
  Container,
  Divider,
  Group,
  Modal,
  ScrollArea,
  Stack,
  Table,
  Text,
  TextInput,
} from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { getApiClient } from "@repo/api/index";
import { LateRequest, StudentInformation } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { calculateNewDueDate, formattedDate } from "@repo/ui/DateUtil";
import { sortData, TableHeader } from "@repo/ui/table/Table";
import { IconSearch } from "@tabler/icons-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { BsPencilSquare } from "react-icons/bs";
import { Link } from "react-router-dom";

export function Requests() {
  const {
    data: studentData,
    error: studentError,
    isLoading: studentIsLoading,
    refetch: refetchStudent,
  } = useQuery<StudentInformation | null>({
    queryKey: ["getCourse"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_course_information_student({
            course_id: store$.id.get() as string,
          })
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });

  const { data, error, isLoading, refetch } = useQuery<LateRequest[] | null>({
    queryKey: ["getExtensions"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_all_extensions({
            course_id: store$.id.get() as string,
          })
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });

  const withdrawExtension = useMutation({
    mutationKey: ["withdrawExtension"],
    mutationFn: ({ extension_id }: { extension_id: string }) =>
      getApiClient()
        .then((client) =>
          client.withdraw_extension({
            course_id: store$.id.get() as string,
            extension_id: extension_id,
          })
        )
        .then((res) => res.data)
        .catch((err) => console.log(err)),
  });

  const [selectedExtension, setSelectedExtension] =
    useState<LateRequest | null>(null);
  const [opened, { open, close }] = useDisclosure(false);
  const [search, setSearch] = useState("");
  const [sortedData, setSortedData] = useState(data || []);
  const [sortBy, setSortBy] = useState<keyof LateRequest | null>(null);
  const [reverseSortDirection, setReverseSortDirection] = useState(false);

  // sync sortedData with data
  useEffect(() => {
    if (data) {
      setSortedData(data);
    }
  }, [data]);

  if (isLoading || !data) return "Loading...";

  if (error) return `An error occured: ${error}`;

  if (studentIsLoading || !studentData) return "Loading...";

  if (studentError) return `An error occured: ${studentError}`;

  const LATE_PASSES_ALLOWED =
    studentData.course.late_request_config.total_late_passes_allowed;

  const setSorting = (field: keyof LateRequest) => {
    const reversed = field === sortBy ? !reverseSortDirection : false;
    setReverseSortDirection(reversed);
    setSortBy(field);
    setSortedData(
      sortData<LateRequest>(data, { sortBy: field, reversed, search })
    );
  };

  const handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { value } = event.currentTarget;
    setSearch(value);
    setSortedData(
      sortData<LateRequest>(data, {
        sortBy,
        reversed: reverseSortDirection,
        search: value,
      })
    );
  };

  const handleWithdraw = (row: LateRequest) => {
    setSelectedExtension(row);
    open();
  };

  const deleteExtension = (extension_id: string) => {
    withdrawExtension.mutate(
      {
        extension_id: extension_id,
      },
      {
        onSuccess: () => {
          refetch();
          refetchStudent();
        },
      }
    );
    close();
  };

  const rows = sortedData.map((row: LateRequest) => (
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
            View
          </Text>
          <BsPencilSquare />
        </Center>
      </Table.Td>
    </Table.Tr>
  ));

  return (
    <>
      <Modal opened={opened} onClose={close} title="View Extension" centered>
        <Center>
          <Stack>
            <Text size="md">
              Request Date: {selectedExtension?.date_submitted}
            </Text>
            <Text size="md">
              Assignment: {selectedExtension?.assignment_name}
            </Text>
            <Text size="md">
              Type:{" "}
              {selectedExtension?.request_type === "late_pass"
                ? "Late Pass"
                : "Extension"}
            </Text>
            <Text size="md">Status: {selectedExtension?.status}</Text>
            {selectedExtension?.request_type === "extension" &&
              selectedExtension.status === "rejected" && (
                <Text>
                  Denial Reason:{" "}
                  {selectedExtension.extension?.response_to_requester}
                </Text>
              )}

            <Button variant="outline" color="gray" onClick={close}>
              Close
            </Button>

            <Button
              color="red"
              onClick={() => {
                deleteExtension(selectedExtension?.id as string);
              }}
            >
              Withdraw Extension
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

        <Group grow mt={25}>
          <Stack>
            {studentData.late_passes_used === LATE_PASSES_ALLOWED ? (
              <>
                <Text size="md" ta="center" c="red.9" fw={700}>
                  You have no late passes remaining!
                </Text>

                <Text ta="center" c="gray.7">
                  Please contact your instructor if you think this is a mistake.
                </Text>
              </>
            ) : (
              <>
                <Text size="md" ta="center">
                  You have{" "}
                  <strong>
                    {LATE_PASSES_ALLOWED - (studentData.late_passes_used ?? 0)}
                  </strong>{" "}
                  late passes remaining.
                </Text>
              </>
            )}
            <Button component={Link} to="/extension" variant="filled">
              Request Late Pass/Extension
            </Button>
          </Stack>
        </Group>
      </Container>
    </>
  );
}
