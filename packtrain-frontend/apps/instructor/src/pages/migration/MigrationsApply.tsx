import {
  Box,
  Button,
  Container,
  Divider,
  Group,
  Select,
  Stack,
  Stepper,
  Text,
} from "@mantine/core";
import { getApiClient } from "@repo/api/index";
import { Migration, Policy, Task } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { Loading } from "@repo/ui/Loading";
import { useMutation } from "@tanstack/react-query";
import React, { useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  useGetCourseInstructor,
  useGetMasterMigration,
  useGetPolicies,
} from "../../hooks";

export function MigrationsApplyPage() {
  const navigate = useNavigate();
  const [selectedPolicies, setSelectedPolicies] = useState<
    Record<string, string>
  >({});
  const [selectedAssignmentIds, setSelectedAssignmentIds] = useState<string[]>(
    []
  );
  const [posting, setPosting] = useState(true);
  const [outstandingTasks, setOutstandingTasks] = useState<Task[]>([]);
  const [error, setError] = useState<string>();

  const {
    data: policyData,
    error: policyError,
    isLoading: policyIsLoading,
  } = useGetPolicies();

  const {
    data: courseData,
    error: courseError,
    isLoading: courseIsLoading,
  } = useGetCourseInstructor();

  const {
    data: migrationData,
    error: migrationError,
    isLoading: migrationIsLoading,
    refetch,
  } = useGetMasterMigration();

  const setPolicyMigration = useMutation({
    mutationKey: ["setPolicy"],
    mutationFn: ({
      master_migration_id,
      migration_id,
      policy_id,
    }: {
      master_migration_id: string;
      migration_id: string;
      policy_id: string;
    }) =>
      getApiClient()
        .then((client) =>
          client.set_policy({
            course_id: store$.id.get() as string,
            master_migration_id: master_migration_id,
            migration_id: migration_id,
            policy_id: policy_id,
          })
        )
        .then((res) => res.data)
        .catch((err) => console.log(err)),
  });

  const applyMasterMigration = useMutation({
    mutationKey: ["applyMasterMigration"],
    mutationFn: ({ master_migration_id }: { master_migration_id: string }) =>
      getApiClient()
        .then((client) =>
          client.apply_master_migration({
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
            console.log(response);
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
          setOutstandingTasks([]);
          setPosting(false);
          navigate("/instructor/migrate/review");
        })
        .catch((error) => {
          console.error("Some tasks failed:", error);
        });
    };

    pollTasks();
  }, [outstandingTasks, pollTaskUntilComplete]);

  useEffect(() => {
    if (migrationData) {
      const assignmentIds: string[] = [];
      migrationData.migrations?.forEach((migration: Migration) => {
        assignmentIds.push(migration.assignment.id as string);
      });
      setSelectedAssignmentIds(assignmentIds);
    }
  }, [migrationData]);

  if (
    migrationIsLoading ||
    !migrationData ||
    courseIsLoading ||
    !courseData ||
    !policyData ||
    policyIsLoading
  )
    return <Loading />;

  if (migrationError || courseError || policyError)
    return `An error occured: ${migrationError}`;

  return (
    <Container size="md">
      <Stepper pl={50} pr={50} pb={30} active={1} allowNextStepsSelect={false}>
        <Stepper.Step label="Load"></Stepper.Step>
        <Stepper.Step label="Apply"></Stepper.Step>
        <Stepper.Step label="Review"></Stepper.Step>
        <Stepper.Step label="Post"></Stepper.Step>
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
                      migrationData.migrations
                        ?.filter((x) => x.assignment.id === selectedAssignment)
                        .at(0)?.assignment.name
                    }
                  </Text>
                </Group>

                <Group>
                  Policy:
                  <Select
                    placeholder="Select policy.."
                    data={
                      policyData &&
                      policyData.map((policy: Policy) => ({
                        label: policy.name ?? "",
                        value: policy.id as string,
                      }))
                    }
                    value={selectedPolicies[selectedAssignment] || ""}
                    error={!selectedPolicies[selectedAssignment] ? error : null}
                    onChange={(value) => {
                      if (!value) return;

                      setSelectedPolicies((prev) => ({
                        ...prev,
                        [selectedAssignment]: value,
                      }));

                      setPolicyMigration.mutate(
                        {
                          master_migration_id:
                            store$.master_migration_id.get() as string,
                          migration_id: migrationData?.migrations
                            ?.filter(
                              (x) => x.assignment.id === selectedAssignment
                            )
                            .at(0)?.id as string,
                          policy_id: value as string,
                        },
                        {
                          onSuccess: () => {
                            refetch();
                          },
                        }
                      );
                    }}
                  />
                </Group>
              </Group>
            </Box>
          </React.Fragment>
        ))}
      </Stack>

      {/* <Text mt="md" fw={700}> */}
      {/*   Statistics */}
      {/* </Text> */}
      {/* <Text> */}
      {/*   <strong>{migrationData.stats?.total_submission}</strong> students */}
      {/* </Text> */}
      {/* <Text> */}
      {/*   <strong>{migrationData.stats?.late_requests}</strong> late submissions */}
      {/* </Text> */}
      {/* <Text> */}
      {/*   <strong>{migrationData.stats?.total_extensions}</strong> extensions */}
      {/* </Text> */}
      {/* <Text> */}
      {/*   <strong>{migrationData.stats?.unapproved_requests}</strong> unapproved */}
      {/*   extensions */}
      {/* </Text> */}

      {!posting && <Text mt={20}>Applying grades..</Text>}

      <Group justify="flex-end" mt="md">
        <Button
          disabled={!posting}
          component={Link}
          to="/instructor/migrate/load"
          color="gray"
          variant="light"
        >
          Previous
        </Button>
        <Button
          disabled={!posting}
          onClick={() => {
            const allPoliciesSelected = selectedAssignmentIds.every(
              (id) => selectedPolicies[id]
            );

            if (!allPoliciesSelected) {
              setError("Please select policies for all selected assignments!");
              return;
            }

            applyMasterMigration.mutate(
              {
                master_migration_id: store$.master_migration_id.get() as string,
              },
              {
                onSuccess: (data) => {
                  console.log(data);
                  setPosting(false);
                  setOutstandingTasks(data);
                },
              }
            );
          }}
          color="blue"
        >
          Next
        </Button>
      </Group>
    </Container>
  );
}
