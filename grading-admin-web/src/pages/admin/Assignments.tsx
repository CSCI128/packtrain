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
  Select,
  Table,
  Text,
  TextInput,
  UnstyledButton,
} from "@mantine/core";
import { DateInput } from "@mantine/dates";
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
import { $api, store$ } from "../../api";
import classes from "../../components/table/Table.module.scss";
import { components } from "../../lib/api/v1";

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
  // const data: AssignmentRowData[] = [
  //   {
  //     id: "1",
  //     name: "Assessment 1",
  //     category: "Assessments",
  //     points: 14,
  //     external_service: "GRADESCOPE",
  //     external_points: 28,
  //     unlock_date: "May 1 2025",
  //     due_date: "May 8 2025",
  //     enabled: true,
  //     status: "Migrated",
  //     canvas_id: 1,
  //     group_assignment: false,
  //     attention_required: false,
  //     frozen: false,
  //   },
  // ];
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

  const [value, setValue] = useState<Date | null>(null);
  const [unlockDateValue, setUnlockDateValue] = useState<Date | null>(null);
  const [opened, { open, close }] = useDisclosure(false);
  const [selectedAssignment, setSelectedAssignment] = useState<
    components["schemas"]["Assignment"] | null
  >(null); // TODO may not be necessary

  const form = useForm({
    mode: "controlled",
    initialValues: {
      name: "",
      category: "",
      canvas_id: -1,
      points: 0,
      due_date: new Date(),
      unlock_date: new Date(),
      external_service: "",
      external_points: 0,
      enabled: true,
      group_assignment: true,
      attention_required: true,
    },
    validate: {
      // TODO validate stuff here
      name: (value) =>
        value && value.length < 1
          ? "Name must have at least 1 character"
          : null,
      enabled: () => null,
    },
  });

  const handleAssignmentEdit = (row: components["schemas"]["Assignment"]) => {
    setValue(new Date(row.due_date));
    setUnlockDateValue(new Date(row.unlock_date));
    setSelectedAssignment(row);
    form.setValues({
      name: row.name,
      category: row.category,
      canvas_id: row.canvas_id,
      points: row.points,
      external_service: row.external_service,
      external_points: row.external_points,
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
      sortData(data?.assignments as AssignmentRowData[], {
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
      sortData(data?.assignments as AssignmentRowData[], {
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
          due_date: values.due_date.toISOString(),
          points: values.points,
          unlock_date: values.unlock_date.toISOString(),
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
        },
      }
    );
  };

  // sync sortedData with data - TODO see if this is necessary
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
            onChange={setUnlockDateValue}
          />

          <DateInput
            label="Due Date"
            placeholder="Pick date"
            value={value}
            onChange={setValue}
          />

          {/* TODO fix and prettify the boolean fields */}
          <InputWrapper
            withAsterisk
            label="Enabled"
            key={form.key("enabled")}
            {...form.getInputProps("enabled")}
          >
            <Checkbox defaultChecked={false} />
          </InputWrapper>

          <Text>
            Group assignment:{" "}
            {selectedAssignment?.group_assignment ? "Yes" : "No"}
          </Text>

          <Text>
            Frozen from re-syncing: {selectedAssignment?.frozen ? "Yes" : "No"}
          </Text>

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

      <Container fluid w="75%" p={35}>
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
