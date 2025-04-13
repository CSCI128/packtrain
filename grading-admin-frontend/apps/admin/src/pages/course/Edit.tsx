import {
  Button,
  Checkbox,
  Container,
  Divider,
  Fieldset,
  Group,
  NumberInput,
  Space,
  TagsInput,
  Text,
  TextInput,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { getApiClient } from "@repo/api/index";
import { Course, CourseSyncTask } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

export function EditCourse() {
  const navigate = useNavigate();
  const { data, error, isLoading } = useQuery<Course>({
    queryKey: ["getCourse"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_course({
            course_id: store$.id.get() as string,
            include: ["members", "assignments", "sections"],
          })
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });

  const mutation = useMutation({
    mutationKey: ["updateCourse"],
    mutationFn: ({ body }: { body: Course }) =>
      getApiClient()
        .then((client) =>
          client.update_course(
            {
              course_id: store$.id.get() as string,
            },
            body
          )
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });

  const updateCourse = (values: typeof form.values) => {
    mutation.mutate(
      {
        body: {
          name: values.courseName as string,
          code: values.courseCode as string,
          term: values.courseTerm as string,
          enabled: true,
          canvas_id: Number(values.canvasId),
          gradescope_id: Number(values.gradescopeId),
          late_request_config: {
            late_passes_enabled: values.latePassesEnabled,
            late_pass_name: values.latePassName,
            enabled_extension_reasons: values.enabledExtensionReasons,
            total_late_passes_allowed: values.totalLatePassesAllowed,
          },
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
      gradescopeId: "",
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
      gradescopeId: (value) =>
        value.length == 6 ? null : "Gradescope ID must be 6 characters",
      totalLatePassesAllowed: (value) =>
        value < 0
          ? "Total number of late passes must be greater than or equal to 0"
          : null,
    },
  });

  const syncAssignmentsMutation = useMutation({
    mutationKey: ["syncAssignments"],
    mutationFn: ({ body }: { body: CourseSyncTask }) =>
      getApiClient()
        .then((client) =>
          client.sync_course(
            {
              course_id: store$.id.get() as string,
            },
            body
          )
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });

  const syncAssignments = () => {
    syncAssignmentsMutation.mutate(
      {
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
        gradescopeId: String(data.gradescope_id),
        latePassesEnabled: data.late_request_config.late_passes_enabled,
        totalLatePassesAllowed:
          data.late_request_config.total_late_passes_allowed,
        latePassName: data.late_request_config.late_pass_name,
        enabledExtensionReasons:
          data.late_request_config.enabled_extension_reasons,
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

          <Space h="md" />

          <Fieldset legend="Course Late Request Config">
            <Checkbox
              pb={8}
              label="Enable Late Passes"
              key={form.key("latePassesEnabled")}
              {...form.getInputProps("latePassesEnabled", { type: "checkbox" })}
            />

            <NumberInput
              pb={8}
              label="Total Late Passes Allowed"
              key={form.key("totalLatePassesAllowed")}
              min={0}
              {...form.getInputProps("totalLatePassesAllowed")}
            />

            <TagsInput
              pb={8}
              label="Allowed Extension Reasons"
              placeholder="Enter tag.."
              key={form.key("enabledExtensionReasons")}
              {...form.getInputProps("enabledExtensionReasons")}
              data={[
                "Bereavement",
                "Excused Absence",
                "Family",
                "Illness",
                "Personal",
                "Other",
              ]}
            />

            <TextInput
              pb={8}
              label="Late Pass Name"
              placeholder="Late Pass"
              key={form.key("latePassName")}
              {...form.getInputProps("latePassName")}
            />
          </Fieldset>

          <Space h="md" />

          <Fieldset legend="External Course Config">
            <TextInput
              pb={8}
              label="Gradescope Course ID"
              placeholder="xxxxxxxx"
              key={form.key("gradescopeId")}
              {...form.getInputProps("gradescopeId")}
            />
          </Fieldset>

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
