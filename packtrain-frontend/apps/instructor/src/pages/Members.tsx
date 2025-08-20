import {
  Container,
  ScrollArea,
  Table,
  Tabs,
  Text,
  TextInput,
} from "@mantine/core";
import { CourseMember } from "@repo/api/openapi";
import { Loading } from "@repo/ui/Loading";
import { TableHeader, useTableData } from "@repo/ui/table/Table";
import { IconSearch } from "@tabler/icons-react";
import { useGetMembersInstructor } from "../hooks";

export function MembersPage() {
  const { data, error, isLoading } = useGetMembersInstructor(
    ["students"],
    "getStudents"
  );

  const {
    data: instructorData,
    error: instructorError,
    isLoading: instructorIsLoading,
  } = useGetMembersInstructor(["instructors"], "getInstructors");

  const {
    search,
    sortedData,
    sortBy,
    reverseSortDirection,
    handleSearchChange,
    handleSort,
    resetTable,
  } = useTableData<CourseMember>(data ?? []);

  const {
    search: searchInstructor,
    sortedData: sortedDataInstructor,
    sortBy: sortByInstructor,
    reverseSortDirection: reverseSortDirectionInstructor,
    handleSearchChange: handleSearchChangeInstructor,
    handleSort: handleSortInstructor,
    resetTable: resetTableInstructor,
  } = useTableData<CourseMember>(instructorData ?? []);

  if (isLoading || !data || instructorIsLoading || !instructorData)
    return <Loading />;

  if (error || instructorError) return `An error occured: ${error}`;

  const rows = sortedData.map((row: CourseMember) => (
    <Table.Tr key={row.cwid}>
      <Table.Td>{row.name}</Table.Td>
      <Table.Td>{row.cwid}</Table.Td>
      <Table.Td>{row.sections?.toSorted().join(", ")}</Table.Td>
    </Table.Tr>
  ));

  const instructorRows = sortedDataInstructor.map((row: CourseMember) => (
    <Table.Tr key={row.cwid}>
      <Table.Td>{row.name}</Table.Td>
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

      <Tabs
        defaultValue="first"
        mt={10}
        onChange={() => {
          resetTable();
          resetTableInstructor();
        }}
      >
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
                    sorted={sortBy === "name"}
                    reversed={reverseSortDirection}
                    onSort={() => handleSort("name")}
                  >
                    Name
                  </TableHeader>
                  <TableHeader
                    sorted={sortBy === "cwid"}
                    reversed={reverseSortDirection}
                    onSort={() => handleSort("cwid")}
                  >
                    CWID
                  </TableHeader>
                  <TableHeader
                    sorted={sortBy === "sections"}
                    reversed={reverseSortDirection}
                    onSort={() => handleSort("sections")}
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
                    <Table.Td
                      colSpan={data?.[0] ? Object.keys(data[0]).length : 1}
                    >
                      <Text fw={500} ta="center">
                        No course members found
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
              value={searchInstructor}
              onChange={handleSearchChangeInstructor}
            />
            <Table horizontalSpacing="md" verticalSpacing="xs" miw={700}>
              <Table.Tbody>
                <Table.Tr>
                  <TableHeader
                    sorted={sortByInstructor === "cwid"}
                    reversed={reverseSortDirectionInstructor}
                    onSort={() => handleSortInstructor("cwid")}
                  >
                    CWID
                  </TableHeader>
                  <TableHeader
                    sorted={sortByInstructor === "name"}
                    reversed={reverseSortDirectionInstructor}
                    onSort={() => handleSortInstructor("name")}
                  >
                    Name
                  </TableHeader>
                  <TableHeader
                    sorted={sortByInstructor === "course_role"}
                    reversed={reverseSortDirectionInstructor}
                    onSort={() => handleSortInstructor("course_role")}
                  >
                    Role
                  </TableHeader>
                  <TableHeader
                    sorted={sortByInstructor === "sections"}
                    reversed={reverseSortDirectionInstructor}
                    onSort={() => handleSortInstructor("sections")}
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
                    <Table.Td
                      colSpan={data?.[0] ? Object.keys(data[0]).length : 1}
                    >
                      <Text fw={500} ta="center">
                        No course members found
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
