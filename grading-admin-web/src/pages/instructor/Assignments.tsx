import { Container, Divider, Table, Text } from "@mantine/core";
import { $api, store$ } from "../../api";

export function AssignmentsPage() {
  const { data, error, isLoading } = $api.useQuery(
    "get",
    "/admin/courses/{course_id}",
    {
      params: {
        path: { course_id: store$.id.get() as string },
        query: { include: ["assignments"] },
      },
    }
  );

  if (isLoading || !data) return "Loading...";

  if (error) return `An error occured: ${error}`;

  console.log(data);

  const rows = data.assignments?.map((element) => (
    <Table.Tr key={element.id}>
      <Table.Td>{element.name}</Table.Td>
      <Table.Td>{element.category}</Table.Td>
      <Table.Td>{element.points}</Table.Td>
      <Table.Td>{element.unlock_date}</Table.Td>
      <Table.Td>{element.due_date}</Table.Td>
      <Table.Td>{element.enabled}</Table.Td>
    </Table.Tr>
  ));

  return (
    <Container size="md">
      <Text size="xl" fw={700}>
        Assignments
      </Text>

      <Divider my="sm" />

      <Table>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Name</Table.Th>
            <Table.Th>Category</Table.Th>
            <Table.Th>Points</Table.Th>
            <Table.Th>Unlock Date</Table.Th>
            <Table.Th>Due Date</Table.Th>
            <Table.Th>Enabled</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>{rows}</Table.Tbody>
      </Table>
    </Container>
  );
}
