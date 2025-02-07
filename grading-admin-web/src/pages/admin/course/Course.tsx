import { Button, Container, Divider, Group, Text } from "@mantine/core";
import { BsBoxArrowUpRight } from "react-icons/bs";
import { Link } from "react-router-dom";

export function CoursePage() {
  const data = {
    id: "1",
    term: "Fall 2020",
    enabled: true,
    name: "Computer Science for STEM",
    code: "CSCI128",
    canvas_id: "1",
  };

  const syncAssignments = () => {
    console.log("hello");
  };

  // const { data, error, isLoading } = $api.useQuery(
  //   "get",
  //   "/admin/course/{course_id}",
  //   {
  //     params: {
  //       path: { course_id: "1" },
  //     },
  //   }
  // );

  // if (isLoading || !data) return "Loading...";

  // if (error) return `An error occured: ${error}`;

  return (
    <>
      <Container size="md">
        <Text size="xl" fw={700}>
          {data.name} ({data.code}) - {data.term}
        </Text>
        <Text>Course Overview</Text>

        <Divider my="md" />

        <Text size="md">
          <b>652</b> active students
        </Text>

        <Text size="md">
          <b>12</b> sections <BsBoxArrowUpRight />
        </Text>

        <Text size="md">
          <b>102</b> assignments <BsBoxArrowUpRight />
        </Text>

        <Group mt={15} gap="xs">
          <Button component={Link} to="/admin/edit" variant="filled">
            Edit
          </Button>
          <Button onClick={syncAssignments} variant="filled">
            Sync assignments
          </Button>
        </Group>
      </Container>
      <Container size="md" mt={20}>
        <Text size="xl" fw={700}>
          Global Course Policies
        </Text>
      </Container>
    </>
  );
}
