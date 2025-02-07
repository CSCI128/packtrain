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

export function EditCourse() {
  const form = useForm({
    mode: "uncontrolled",
    initialValues: {
      email: "",
      termsOfService: false,
    },

    validate: {
      email: (value) => (/^\S+@\S+$/.test(value) ? null : "Invalid email"),
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
            placeholder="Computer Science For STEM"
            key={form.key("courseName")}
          />

          <TextInput
            pb={8}
            label="Course Code"
            placeholder="CSCI128"
            key={form.key("courseCode")}
          />

          <TextInput
            pb={8}
            label="Term"
            placeholder="Fall 2024"
            key={form.key("courseTerm")}
          />

          <TextInput
            label="Canvas ID"
            placeholder="xxxx-xxxx-xxxx-xxxx"
            key={form.key("canvasId")}
          />

          <Group justify="flex-end" mt="md">
            <Button type="submit">Save</Button>
          </Group>
        </form>

        <Divider my="md" />

        <p>Add External Services</p>
        <Chip.Group multiple>
          <Group mt="md">
            <Chip value="1">Gradescope</Chip>
            <Chip value="2">Runestone</Chip>
            <Chip value="3">PrairieLearn</Chip>
          </Group>
        </Chip.Group>

        <Divider my="md" />
      </Container>
    </>
  );
}
