import {
  Button,
  Center,
  Container,
  Divider,
  Group,
  Table,
  Text,
} from "@mantine/core";
import { $api, store$, userManager } from "@repo/api/api";
import { TableHeader } from "@repo/ui/table/Table";
import axios from "axios";
import { useEffect, useState } from "react";
import { BsBoxArrowUpRight, BsTrash } from "react-icons/bs";
import { Link, useNavigate } from "react-router-dom";
import { components } from "../../lib/api/v1";

export function CoursePage() {
  const navigate = useNavigate();

  const {
    data: courseData,
    error: courseError,
    isLoading: courseIsLoading,
  } = $api.useQuery("get", "/admin/courses/{course_id}", {
    params: {
      path: { course_id: store$.id.get() as string },
      query: {
        include: ["members", "assignments", "sections"],
      },
    },
  });

  const {
    data: policyData,
    error: policyError,
    isLoading: policyIsLoading,
  } = $api.useQuery("get", "/instructor/courses/{course_id}/policies", {
    params: {
      path: { course_id: store$.id.get() },
    },
  });

  const [policyDataState, setPolicyData] = useState<
    components["schemas"]["Policy"][]
  >(policyData || []);

  useEffect(() => {
    if (policyData) {
      setPolicyData(policyData);
    }
  }, [policyData]);

  const handlePolicyDelete = (element: components["schemas"]["Policy"]) => {
    userManager.getUser().then((u) => {
      axios
        .delete(
          `/api/instructor/course/${store$.id.get()}/policies/${element.id}`,
          {
            headers: {
              authorization: `Bearer ${u.access_token}`,
            },
          }
        )
        .then(() => location.reload());
    });
  };

  const rows = policyDataState.map((element) => (
    <Table.Tr key={element.name}>
      <Table.Td>{element.name}</Table.Td>
      <Table.Td>{element.description}</Table.Td>
      <Table.Td>
        <a href={element.uri}>S3 URI</a>
      </Table.Td>
      <Table.Td>{element.number_of_migrations}</Table.Td>
      {(element.number_of_migrations == 0 && (
        <Table.Td onClick={() => handlePolicyDelete(element)}>
          <Center>
            <Text size="sm" pr={5}>
              Delete
            </Text>
            <BsTrash />
          </Center>
        </Table.Td>
      )) || (
        <Table.Td>
          <Center>
            <Text size="sm" pr={5}>
              Delete Disabled!
            </Text>
          </Center>
        </Table.Td>
      )}
    </Table.Tr>
  ));

  if (courseIsLoading || !courseData) return "Loading...";

  if (courseError) return `An error occurred: ${courseError}`;

  return (
    <>
      <Container size="md">
        <Text size="xl" fw={700}>
          {courseData.name} ({courseData.code}) - {courseData.term}
        </Text>
        <Text>Course Overview</Text>

        <Divider my="md" />

        <Text size="md">
          <b>{courseData.members?.length}</b> members
        </Text>

        <Text size="md">
          <b>{courseData.sections?.length}</b> sections
        </Text>

        <Text size="md" onClick={() => navigate("/admin/assignments")}>
          <b>{courseData.assignments?.length}</b> assignments{" "}
          <BsBoxArrowUpRight />
        </Text>

        <Group mt={15} gap="xs">
          <Button component={Link} to="/admin/edit" variant="filled">
            Edit
          </Button>
        </Group>
      </Container>

      <Container size="md" mt={20}>
        <Group justify={"space-between"}>
          <Text size="xl" fw={700}>
            Course Policies
          </Text>

          <Button component={Link} to="/admin/policies/new" variant="filled">
            New
          </Button>
        </Group>

        <Divider my="md" />

        {(policyIsLoading && "Loading course policies...") ||
          (policyError && "An error occurred") || (
            <Table horizontalSpacing={"md"}>
              <Table.Tbody>
                <TableHeader reversed={false} sorted={false} onSort={undefined}>
                  Name
                </TableHeader>
                <TableHeader reversed={false} sorted={false} onSort={undefined}>
                  Description
                </TableHeader>
                <TableHeader reversed={false} sorted={false} onSort={undefined}>
                  URI
                </TableHeader>
                <TableHeader reversed={false} sorted={false} onSort={undefined}>
                  Number of Migrations
                </TableHeader>
              </Table.Tbody>
              <Table.Tbody>{rows}</Table.Tbody>
            </Table>
          )}
        <Divider my="md" />
      </Container>
    </>
  );
}
