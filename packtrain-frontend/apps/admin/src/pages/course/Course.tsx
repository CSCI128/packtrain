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
import { Policy } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { TableHeader } from "@repo/ui/table/Table";
import { useMutation } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { BsTrash } from "react-icons/bs";
import { Link, useNavigate } from "react-router-dom";
import { useGetCourse, useGetPolicies } from "../../hooks";

export function CoursePage() {
  const navigate = useNavigate();
  const {
    data: courseData,
    error: courseError,
    isLoading: courseIsLoading,
  } = useGetCourse(["members", "assignments", "sections"]);
  const {
    data: policyData,
    error: policyError,
    isLoading: policyIsLoading,
    refetch: refetchPolicies,
  } = useGetPolicies();
  const [policyDataState, setPolicyData] = useState<Policy[]>(policyData || []);

  const deletePolicy = useMutation({
    mutationKey: ["deletePolicy"],
    mutationFn: (policy_id: string) =>
      getApiClient()
        .then((client) =>
          client.delete_policy({
            course_id: store$.id.get() as string,
            policy_id: policy_id,
          })
        )
        .then((res) => res.data)
        .catch((err) => console.log(err)),
  });

  useEffect(() => {
    if (policyData) {
      setPolicyData(policyData);
    }
  }, [policyData]);

  const rows = policyDataState.map((element) => (
    <Table.Tr key={element.name}>
      <Table.Td>{element.name}</Table.Td>
      <Table.Td>{element.description}</Table.Td>
      <Table.Td>
        <a href={element.uri}>S3 URI</a>
      </Table.Td>
      <Table.Td>{element.number_of_migrations}</Table.Td>
      {(element.number_of_migrations == 0 && (
        <Table.Td
          onClick={() => {
            deletePolicy.mutate(element.id);
            refetchPolicies();
          }}
        >
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

  return (
    <>
      <Container size="md">
        {courseIsLoading || !courseData ? (
          <Text>Course is loading...</Text>
        ) : courseError ? (
          <Text>There was an error while trying to fetch the course!</Text>
        ) : (
          <>
            <Text size="xl" fw={700}>
              {courseData.name} ({courseData.code}) - {courseData.term}{" "}
              {!courseData.enabled && <>(DISABLED)</>}
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
            </Text>

            <Group mt={15} gap="xs" justify="flex-end">
              <Button component={Link} to="/admin/edit" variant="filled">
                Edit
              </Button>
            </Group>
          </>
        )}
      </Container>

      <Container size="md" mt={20}>
        <Group justify="space-between">
          <Text size="xl" fw={700}>
            Course Policies
          </Text>

          {courseData && courseData.enabled && (
            <Button component={Link} to="/admin/policies/new" variant="filled">
              New
            </Button>
          )}
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
