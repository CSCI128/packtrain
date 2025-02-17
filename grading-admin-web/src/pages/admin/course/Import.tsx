import {
  Button,
  Container,
  Divider,
  Group,
  Text,
  TextInput,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { useState } from "react";
import { Link } from "react-router-dom";
import { $api } from "../../../api";

export function ImportPage() {
  const [imported, setImported] = useState(false);

  const form = useForm({
    mode: "uncontrolled",
    initialValues: {
      canvasAPIKey: "",
      courseId: "",
    },
    validate: {
      canvasAPIKey: (value) =>
        value.length < 1 ? "API key must have at least 1 character" : null,
      courseId: (value) =>
        /^[0-9]+$/.test(value) ? null : "Invalid Canvas course ID",
    },
  });

  const {
    data: data,
    error: error,
    isLoading: isLoading,
  } = $api.useQuery("get", "/user/credential");

  const mutation = $api.useMutation(
    "post",
    "/admin/courses/{course_id}/import"
  );

  if (isLoading || !data) return "Loading...";

  if (error) return `An error occured: ${error}`;

  const hasCanvasCredential = () => {
    for (var credential of data) {
      if (credential.service === "canvas") {
        return true;
      }
    }
    return false;
  };

  const importCourse = (values: typeof form.values) => {
    mutation.mutate({
      params: {
        path: {
          course_id: values.courseId,
        },
      },
      body: {
        canvas_id: Number(values.courseId),
        overwrite_name: false,
        overwrite_code: false,
        import_users: true,
        import_assignments: true,
      },
    });
  };

  return (
    <>
      <Container size="xs">
        <Text size="xl" fw={700}>
          Link Canvas Course
        </Text>

        <Divider my="sm" />

        <form onSubmit={form.onSubmit(importCourse)}>
          {hasCanvasCredential() ? (
            <>
              <Text mt={15} mb={15} c="dimmed" size="sm">
                Using existing Canvas credential..
              </Text>
            </>
          ) : (
            <>
              {/* TODO do we even want to handle this case? */}
              <TextInput
                label="Canvas API Key"
                placeholder="xxxxxxxx"
                key={form.key("canvasAPIKey")}
                {...form.getInputProps("canvasAPIKey")}
              />
            </>
          )}

          <TextInput
            label="Canvas Course ID"
            placeholder="xxxxxxxx"
            key={form.key("courseId")}
            {...form.getInputProps("courseId")}
          />

          {/* TODO hide once import is done */}
          <Group justify="flex-end" mt="md">
            <Button type="submit">Import</Button>
          </Group>
        </form>

        {/* TODO gray this out if import isn't done */}
        <Group justify="flex-end" mt="md">
          <Button
            color={imported ? "blue" : "gray"}
            component={Link}
            to="/admin/edit"
          >
            Continue
          </Button>
        </Group>
      </Container>
    </>
  );
}
