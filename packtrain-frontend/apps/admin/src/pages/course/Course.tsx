import {
  Button,
  Container,
  Divider,
  Group,
  Stack,
  Table,
  Text,
} from "@mantine/core";
import { getApiClient } from "@repo/api/index";
import { Policy } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { Loading } from "@repo/ui/components/Loading";
import { TableHeader, useTableData } from "@repo/ui/components/table/Table";
import { useMutation } from "@tanstack/react-query";
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

  const deletePolicy = useMutation({
    mutationKey: ["deletePolicy"],
    mutationFn: async (policy_id: string) => {
      const client = await getApiClient();
      const res = await client.delete_policy({
        course_id: store$.id.get() as string,
        policy_id: policy_id,
      });
      return res.data;
    },
  });

  const { sortedData, sortBy, reverseSortDirection, handleSort } =
    useTableData<Policy>(policyData as Policy[]);

  if (policyIsLoading || !policyData || !courseData || courseIsLoading)
    return <Loading />;

  if (policyError || courseError)
    return `An error occured: ${courseError} ${policyError}`;

  const rows = sortedData.map((element: Policy) => (
    <Table.Tr key={element.name}>
      <Table.Td>{element.name}</Table.Td>
      <Table.Td>{element.description}</Table.Td>
      <Table.Td>{element.number_of_migrations}</Table.Td>
      <Table.Td>
        <Stack>
          <Button
            variant="outline"
            component={Link}
            size="compact-sm"
            to={`/admin/policies/edit/${element.id}`}
          >
            Edit Policy
          </Button>
          <Button
            size="compact-sm"
            disabled={element.number_of_migrations != 0}
            onClick={() => {
              deletePolicy.mutate(element.id as string);
              refetchPolicies();
            }}
            variant="outline"
            color="red"
          >
            {element.number_of_migrations == 0 ? "Delete" : "Delete Disabled"}
          </Button>
        </Stack>
      </Table.Td>
    </Table.Tr>
  ));

  return (
    <>
      <Container size="md">
        {courseIsLoading || !courseData ? (
          <Loading></Loading>
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
                    sorted={sortBy === "name"}
                    reversed={reverseSortDirection}
                    onSort={() => handleSort("name")}
                  >
                    Name
                  </TableHeader>
                  <TableHeader
                    sorted={false}
                    reversed={reverseSortDirection}
                    onSort={undefined}
                  >
                    Description
                  </TableHeader>
                  <TableHeader
                    sorted={sortBy === "number_of_migrations"}
                    reversed={reverseSortDirection}
                    onSort={() => handleSort("number_of_migrations")}
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
