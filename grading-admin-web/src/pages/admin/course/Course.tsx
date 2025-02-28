import { Button, Container, Divider, Group, Text } from "@mantine/core";
import { BsBoxArrowUpRight } from "react-icons/bs";
import { Link, useNavigate } from "react-router-dom";
import { $api, store$ } from "../../../api";

export function CoursePage() {
  const navigate = useNavigate();

  const { data, error, isLoading } = $api.useQuery(
    "get",
    "/admin/courses/{course_id}",
    {
      params: {
        path: { course_id: store$.id.get() as string },
        query: {
          include: ["members", "assignments", "sections"],
        },
      },
    }
  );

  const mutation = $api.useMutation("post", "/admin/courses/{course_id}/sync");

  if (isLoading || !data) return "Loading...";

  if (error) return `An error occured: ${error}`;

  const syncAssignments = () => {
    mutation.mutate(
      {
        params: {
          path: {
            course_id: "1",
          },
        },
        body: {
          canvas_id: 1,
          overwrite_name: false,
          overwrite_code: false,
          import_users: true,
          import_assignments: true,
        },
      },
      {
        onSuccess: (response) => {
          console.log(response);
        },
      }
    );
  };

  return (
    <>
      <Container size="md">
        <Text size="xl" fw={700}>
          {data.name} ({data.code}) - {data.term}
        </Text>
        <Text>Course Overview</Text>

        <Divider my="md" />

        <Text size="md">
          <b>{data.members?.length}</b> members
        </Text>

        <Text size="md">
          <b>{data.sections?.length}</b> sections
        </Text>

        <Text size="md" onClick={() => navigate("/admin/assignments")}>
          <b>{data.assignments?.length}</b> assignments <BsBoxArrowUpRight />
        </Text>

        <Group mt={15} gap="xs">
          <Button component={Link} to="/admin/edit" variant="filled">
            Edit
          </Button>
          <Button onClick={syncAssignments} variant="filled">
            Sync Assignments
          </Button>
        </Group>
      </Container>
      <Container size="md" mt={20}>
        <Text size="xl" fw={700}>
          Course Policies
        </Text>

        <Divider my="md" />
      </Container>
    </>
  );
}
