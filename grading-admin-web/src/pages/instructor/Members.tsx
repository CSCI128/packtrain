import {
  Container,
  ScrollArea,
  Table,
  Tabs,
  Text,
  TextInput,
} from "@mantine/core";
import { IconSearch } from "@tabler/icons-react";
import React, { useEffect, useState } from "react";
import { $api, store$ } from "../../api";
import { sortData, TableHeader } from "../../components/table/Table";

interface MemberRowData {
  cwid: string;
  canvas_id: string;
  course_role: "student" | "instructor" | "ta" | "owner";
  sections?: string[] | undefined;
}

export function MembersPage() {
  const { data, error, isLoading } = $api.useQuery(
    "get",
    "/admin/courses/{course_id}/members",
    {
      params: {
        path: { course_id: store$.id.get() as string },
        query: {
          enrollments: ["students"],
        },
      },
    }
  );

  const {
    data: instructorData,
    error: instructorError,
    isLoading: instructorIsLoading,
  } = $api.useQuery("get", "/admin/courses/{course_id}/members", {
    params: {
      path: { course_id: store$.id.get() as string },
      query: {
        enrollments: ["instructors"],
      },
    },
  });

  const [search, setSearch] = useState("");
  const [sortedData, setSortedData] = useState(data || []);
  const [sortedInstructorData, setSortedInstructorData] = useState(
    instructorData || []
  );
  const [sortBy, setSortBy] = useState<keyof MemberRowData | null>(null);
  const [reverseSortDirection, setReverseSortDirection] = useState(false);

  // sync sortedData with data
  useEffect(() => {
    if (data) {
      setSortedData(data);
    }
  }, [data]);

  // sync sortedData with instructordata
  useEffect(() => {
    if (instructorData) {
      setSortedInstructorData(instructorData);
    }
  }, [instructorData]);

  if (isLoading || !data || instructorIsLoading || !instructorData)
    return "Loading...";

  if (error || instructorError) return `An error occured: ${error}`;

  const setSorting = (field: keyof MemberRowData) => {
    const reversed = field === sortBy ? !reverseSortDirection : false;
    setReverseSortDirection(reversed);
    setSortBy(field);
    setSortedData(
      sortData<MemberRowData>(data, { sortBy: field, reversed, search })
    );
  };

  const handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { value } = event.currentTarget;
    setSearch(value);
    setSortedData(
      sortData<MemberRowData>(data, {
        sortBy,
        reversed: reverseSortDirection,
        search: value,
      })
    );
  };

  const setInstructorSorting = (field: keyof MemberRowData) => {
    const reversed = field === sortBy ? !reverseSortDirection : false;
    setReverseSortDirection(reversed);
    setSortBy(field);
    setSortedInstructorData(
      sortData<MemberRowData>(instructorData, {
        sortBy: field,
        reversed,
        search,
      })
    );
  };

  const handleSearchInstructorChange = (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    const { value } = event.currentTarget;
    setSearch(value);
    setSortedInstructorData(
      sortData<MemberRowData>(instructorData, {
        sortBy,
        reversed: reverseSortDirection,
        search: value,
      })
    );
  };

  const rows = sortedData.map((row) => (
    <Table.Tr key={row.cwid}>
      <Table.Td>{row.cwid}</Table.Td>
      <Table.Td>{row.course_role}</Table.Td>
      <Table.Td>{row.sections?.toSorted().join(", ")}</Table.Td>
    </Table.Tr>
  ));

  const instructorRows = sortedInstructorData.map((row) => (
    <Table.Tr key={row.cwid}>
      <Table.Td>{row.cwid}</Table.Td>
      <Table.Td>{row.course_role}</Table.Td>
      <Table.Td>{row.sections?.toSorted().join(", ")}</Table.Td>
    </Table.Tr>
  ));

  return (
    <Container size="md">
      <Text size="xl" fw={700}>
        Course Members
      </Text>

      <Tabs defaultValue="first" mt={10} onChange={() => setSearch("")}>
        <Tabs.List>
          <Tabs.Tab value="first">Students</Tabs.Tab>
          <Tabs.Tab value="second">Instructors</Tabs.Tab>
        </Tabs.List>

        <Tabs.Panel value="first" pt="xs">
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
                    sorted={sortBy === "cwid"}
                    reversed={reverseSortDirection}
                    onSort={() => setSorting("cwid")}
                  >
                    CWID
                  </TableHeader>
                  <TableHeader
                    sorted={sortBy === "course_role"}
                    reversed={reverseSortDirection}
                    onSort={() => setSorting("course_role")}
                  >
                    Role
                  </TableHeader>
                  <TableHeader
                    sorted={sortBy === "sections"}
                    reversed={reverseSortDirection}
                    onSort={() => setSorting("sections")}
                  >
                    Sections
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
        </Tabs.Panel>

        <Tabs.Panel value="second" pt="xs">
          <ScrollArea h={750}>
            <TextInput
              placeholder="Search by any field"
              mb="md"
              leftSection={<IconSearch size={16} stroke={1.5} />}
              value={search}
              onChange={handleSearchInstructorChange}
            />
            <Table horizontalSpacing="md" verticalSpacing="xs" miw={700}>
              <Table.Tbody>
                <Table.Tr>
                  <TableHeader
                    sorted={sortBy === "cwid"}
                    reversed={reverseSortDirection}
                    onSort={() => setInstructorSorting("cwid")}
                  >
                    CWID
                  </TableHeader>
                  <TableHeader
                    sorted={sortBy === "course_role"}
                    reversed={reverseSortDirection}
                    onSort={() => setInstructorSorting("course_role")}
                  >
                    Role
                  </TableHeader>
                  <TableHeader
                    sorted={sortBy === "sections"}
                    reversed={reverseSortDirection}
                    onSort={() => setInstructorSorting("sections")}
                  >
                    Sections
                  </TableHeader>
                </Table.Tr>
              </Table.Tbody>
              <Table.Tbody>
                {instructorRows.length > 0 ? (
                  instructorRows
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
        </Tabs.Panel>
      </Tabs>
    </Container>
  );
}
