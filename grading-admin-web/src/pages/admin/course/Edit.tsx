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
import { Link } from "react-router-dom";
import { $api } from "../../../api";

export function EditCourse() {
  const updateCourse = () => {
    const { data, error, isLoading } = $api.useQuery(
      "put",
      "/admin/course/{course_id}",
      {
        params: {
          path: { course_id: "5" },
        },
      }
    );

    if (isLoading || !data) return "Loading...";

    if (error) return `An error occured: ${error}`;

    console.log(data);
    return data;
  };

  const form = useForm({
    mode: "uncontrolled",
    initialValues: {
      canvasId: "",
    },
    validate: {
      canvasId: (value) =>
        value.length == 8 ? null : "Canvas ID must be 8 characters",
    },
  });

  return (
    <>
      <Container size="md">
        <Text size="xl" fw={700}>
          Edit Course
        </Text>

        <Divider my="md" />

        <form onSubmit={form.onSubmit((values) => console.log(values))}>
          <TextInput
            pb={8}
            label="Course Name"
            defaultValue="Computer Science For STEM"
            placeholder="Computer Science For STEM"
            key={form.key("courseName")}
          />

          <TextInput
            pb={8}
            label="Course Code"
            defaultValue="CSCI128"
            placeholder="CSCI128"
            key={form.key("courseCode")}
          />

          <TextInput
            pb={8}
            label="Term"
            defaultValue="Fall 2024"
            placeholder="Fall 2024"
            key={form.key("courseTerm")}
          />

          <TextInput
            label="Canvas ID"
            placeholder="xxxxxxxx"
            key={form.key("canvasId")}
          />

          <p>External Services</p>
          <Chip.Group multiple>
            <Group>
              <Chip value="1">Gradescope</Chip>
              <Chip value="2">Runestone</Chip>
              <Chip value="3">PrairieLearn</Chip>
            </Group>
          </Chip.Group>

          <Group justify="flex-end" mt="md">
            {/* TODO maybe make this another button once external services are added */}
            <Button component={Link} to="/admin/home" type="submit">
              Save
            </Button>
          </Group>
        </form>
      </Container>
    </>
  );
}
