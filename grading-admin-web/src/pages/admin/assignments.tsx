import { Container } from "@mantine/core";

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

  return (
    <>
      <Container size="md">
        <p>Assignments</p>
      </Container>
    </>
  );
}
