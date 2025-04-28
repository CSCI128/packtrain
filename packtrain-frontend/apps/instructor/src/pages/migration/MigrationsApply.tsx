import {
  Box,
  Button,
  Center,
  Container,
  Divider,
  Group,
  Modal,
  Select,
  Stack,
  Stepper,
  Text,
} from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { getApiClient } from "@repo/api/index";
import { Course, MasterMigration, Migration, Policy } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { useQuery } from "@tanstack/react-query";
import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";

export function MigrationsApplyPage() {
  const [opened, { open, close }] = useDisclosure(false);
  const [selectedMigration, setSelectedMigration] = useState<Migration | null>(
    null
  );

  const {
    data: policyData,
    error: policyError,
    isLoading: policyIsLoading,
  } = useQuery<Policy[] | null>({
    queryKey: ["getCourse"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_all_policies({
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

  // /instructor/courses/{course_id}/migrations/{master_migration_id}/{migration_id}/policy:
  // post:
  //   tags:
  //     - instructor
  //   operationId: set_policy

  const handleSetPolicy = (row: Migration) => {
    setSelectedMigration(row);
    open();
  };

  useEffect(() => {
    if (migrationData) {
      let assignmentIds: string[] = [];
      migrationData.migrations?.forEach((migration: Migration) => {
        assignmentIds.push(migration.assignment.id as string);
      });
      setSelectedAssignmentIds(assignmentIds);
    }
  }, [migrationData]);

  const [selectedAssignmentIds, setSelectedAssignmentIds] = useState<string[]>(
    []
  );

  if (migrationIsLoading || !migrationData || courseIsLoading || !courseData)
    return "Loading...";

  if (migrationError || courseError)
    return `An error occured: ${migrationError}`;

  return (
    <>
      <Modal opened={opened} onClose={close} title="Select Policy" centered>
        <Center>
          <Stack>
            <Select
              label="Select a policy:"
              placeholder="Select policy.."
              data={
                policyData
                  ? policyData.flatMap((policy: Policy) => [
                      { label: policy.name ?? "", value: policy.id as string },
                    ])
                  : undefined
              }
            />
            <Group justify="flex-end">
              <Button color="gray" variant="light" onClick={close}>
                Cancel
              </Button>

              <Button color="blue" onClick={() => {}} variant="filled">
                Select policy
              </Button>
            </Group>
          </Stack>
        </Center>
      </Modal>

      <Container size="md">
        <Stepper
          pl={50}
          pr={50}
          pb={30}
          active={1}
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
          Apply Policies
        </Text>

        <Divider my="sm" />

        <Stack>
          {selectedAssignmentIds.map((selectedAssignment) => (
            <React.Fragment key={selectedAssignment}>
              <Box p={20} bg="gray.1">
                <Group justify="space-between">
                  <Group>
                    <Text fw={800}>
                      {
                        courseData.assignments?.filter(
                          (a) => a.id === selectedAssignment
                        )[0]?.name
                      }
                    </Text>
                  </Group>

                  <Group>
                    <Button color="blue">Select</Button>
                  </Group>
                </Group>
              </Box>
            </React.Fragment>
          ))}
        </Stack>

        <Text mt="md" fw={700}>
          Statistics
        </Text>
        <Text>
          <strong>0</strong> students
        </Text>
        <Text>
          <strong>0</strong> late submissions
        </Text>
        <Text>
          <strong>0</strong> extensions
        </Text>
        <Text>
          <strong>0</strong> unapproved extensions
        </Text>

        <Group justify="flex-end" mt="md">
          <Button
            component={Link}
            to="/instructor/migrate/load"
            variant="filled"
            color="gray"
          >
            Previous
          </Button>
          <Button
            component={Link}
            to="/instructor/migrate/review"
            variant="filled"
            color="blue"
          >
            Next
          </Button>
        </Group>
      </Container>
    </>
  );
}
