import { Button, Container, Group, Text, TextInput } from "@mantine/core";
import { useForm } from "@mantine/form";
import { useState } from "react";
import { Link } from "react-router-dom";
import { $api } from "../../../api";

export function ImportPage() {
  const [imported, setImported] = useState(false);
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

  const { data, error, isLoading } = $api.useQuery(
    "post",
    "/admin/course/new/{canvas_id}",
    {
      params: {
        path: { canvas_id: "5" },
      },
    }
  );

  // https://localhost.dev/api/user/credential

  const importCourse = (values: any) => {
    console.log(values);
  };

  if (isLoading || !data) return "Loading...";

  if (error) return `An error occured: ${error}`;

  return (
    <>
      <Container size="xs">
        <Text size="xl" fw={700}>
          Link Canvas Course
        </Text>
        <form onSubmit={form.onSubmit((values) => importCourse(values))}>
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
