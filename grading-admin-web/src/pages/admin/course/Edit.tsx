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
import { $api, store$ } from "../../../api";

export function EditCourse() {
  const { data, error, isLoading } = $api.useQuery(
    "get",
    "/admin/courses/{course_id}",
    {
      params: {
        path: { course_id: store$.id.get() as string },
      },
    }
  );

  const mutation = $api.useMutation("put", "/admin/courses/{course_id}");

  // TODO add/link service mutation here

  const updateCourse = (values: typeof form.values) => {
    mutation.mutate({
      params: {
        path: {
          course_id: "1",
        },
      },
      body: {
        name: values.courseName,
        code: values.courseCode,
        term: values.courseTerm,
        enabled: true,
        canvas_id: Number(values.canvasId), // TODO they shouldn't really be editing canvas_id here
      },
    });
  };

  const form = useForm({
    mode: "uncontrolled",
    initialValues: {
      courseName: data?.name,
      courseCode: data?.code,
      courseTerm: data?.term,
      canvasId: data?.canvas_id,
    },
    validate: {
      // canvasId: (value) =>
      //   value.length == 8 ? null : "Canvas ID must be 8 characters",
      courseName: (value) =>
        value && value.length < 1
          ? "Course name must have at least 1 character"
          : null,
      courseCode: (value) =>
        value && value.length < 1
          ? "Course code must have at least 1 character"
          : null,
      courseTerm: (value) =>
        value && value.length < 1
          ? "Course term must have at least 1 character"
          : null,
    },
  });

  const syncAssignmentsMutation = $api.useMutation(
    "post",
    "/admin/courses/{course_id}/sync"
  );

  const syncAssignments = () => {
    syncAssignmentsMutation.mutate(
      {
        params: {
          path: {
            course_id: "1",
          },
        },
        body: {
          canvas_id: 1,
          overwrite_name: false,
          overwrite_code: false,
          import_users: true,
          import_assignments: true,
        },
      },
      {
        onSuccess: (response) => {
          console.log(response);
        },
      }
    );
  };

  if (isLoading || !data) return "Loading...";

  if (error) return `An error occured: ${error}`;

  return (
    <>
      <Container size="md">
        <Text size="xl" fw={700}>
          Edit Course
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

          {/* TODO dont allow this, this shouldnt really happen or should resync? */}
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
            <Button onClick={syncAssignments} variant="filled">
              Sync Assignments
            </Button>

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
