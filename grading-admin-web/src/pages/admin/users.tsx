import { Container, Divider, ScrollArea, Table, Text } from "@mantine/core";
import { $api } from "../../api";

export function UsersPage() {
  const { data, error, isLoading } = $api.useQuery("get", "/admin/users");

  if (isLoading || !data) return "Loading...";

  if (error) return `An error occured: ${error}`;

  const rows = data.map((element) => (
    <Table.Tr key={element.cwid}>
      <Table.Td>{element.name}</Table.Td>
      <Table.Td>{element.email}</Table.Td>
      <Table.Td>{element.cwid}</Table.Td>
    </Table.Tr>
  ));

  return (
    <Container size="md">
      <Text size="xl" fw={700}>
        Users
      </Text>

      <Divider my="sm" />

      <ScrollArea h={500}>
        <Table>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Name</Table.Th>
              <Table.Th>Email</Table.Th>
              <Table.Th>CWID</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>{rows}</Table.Tbody>
        </Table>
      </ScrollArea>
    </Container>
  );
}
