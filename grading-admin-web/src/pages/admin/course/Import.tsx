import {
  Button,
  Container,
  Divider,
  Group,
  Loader,
  Text,
  TextInput,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { $api } from "../../../api";

// TODO migrate this all to createpage.

export function ImportPage() {
  const [importing, setImporting] = useState(true);

  const location = useLocation();

  const form = useForm({
    mode: "uncontrolled",
    initialValues: {
      courseId: "",
    },
    validate: {
      courseId: (value) =>
        /^[0-9]+$/.test(value) ? null : "Invalid Canvas course ID",
    },
  });

  const [taskId, setTaskId] = useState(-1);
  const [taskId2, setTaskId2] = useState(-1);
  const [taskId3, setTaskId3] = useState(-1);

  const {
    data: data,
    error: error,
    isLoading: isLoading,
  } = $api.useQuery("get", "/user/credentials");

  const mutation = $api.useMutation(
    "post",
    "/admin/courses/{course_id}/import"
  );

  const hasCanvasCredential = () => {
    for (let credential of data) {
      if (credential.service === "canvas") {
        return true;
      }
    }
    return false;
  };

  const importCourse = (values: typeof form.values) => {
    mutation.mutate(
      {
        params: {
          path: {
            course_id: location.state.course_id,
          },
        },
        body: {
          canvas_id: Number(location.state.canvas_id),
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
          setImporting(false);
        },
      }
    );
  };

  const POLLING_INTERVAL = 10000;
  const [allTasksCompleted, setAllTasksCompleted] = useState(false);

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

        // QUERY course by ID again and display temp importStats
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

  if (isLoading || !data) return "Loading...";

  if (error) return `An error occured: ${error}`;

  return (
    <>
      <Container size="xs">
        <Text size="xl" fw={700}>
          Link Canvas Course
        </Text>
        <Divider my="sm" />
        {/* 
        <Text size="md">Using canvas id: {location.state.canvas_id}</Text>

        <Text size="md">Using course id: {location.state.course_id}</Text> */}

        {allTasksCompleted ? <></> : <Loader color="blue" />}
        <p>{allTasksCompleted ? "COMPLETE" : "RUNNING TASKS"}</p>
        <form onSubmit={form.onSubmit(importCourse)}>
          {/* dont think this is working with hidden stuff */}
          {/* {hasCanvasCredential() ? (
            <>
              <Text mt={15} mb={15} c="dimmed" size="sm">
                Using existing Canvas credential..
              </Text>
            </>
          ) : (
            <>
              <TextInput
                label="Canvas API Key"
                placeholder="xxxxxxxx"
                key={form.key("canvasAPIKey")}
                {...form.getInputProps("canvasAPIKey")}
              />
            </>
          )} */}
          {/* TODO do we even want to handle this case? */}

          {/* TODO rethink how this is shown and if this is editable */}
          <TextInput
            label="Canvas Course ID"
            placeholder="xxxxxxxx"
            key={form.key("courseId")}
            {...form.getInputProps("courseId")}
          />

          {/* TODO hide once import is done */}
          <Group justify="flex-end" mt="md">
            <Button type="submit">Import</Button>
          </Group>
        </form>
        {/* TODO gray this out if import isn't done */}
        {!importing ? (
          <Group justify="flex-end" mt="md">
            <Button
              color={importing ? "blue" : "gray"}
              component={Link}
              to="/admin/course"
            >
              Continue
            </Button>
          </Group>
        ) : (
          <></>
        )}
      </Container>
    </>
  );
}
