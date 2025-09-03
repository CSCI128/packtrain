import {
  Box,
  Button,
  Center,
  Checkbox,
  Container,
  Divider,
  Group,
  InputWrapper,
  Modal,
  ScrollArea,
  Select,
  Table,
  Text,
  TextInput,
} from "@mantine/core";
import { DateInput } from "@mantine/dates";
import { useForm } from "@mantine/form";
import { useDisclosure } from "@mantine/hooks";
import { getApiClient } from "@repo/api/index";
import { Assignment } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { formattedDate } from "@repo/ui/DateUtil";
import { Loading } from "@repo/ui/Loading";
import { TableHeader, useTableData } from "@repo/ui/table/Table";
import { IconSearch } from "@tabler/icons-react";
import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { BsPencilSquare } from "react-icons/bs";
import { useGetCourse } from "../hooks";

interface AssignmentRowData {
  id: string;
  name: string;
  category: string;
  canvas_id: number;
  points: number;
  external_service: string;
  external_points: number;
  unlock_date: string;
  due_date: string;
  enabled: boolean;
  status: string;
  group_assignment: boolean;
  attention_required: boolean;
  frozen: boolean;
}

export function AssignmentsPage() {
  const { data, error, isLoading, refetch } = useGetCourse(["assignments"]);
  const [value, setValue] = useState<Date>();
  const [unlockDateValue, setUnlockDateValue] = useState<Date>();
  const [opened, { open, close }] = useDisclosure(false);
  const [selectedAssignment, setSelectedAssignment] =
    useState<Assignment | null>(null);

  const form = useForm({
    mode: "uncontrolled",
    initialValues: {
      name: "",
      category: "",
      canvas_id: -1,
      points: 0,
      due_date: value,
      unlock_date: unlockDateValue,
      external_service: "",
      external_points: 0,
      enabled: true,
      group_assignment: true,
      attention_required: true,
    },
    validate: {
      name: (value) =>
        value && value.length < 1
          ? "Name must have at least 1 character"
          : null,
      category: (value) =>
        value && value.length < 1
          ? "Category must have at least 1 character"
          : null,
      points: (value) =>
        value && value <= 0 ? "Points must be greater than 0" : null,
    },
  });

  const handleAssignmentEdit = (row: Assignment) => {
    setSelectedAssignment(row);
    form.setValues({
      name: row.name,
      category: row.category,
      canvas_id: row.canvas_id,
      points: row.points,
      external_service: row.external_service,
      external_points: row.external_points,
      due_date: new Date(row.due_date),
      unlock_date: new Date(row.unlock_date),
      enabled: row.enabled,
      group_assignment: row.group_assignment,
      attention_required: row.attention_required,
    });
    open();
  };

  const updateAssignment = useMutation({
    mutationKey: ["updateAssignment"],
    mutationFn: ({ body }: { body: Assignment }) =>
      getApiClient()
        .then((client) =>
          client.update_assignment(
            {
              course_id: store$.id.get() as string,
            },
            body
          )
        )
        .then((res) => res.data)
        .catch((err) => console.log(err)),
  });

  const editAssignment = (values: typeof form.values) => {
    console.log(values.group_assignment);
    updateAssignment.mutate(
      {
        body: {
          id: selectedAssignment?.id,
          name: values.name,
          category: values.category,
          due_date: value?.toISOString(),
          points: values.points,
          unlock_date: unlockDateValue?.toISOString(),
          external_service: values.external_service,
          external_points: values.external_points,
          canvas_id: values.canvas_id,
          enabled: values.enabled,
          attention_required: values.attention_required,
          group_assignment: values.group_assignment,
        },
      },
      {
        onSuccess: () => {
          close();
          refetch();
          // TODO are these values even needed?
          setValue(new Date());
          setUnlockDateValue(new Date());
        },
      }
    );
  };

  const {
    search,
    sortedData,
    sortBy,
    reverseSortDirection,
    handleSearchChange,
    handleSort,
  } = useTableData<Assignment>(
    (data?.assignments as AssignmentRowData[]) ?? []
  );

  if (isLoading || !data) return <Loading />;

  if (error) return `An error occurred: ${error}`;

  const rows = sortedData.map((element) => (
    <Table.Tr key={element.id}>
      <Table.Td>{element.name}</Table.Td>
      <Table.Td>{element.category}</Table.Td>
      <Table.Td>{element.points}</Table.Td>
      <Table.Td>{element.external_service}</Table.Td>
      <Table.Td>{element.external_points}</Table.Td>
      <Table.Td>{formattedDate(new Date(element.due_date as string))}</Table.Td>
      <Table.Td>{element.enabled ? "Yes" : "No"}</Table.Td>
      <Table.Td>{element.attention_required && "Attention required!"}</Table.Td>
      <Table.Td onClick={() => handleAssignmentEdit(element)}>
        {data.enabled && (
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
      <Modal opened={opened} onClose={close} title="Edit Assignment">
        <Box w="95%" mx="auto">
          <form onSubmit={form.onSubmit(editAssignment)}>
            <TextInput
              disabled
              label="Name"
              key={form.key("name")}
              {...form.getInputProps("name")}
            />

            <TextInput
              disabled
              label="Category"
              key={form.key("category")}
              {...form.getInputProps("category")}
            />

            <TextInput
              disabled
              label="Canvas ID"
              key={form.key("canvas_id")}
              {...form.getInputProps("canvas_id")}
            />

            <TextInput
              disabled
              label="Points"
              key={form.key("points")}
              {...form.getInputProps("points")}
            />

            <Select
              withAsterisk
              label="External Service:"
              placeholder="Pick value"
              data={[
                { value: "GRADESCOPE", label: "Gradescope" },
                { value: "PRAIRIELEARN", label: "PrairieLearn" },
                { value: "RUNESTONE", label: "Runestone" },
              ]}
              key={form.key("external_service")}
              {...form.getInputProps("external_service")}
            />

            <TextInput
              withAsterisk
              label="External Points"
              key={form.key("external_points")}
              {...form.getInputProps("external_points")}
            />

            <DateInput
              disabled
              label="Unlock Date"
              placeholder="Pick date"
              value={unlockDateValue}
              {...form.getInputProps("unlock_date")}
            />

            <DateInput
              disabled
              label="Due Date"
              placeholder="Pick date"
              value={value}
              {...form.getInputProps("due_date")}
            />

            <InputWrapper withAsterisk label="Enabled">
              <Checkbox
                defaultChecked={form.values.enabled}
                key={form.key("enabled")}
                {...form.getInputProps("enabled", { type: "checkbox" })}
              />
            </InputWrapper>

            <InputWrapper withAsterisk label="Group Assignment">
              <Checkbox
                defaultChecked={form.values.group_assignment}
                key={form.key("group_assignment")}
                {...form.getInputProps("group_assignment", {
                  type: "checkbox",
                })}
              />
            </InputWrapper>

            <InputWrapper
              label={
                <>
                  Frozen from re-syncing:{" "}
                  {selectedAssignment?.frozen ? "Yes" : "No"}
                </>
              }
            ></InputWrapper>

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
        </Box>
      </Modal>

      <Container fluid w="100%" p={35}>
        <Group>
          <Text size="xl" fw={700}>
            Assignments
          </Text>

          <TextInput
            placeholder="Search by any field"
            leftSection={<IconSearch size={16} stroke={1.5} />}
            value={search}
            onChange={handleSearchChange}
          />
        </Group>

        <Divider my="sm" />

        <ScrollArea h={650}>
          <Table horizontalSpacing="md" verticalSpacing="xs" miw={700}>
            <Table.Tbody>
              <Table.Tr>
                <TableHeader
                  sorted={sortBy === "name"}
                  reversed={reverseSortDirection}
                  onSort={() => handleSort("name")}
                >
                  Name
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "category"}
                  reversed={reverseSortDirection}
                  onSort={() => handleSort("category")}
                >
                  Category
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "points"}
                  reversed={reverseSortDirection}
                  onSort={() => handleSort("points")}
                >
                  Points
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "external_service"}
                  reversed={reverseSortDirection}
                  onSort={() => handleSort("external_service")}
                >
                  External Service
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "external_points"}
                  reversed={reverseSortDirection}
                  onSort={() => handleSort("external_points")}
                >
                  External Points
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "due_date"}
                  reversed={reverseSortDirection}
                  onSort={() => handleSort("due_date")}
                >
                  Due Date
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "enabled"}
                  reversed={reverseSortDirection}
                  onSort={() => handleSort("enabled")}
                >
                  Enabled
                </TableHeader>
                <TableHeader
                  sorted={undefined}
                  reversed={reverseSortDirection}
                  onSort={undefined}
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
                  <Table.Td colSpan={data.assignments?.length}>
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
