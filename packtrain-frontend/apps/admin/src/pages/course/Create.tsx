import {
  Anchor,
  Box,
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
import { getApiClient } from "@repo/api/index";
import { Course, CourseSyncTask, Credential, Task } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";

export function CreatePage() {
  const [userHasCredential, setUserHasCredential] = useState<boolean>(false);
  const [outstandingTasks, setOutstandingTasks] = useState<Task[]>([]);
  const [allTasksCompleted, setAllTasksCompleted] = useState(false);
  const [courseCreated, setCourseCreated] = useState(false);
  const [importStatistics, setImportStatistics] = useState({
    members: 0,
    assignments: 0,
    sections: 0,
  });
  const [tasksFailed, setTasksFailed] = useState(false);
  const [courseDeleted, setCourseDeleted] = useState(false);

  const mutation = useMutation({
    mutationKey: ["newCourse"],
    mutationFn: ({ body }: { body: Course }) =>
      getApiClient()
        .then((client) =>
          client.new_course(
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

  const importMutation = useMutation({
    mutationKey: ["importCourse"],
    mutationFn: ({ body }: { body: CourseSyncTask }) =>
      getApiClient()
        .then((client) =>
          client.import_course(
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

  const deleteCourseMutation = useMutation({
    mutationKey: ["deleteCourse"],
    mutationFn: ({ course_id }: { course_id: string }) =>
      getApiClient()
        .then((client) => client.delete_course(course_id))
        .then((res) => res.data)
        .catch((err) => console.log(err)),
  });

  const { data: credentialData, error: credentialError } = useQuery<
    Credential[]
  >({
    queryKey: ["getCredentials"],
    queryFn: () =>
      getApiClient()
        .then((client) => client.get_credentials())
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });

  const importCourse = (canvasId: string) => {
    importMutation.mutate(
      {
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

  const { mutateAsync: fetchTask } = useMutation({
    mutationKey: ["getTask"],
    mutationFn: ({ task_id }: { task_id: number }) =>
      getApiClient()
        .then((client) =>
          client.get_task({
            task_id: task_id,
          })
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });

  const { data, error } = useQuery<Course>({
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
    enabled: courseCreated && allTasksCompleted,
  });

  const pollTaskUntilComplete = useCallback(
    async (taskId: number, delay = 5000) => {
      let tries = 0;
      while (true) {
        try {
          if (tries > 20) {
            throw new Error("Maximum attempts (20) at polling exceeded..");
          }

          const response = await fetchTask({ task_id: taskId });

          if (response.status === "COMPLETED") {
            console.log(`Task ${taskId} is completed!`);
            return response;
          } else if (response.status === "FAILED") {
            console.log(`Task ${taskId} failed`);
            setTasksFailed(true);
            throw new Error("ERR");
          } else {
            console.log(
              `Task ${taskId} is still in progress, retrying in ${delay}ms...`
            );
            tries++;
            await new Promise((res) => setTimeout(res, delay));
          }
        } catch (error) {
          console.error(`Error fetching task ${taskId}:`, error);
          return;
        }
      }
    },
    [fetchTask]
  );

  useEffect(() => {
    if (outstandingTasks.length === 0) return;

    const pollTasks = async () => {
      Promise.all(
        outstandingTasks.map((task) => pollTaskUntilComplete(task.id))
      )
        .then(() => {
          console.log("All tasks are completed!");
          setAllTasksCompleted(true);
          setOutstandingTasks([]);
        })
        .catch((error) => {
          console.error("Some tasks failed:", error);
        });
    };

    pollTasks();
  }, [outstandingTasks, pollTaskUntilComplete]);

  const deleteCourse = (course_id: string) => {
    deleteCourseMutation.mutate(
      {
        course_id: course_id,
      },
      {
        onSuccess: () => {
          console.log("Delete course successful..");
          store$.id.delete();
          store$.name.delete();
        },
      }
    );
  };

  useEffect(() => {
    if (allTasksCompleted && data) {
      setImportStatistics({
        members: data.members?.length || 0,
        sections: data.sections?.length || 0,
        assignments: data.assignments?.length || 0,
      });
    }

    if (tasksFailed && store$.id.get() && !courseDeleted) {
      console.log(
        `Tasks failed, deleting course by ID: ${store$.id.get() as string}`
      );
      deleteCourse(store$.id.get() as string);
      setCourseDeleted(true);
    }
  }, [allTasksCompleted, data, tasksFailed, courseDeleted]);

  const createCourse = (values: typeof form.values) => {
    mutation.mutate(
      {
        body: {
          name: values.courseName,
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
          setCourseCreated(true);
          store$.id.set(response.id as string);
          store$.name.set(response.name);
          importCourse(values.canvasId);
        },
      }
    );
  };

  const form = useForm({
    mode: "uncontrolled",
    initialValues: {
      courseName: "",
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
      courseTerm: (value) =>
        value.length < 1 ? "Course term must have at least 1 character" : null,
      gradescopeId: (value) =>
        value && value.length !== 6
          ? "Gradescope ID must be 6 characters"
          : null,
      totalLatePassesAllowed: (value) =>
        value < 0 || value > 1000
          ? "Total number of late passes must be greater than or equal to 0 and less than 1000"
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
            <Text>
              Please add a credential under{" "}
              <Anchor href="/admin/profile"> your profile</Anchor> to proceed.
            </Text>
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
              <Box p={20}>
                {!allTasksCompleted ? (
                  <>
                    <Text>Importing, this may take a moment..</Text>{" "}
                    <Loader color="blue" />
                  </>
                ) : (
                  <>
                    {!tasksFailed ? (
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
                    ) : (
                      <>
                        <Text>Import Failed!</Text>
                        <Text>Deleting attempted import!</Text>
                      </>
                    )}
                  </>
                )}

                <Group justify="flex-end" mt="md">
                  {!tasksFailed ? (
                    <Button
                      disabled={!allTasksCompleted}
                      color={allTasksCompleted ? "blue" : "gray"}
                      component={Link}
                      to="/admin"
                    >
                      Continue
                    </Button>
                  ) : (
                    <Button
                      disabled={!allTasksCompleted}
                      color={allTasksCompleted ? "blue" : "gray"}
                      component={Link}
                      to="/select"
                    >
                      Return
                    </Button>
                  )}
                </Group>
              </Box>
            )}
          </>
        )}
      </Container>
    </>
  );
}
