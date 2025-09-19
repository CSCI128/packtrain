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
import { Migration, Policy } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { Loading } from "@repo/ui/components/Loading";
import { useWebSocketClient } from "@repo/ui/WebSocketHooks";
import { useMutation } from "@tanstack/react-query";
import { User } from "oidc-client-ts";
import React, { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { userManager } from "../../auth";
import {
  useGetCourseInstructor,
  useGetMasterMigration,
  useGetPolicies,
} from "../../hooks";

type MigrationApplyNotificationDTO = {
  error?: string;
  zero_submissions_complete?: boolean;
  process_extensions_complete?: boolean;
};

export function MigrationsApplyPage() {
  const navigate = useNavigate();
  const [selectedPolicies, setSelectedPolicies] = useState<
    Record<string, string>
  >({});
  const [selectedAssignmentIds, setSelectedAssignmentIds] = useState<string[]>(
    []
  );
  const [posting, setPosting] = useState<boolean>(false);
  const [tasksFailed, setTasksFailed] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string>(""); // WS error
  const [error, setError] = useState<string>(""); // policy validation error

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
    mutationFn: async ({
      master_migration_id,
      migration_id,
      policy_id,
    }: {
      master_migration_id: string;
      migration_id: string;
      policy_id: string;
    }) => {
      const client = await getApiClient();
      const res = await client.set_policy({
        course_id: store$.id.get() as string,
        master_migration_id: master_migration_id,
        migration_id: migration_id,
        policy_id: policy_id,
      });
      return res.data;
    },
  });

  const [completed, setCompleted] = useState({
    extensions: false,
    zero: false,
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
      client.subscribe("/migrations/apply", (msg) => {
        const payload: MigrationApplyNotificationDTO = JSON.parse(msg.body);

        if (payload.error) {
          setErrorMessage(payload.error);
          setTasksFailed(true);
        }

        if (payload.process_extensions_complete) {
          setCompleted((prev) => ({ ...prev, extensions: true }));
        }
        if (payload.zero_submissions_complete) {
          setCompleted((prev) => ({ ...prev, zero: true }));
        }
      });
    },
  });

  useEffect(() => {
    if (completed.extensions && completed.zero) {
      setPosting(false);
    }
  }, [completed]);

  const applyMasterMigration = useMutation({
    mutationKey: ["applyMasterMigration"],
    mutationFn: async ({
      master_migration_id,
    }: {
      master_migration_id: string;
    }) => {
      const client = await getApiClient();
      const res = await client.apply_master_migration({
        course_id: store$.id.get() as string,
        master_migration_id: master_migration_id,
      });
      return res.data;
    },
  });

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
    return (
      <Text>
        An error occured: {courseError?.message} {policyError?.message}{" "}
        {migrationError?.message}
      </Text>
    );

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
                      migrationData.migrations?.find(
                        (x) => x.assignment.id === selectedAssignment
                      )?.assignment.name
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
                          migration_id: migrationData?.migrations?.find(
                            (x) => x.assignment.id === selectedAssignment
                          )?.id as string,
                          policy_id: value as string,
                        },
                        {
                          onSuccess: () => refetch(),
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

      {posting && <Text mt={20}>Applying grades..</Text>}

      {completed.extensions && completed.zero && (
        <Text mt={20}>Applied grades successfully!</Text>
      )}

      {tasksFailed && (
        <>
          <Text size="md" ta="center" c="red.9" fw={700}>
            Migration Apply Failed
          </Text>
          <Text ta="center" c="gray.7">
            {errorMessage}
          </Text>
        </>
      )}

      <Group justify="flex-end" mt="md">
        <Button
          disabled={posting}
          component={Link}
          to="/instructor/migrate/load"
          color="gray"
          variant="light"
        >
          Previous
        </Button>
        <Button
          disabled={posting}
          onClick={() => {
            const allPoliciesSelected = selectedAssignmentIds.every(
              (id) => selectedPolicies[id]
            );

            if (!allPoliciesSelected) {
              setError("Please select policies for all selected assignments!");
              return;
            }

            if (completed.extensions && completed.zero) {
              navigate("/instructor/migrate/review");
            } else {
              applyMasterMigration.mutate({
                master_migration_id: store$.master_migration_id.get() as string,
              });
              setPosting(true);
            }
          }}
          color="blue"
        >
          Next
        </Button>
      </Group>
    </Container>
  );
}
