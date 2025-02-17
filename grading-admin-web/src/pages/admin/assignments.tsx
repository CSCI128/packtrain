import { Container, Divider, Table, Text } from "@mantine/core";

export function AssignmentsPage() {
  // const { data, error, isLoading } = $api.useQuery(
  //   "get",
  //   "/admin/course/{course_id}/assignments",
  //   {
  //     params: {
  //       path: { course_id: "1" },
  //     },
  //   }
  // );

  // if (isLoading || !data) return "Loading...";

  // if (error) return `An error occured: ${error}`;

  const elements = [
    { position: 6, mass: 12.011, symbol: "C", name: "Carbon" },
    { position: 7, mass: 14.007, symbol: "N", name: "Nitrogen" },
    { position: 39, mass: 88.906, symbol: "Y", name: "Yttrium" },
    { position: 56, mass: 137.33, symbol: "Ba", name: "Barium" },
    { position: 58, mass: 140.12, symbol: "Ce", name: "Cerium" },
  ];

  const rows = elements.map((element) => (
    <Table.Tr key={element.name}>
      <Table.Td>{element.position}</Table.Td>
      <Table.Td>{element.name}</Table.Td>
      <Table.Td>{element.symbol}</Table.Td>
      <Table.Td>{element.mass}</Table.Td>
    </Table.Tr>
  ));

  return (
    <>
      <Container size="md">
        <Text size="xl" fw={700}>
          Assignments
        </Text>

        <Divider my="sm" />

        <Table>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Element position</Table.Th>
              <Table.Th>Element name</Table.Th>
              <Table.Th>Symbol</Table.Th>
              <Table.Th>Atomic mass</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>{rows}</Table.Tbody>
        </Table>
      </Container>
    </>
  );
}
