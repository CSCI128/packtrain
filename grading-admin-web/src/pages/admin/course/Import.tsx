import { Button, Container, Group, Text, TextInput } from "@mantine/core";
import { useForm } from "@mantine/form";
import { Link } from "react-router-dom";

export function ImportPage() {
  const form = useForm({
    mode: "uncontrolled",
    initialValues: {
      email: "",
    },

    validate: {
      // TODO validate things
      email: (value) => (/^\S+@\S+$/.test(value) ? null : "Invalid email"),
    },
  });

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
        <Text size="xl" fw={700}>
          Link Canvas Course
        </Text>
        <form onSubmit={form.onSubmit((values) => console.log(values))}>
          <TextInput
            label="Canvas API Key"
            placeholder="xxxxxxxx"
            key={form.key("canvasId")}
          />

          {/* TODO hide once import is done */}
          <Group justify="flex-end" mt="md">
            <Button type="submit">Import</Button>
          </Group>
        </form>

        {/* TODO gray this out if import isn't done */}
        <Group justify="flex-end" mt="md">
          <Button component={Link} to="/admin/edit">
            Continue
          </Button>
        </Group>
      </Container>
    </>
  );
}
