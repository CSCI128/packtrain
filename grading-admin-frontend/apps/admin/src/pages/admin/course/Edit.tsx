import {
  Button, Checkbox,
  Chip,
  Container,
  Divider, Fieldset,
  Group,
  InputWrapper, MultiSelect, NumberInput, Space,
  Text,
  TextInput,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { $api, store$ } from "../../../api";

export function EditCourse() {
  const navigate = useNavigate();
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
    mutation.mutate(
      {
        params: {
          path: {
            course_id: store$.id.get() as string,
          },
        },
        body: {
          name: values.courseName as string,
          code: values.courseCode as string,
          term: values.courseTerm as string,
          enabled: true,
          canvas_id: Number(values.canvasId),
          late_request_config: {
            late_passes_enabled: values.latePassesEnabled,
            late_pass_name: values.latePassName,
            enabled_extension_reasons: values.enabledExtensionReasons,
            total_late_passes_allowed: values.totalLatePassesAllowed,
          }
        },
      },
      {
        onSuccess: () => {
          navigate("/admin/home");
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
      canvasId: 0,
      latePassesEnabled: false,
      totalLatePassesAllowed: 0,
      enabledExtensionReasons: [] as string[],
      latePassName: "",
    },
    validate: {
      courseName: (value) =>
        value.length < 1 ? "Course name must have at least 1 character" : null,
      courseCode: (value) =>
        value.length < 1 ? "Course code must have at least 1 character" : null,
      courseTerm: (value) =>
        value.length < 1 ? "Course term must have at least 1 character" : null,
      totalLatePassesAllowed: (value) =>
        value < 0 ? "Total number of late passes must be greater than or equal to 0" : null,
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

  useEffect(() => {
    if (data) {
      form.setValues({
        courseName: data.name,
        courseCode: data.code,
        courseTerm: data.term,
        canvasId: data.canvas_id,
        latePassesEnabled: data.late_request_config.late_passes_enabled,
        totalLatePassesAllowed: data.late_request_config.total_late_passes_allowed,
        latePassName: data.late_request_config.late_pass_name,
        enabledExtensionReasons: data.late_request_config.enabled_extension_reasons,
      });
    }
  }, [data]);

  if (isLoading || !data) return "Loading...";

  if (error) return `An error occurred: ${error}`;

  return (
    <>
      <Container size="md">
        <Text size="xl" fw={700}>
          Edit Course
        </Text>

        <Divider my="md" />

        <form onSubmit={form.onSubmit(updateCourse)}>

          <Fieldset legend="Course Information">
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
              pb={8}
              disabled
              label="Canvas ID"
              placeholder="xxxxxxxx"
              key={form.key("canvasId")}
              {...form.getInputProps("canvasId")}
            />
          </Fieldset>

          <Space h="md"/>

          <Fieldset legend="Course Late Request Config">
            <Checkbox
              pb={8}
              label="Enable Late Passes"
              key = {form.key("latePassesEnabled")}
              {...form.getInputProps("latePassesEnabled", {type: "checkbox"})}
            />

            <NumberInput
              pb={8}
              label="Total Late Passes Allowed"
              key={form.key("totalLatePassesAllowed")}
              min={0}
              {...form.getInputProps("totalLatePassesAllowed")}
            />

            <MultiSelect
              pb={8}
              label="Allowed Extension Reasons"
              key={form.key("enabledExtensionReasons")}
              {...form.getInputProps("enabledExtensionReasons")}
              data={['Bereavement', 'Excused Absence', 'Family', 'Illness', 'Personal', 'Other']}
            />

            <TextInput
              pb={8}
              label="Late Pass Name"
              placeholder="Late Pass"
              key={form.key("latePassName")}
              {...form.getInputProps("latePassName")}
            />
          </Fieldset>

          <InputWrapper label="External Services">
            <Chip.Group multiple>
              <Group>
                <Chip value="1">Gradescope</Chip>
                <Chip value="2">Runestone</Chip>
                <Chip value="3">PrairieLearn</Chip>
              </Group>
            </Chip.Group>
          </InputWrapper>

          <Group justify="flex-end" mt="md">
            <Button onClick={syncAssignments} variant="filled">
              Sync Assignments
            </Button>

            <Button type="submit">Save</Button>
          </Group>
        </form>
      </Container>
    </>
  );
}
