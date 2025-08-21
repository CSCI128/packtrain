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
import { Course, Task } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { useMutation, useQuery } from "@tanstack/react-query";
import { EventSourcePolyfill } from "event-source-polyfill";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { userManager } from "../../auth";
import { useGetCredentials } from "../../hooks";

const baseURL: string =
  window.__ENV__?.VITE_API_URL || "https://localhost.dev/api";

export function CreatePage() {
  const { data: credentialData, error: credentialError } = useGetCredentials();
  const [userHasCredential, setUserHasCredential] = useState<boolean>(false);
  const [outstandingTasks, setOutstandingTasks] = useState<Task[]>([]);
  const [allTasksCompleted, setAllTasksCompleted] = useState(false);
  const [courseCreated, setCourseCreated] = useState(false);
  const [importStatistics, setImportStatistics] = useState({
    members: 0,
    assignments: 0,
    sections: 0,
  });
  const [tasksFailed, setTasksFailed] = useState<boolean>(false);
  // const [courseDeleted, setCourseDeleted] = useState<boolean>(false);
  const [courseId, setCourseId] = useState<string>();

  const [canvasId, setCanvasId] = useState<string>();

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

  const deleteCourseMutation = useMutation({
    mutationKey: ["deleteCourse"],
    mutationFn: ({ course_id }: { course_id: string }) =>
      getApiClient()
        .then((client) => client.delete_course(course_id))
        .then((res) => res.data)
        .catch((err) => console.log(err)),
  });

  // TODO events should not be needed at all, this is for display only
  const [events, setEvents] = useState([]);
  const [userData, setUserData] = useState(null);

  // TODO see if there's a better way to do this later or if we can make a default bearer token exported
  useEffect(() => {
    const fetchData = async () => {
      try {
        const u = await userManager.getUser();
        if (!u) throw new Error("Failed to get user");
        setUserData(u);
      } catch (err) {
        console.error(err);
      }
    };
    fetchData();
  }, []);

  useEffect(() => {
    if (!userData || !courseId || !courseCreated) return;

    const url = `${baseURL}/owner/courses/${courseId}/import?canvas_id=${canvasId}`;
    const eventSource = new EventSourcePolyfill(url, {
      headers: {
        Authorization: `Bearer ${userData.access_token}`,
      },
    });

    let manuallyClosed = false;

    eventSource.addEventListener("ping", (event: { data: any }) => {
      setEvents((prev) => [...prev, `ping: ${event.data}`]);
      console.log("PING:", event.data);
    });

    eventSource.addEventListener("status", (event: { data: any }) => {
      setEvents((prev) => [...prev, `status: ${event.data}`]);
      console.log("STATUS UPDATE:", event.data);
    });

    eventSource.addEventListener("result", (event: { data: any }) => {
      setEvents((prev) => [...prev, `result: ${event.data}`]);
      console.log("RECEIVED EVENT RESULT:", event.data);
      setOutstandingTasks(event.data);
    });

    eventSource.addEventListener("end", (e) => {
      manuallyClosed = true;
      setAllTasksCompleted(true);
      console.log("Server signaled end of stream");
      eventSource.close();
    });

    eventSource.onerror = (err: any) => {
      if (manuallyClosed) return; // ignore error after close
      setEvents((prev) => [...prev, "EventSource failed or closed"]);
      console.error("SSE error:", err);
      setTasksFailed(true);
      console.log(`Tasks failed, deleting course by ID: ${courseId}`);
      deleteCourse(courseId);
      // setCourseDeleted(true);
      eventSource.close();
    };

    return () => eventSource.close();
  }, [userData, courseCreated, canvasId]);

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

  // useEffect(() => {
  //   if (outstandingTasks.length === 0) return;

  //   const pollTasks = async () => {
  //     Promise.all(
  //       outstandingTasks.map((task) => pollTaskUntilComplete(task.id))
  //     )
  //       .then(() => {
  //         console.log("All tasks are completed!");
  //         setAllTasksCompleted(true);
  //         setOutstandingTasks([]);
  //       })
  //       .catch((error) => {
  //         console.error("Some tasks failed:", error);
  //       });
  //   };

  //   pollTasks();
  // }, [outstandingTasks, pollTaskUntilComplete]);

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
  }, [allTasksCompleted, data]);

  //   if (tasksFailed && store$.id.get() && !courseDeleted) {
  //     console.log(
  //       `Tasks failed, deleting course by ID: ${store$.id.get() as string}`
  //     );
  //     deleteCourse(store$.id.get() as string);
  //     setCourseDeleted(true);
  //   }
  // }, [allTasksCompleted, data, tasksFailed, courseDeleted]);

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
          // TODO see if all of this state is necessary
          setCourseCreated(true);
          store$.id.set(response.id as string);
          store$.name.set(response.name);
          setCourseId(response.id);
          setCanvasId(values.canvasId);
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
            <div>
              <h2>SSE Test</h2>
              <ul>
                {events.map((e, i) => (
                  <li key={i}>{e}</li>
                ))}
              </ul>
            </div>

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
