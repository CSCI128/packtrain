import {
  Box,
  Button,
  Container,
  Divider,
  FileInput,
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
import { useNavigate } from "react-router-dom";

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

  const createMigration = useMutation({
    mutationKey: ["createMigration"],
    mutationFn: ({
      master_migration_id,
      assignment_id,
    }: {
      master_migration_id: string;
      assignment_id: string;
    }) =>
      getApiClient()
        .then((client) =>
          client.create_migration_for_master_migration({
            course_id: store$.id.get() as string,
            master_migration_id: master_migration_id,
            assignment: assignment_id,
          })
        )
        .then((res) => res.data)
        .catch((err) => console.log(err)),
  });

  const uploadScores = useMutation({
    mutationKey: ["uploadScores"],
    mutationFn: ({
      master_migration_id,
      migration_id,
      file,
    }: {
      master_migration_id: string;
      migration_id: string;
      file: string;
    }) =>
      getApiClient()
        .then((client) => {
          return client.upload_raw_scores(
            {
              course_id: store$.id.get() as string,
              master_migration_id: master_migration_id,
              migration_id: migration_id,
            },
            file,
            {
              headers: {
                "Content-Type": "application/octet-stream",
              },
            }
          );
        })
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

  const deleteMasterMigration = useMutation({
    mutationKey: ["deleteMasterMigration"],
    mutationFn: ({ master_migration_id }: { master_migration_id: string }) =>
      getApiClient()
        .then((client) =>
          client.delete_master_migration({
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
    loadValidateMasterMigration.mutate(
      {
        master_migration_id: masterMigrationId,
      },
      {
        onSuccess: () => {
          setValidated(true);
        },
      }
    );
  };

  const uploadForAssignment = (file: File, selectedAssignmentId: string) => {
    console.log(selectedAssignmentId);
    createMigration.mutate(
      {
        master_migration_id: masterMigrationId,
        assignment_id: selectedAssignmentId,
      },
      {
        onSuccess: async (data: MasterMigration | void) => {
          console.log(data);

          uploadScores.mutate({
            master_migration_id: masterMigrationId,
            migration_id: data?.migrations
              ?.filter((x) => x.assignment.id === selectedAssignmentId)
              .at(0)?.id as string,
            // @ts-ignore
            file,
          });
        },
      }
    );
  };

  useEffect(() => {
    if (!store$.master_migration_id.get()) {
      createMasterMigration.mutate(undefined, {
        onSuccess: (data) => {
          store$.master_migration_id.set(data?.id);
          setMasterMigrationId(store$.master_migration_id.get() as string);
        },
      });
    }
    console.log(
      `Setting master migration id ${store$.master_migration_id.get()}..`
    );
    setMasterMigrationId(store$.master_migration_id.get() as string);
  }, [store$]);

  useEffect(() => {
    if (selectedAssignmentIds && selectedAssignmentIds.length > 0) {
      console.log("CHANGED:", selectedAssignmentIds);
    }
  }, [selectedAssignmentIds]);

  const loadAssignments = () => {
    loadMasterMigration.mutate(
      {
        master_migration_id: masterMigrationId,
      },
      {
        onSuccess: (data) => {
          console.log(data);
          navigate("/instructor/migrate/apply");
        },
      }
    );
  };

  if (isLoading || !data) return "Loading...";

  if (error) return `An error occured: ${error}`;

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
            data.assignments
              .map((x) => ({
                label: x.name,
                value: x.id as string,
              }))
              .toSorted((x, y) => x.label.localeCompare(y.label))
          }
          key={form.key("assignmentIds")}
          {...form.getInputProps("assignmentIds")}
          onChange={(value: string[]): void => {
            setSelectedAssignmentIds(value);
          }}
        />

        {selectedAssignmentIds.map((selectedAssignment: string) => (
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
                  {/*<Text>Status</Text>*/}
                </Group>

                <Group>
                  {/*<Text>View Data</Text>*/}
                  <FileInput
                    placeholder="Upload a file.."
                    onChange={(file) =>
                      uploadForAssignment(file as File, selectedAssignment)
                    }
                  />
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
              deleteMasterMigration.mutate({
                master_migration_id: store$.master_migration_id.get() as string,
              });
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
            <Button color="blue" onClick={loadAssignments}>
              Next
            </Button>
          ) : (
            <Button disabled color="gray">
              Next
            </Button>
          )}
        </Group>
      </Group>
    </Container>
  );
}
