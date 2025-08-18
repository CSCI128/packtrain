import {
  Container,
  ScrollArea,
  Table,
  Tabs,
  Text,
  TextInput,
} from "@mantine/core";
import { getApiClient } from "@repo/api/index";
import { CourseMember } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { Loading } from "@repo/ui/Loading";
import { sortData, TableHeader } from "@repo/ui/table/Table";
import { IconSearch } from "@tabler/icons-react";
import { useQuery } from "@tanstack/react-query";
import React, { useEffect, useState } from "react";

export function MembersPage() {
  const { data, error, isLoading } = useQuery<CourseMember[]>({
    queryKey: ["getMembers"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_members({
            course_id: store$.id.get() as string,
            enrollments: ["students"],
          })
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return [];
        }),
  });

  const {
    data: instructorData,
    error: instructorError,
    isLoading: instructorIsLoading,
  } = useQuery<CourseMember[]>({
    queryKey: ["getInstructors"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_members({
            course_id: store$.id.get() as string,
            enrollments: ["instructors"],
          })
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return [];
        }),
  });

  const [search, setSearch] = useState("");
  const [sortedData, setSortedData] = useState(data || []);
  const [sortedInstructorData, setSortedInstructorData] = useState(
    instructorData || []
  );
  const [sortBy, setSortBy] = useState<keyof CourseMember | null>("name");
  const [reverseSortDirection, setReverseSortDirection] = useState(false);

  // sync sortedData with data
  useEffect(() => {
    if (data) {
      setSortedData(
        sortData<CourseMember>(data as CourseMember[], {
          sortBy: sortBy ?? "name",
          reversed: reverseSortDirection,
          search,
        })
      );
    }
  }, [data, sortBy, reverseSortDirection, search]);

  // sync sortedData with instructordata
  useEffect(() => {
    if (instructorData) {
      setSortedInstructorData(
        sortData<CourseMember>(instructorData as CourseMember[], {
          sortBy: sortBy ?? "name",
          reversed: reverseSortDirection,
          search,
        })
      );
    }
  }, [instructorData, sortBy, reverseSortDirection, search]);

  if (isLoading || !data || instructorIsLoading || !instructorData)
    return <Loading />;

  if (error || instructorError) return `An error occured: ${error}`;

  const setSorting = (field: keyof CourseMember) => {
    const reversed = field === sortBy ? !reverseSortDirection : false;
    setReverseSortDirection(reversed);
    setSortBy(field);
    setSortedData(
      sortData<CourseMember>(data as CourseMember[], {
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
      sortData<CourseMember>(data as CourseMember[], {
        sortBy,
        reversed: reverseSortDirection,
        search: value,
      })
    );
  };

  const setInstructorSorting = (field: keyof CourseMember) => {
    const reversed = field === sortBy ? !reverseSortDirection : false;
    setReverseSortDirection(reversed);
    setSortBy(field);
    setSortedInstructorData(
      sortData<CourseMember>(instructorData, {
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
      sortData<CourseMember>(instructorData, {
        sortBy,
        reversed: reverseSortDirection,
        search: value,
      })
    );
  };

  const rows = sortedData.map((row) => (
    <Table.Tr key={row.cwid}>
      <Table.Td>{row.name}</Table.Td>
      <Table.Td>{row.cwid}</Table.Td>
      <Table.Td>{row.sections?.toSorted().join(", ")}</Table.Td>
    </Table.Tr>
  ));

  const instructorRows = sortedInstructorData.map((row) => (
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
                    sorted={sortBy === "name"}
                    reversed={reverseSortDirection}
                    onSort={() => setSorting("name")}
                  >
                    Name
                  </TableHeader>
                  <TableHeader
                    sorted={sortBy === "cwid"}
                    reversed={reverseSortDirection}
                    onSort={() => setSorting("cwid")}
                  >
                    CWID
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
                    sorted={sortBy === "name"}
                    reversed={reverseSortDirection}
                    onSort={() => setSorting("name")}
                  >
                    Name
                  </TableHeader>
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
