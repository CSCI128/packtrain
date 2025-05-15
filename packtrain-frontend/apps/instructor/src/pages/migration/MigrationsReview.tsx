import {
  Button,
  Center,
  Container,
  Divider,
  Group,
  Modal,
  ScrollArea,
  Stack,
  Stepper,
  Table,
  Tabs,
  Text,
  TextInput,
} from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { getApiClient } from "@repo/api/index";
import {
  Course,
  MasterMigration,
  MigrationWithScores,
  Score,
} from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { sortData, TableHeader } from "@repo/ui/table/Table";
import { IconSearch } from "@tabler/icons-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import React, { useEffect, useState } from "react";
import { BsPencilSquare } from "react-icons/bs";
import { useNavigate } from "react-router-dom";

export function MigrationsReviewPage() {
  const navigate = useNavigate();
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

  const reviewMasterMigration = useMutation({
    mutationKey: ["reviewMasterMigration"],
    mutationFn: ({ master_migration_id }: { master_migration_id: string }) =>
      getApiClient()
        .then((client) =>
          client.review_master_migration({
            course_id: store$.id.get() as string,
            master_migration_id: master_migration_id,
          })
        )
        .then((res) => res.data)
        .catch((err) => console.log(err)),
  });

  const updateStudentScore = useMutation({
    mutationKey: ["updateStudentScore"],
    mutationFn: ({
      master_migration_id,
      migration_id,
    }: {
      master_migration_id: string;
      migration_id: string;
    }) =>
      getApiClient()
        .then((client) =>
          client.update_student_score({
            course_id: store$.id.get() as string,
            master_migration_id: master_migration_id,
            migration_id: migration_id,
          })
        )
        .then((res) => res.data)
        .catch((err) => console.log(err)),
  });

  const [confirmOpened, { open: openConfirm, close: closeConfirm }] =
    useDisclosure(false);
  const [selectedAssignmentIds, setSelectedAssignmentIds] = useState<string[]>(
    []
  );
  const [selectedAssignment, setSelectedAssignment] = useState<string>("");
  const [search, setSearch] = useState("");
  const [sortedData, setSortedData] = useState<Score[]>([]);
  const [sortBy, setSortBy] = useState<keyof Score | null>(null);
  const [reverseSortDirection, setReverseSortDirection] = useState(false);
  const [editScoreOpened, { open: openEditScore, close: closeEditScore }] =
    useDisclosure(false);
  const [selectedScore, setSelectedScore] = useState<Score | null>(null);

  useEffect(() => {
    if (migrationData && migrationData.migrations) {
      setSelectedAssignmentIds(
        migrationData.migrations.map(
          ({ assignment }) => assignment.id as string
        )
      );
    }
  }, [migrationData]);

  useEffect(() => {
    if (migrationData2) {
      if (selectedAssignmentIds.length > 0 && migrationData2) {
        setSelectedAssignment(selectedAssignmentIds[0]);
      }

      const matchingMigration = migrationData2.find(
        (m) => m.assignment?.id === selectedAssignmentIds[0]
      );

      setSortedData(matchingMigration?.scores ?? []);
    }
  }, [selectedAssignmentIds, migrationData2]);

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

  const setSorting = (field: keyof Score) => {
    const reversed = field === sortBy ? !reverseSortDirection : false;
    setReverseSortDirection(reversed);
    setSortBy(field);
    setSortedData(
      sortData<Score>(
        migrationData2
          ?.filter((x) => x.assignment?.id === selectedAssignment)
          .at(0)?.scores as Score[],
        {
          sortBy: field,
          reversed,
          search,
        }
      )
    );
  };

  const handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { value } = event.currentTarget;
    setSearch(value);
    setSortedData(
      sortData<Score>(
        migrationData2
          ?.filter((x) => x.assignment?.id === selectedAssignment)
          .at(0)?.scores as Score[],
        {
          sortBy,
          reversed: reverseSortDirection,
          search: value,
        }
      )
    );
  };

  const handleEditOpen = (row: Score) => {
    setSelectedScore(row);
    openEditScore();
  };

  const rows = sortedData.map((row: Score) => (
    // student name, days late, score to apply, raw score, status
    <Table.Tr key={row.student?.cwid}>
      <Table.Td>{row.student?.name}</Table.Td>
      <Table.Td>{row.submission_date}</Table.Td>
      <Table.Td>
        <Center>
          {/* TODO add margin here */}
          {row.score}
          <BsPencilSquare onClick={() => handleEditOpen(row)} />
        </Center>
      </Table.Td>
      <Table.Td>{row.score}</Table.Td>
      <Table.Td>{row.status}</Table.Td>
    </Table.Tr>
  ));

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
              onClick={() => {
                reviewMasterMigration.mutate(
                  {
                    master_migration_id:
                      store$.master_migration_id.get() as string,
                  },
                  {
                    onSuccess: (data) => {
                      console.log("COMPLETED POST WITH:", data);
                      navigate("/instructor/migrate/post");
                    },
                  }
                );
              }}
              variant="filled"
            >
              Post
            </Button>
          </Stack>
        </Center>
      </Modal>
      <Modal
        opened={editScoreOpened}
        onClose={closeEditScore}
        title="Edit Score to Apply"
      >
        <>
          <Text>Bruh</Text>

          <Group gap="xs" justify="flex-end">
            <Button color="gray" variant="light" onClick={closeEditScore}>
              Cancel
            </Button>
            <Button color="green" type="submit">
              Add
            </Button>
          </Group>
        </>
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
          <Tabs
            defaultValue={selectedAssignmentIds.at(0)}
            onChange={(value) => {
              setSearch("");
              setSelectedAssignment(value as string);
            }}
          >
            <Tabs.List>
              {selectedAssignmentIds.map((assignment) => (
                <Tabs.Tab key={assignment} value={assignment}>
                  {
                    courseData?.assignments?.filter(
                      (a) => a.id === assignment
                    )[0]?.name
                  }
                </Tabs.Tab>
              ))}
            </Tabs.List>

            {selectedAssignmentIds.map((assignment) => (
              <React.Fragment key={assignment}>
                <Tabs.Panel value={assignment} p={20}>
                  <ScrollArea h={750} pb={20}>
                    <TextInput
                      placeholder="Search by any field"
                      mb="md"
                      leftSection={<IconSearch size={16} stroke={1.5} />}
                      value={search}
                      onChange={handleSearchChange}
                    />
                    <Table
                      horizontalSpacing="md"
                      verticalSpacing="xs"
                      miw={700}
                    >
                      <Table.Tbody>
                        <Table.Tr>
                          <TableHeader
                            sorted={sortBy === "student"}
                            reversed={reverseSortDirection}
                            onSort={() => setSorting("student")}
                          >
                            Student
                          </TableHeader>
                          <TableHeader
                            sorted={sortBy === "student"}
                            reversed={reverseSortDirection}
                            onSort={() => setSorting("student")}
                          >
                            {/* TODO need this */}
                            Days Late
                          </TableHeader>
                          <TableHeader
                            sorted={sortBy === "score"}
                            reversed={reverseSortDirection}
                            onSort={() => setSorting("score")}
                          >
                            Score to Apply
                          </TableHeader>
                          <TableHeader
                            sorted={sortBy === "score"}
                            reversed={reverseSortDirection}
                            onSort={() => setSorting("score")}
                          >
                            {/* TODO need this */}
                            Raw Score
                          </TableHeader>
                          <TableHeader
                            sorted={sortBy === "status"}
                            reversed={reverseSortDirection}
                            onSort={() => setSorting("status")}
                          >
                            Status
                          </TableHeader>
                        </Table.Tr>
                      </Table.Tbody>
                      <Table.Tbody>
                        {rows.length > 0 ? (
                          rows
                        ) : (
                          <Table.Tr>
                            <Table.Td
                              colSpan={
                                migrationData2[0] &&
                                Object.keys(migrationData2[0].scores).length
                              }
                            >
                              <Text fw={500} ta="center">
                                No migration data found
                              </Text>
                            </Table.Td>
                          </Table.Tr>
                        )}
                      </Table.Tbody>
                    </Table>
                  </ScrollArea>

                  <Group>
                    <Text>
                      <b>X</b> extensions applied
                    </Text>
                    <Text>
                      <b>X</b> zero credit
                    </Text>
                    <Text>
                      <b>X</b> missing assignments
                    </Text>
                    <Text>
                      <b>X</b> late penalties
                    </Text>
                  </Group>
                </Tabs.Panel>
              </React.Fragment>
            ))}
          </Tabs>
        ) : (
          <Text>No migrations were found!</Text>
        )}

        <Group justify="space-between" mt="xl">
          <Button color="gray">Export</Button>

          <Group>
            <Button onClick={openConfirm}>Post</Button>
          </Group>
        </Group>
      </Container>
    </>
  );
}
