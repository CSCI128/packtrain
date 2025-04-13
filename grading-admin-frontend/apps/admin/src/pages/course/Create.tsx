import {
  Button,
  Checkbox,
  Container,
  Divider,
  Fieldset,
  Group,
  Loader,
  NumberInput,
  Space,
  TagsInput,
  Text,
  TextInput,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { store$ } from "@repo/api/store";
import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { $api } from "../../api";
import { components } from "../../lib/api/v1";

export function CreatePage() {
  const mutation = $api.useMutation("post", "/admin/courses");
  const importMutation = $api.useMutation(
    "post",
    "/admin/courses/{course_id}/import"
  );
  const [userHasCredential, setUserHasCredential] = useState<boolean>(false);
  const [outstandingTasks, setOutstandingTasks] = useState<
    components["schemas"]["Task"][]
  >([]);
  const [canvasId, setCanvasId] = useState("");
  const [courseId, setCourseId] = useState("");
  const [allTasksCompleted, setAllTasksCompleted] = useState(false);
  const [courseCreated, setCourseCreated] = useState(false);
  const [importStatistics, setImportStatistics] = useState({
    members: 0,
    assignments: 0,
    sections: 0,
  });

  const { data: credentialData, error: credentialError } = $api.useQuery(
    "get",
    "/user/credentials"
  );

  const importCourse = () => {
    importMutation.mutate(
      {
        params: {
          path: {
            course_id: store$.id.get() as string,
          },
        },
        body: {
          canvas_id: Number(canvasId),
          overwrite_name: false,
          overwrite_code: true,
          import_users: true,
          import_assignments: true,
        },
      },
      {
        onSuccess: (response) => {
          setOutstandingTasks(response);
        },
      }
    );
  };

  const { mutateAsync: fetchTask } = $api.useMutation(
    "get",
    "/tasks/{task_id}"
  );

  const pollTaskUntilComplete = useCallback(
    async (taskId: number, delay = 5000) => {
      while (true) {
        try {
          const response = await fetchTask({
            params: { path: { task_id: taskId } },
          });

          if (response.status === "COMPLETED") {
            console.log(`Task ${taskId} is completed!`);
            return response;
          } else if (response.status === "FAILED") {
            console.log(`Task ${taskId} failed`);
            throw new Error("ERR");
          } else {
            console.log(
              `Task ${taskId} is still in progress, retrying in ${delay}ms...`
            );
            await new Promise((res) => setTimeout(res, delay));
          }
        } catch (error) {
          console.error(`Error fetching task ${taskId}:`, error);
          return;
        }
      }
    },
    []
  );

  useEffect(() => {
    if (outstandingTasks.length === 0) return;

    const pollTasks = async () => {
      Promise.all(
        outstandingTasks.map((task) => pollTaskUntilComplete(task.id))
      )
        .then((results) => {
          console.log("All tasks are completed:", results);
          setAllTasksCompleted(true);
          setOutstandingTasks([]);
        })
        .catch((error) => {
          console.error("Some tasks failed:", error);
        });
    };

    pollTasks();
  }, [outstandingTasks, pollTaskUntilComplete]);

  const { data, error } = $api.useQuery(
    "get",
    "/admin/courses/{course_id}",
    {
      params: {
        path: { course_id: courseId as string },
        query: {
          include: ["members", "assignments", "sections"],
        },
      },
    },
    {
      enabled: courseCreated && allTasksCompleted,
    }
  );

  useEffect(() => {
    if (courseCreated && allTasksCompleted && data) {
      setImportStatistics({
        members: data.members?.length || 0,
        sections: data.sections?.length || 0,
        assignments: data.assignments?.length || 0,
      });
    }
  }, [courseCreated, allTasksCompleted, data]);

  const createCourse = (values: typeof form.values) => {
    mutation.mutate(
      {
        body: {
          name: values.courseName,
          code: values.courseCode,
          term: values.courseTerm,
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
        onSuccess: (response) => {
          setCanvasId(values.canvasId);
          setCourseId(response.id as string);
          setCourseCreated(true);
          store$.id.set(response.id as string);
          store$.name.set(response.name);
        },
      }
    );
  };

  useEffect(() => {
    if (courseCreated) {
      importCourse();
    }
  }, [courseCreated]);

  const form = useForm({
    mode: "uncontrolled",
    initialValues: {
      courseName: "",
      courseCode: "",
      courseTerm: "",
      canvasId: "",
      gradescopeId: "",
      latePassesEnabled: false,
      totalLatePassesAllowed: 0,
      enabledExtensionReasons: [],
      latePassName: "",
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
      gradescopeId: (value) =>
        value.length == 6 ? null : "Gradescope ID must be 6 characters",
      totalLatePassesAllowed: (value) =>
        value < 0
          ? "Total number of late passes must be greater than or equal to 0"
          : null,
    },
  });

  useEffect(() => {
    if (credentialData) {
      for (const credential of credentialData) {
        if (credential.service === "canvas") {
          setUserHasCredential(true);
        }
      }
    }
  }, [credentialData]);

  if (error) {
    return <p>Error while querying created course!</p>;
  }

  if (credentialError) {
    return <p>Error while querying credentials!</p>;
  }

  return (
    <>
      <Container size="md">
        <Text size="xl" fw={700}>
          Create Course
        </Text>

        <Divider my="md" />

        {!userHasCredential ? (
          <>
            <Text fw={400}>You do not have any active Canvas credentials!</Text>
            <Text>Please add a credential under your profile to proceed.</Text>
          </>
        ) : (
          <>
            <form onSubmit={form.onSubmit(createCourse)}>
              <Fieldset legend="Course Information">
                <TextInput
                  disabled={courseCreated}
                  pb={8}
                  label="Course Name"
                  defaultValue="Computer Science For STEM"
                  placeholder="Computer Science For STEM"
                  key={form.key("courseName")}
                  {...form.getInputProps("courseName")}
                />

                <TextInput
                  disabled={courseCreated}
                  pb={8}
                  label="Course Code"
                  defaultValue="CSCI128"
                  placeholder="CSCI128"
                  key={form.key("courseCode")}
                  {...form.getInputProps("courseCode")}
                />

                <TextInput
                  disabled={courseCreated}
                  pb={8}
                  label="Term"
                  defaultValue="Fall 2024"
                  placeholder="Fall 2024"
                  key={form.key("courseTerm")}
                  {...form.getInputProps("courseTerm")}
                />

                <TextInput
                  disabled={courseCreated}
                  pb={8}
                  label="Canvas ID"
                  placeholder="xxxxxxxx"
                  key={form.key("canvasId")}
                  {...form.getInputProps("canvasId")}
                />
              </Fieldset>

              <Space h="md" />

              <Fieldset legend="Course Late Request Config">
                <Checkbox
                  disabled={courseCreated}
                  pb={8}
                  label="Enable Late Passes"
                  key={form.key("latePassesEnabled")}
                  {...form.getInputProps("latePassesEnabled", {
                    type: "checkbox",
                  })}
                />

                <NumberInput
                  disabled={courseCreated}
                  pb={8}
                  label="Total Late Passes Allowed"
                  key={form.key("totalLatePassesAllowed")}
                  min={0}
                  {...form.getInputProps("totalLatePassesAllowed")}
                />

                <TagsInput
                  disabled={courseCreated}
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
                  disabled={courseCreated}
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
                  disabled={courseCreated}
                  pb={8}
                  label="Gradescope Course ID"
                  placeholder="xxxxxxxx"
                  key={form.key("gradescopeId")}
                  {...form.getInputProps("gradescopeId")}
                />
              </Fieldset>

              {!courseCreated && (
                <Group justify="flex-end" mt="md">
                  <Button type="submit">Create</Button>
                </Group>
              )}
            </form>

            {courseCreated && (
              <>
                {!allTasksCompleted ? (
                  <>
                    <Text>Importing, this may take a moment..</Text>{" "}
                    <Loader color="blue" />
                  </>
                ) : (
                  <>
                    <Text>Import complete!</Text>
                    <Text>
                      <strong>{importStatistics.assignments}</strong>{" "}
                      assignments imported
                    </Text>
                    <Text>
                      <strong>{importStatistics.members}</strong> members
                      imported
                    </Text>
                    <Text>
                      <strong>{importStatistics.sections}</strong> sections
                      imported
                    </Text>
                  </>
                )}

                <Group justify="flex-end" mt="md">
                  <Button
                    disabled={!allTasksCompleted}
                    color={allTasksCompleted ? "blue" : "gray"}
                    component={Link}
                    to="/admin/home"
                  >
                    Continue
                  </Button>
                </Group>
              </>
            )}
          </>
        )}
      </Container>
    </>
  );
}
