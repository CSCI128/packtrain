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

  if (isLoading || !data) return "Loading...";

  if (error) return `An error occurred: ${error}`;

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
        </Group>
      </Container>
      <Container size="md" mt={20}>
        <Text size="xl" fw={700}>
          Course Policies
        </Text>
        <Button component={Link} to="/admin/policies/new" variant="filled">
          New
        </Button>

        <Divider my="md" />
      </Container>
    </>
  );
}
