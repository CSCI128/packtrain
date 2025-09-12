import {
  Box,
  Button,
  Container,
  Divider,
  Group,
  Progress,
  Stepper,
  Text,
} from "@mantine/core";
import { getApiClient } from "@repo/api/index";
import { store$ } from "@repo/api/store";
import { useWebSocketClient } from "@repo/ui/WebSocketHooks";
import { useMutation } from "@tanstack/react-query";
import { User } from "oidc-client-ts";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { userManager } from "../../auth";

type MigrationPostNotificationDTO = {
  error?: string;
  post_complete?: boolean;
};

export function MigrationsPostPage() {
  const navigate = useNavigate();
  const [posting, setPosting] = useState<boolean>(false);
  const [postingFinished, setPostingFinished] = useState<boolean>(false);
  const [tasksFailed, setTasksFailed] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string>("");
  const [completed, setCompleted] = useState<boolean>(false);

  const postMasterMigration = useMutation({
    mutationKey: ["postMasterMigration"],
    mutationFn: async ({
      master_migration_id,
    }: {
      master_migration_id: string;
    }) => {
      const client = await getApiClient();
      const res = await client.post_master_migration({
        course_id: store$.id.get() as string,
        master_migration_id: master_migration_id,
      });
      return res.data;
    },
  });

  const finalizeMasterMigration = useMutation({
    mutationKey: ["finalizeMasterMigration"],
    mutationFn: async ({
      master_migration_id,
    }: {
      master_migration_id: string;
    }) => {
      const client = await getApiClient();
      const res = await client.finalize_master_migration({
        course_id: store$.id.get() as string,
        master_migration_id: master_migration_id,
      });
      return res.data;
    },
  });

  const [userData, setUserData] = useState<User>();

  useEffect(() => {
    const fetchUser = async () => {
      try {
        const u = await userManager.getUser();
        if (!u) throw new Error("Failed to get user");
        setUserData(u);
      } catch (err) {
        console.error(err);
      }
    };
    fetchUser();
  }, []);

  useWebSocketClient({
    authToken: userData?.access_token as string,
    onConnect: (client) => {
      client.subscribe("/migrations/post", (msg) => {
        const payload: MigrationPostNotificationDTO = JSON.parse(msg.body);

        console.log("DEBUG: received payload:", payload);

        if (payload.error) {
          setErrorMessage(payload.error);
          setTasksFailed(true);
        }

        if (payload.post_complete) {
          setCompleted(true);
        }
      });
    },
  });

  useEffect(() => {
    if (completed) {
      setPostingFinished(true);
      setPosting(false);
      finalizeMasterMigration.mutate({
        master_migration_id: store$.master_migration_id.get() as string,
      });
    }
  }, [completed, finalizeMasterMigration]);

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
        </Stepper>

        <Text size="xl" fw={700}>
          Post Grades
        </Text>

        <Divider my="sm" />

        {posting && (
          <>
            <Text size="lg" fw={700}>
              Posting grades..
            </Text>
            <Progress my="md" radius="xl" size="lg" value={50} animated />
          </>
        )}

        {tasksFailed && (
          <>
            <Text size="md" ta="center" c="red.9" fw={700}>
              Migration Post Failed
            </Text>
            <Text ta="center" c="gray.7">
              {errorMessage}
            </Text>
          </>
        )}

        {postingFinished ? (
          <>
            <Box mt={20}>
              <Text size="lg" ta="center" fw={500}>
                Done posting!
              </Text>

              <Text size="md" c="gray.6" ta="center">
                Reminder: You must post/publish the grades in Canvas for
                students to see them!
              </Text>
            </Box>
            <Group justify="flex-end" mt="md">
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
          </>
        ) : (
          <Button
            color="blue"
            onClick={() => {
              postMasterMigration.mutate(
                {
                  master_migration_id:
                    store$.master_migration_id.get() as string,
                },
                {
                  onSuccess: () => {
                    setPosting(true);
                  },
                }
              );
            }}
            variant="filled"
          >
            Post
          </Button>
        )}
      </Container>
    </>
  );
}
