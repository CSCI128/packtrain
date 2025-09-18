import {
  Button,
  Center,
  Container,
  Divider,
  Group,
  Modal,
  NumberInput,
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
  MigrationScoreChange,
  MigrationWithScores,
  Score,
} from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { Loading } from "@repo/ui/components/Loading";
import { TableHeader, useTableData } from "@repo/ui/components/table/Table";
import { IconSearch } from "@tabler/icons-react";
import { useMutation } from "@tanstack/react-query";
import React, { useEffect, useState } from "react";
import { BsPencilSquare } from "react-icons/bs";
import { useNavigate } from "react-router-dom";
import {
  useGetCourseInstructor,
  useGetMasterMigration,
  useGetMigrationWithScores,
} from "../../hooks";

type ExportRow = {
  name: string;
  canvas_id: string;
  score: number;
};

export function MigrationsReviewPage() {
  const navigate = useNavigate();
  const {
    data: courseData,
    error: courseError,
    isLoading: courseIsLoading,
  } = useGetCourseInstructor();

  const {
    data: masterMigrationData,
    error: masterMigrationError,
    isLoading: masterMigrationIsLoading,
  } = useGetMasterMigration();

  const {
    data: migrationData,
    error: migrationError,
    isLoading: migrationIsLoading,
    refetch,
  } = useGetMigrationWithScores();

  const reviewMasterMigration = useMutation({
    mutationKey: ["reviewMasterMigration"],
    mutationFn: async ({
      master_migration_id,
    }: {
      master_migration_id: string;
    }) => {
      const client = await getApiClient();
      const res = await client.review_master_migration({
        course_id: store$.id.get() as string,
        master_migration_id: master_migration_id,
      });
      return res.data;
    },
  });

  const updateStudentScore = useMutation({
    mutationKey: ["updateStudentScore"],
    mutationFn: async ({
      master_migration_id,
      migration_id,
      migration_score_change,
    }: {
      master_migration_id: string;
      migration_id: string;
      migration_score_change: MigrationScoreChange;
    }) => {
      const client = await getApiClient();
      const res = await client.update_student_score(
        {
          course_id: store$.id.get() as string,
          master_migration_id: master_migration_id,
          migration_id: migration_id,
        },
        {
          cwid: migration_score_change.cwid,
          new_score: migration_score_change.new_score,
          justification: migration_score_change.justification,
          submission_status: migration_score_change.submission_status,
          adjusted_submission_date:
            migration_score_change.adjusted_submission_date,
        }
      );
      return res.data;
    },
  });

  const [confirmOpened, { open: openConfirm, close: closeConfirm }] =
    useDisclosure(false);
  const [selectedAssignmentIds, setSelectedAssignmentIds] = useState<string[]>(
    []
  );
  const [selectedAssignment, setSelectedAssignment] = useState<string>("");

  const {
    search,
    sortedData,
    sortBy,
    reverseSortDirection,
    handleSearchChange,
    handleSort,
    resetTable,
  } = useTableData<Score>(
    migrationData?.find((x) => x.assignment?.id === selectedAssignment)
      ?.scores ?? []
  );

  const [editScoreOpened, { open: openEditScore, close: closeEditScore }] =
    useDisclosure(false);
  const [selectedScore, setSelectedScore] = useState<Score | null>(null);
  const [updatedScore, setUpdatedScore] = useState<number>(0);
  // const [statistics, setStatistics] = useState<{
  //   extensionsApplied: 0;
  //   zeroCredit: 0;
  //   missingAssignments: 0;
  //   latePenalties: 0;
  // }>();

  useEffect(() => {
    if (masterMigrationData && masterMigrationData.migrations) {
      setSelectedAssignmentIds(
        masterMigrationData.migrations.map(
          ({ assignment }) => assignment.id as string
        )
      );
    }
  }, [masterMigrationData]);

  useEffect(() => {
    if (selectedAssignmentIds.length > 0 && migrationData) {
      setSelectedAssignment(selectedAssignmentIds[0]);
    }
  }, [selectedAssignmentIds, migrationData]);

  if (
    masterMigrationIsLoading ||
    !masterMigrationData ||
    courseIsLoading ||
    !courseData ||
    !migrationData ||
    migrationIsLoading
  )
    return <Loading />;

  if (masterMigrationError || courseError || migrationError)
    return (
      <Text>
        An error occured: {courseError?.message} {masterMigrationError?.message}{" "}
        {migrationError?.message}
      </Text>
    );

  const handleEditOpen = (row: Score) => {
    setSelectedScore(row);
    openEditScore();
  };

  const handleEditSubmit = () => {
    updateStudentScore.mutate(
      {
        master_migration_id: store$.master_migration_id.get() as string,
        migration_id: migrationData?.find(
          (x) => x.assignment?.id === selectedAssignment
        )?.migration_id as string,
        migration_score_change: {
          // TODO finish these fields
          cwid: selectedScore?.student?.cwid as string,
          justification: "",
          new_score: updatedScore,
          submission_status:
            selectedScore?.status as MigrationScoreChange["submission_status"],
          adjusted_submission_date: selectedScore?.submission_date,
        },
      },
      {
        onSuccess: () => {
          closeEditScore();
          refetch();
        },
      }
    );
  };

  const exportData: ExportRow[] = sortedData.map((row: Score) => ({
    name: row.student?.name ?? "",
    canvas_id: row.student?.canvas_id ?? "",
    score: row.score ?? 0.0,
  }));

  function exportToCanvasCsv(data: ExportRow[], filename = "scores.csv") {
    // ex: "HW6 - Logic & Circuits (279003)";
    const assignment = migrationData?.find(
      (x: MigrationWithScores) => x.assignment?.id === selectedAssignment
    )?.assignment;

    if (!assignment) {
      console.error("Assignment not found; could not export Canvas CSV");
      return;
    }

    const assignmentName = assignment.name;
    const assignmentCanvasId = assignment.canvas_id;

    // Canvas header rows - SIS User ID,SIS Login ID,Section don't need to be populated bc Canvas is stupid
    const header1 = `Student,ID,SIS User ID,SIS Login ID,Section,${assignmentName} (${assignmentCanvasId})`;

    const rows = data.map((d) => [
      d.name ?? "",
      d.canvas_id ?? "",
      "",
      "",
      "",
      d.score ?? "",
    ]);

    const csvContent =
      "data:text/csv;charset=utf-8," +
      [header1, ...rows.map((r) => r.join(","))].join("\n");

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", filename);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  const rows = sortedData.map((row: Score) => (
    // student name, days late, score to apply, raw score, status
    <Table.Tr key={row.student?.cwid}>
      <Table.Td>{row.student?.name}</Table.Td>
      <Table.Td>
        <Center>
          <Text size="sm" mr={5}>
            {row.score}
          </Text>
          <BsPencilSquare onClick={() => handleEditOpen(row)} />
        </Center>
      </Table.Td>
      <Table.Td>
        <Center>{row.raw_score}</Center>
      </Table.Td>
      <Table.Td>
        <Center>{row.days_late}</Center>
      </Table.Td>
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
            <Text size="md">
              Are you sure you want to continue? This action is irreversible!
            </Text>

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
                    onSuccess: () => navigate("/instructor/migrate/post"),
                  }
                );
              }}
              variant="filled"
            >
              Yes
            </Button>
          </Stack>
        </Center>
      </Modal>
      <Modal
        opened={editScoreOpened}
        onClose={closeEditScore}
        title={"Edit Score for " + selectedScore?.student?.name}
      >
        <Stack mb={20}>
          <NumberInput
            label="Score:"
            value={selectedScore?.score}
            onChange={(value) => setUpdatedScore(Number(value))}
            required
          />
        </Stack>

        <Group gap="xs" justify="flex-end">
          <Button color="gray" variant="light" onClick={closeEditScore}>
            Cancel
          </Button>
          <Button color="green" type="submit" onClick={handleEditSubmit}>
            Add
          </Button>
        </Group>
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
        </Stepper>

        <Text size="xl" fw={700}>
          Review
        </Text>

        <Divider my="sm" />

        {selectedAssignmentIds.length > 0 ? (
          <Tabs
            defaultValue={selectedAssignmentIds.at(0)}
            onChange={(value) => {
              resetTable();
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
                            sorted={sortBy === "student.name"}
                            reversed={reverseSortDirection}
                            onSort={() => handleSort("student.name")}
                          >
                            Student
                          </TableHeader>
                          <TableHeader
                            sorted={sortBy === "score"}
                            reversed={reverseSortDirection}
                            onSort={() => handleSort("score")}
                          >
                            Score to Apply
                          </TableHeader>
                          <TableHeader
                            sorted={sortBy === "raw_score"}
                            reversed={reverseSortDirection}
                            onSort={() => handleSort("raw_score")}
                          >
                            Raw Score
                          </TableHeader>
                          <TableHeader
                            sorted={sortBy === "days_late"}
                            reversed={reverseSortDirection}
                            onSort={() => handleSort("days_late")}
                          >
                            Days Late
                          </TableHeader>
                          <TableHeader
                            sorted={sortBy === "status"}
                            reversed={reverseSortDirection}
                            onSort={() => handleSort("status")}
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
                                migrationData[0] &&
                                Object.keys(migrationData[0].scores).length
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
                      <b>{migrationData[0].stats?.total_submission}</b> Total
                      submissions
                    </Text>
                    <Text>
                      <b>{migrationData[0].stats?.late_requests}</b> Late
                      requests
                    </Text>
                    <Text>
                      <b>{migrationData[0].stats?.total_extensions}</b>{" "}
                      Extensions applied
                    </Text>
                    <Text>
                      <b>{migrationData[0].stats?.total_late_passes}</b> Total
                      late passes
                    </Text>
                  </Group>
                </Tabs.Panel>
              </React.Fragment>
            ))}
          </Tabs>
        ) : (
          <Text>No migrations were found!</Text>
        )}

        <Group justify="space-between" mt="l">
          <Button color="gray" onClick={() => exportToCanvasCsv(exportData)}>
            Export
          </Button>

          <Group>
            <Button onClick={openConfirm}>Post</Button>
          </Group>
        </Group>
      </Container>
    </>
  );
}
