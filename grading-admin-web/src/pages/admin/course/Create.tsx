import {
  Button,
  Chip,
  Container,
  Divider,
  Group,
  InputWrapper,
  Loader,
  Text,
  TextInput,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { $api, store$ } from "../../../api";
import { components } from "../../../lib/api/v1";

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
  const [importing, setImporting] = useState(false);
  const [importStatistics, setImportStatistics] = useState({
    members: 0,
    assignments: 0,
    sections: 0,
  });

  // TODO add/link service mutation here

  const { data: credentialData, error: credentialError } = $api.useQuery(
    "get",
    "/user/credentials"
  );

  const importCourse = () => {
    setImporting(true);
    importMutation.mutate(
      {
        params: {
          path: {
            course_id: courseId,
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
            await new Promise((res) => setTimeout(res, delay)); // Wait before retrying
          }
        } catch (error) {
          console.error(`Error fetching task ${taskId}:`, error);
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
          setImporting(false);
          setOutstandingTasks([]);
        })
        .catch((error) => {
          console.error("Some tasks failed:", error);
        });
    };

    pollTasks();
  }, [outstandingTasks, pollTaskUntilComplete, importing]);

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
      setImporting(false);
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
        },
      },
      {
        onSuccess: (response) => {
          setCanvasId(values.canvasId);
          setCourseId(response.id as string);
          setCourseCreated(true);
          setAllTasksCompleted(false);
          store$.id.set(response.id as string);
          store$.name.set(response.name);
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

  useEffect(() => {
    if (credentialData) {
      for (let credential of credentialData) {
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

              <InputWrapper label="External Services">
                <Chip.Group multiple>
                  <Group>
                    <Chip value="1">Gradescope</Chip>
                    <Chip value="2">Runestone</Chip>
                    <Chip value="3">PrairieLearn</Chip>
                  </Group>
                </Chip.Group>
              </InputWrapper>

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
                    {importing ? (
                      <>
                        <p>Importing, this may take a moment..</p>{" "}
                        <Loader color="blue" />
                      </>
                    ) : (
                      <Group justify="flex-end" mt="md">
                        <Button onClick={importCourse}>Import</Button>
                      </Group>
                    )}
                  </>
                ) : (
                  <>
                    <p>Import complete!</p>
                    <p>{importStatistics.assignments} assignments imported</p>
                    <p>{importStatistics.members} members imported</p>
                    <p>{importStatistics.sections} sections imported</p>
                  </>
                )}

                {/* TODO disable this if import isn't done */}
                <Group justify="flex-end" mt="md">
                  <Button
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
