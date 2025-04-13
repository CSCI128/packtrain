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
import { store$ } from "@repo/api/store";
import { sortData, TableHeader } from "@repo/ui/table/Table";
import { IconSearch } from "@tabler/icons-react";
import React, { useEffect, useState } from "react";
import { BsPencilSquare } from "react-icons/bs";
import { $api } from "../api";
import { components } from "../lib/api/v1";

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
  const { data, error, isLoading, refetch } = $api.useQuery(
    "get",
    "/admin/courses/{course_id}",
    {
      params: {
        path: { course_id: store$.id.get() as string },
        query: { include: ["assignments"] },
      },
    }
  );

  const [value, setValue] = useState<Date | null>(new Date());
  const [unlockDateValue, setUnlockDateValue] = useState<Date | null>(
    new Date()
  );
  const [opened, { open, close }] = useDisclosure(false);
  const [selectedAssignment, setSelectedAssignment] = useState<
    components["schemas"]["Assignment"] | null
  >(null);

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

  const handleAssignmentEdit = (row: components["schemas"]["Assignment"]) => {
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

  const [search, setSearch] = useState("");
  const [sortedData, setSortedData] = useState(data?.assignments || []);
  const [sortBy, setSortBy] = useState<keyof AssignmentRowData | null>(null);
  const [reverseSortDirection, setReverseSortDirection] = useState(false);

  const setSorting = (field: keyof AssignmentRowData) => {
    const reversed = field === sortBy ? !reverseSortDirection : false;
    setReverseSortDirection(reversed);
    setSortBy(field);
    setSortedData(
      sortData<AssignmentRowData>(data?.assignments as AssignmentRowData[], {
        sortBy: field,
        reversed,
        search,
      })
    );
  };

  const handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { value } = event.currentTarget;
    setSearch(value);
    setSortedData(
      sortData<AssignmentRowData>(data?.assignments as AssignmentRowData[], {
        sortBy,
        reversed: reverseSortDirection,
        search: value,
      })
    );
  };

  const updateAssignment = $api.useMutation(
    "put",
    "/admin/courses/{course_id}/assignments"
  );

  const editAssignment = (values: typeof form.values) => {
    updateAssignment.mutate(
      {
        params: {
          path: {
            course_id: store$.id.get() as string,
          },
        },
        body: {
          id: selectedAssignment?.id,
          name: values.name,
          category: values.category,
          due_date: value?.toISOString()!,
          points: values.points,
          unlock_date: unlockDateValue?.toISOString()!,
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
          setSearch("");
          setValue(new Date());
          setUnlockDateValue(new Date());
        },
      }
    );
  };

  // sync sortedData with data
  useEffect(() => {
    if (data?.assignments) {
      setSortedData(data.assignments);
    }
  }, [data?.assignments]);

  if (isLoading || !data) return "Loading...";

  if (error) return `An error occurred: ${error}`;

  // need to add external service stuff - but that needs to be setup
  // the attention required field means that the grades are coming from an external source in canvas
  // and need to be setup
  const rows = sortedData.map((element) => (
    <Table.Tr key={element.id}>
      <Table.Td>{element.name}</Table.Td>
      <Table.Td>{element.category}</Table.Td>
      <Table.Td>{element.points}</Table.Td>
      <Table.Td>{element.external_service}</Table.Td>
      <Table.Td>{element.external_points}</Table.Td>
      <Table.Td>{element.due_date}</Table.Td>
      <Table.Td>{element.enabled ? "Yes" : "No"}</Table.Td>
      <Table.Td>{element.attention_required && "Attention required!"}</Table.Td>
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
        <Box w="95%" mx="auto">
          <form onSubmit={form.onSubmit(editAssignment)}>
            <TextInput
              withAsterisk
              label="Name"
              key={form.key("name")}
              {...form.getInputProps("name")}
            />

            <TextInput
              withAsterisk
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
              withAsterisk
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
              onChange={setUnlockDateValue}
            />

            <DateInput
              label="Due Date"
              placeholder="Pick date"
              value={value}
              {...form.getInputProps("due_date")}
              onChange={setValue}
            />

            <InputWrapper withAsterisk label="Enabled">
              <Checkbox
                defaultChecked={form.values.enabled}
                key={form.key("enabled")}
                {...form.getInputProps("enabled", { type: "checkbox" })}
              />
            </InputWrapper>

            <Text>
              Group assignment:{" "}
              {selectedAssignment?.group_assignment ? "Yes" : "No"}
            </Text>

            <Text>
              Frozen from re-syncing:{" "}
              {selectedAssignment?.frozen ? "Yes" : "No"}
            </Text>

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

        <ScrollArea h={500}>
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
                  sorted={sortBy === "external_service"}
                  reversed={reverseSortDirection}
                  onSort={() => setSorting("external_service")}
                >
                  External Service
                </TableHeader>
                <TableHeader
                  sorted={sortBy === "external_points"}
                  reversed={reverseSortDirection}
                  onSort={() => setSorting("external_points")}
                >
                  External Points
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
