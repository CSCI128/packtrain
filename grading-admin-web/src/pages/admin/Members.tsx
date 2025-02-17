import { Container, Divider, Table, Tabs, Text } from "@mantine/core";
import { $api } from "../../api";

export function MembersPage() {
  const { data, error, isLoading } = $api.useQuery("get", "/admin/users");

  if (isLoading || !data) return "Loading...";

  if (error) return `An error occured: ${error}`;

  console.log(data);

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
          Course Members
        </Text>

        <Divider my="sm" />

        <Tabs defaultValue="first" mt={10}>
          <Tabs.List>
            <Tabs.Tab value="first">Members</Tabs.Tab>
            <Tabs.Tab value="second">Instructors</Tabs.Tab>
          </Tabs.List>

          <Tabs.Panel value="first" pt="xs">
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
          </Tabs.Panel>

          <Tabs.Panel value="second" pt="xs">
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
          </Tabs.Panel>
        </Tabs>
      </Container>
    </>
  );
}
