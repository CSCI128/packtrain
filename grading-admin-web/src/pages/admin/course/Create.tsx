import {
  Button,
  Chip,
  Container,
  Divider,
  Group,
  Text,
  TextInput,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { useNavigate } from "react-router-dom";
import { $api } from "../../../api";

export function CreatePage() {
  const navigate = useNavigate();
  const mutation = $api.useMutation("post", "/admin/courses");

  // TODO add/link service mutation here

  const updateCourse = (values: typeof form.values) => {
    mutation.mutate(
      {
        body: {
          name: values.courseName,
          code: values.courseCode,
          term: values.courseTerm,
          enabled: true,
          canvas_id: Number(values.canvasId),
        },
      },
      {
        onSuccess: (response) => {
          navigate("/admin/import", {
            state: {
              canvas_id: Number(values.canvasId),
              course_id: response.id,
            },
          });
        },
      }
    );
  };

  const form = useForm({
    mode: "uncontrolled",
    initialValues: {
      courseName: "",
      courseCode: "",
      courseTerm: "",
      canvasId: "",
    },
    validate: {
      canvasId: (value) =>
        value.length == 5 ? null : "Canvas ID must be 5 characters",
      courseName: (value) =>
        value.length < 1 ? "Course name must have at least 1 character" : null,
      courseCode: (value) =>
        value.length < 1 ? "Course code must have at least 1 character" : null,
      courseTerm: (value) =>
        value.length < 1 ? "Course term must have at least 1 character" : null,
    },
  });

  return (
    <>
      <Container size="md">
        <Text size="xl" fw={700}>
          Create Course
        </Text>

        <Divider my="md" />

        <form onSubmit={form.onSubmit(updateCourse)}>
          <TextInput
            pb={8}
            label="Course Name"
            defaultValue="Computer Science For STEM"
            placeholder="Computer Science For STEM"
            key={form.key("courseName")}
            {...form.getInputProps("courseName")}
          />

          <TextInput
            pb={8}
            label="Course Code"
            defaultValue="CSCI128"
            placeholder="CSCI128"
            key={form.key("courseCode")}
            {...form.getInputProps("courseCode")}
          />

          <TextInput
            pb={8}
            label="Term"
            defaultValue="Fall 2024"
            placeholder="Fall 2024"
            key={form.key("courseTerm")}
            {...form.getInputProps("courseTerm")}
          />

          <TextInput
            label="Canvas ID"
            placeholder="xxxxxxxx"
            key={form.key("canvasId")}
            {...form.getInputProps("canvasId")}
          />

          <Text size="md">External Services</Text>

          <Chip.Group multiple>
            <Group>
              <Chip value="1">Gradescope</Chip>
              <Chip value="2">Runestone</Chip>
              <Chip value="3">PrairieLearn</Chip>
            </Group>
          </Chip.Group>

          <Group justify="flex-end" mt="md">
            <Button type="submit">Create</Button>
          </Group>
        </form>
      </Container>
    </>
  );
}
