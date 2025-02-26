import { Container, ScrollArea, Table, Tabs, Text } from "@mantine/core";
import { $api, store$ } from "../../api";

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

  if (isLoading || !data || instructorIsLoading || !instructorData)
    return "Loading...";

  if (error || instructorError) return `An error occured: ${error}`;

  const memberRows = data.map((element) => (
    <Table.Tr key={element.cwid}>
      <Table.Td>{element.cwid}</Table.Td>
      <Table.Td>{element.course_role}</Table.Td>
      <Table.Td>{element.sections}</Table.Td>
    </Table.Tr>
  ));

  const instructorRows = instructorData.map((element) => (
    <Table.Tr key={element.cwid}>
      <Table.Td>{element.cwid}</Table.Td>
      <Table.Td>{element.course_role}</Table.Td>
      <Table.Td>{element.sections?.toSorted().join(", ")}</Table.Td>
    </Table.Tr>
  ));

  return (
    <>
      <Container size="md">
        <Text size="xl" fw={700}>
          Course Members
        </Text>

        <Tabs defaultValue="first" mt={10}>
          <Tabs.List>
            <Tabs.Tab value="first">Students</Tabs.Tab>
            <Tabs.Tab value="second">Instructors</Tabs.Tab>
          </Tabs.List>

          <Tabs.Panel value="first" pt="xs">
            <ScrollArea h={500}>
              <Table>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>CWID</Table.Th>
                    <Table.Th>Role</Table.Th>
                    <Table.Th>Sections</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>{memberRows}</Table.Tbody>
              </Table>
            </ScrollArea>
          </Tabs.Panel>

          <Tabs.Panel value="second" pt="xs">
            <ScrollArea h={500}>
              <Table>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>CWID</Table.Th>
                    <Table.Th>Role</Table.Th>
                    <Table.Th>Sections</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>{instructorRows}</Table.Tbody>
              </Table>
            </ScrollArea>
          </Tabs.Panel>
        </Tabs>
      </Container>
    </>
  );
}
