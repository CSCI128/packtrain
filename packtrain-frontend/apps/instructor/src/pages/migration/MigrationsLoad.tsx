import {
  Box,
  Button,
  Container,
  Divider,
  Group,
  MultiSelect,
  Stack,
  Stepper,
  Text,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { getApiClient } from "@repo/api/index";
import { Course, MasterMigration } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { useMutation, useQuery } from "@tanstack/react-query";
import React, { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";

export function MigrationsLoadPage() {
  const navigate = useNavigate();
  const [searchValue, setSearchValue] = useState("");
  const [selectedAssignmentIds, setSelectedAssignmentIds] = useState<string[]>(
    []
  );
  const [validated, setValidated] = useState(false);
  const [masterMigrationId, setMasterMigrationId] = useState<string>("");

  const { data, error, isLoading } = useQuery<Course | null>({
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
    queryKey: ["getCourse"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_master_migrations({
            course_id: store$.id.get() as string,
            master_migration_id: "",
          })
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });

  const createMasterMigration = useMutation({
    mutationKey: ["createMasterMigration"],
    mutationFn: () =>
      getApiClient()
        .then((client) =>
          client.create_master_migration({
            course_id: store$.id.get() as string,
          })
        )
        .then((res) => res.data)
        .catch((err) => console.log(err)),
  });

  const loadMasterMigration = useMutation({
    mutationKey: ["loadMasterMigration"],
    mutationFn: ({ master_migration_id }: { master_migration_id: string }) =>
      getApiClient()
        .then((client) =>
          client.load_master_migration({
            course_id: store$.id.get() as string,
            master_migration_id: master_migration_id,
          })
        )
        .then((res) => res.data)
        .catch((err) => console.log(err)),
  });

  const loadValidateMasterMigration = useMutation({
    mutationKey: ["loadValidateMasterMigration"],
    mutationFn: ({ master_migration_id }: { master_migration_id: string }) =>
      getApiClient()
        .then((client) =>
          client.load_validate_master_migration({
            course_id: store$.id.get() as string,
            master_migration_id: master_migration_id,
          })
        )
        .then((res) => res.data)
        .catch((err) => console.log(err)),
  });

  const form = useForm({
    mode: "uncontrolled",
    initialValues: {
      name: "",
      category: "",
      canvas_id: -1,
      points: 0,
      due_date: new Date(),
      unlock_date: new Date(),
      external_service: "",
      external_points: 0,
      enabled: true,
      group_assignment: true,
      attention_required: true,
      assignmentIds: [],
    },
    validate: {
      name: (value) =>
        value && value.length < 1
          ? "Name must have at least 1 character"
          : null,
      category: (value) =>
        value && value.length < 1
          ? "Category must have at least 1 character"
          : null,
      points: (value) =>
        value && value <= 0 ? "Points must be greater than 0" : null,
    },
  });

  const validateAssignments = () => {
    // TODO validate selected assignments
    loadValidateMasterMigration.mutate({
      master_migration_id: "",
    });
    console.log("validating");
    setValidated(true);
  };

  // create_migration_for_master_migration
  //  /instructor/course/{course_id}/migrations/{master_migration_id}/{migration_id}/scores
  // /instructor/courses/{course_id}/migrations/{master_migration_id}:

  useEffect(() => {
    if (!store$.master_migration_id.get()) {
      createMasterMigration.mutate(undefined, {
        onSuccess: (data) => {
          store$.master_migration_id.set(data?.id);
        },
      });
    }
    console.log(
      `Setting master migration id ${store$.master_migration_id.get()}..`
    );
    setMasterMigrationId(store$.master_migration_id.get() as string);
  }, [store$]);

  const submitLoadAssignments = (values: typeof form.values) => {
    // postForm.mutate(
    //   {
    //     params: {
    //       path: {
    //         course_id: store$.id.get() as string,
    //       },
    //     },
    //     body: {
    //       user_requester_id: auth.user?.profile.id as string,
    //       assignment_id: values.assignmentId,
    //       date_submitted: new Date().toISOString(),
    //       num_days_requested: values.daysRequested,
    //       request_type: "extension",
    //       status: "pending",
    //       extension: {
    //         comments: values.comments,
    //         reason: values.extensionReason,
    //       },
    //     },
    //   },
    //   {
    //     onSuccess: () => {
    //       navigate("/requests");
    //     },
    //   }
    // );
  };

  if (isLoading || !data || migrationIsLoading || !migrationData)
    return "Loading...";

  if (error || migrationError) return `An error occured: ${error}`;

  return (
    <Container size="md">
      <Stepper pl={50} pr={50} pb={30} active={0} allowNextStepsSelect={false}>
        <Stepper.Step label="Load"></Stepper.Step>
        <Stepper.Step label="Apply"></Stepper.Step>
        <Stepper.Step label="Review"></Stepper.Step>
        <Stepper.Step label="Post"></Stepper.Step>
        <Stepper.Completed>
          Completed, click back button to get to previous step
        </Stepper.Completed>
      </Stepper>

      <Text size="xl" fw={700}>
        Load Assignments
      </Text>

      <Divider my="sm" />

      <form onSubmit={form.onSubmit(submitLoadAssignments)}>
        <Stack>
          <MultiSelect
            withAsterisk
            searchable
            searchValue={searchValue}
            onSearchChange={setSearchValue}
            label="Assignment"
            placeholder="Select more.."
            data={
              data.assignments &&
              data.assignments.flatMap((x) => [
                {
                  label: x.name,
                  value: x.id as string,
                },
              ])
            }
            key={form.key("assignmentIds")}
            {...form.getInputProps("assignmentIds")}
            onChange={(value: string[]): void => {
              setSelectedAssignmentIds(value);
            }}
          />

          {selectedAssignmentIds.map((selectedAssignment) => (
            <React.Fragment key={selectedAssignment}>
              <Box p={20} bg="gray.1">
                <Group justify="space-between">
                  <Group>
                    <Text fw={800}>
                      {
                        data.assignments?.filter(
                          (a) => a.id === selectedAssignment
                        )[0]?.name
                      }
                    </Text>
                    <Text>Status</Text>
                  </Group>

                  <Group>
                    <Text>View Data</Text>
                    <Button color="gray">Upload</Button>
                  </Group>
                </Group>
              </Box>
            </React.Fragment>
          ))}
        </Stack>

        <Group justify="space-between" mt="xl">
          <Button
            color="gray"
            onClick={() => {
              form.reset();
              setSelectedAssignmentIds([]);
            }}
          >
            Clear
          </Button>

          <Group>
            <Button
              variant="light"
              onClick={() => {
                navigate("/instructor/migrate");
                store$.master_migration_id.delete();
              }}
              color="gray"
            >
              Cancel
            </Button>

            <Button color="green" onClick={validateAssignments}>
              Validate
            </Button>

            {validated ? (
              <Button
                color="blue"
                component={Link}
                to="/instructor/migrate/apply"
                variant="filled"
              >
                Next
              </Button>
            ) : (
              <Button disabled color="gray">
                Next
              </Button>
            )}
          </Group>
        </Group>
      </form>
    </Container>
  );
}
