import { Container } from "@mantine/core";

export function ImportPage() {
  //   const { data, error, isLoading } = $api.useQuery(
  //     "put",
  //     "/admin/course/new/{canvas_id}",
  //     {
  //       params: {
  //         path: { canvas_id: "5" },
  //       },
  //     }
  //   );

  //   if (isLoading || !data) return "Loading...";

  //   if (error) return `An error occured: ${error}`;

  //   return (
  //     <>
  //       <div>{data.name}</div>
  //     </>
  //   );

  return (
    <>
      <Container size="xs">
        xs Container
        <p>Hello</p>
      </Container>
    </>
  );
}
