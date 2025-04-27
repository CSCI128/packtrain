import {
  Box,
  Button,
  Center,
  Container,
  Divider,
  Modal,
  ScrollArea,
  Stack,
  Table,
  Text,
  Textarea,
  TextInput,
} from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { getApiClient } from "@repo/api/index";
import { LateRequest } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { calculateNewDueDate, formattedDate } from "@repo/ui/DateUtil";
import { sortData, TableHeader } from "@repo/ui/table/Table";
import { IconSearch } from "@tabler/icons-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";

export function ApprovalPage() {
  const [denyOpened, { open: openDeny, close: closeDeny }] =
    useDisclosure(false);
  const [approveOpened, { open: openApprove, close: closeApprove }] =
    useDisclosure(false);
  const [selectedExtension, setSelectedExtension] =
    useState<LateRequest | null>(null);
  const [denialReason, setDenialReason] = useState<string>("");
  const [approvalReason, setApprovalReason] = useState<string>("");

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

  const approveExtensionMutation = useMutation({
    mutationKey: ["approveExtension"],
    mutationFn: ({
      assignment_id,
      user_id,
      extension_id,
      reason,
    }: {
      assignment_id: string;
      user_id: string;
      extension_id: string;
      reason: string;
    }) =>
      getApiClient()
        .then((client) =>
          client.approve_extension({
            course_id: store$.id.get() as string,
            assignment_id: assignment_id,
            user_id: user_id,
            extension_id: extension_id,
            reason: reason,
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
      reason,
    }: {
      assignment_id: string;
      user_id: string;
      extension_id: string;
      reason: string;
    }) =>
      getApiClient()
        .then((client) =>
          client.deny_extension({
            course_id: store$.id.get() as string,
            assignment_id: assignment_id,
            user_id: user_id,
            extension_id: extension_id,
            reason: reason,
          })
        )
        .then((res) => res.data)
        .catch((err) => console.log(err)),
  });

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

  const approveExtension = (request: LateRequest, reason: string) => {
    approveExtensionMutation.mutate(
      {
        assignment_id: request.assignment_id as string,
        user_id: request.user_requester_id as string,
        extension_id: request.id as string,
        reason: reason,
      },
      {
        onSuccess: () => {
          closeApprove();
          refetch();
        },
      }
    );
  };

  const denyExtension = (request: LateRequest, reason: string) => {
    denyExtensionMutation.mutate(
      {
        assignment_id: request.assignment_id as string,
        user_id: request.user_requester_id as string,
        extension_id: request.id as string,
        reason: reason,
      },
      {
        onSuccess: () => {
          closeDeny();
          refetch();
        },
      }
    );
  };

  const handleApproveExtension = (request: LateRequest) => {
    openApprove();
    setSelectedExtension(request);
  };

  const handleDenyExtension = (request: LateRequest) => {
    openDeny();
    setSelectedExtension(request);
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
      <Table.Td>{row.user_requester_id}</Table.Td>
      <Table.Td>{row.extension?.reason}</Table.Td>
      <Table.Td>{row.status}</Table.Td>
      <Table.Td>
        {row.request_type === "late_pass" ? (
          <></>
        ) : (
          <Center>
            {row.status !== "approved" && (
              <Box
                size="sm"
                py={5}
                px={10}
                mr={5}
                bd="1px solid green"
                c="green"
                onClick={() => handleApproveExtension(row)}
              >
                Approve
              </Box>
            )}
            {row.status !== "rejected" && (
              <Box
                size="sm"
                py={5}
                px={10}
                mr={5}
                bd="1px solid red"
                c="red"
                onClick={() => handleDenyExtension(row)}
              >
                Deny
              </Box>
            )}
          </Center>
        )}
      </Table.Td>
    </Table.Tr>
  ));

  return (
    <>
      <Modal
        opened={approveOpened}
        onClose={closeApprove}
        title="Approve Extension"
        centered
      >
        <Center>
          <Stack>
            <Textarea
              label="Please provide a reason for approving this extension:"
              placeholder="Some approval reason.."
              onChange={(text) => setApprovalReason(text.target.value)}
            />

            <Button variant="light" color="gray" onClick={closeApprove}>
              Cancel
            </Button>

            <Button
              color="green"
              onClick={() =>
                approveExtension(selectedExtension!, approvalReason)
              }
            >
              Approve
            </Button>
          </Stack>
        </Center>
      </Modal>

      <Modal
        opened={denyOpened}
        onClose={closeDeny}
        title="Deny Extension"
        centered
      >
        <Center>
          <Stack>
            <Textarea
              label="Please provide a reason for denying this extension:"
              placeholder="Some denial reason.."
              onChange={(text) => setDenialReason(text.target.value)}
            />

            <Button variant="light" color="gray" onClick={closeDeny}>
              Cancel
            </Button>

            {!!denialReason && (
              <Button
                color="red"
                onClick={() => denyExtension(selectedExtension!, denialReason)}
              >
                Deny
              </Button>
            )}
          </Stack>
        </Center>
      </Modal>

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
                  sorted={sortBy === "user_requester_id"}
                  reversed={reverseSortDirection}
                  onSort={() => setSorting("user_requester_id")}
                >
                  Requester
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "extension"}
                  reversed={reverseSortDirection}
                  onSort={() => setSorting("extension")}
                >
                  Reason
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
