import {
  Button,
  Center,
  Container,
  Divider,
  Group,
  Modal,
  Stack,
  Stepper,
  Tabs,
  Text,
} from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { getApiClient } from "@repo/api/index";
import {
  Course,
  MasterMigration,
  MigrationWithScores,
} from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Link } from "react-router-dom";

export function MigrationsReviewPage() {
  const {
    data: courseData,
    error: courseError,
    isLoading: courseIsLoading,
  } = useQuery<Course | null>({
    queryKey: ["getCourse"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_course_information_instructor({
            course_id: store$.id.get() as string,
          })
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });

  const {
    data: migrationData,
    error: migrationError,
    isLoading: migrationIsLoading,
  } = useQuery<MasterMigration | null>({
    queryKey: ["getMasterMigrations"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_master_migrations({
            course_id: store$.id.get() as string,
            master_migration_id: store$.master_migration_id.get() as string,
          })
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });

  // rename to migrationData, rename above to mastermigrationdata
  const {
    data: migrationData2,
    error: migrationError2,
    isLoading: migrationIsLoading2,
  } = useQuery<MigrationWithScores[] | null>({
    queryKey: ["getMigrationsWithScores"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_master_migration_to_review({
            course_id: store$.id.get() as string,
            master_migration_id: store$.master_migration_id.get() as string,
          })
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });

  const [confirmOpened, { open: openConfirm, close: closeConfirm }] =
    useDisclosure(false);
  const [selectedAssignmentIds, setSelectedAssignmentIds] = useState<string[]>(
    []
  );

  if (
    migrationIsLoading ||
    !migrationData ||
    courseIsLoading ||
    !courseData ||
    !migrationData2 ||
    migrationIsLoading2
  )
    return "Loading...";

  if (migrationError || courseError || migrationError2)
    return `An error occured: ${migrationError}`;

  console.log(migrationData);

  return (
    <>
      <Modal
        opened={confirmOpened}
        onClose={closeConfirm}
        title="Are you sure?"
        centered
      >
        <Center>
          <Stack>
            <Text size="md">Are you sure you want to post grades?</Text>

            <Button color="gray" variant="light" onClick={closeConfirm}>
              Cancel
            </Button>

            <Button
              color="blue"
              component={Link}
              to="/instructor/migrate/post"
              variant="filled"
            >
              Post
            </Button>
          </Stack>
        </Center>
      </Modal>

      <Container size="md">
        <Stepper
          pl={50}
          pr={50}
          pb={30}
          active={2}
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
          Review
        </Text>

        <Divider my="sm" />

        {selectedAssignmentIds.length > 0 ? (
          <Tabs defaultValue={selectedAssignmentIds.at(0)}>
            <Tabs.List>
              {selectedAssignmentIds.map((assignment) => (
                <Tabs.Tab value={assignment}>
                  {
                    courseData?.assignments?.filter(
                      (a) => a.id === assignment
                    )[0]?.name
                  }
                </Tabs.Tab>
              ))}
            </Tabs.List>

            {selectedAssignmentIds.map((assignment) => (
              <Tabs.Panel value={assignment} p={20}>
                <Text>Hello</Text>
              </Tabs.Panel>
            ))}
          </Tabs>
        ) : (
          <Text>No migrations were found!</Text>
        )}

        <Group justify="space-between" mt="xl">
          <Button color="gray">Export</Button>

          <Group>
            <Button
              component={Link}
              to="/instructor/migrate/apply"
              color="gray"
            >
              Previous
            </Button>

            <Button onClick={openConfirm}>Post</Button>
          </Group>
        </Group>
      </Container>
    </>
  );
}
