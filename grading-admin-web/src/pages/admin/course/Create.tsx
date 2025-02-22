import {
  Button,
  Chip,
  Container,
  Divider,
  Group,
  Loader,
  Text,
  TextInput,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { $api, store$ } from "../../../api";

export function CreatePage() {
  const mutation = $api.useMutation("post", "/admin/courses");
  const mutation2 = $api.useMutation(
    "post",
    "/admin/courses/{course_id}/import"
  );
  const [taskId, setTaskId] = useState(-1);
  const [taskId2, setTaskId2] = useState(-1);
  const [taskId3, setTaskId3] = useState(-1);
  const [canvasId, setCanvasId] = useState("");
  const [courseId, setCourseId] = useState("");
  const POLLING_INTERVAL = 10000;
  const [allTasksCompleted, setAllTasksCompleted] = useState(false);
  const [courseCreated, setCourseCreated] = useState(false);
  const [importing, setImporting] = useState(false);
  const [membersImported, setMembersImported] = useState(0);
  const [assignmentsImported, setAssignmentsImported] = useState(0);
  const [sectionsImported, setSectionsImported] = useState(0);
  // TODO make struct

  // TODO add/link service mutation here

  // const {
  //   data: data,
  //   error: error,
  //   isLoading: isLoading,
  // } = $api.useQuery("get", "/user/credentials");

  // TODO check canvas credentials somehow, ensure credentials exist for user.
  // disable form if none found

  // const hasCanvasCredential = () => {
  //   for (var credential of data) {
  //     if (credential.service === "canvas") {
  //       return true;
  //     }
  //   }
  //   return false;
  // };

  const importCourse = () => {
    setImporting(true);
    mutation2.mutate(
      {
        params: {
          path: {
            course_id: courseId,
          },
        },
        body: {
          canvas_id: Number(canvasId),
          overwrite_name: false,
          overwrite_code: false,
          import_users: true,
          import_assignments: true,
        },
      },
      {
        onSuccess: (response) => {
          setTaskId(response[0].id);
          setTaskId2(response[1].id);
          setTaskId3(response[2].id);
        },
      }
    );
  };

  const {
    data: taskData,
    error: taskError,
    isLoading: taskIsLoading,
    refetch,
  } = $api.useQuery(
    "get",
    `/tasks/{task_id}`,
    {
      params: {
        path: { task_id: taskId },
      },
    },
    {
      enabled: taskId != -1,
    }
  );

  const {
    data: taskData2,
    error: taskError2,
    isLoading: taskIsLoading2,
    refetch: refetch2,
  } = $api.useQuery(
    "get",
    `/tasks/{task_id}`,
    {
      params: {
        path: { task_id: taskId2 },
      },
    },
    {
      enabled: taskId2 != -1,
    }
  );

  const {
    data: taskData3,
    error: taskError3,
    isLoading: taskIsLoading3,
    refetch: refetch3,
  } = $api.useQuery(
    "get",
    `/tasks/{task_id}`,
    {
      params: {
        path: { task_id: taskId3 },
      },
    },
    {
      enabled: taskId3 != -1,
    }
  );

  useEffect(() => {
    if (allTasksCompleted) return;

    let interval1: number, interval2: number, interval3: number;

    const checkAllCompleted = () => {
      const allCompleted =
        taskData?.status === "COMPLETED" &&
        taskData2?.status === "COMPLETED" &&
        taskData3?.status === "COMPLETED";
      if (allCompleted) {
        setAllTasksCompleted(true);
      }
    };

    if (taskData?.status !== "COMPLETED" && taskId != -1) {
      interval1 = setInterval(() => {
        refetch();
      }, POLLING_INTERVAL);
    }

    if (taskData2?.status !== "COMPLETED" && taskId2 != -1) {
      interval2 = setInterval(() => {
        refetch2();
      }, POLLING_INTERVAL);
    }

    if (taskData3?.status !== "COMPLETED" && taskId3 != -1) {
      interval3 = setInterval(() => {
        refetch3();
      }, POLLING_INTERVAL);
    }

    checkAllCompleted();

    return () => {
      if (interval1) clearInterval(interval1);
      if (interval2) clearInterval(interval2);
      if (interval3) clearInterval(interval3);
    };
  }, [
    taskData?.status,
    taskData2?.status,
    taskData3?.status,
    refetch,
    refetch2,
    refetch3,
  ]);

  const { data, error, isLoading } = $api.useQuery(
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
      setAssignmentsImported(data.assignments?.length || 0);
      setSectionsImported(data.sections?.length || 0);
      setMembersImported(data.members?.length || 0);
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

  return (
    <>
      <Container size="md">
        <Text size="xl" fw={700}>
          Create Course
        </Text>

        <Divider my="md" />

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

          <Text size="md">External Services</Text>

          <Chip.Group multiple>
            <Group>
              <Chip value="1">Gradescope</Chip>
              <Chip value="2">Runestone</Chip>
              <Chip value="3">PrairieLearn</Chip>
            </Group>
          </Chip.Group>

          {courseCreated ? (
            <></>
          ) : (
            <Group justify="flex-end" mt="md">
              <Button type="submit">Create</Button>
            </Group>
          )}
        </form>

        {courseCreated ? (
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
                <p>{assignmentsImported} assignments imported</p>
                <p>{membersImported} members imported</p>
                <p>{sectionsImported} sections imported</p>
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
        ) : (
          <></>
        )}
      </Container>
    </>
  );
}
