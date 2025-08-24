import {
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
import { Loading } from "@repo/ui/Loading";
import { TableHeader, useTableData } from "@repo/ui/table/Table";
import { IconSearch } from "@tabler/icons-react";
import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { useAuth } from "react-oidc-context";
import { useGetCourseInstructor, useGetExtensions } from "../hooks";

type LateRequestWithDueDate = LateRequest & {
  new_due_date: Date;
};

export function ApprovalPage() {
  const auth = useAuth();
  const {
    data: courseData,
    error: courseError,
    isLoading: courseIsLoading,
  } = useGetCourseInstructor();
  const { data, error, isLoading, refetch } = useGetExtensions();
  const [denyOpened, { open: openDeny, close: closeDeny }] =
    useDisclosure(false);
  const [approveOpened, { open: openApprove, close: closeApprove }] =
    useDisclosure(false);
  const [selectedExtension, setSelectedExtension] =
    useState<LateRequest | null>(null);
  const [denialReason, setDenialReason] = useState<string>("");
  const [approvalReason, setApprovalReason] = useState<string>("");

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
            cwid: user_id,
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
            cwid: user_id,
            extension_id: extension_id,
            reason: reason,
          })
        )
        .then((res) => res.data)
        .catch((err) => console.log(err)),
  });

  const preprocessedData = (data ?? []).map((row) => ({
    ...row,
    new_due_date: calculateNewDueDate(
      new Date(row.date_submitted),
      row.num_days_requested
    ),
  }));

  const {
    search,
    sortedData,
    sortBy,
    reverseSortDirection,
    handleSearchChange,
    handleSort,
  } = useTableData<LateRequestWithDueDate>(preprocessedData);

  if (isLoading || !data || courseIsLoading || !courseData) return <Loading />;

  if (error || courseError) return `An error occured: ${error} ${courseError}`;

  const approveExtension = (request: LateRequest, reason: string) => {
    approveExtensionMutation.mutate(
      {
        assignment_id: request.assignment_id as string,
        user_id: auth?.user?.profile.cwid as string,
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
        user_id: auth?.user?.profile.cwid as string,
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

  const ApproveRejectButtons = (row: LateRequestWithDueDate) => {
    return (
      <Center>
        {row.status !== "approved" && (
          <Button
            size="sm"
            py={5}
            px={10}
            mr={5}
            radius={10}
            bg="green"
            onClick={() => handleApproveExtension(row)}
          >
            Approve
          </Button>
        )}
        {row.status !== "rejected" && (
          <Button
            size="sm"
            py={5}
            px={10}
            mr={5}
            radius={10}
            bg="red"
            onClick={() => handleDenyExtension(row)}
          >
            Deny
          </Button>
        )}
      </Center>
    );
  };

  const rows = sortedData.map((row: LateRequestWithDueDate) => (
    <Table.Tr key={row.id}>
      <Table.Td>{formattedDate(new Date(row.date_submitted))}</Table.Td>
      <Table.Td>{formattedDate(row.new_due_date)}</Table.Td>
      <Table.Td>
        {row.request_type === "late_pass" ? "Late Pass" : "Extension"}
      </Table.Td>
      <Table.Td>{row.assignment_name}</Table.Td>
      <Table.Td>{row.requestor}</Table.Td>
      <Table.Td>{row.user_reviewer}</Table.Td>
      <Table.Td>{row.instructor}</Table.Td>
      <Table.Td>{row.extension?.reason}</Table.Td>
      <Table.Td>{row.status}</Table.Td>
      <Table.Td>
        {row.request_type !== "late_pass" && courseData?.enabled && (
          <ApproveRejectButtons {...row} />
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

      <Container size="xxl">
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
                  onSort={() => handleSort("date_submitted")}
                >
                  Request Date
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "new_due_date"}
                  reversed={reverseSortDirection}
                  onSort={() => handleSort("new_due_date")}
                >
                  New Due Date
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "request_type"}
                  reversed={reverseSortDirection}
                  onSort={() => handleSort("request_type")}
                >
                  Type
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "assignment_name"}
                  reversed={reverseSortDirection}
                  onSort={() => handleSort("assignment_name")}
                >
                  Assignment(s)
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "requestor"}
                  reversed={reverseSortDirection}
                  onSort={() => handleSort("requestor")}
                >
                  Requestor
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "reviewer"}
                  reversed={reverseSortDirection}
                  onSort={() => handleSort("reviewer")}
                >
                  Reviewer
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "instructor"}
                  reversed={reverseSortDirection}
                  onSort={() => handleSort("instructor")}
                >
                  Section Instructor
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "extension"}
                  reversed={reverseSortDirection}
                  onSort={() => handleSort("extension")}
                >
                  Reason
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "status"}
                  reversed={reverseSortDirection}
                  onSort={() => handleSort("status")}
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
