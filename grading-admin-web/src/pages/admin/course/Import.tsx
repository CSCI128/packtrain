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
import { Link, useLocation, useNavigate } from "react-router-dom";
import { $api } from "../../../api";

export function ImportPage() {
  const [importing, setImporting] = useState(true);

  const location = useLocation();

  const navigate = useNavigate();

  const form = useForm({
    mode: "uncontrolled",
    initialValues: {
      // canvasAPIKey: "",
      courseId: "",
    },
    validate: {
      // canvasAPIKey: (value) =>
      // value.length < 1 ? "API key must have at least 1 character" : null,
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
    mutation.mutate(
      {
        params: {
          path: {
            course_id: location.state.course_id,
          },
        },
        body: {
          canvas_id: Number(location.state.canvas_id),
          overwrite_name: false,
          overwrite_code: false,
          import_users: true,
          import_assignments: true,
        },
      },
      {
        onSuccess: (response) => {
          console.log(response);
          // navigate("/admin/edit");
          setImporting(false);
        },
      }
    );
  };

  return (
    <>
      <Container size="xs">
        <Text size="xl" fw={700}>
          Link Canvas Course
        </Text>

        <Divider my="sm" />

        <Text size="md">Using canvas id: {location.state.canvas_id}</Text>

        <Text size="md">Using course id: {location.state.course_id}</Text>

        <form onSubmit={form.onSubmit(importCourse)}>
          {/* dont think this is working with hidden stuff */}
          {/* {hasCanvasCredential() ? (
            <>
              <Text mt={15} mb={15} c="dimmed" size="sm">
                Using existing Canvas credential..
              </Text>
            </>
          ) : (
            <>
              <TextInput
                label="Canvas API Key"
                placeholder="xxxxxxxx"
                key={form.key("canvasAPIKey")}
                {...form.getInputProps("canvasAPIKey")}
              />
            </>
          )} */}
          {/* TODO do we even want to handle this case? */}

          {/* TODO rethink how this is shown and if this is editable */}
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
        {!importing ? (
          <Group justify="flex-end" mt="md">
            <Button
              color={importing ? "blue" : "gray"}
              component={Link}
              to="/admin/edit"
            >
              Continue
            </Button>
          </Group>
        ) : (
          <></>
        )}
      </Container>
    </>
  );
}
