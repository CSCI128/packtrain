import {
  Button,
  Center,
  Container,
  Divider,
  Group,
  Table,
  Text,
} from "@mantine/core";
import { getApiClient } from "@repo/api/index";
import { Course, Policy } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { TableHeader } from "@repo/ui/table/Table";
import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { useEffect, useState } from "react";
import { BsBoxArrowUpRight, BsTrash } from "react-icons/bs";
import { Link, useNavigate } from "react-router-dom";
import { userManager } from "../../api";

export function CoursePage() {
  const navigate = useNavigate();

  const {
    data: courseData,
    error: courseError,
    isLoading: courseIsLoading,
  } = useQuery<Course>({
    queryKey: ["getCourse"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_course({
            course_id: store$.id.get() as string,
            include: ["members", "assignments", "sections"],
          })
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });

  const {
    data: policyData,
    error: policyError,
    isLoading: policyIsLoading,
  } = useQuery<Policy[]>({
    queryKey: ["getPolicies"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.admin_get_all_policies({
            course_id: store$.id.get() as string,
          })
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });

  const [policyDataState, setPolicyData] = useState<Policy[]>(policyData || []);

  useEffect(() => {
    if (policyData) {
      setPolicyData(policyData);
    }
  }, [policyData]);

  const handlePolicyDelete = (element: Policy) => {
    userManager.getUser().then((u) => {
      axios
        .delete(`/api/admin/course/${store$.id.get()}/policies/${element.id}`, {
          headers: {
            authorization: `Bearer ${u.access_token}`,
          },
        })
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
                <Table.Tr>
                  <TableHeader
                    reversed={false}
                    sorted={false}
                    onSort={undefined}
                  >
                    Name
                  </TableHeader>
                  <TableHeader
                    reversed={false}
                    sorted={false}
                    onSort={undefined}
                  >
                    Description
                  </TableHeader>
                  <TableHeader
                    reversed={false}
                    sorted={false}
                    onSort={undefined}
                  >
                    URI
                  </TableHeader>
                  <TableHeader
                    reversed={false}
                    sorted={false}
                    onSort={undefined}
                  >
                    Number of Migrations
                  </TableHeader>
                </Table.Tr>
              </Table.Tbody>
              <Table.Tbody>{rows}</Table.Tbody>
            </Table>
          )}
        <Divider my="md" />
      </Container>
    </>
  );
}
