import {
  Button,
  Container,
  Divider,
  Group,
  Progress,
  Stepper,
  Text,
} from "@mantine/core";
import { getApiClient } from "@repo/api/index";
import { Task } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { useMutation } from "@tanstack/react-query";
import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

export function MigrationsPostPage() {
  const navigate = useNavigate();
  const [posting, setPosting] = useState(false);
  const [postingFinished, setPostingFinished] = useState(false);
  const [outstandingTasks, setOutstandingTasks] = useState<Task[]>([]);

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

  const pollTaskUntilComplete = useCallback(
    async (taskId: number, delay = 5000) => {
      let tries = 0;
      while (true) {
        try {
          if (tries > 20) {
            throw new Error("Maximum attempts (20) at polling exceeded..");
          }

          const response = await fetchTask({ task_id: taskId });

          if (response?.status === "COMPLETED") {
            console.log(`Task ${taskId} is completed!`);
            return response;
          } else if (response?.status === "FAILED") {
            console.log(`Task ${taskId} failed`);
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
        .then((results) => {
          console.log("All tasks are completed:", results);
          setOutstandingTasks([]);
          setPostingFinished(true);
          setPosting(false);
          finalizeMasterMigration.mutate({
            master_migration_id: store$.master_migration_id.get() as string,
          });
        })
        .catch((error) => {
          console.error("Some tasks failed:", error);
        });
    };

    pollTasks();
  }, [outstandingTasks, pollTaskUntilComplete]);

  const postMasterMigration = useMutation({
    mutationKey: ["postMasterMigration"],
    mutationFn: ({ master_migration_id }: { master_migration_id: string }) =>
      getApiClient()
        .then((client) =>
          client.post_master_migration({
            course_id: store$.id.get() as string,
            master_migration_id: master_migration_id,
          })
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return [];
        }),
  });

  const finalizeMasterMigration = useMutation({
    mutationKey: ["finalizeMasterMigration"],
    mutationFn: ({ master_migration_id }: { master_migration_id: string }) =>
      getApiClient()
        .then((client) =>
          client.finalize_master_migration({
            course_id: store$.id.get() as string,
            master_migration_id: master_migration_id,
          })
        )
        .then((res) => res.data)
        .catch((err) => console.log(err)),
  });

  const handlePostMasterMigration = () => {
    console.log("HERE");
    postMasterMigration.mutate(
      {
        master_migration_id: store$.master_migration_id.get() as string,
      },
      {
        onSuccess: (response) => {
          console.log("RESP:", response);
          setPosting(true);
          setOutstandingTasks(response);
        },
      }
    );
  };

  useEffect(() => {
    handlePostMasterMigration();
  }, [posting]);

  return (
    <>
      <Container size="md">
        <Stepper
          pl={50}
          pr={50}
          pb={30}
          active={3}
          allowNextStepsSelect={false}
        >
          <Stepper.Step label="Load"></Stepper.Step>
          <Stepper.Step label="Apply"></Stepper.Step>
          <Stepper.Step label="Review"></Stepper.Step>
          <Stepper.Step label="Post"></Stepper.Step>
          <Stepper.Completed>
            Completed, click back button to get to previous step
          </Stepper.Completed>
        </Stepper>

        <Text size="xl" fw={700}>
          Post Grades
        </Text>

        <Divider my="sm" />

        {posting ? (
          <>
            <Text size="lg" fw={700}>
              Posting grades..
            </Text>
            <Progress my="md" radius="xl" size="lg" value={50} animated />
          </>
        ) : (
          <Text size="lg" fw={700}>
            Done posting!
          </Text>
        )}

        <Text size="md" c="gray.6" ta="center">
          Reminder: You must post/publish the grades in Canvas for students to
          see them!
        </Text>

        {postingFinished && (
          <Group justify="flex-end" mt="md">
            <Button
              color="blue"
              onClick={() => {
                store$.master_migration_id.delete();
                navigate("/instructor/migrate/load");
              }}
            >
              Again
            </Button>

            <Button
              color="blue"
              onClick={() => {
                store$.master_migration_id.delete();
                navigate("/instructor/migrate");
              }}
              variant="filled"
            >
              Done
            </Button>
          </Group>
        )}
      </Container>
    </>
  );
}
