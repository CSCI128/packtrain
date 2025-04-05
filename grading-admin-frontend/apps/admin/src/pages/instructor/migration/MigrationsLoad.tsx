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
import React, { useState } from "react";
import { Link } from "react-router-dom";
import { components } from "../../../lib/api/v1";

export function MigrationsLoadPage() {
  const courseData: components["schemas"]["Course"][] = [
    {
      late_request_config: {
        enabled_extension_reasons: ["ext1"],
        late_pass_name: "Late Pass",
        late_passes_enabled: true,
        total_late_passes_allowed: 5,
      },
      id: "999-9999-9999-99",
      term: "Fall 2020",
      enabled: true,
      name: "EXCL101",
      code: "Fall.2020.EXCL.101",
      canvas_id: 123456,
      members: [
        {
          cwid: "CWID123456",
          canvas_id: "9876543",
          course_role: "instructor",
          sections: ["fall.2020.excl.101.section.a"],
          late_passes_used: 1,
        },
        {
          cwid: "CWID654321",
          canvas_id: "1234567",
          course_role: "student",
          sections: ["fall.2020.excl.101.section.b"],
          late_passes_used: 0,
        },
      ],
      assignments: [
        {
          id: "999-9999-9999-99",
          name: "Assessment 1",
          category: "Quiz",
          canvas_id: 1245678,
          points: 15.0,
          external_service: "Gradescope",
          external_points: 14,
          due_date: "2020-01-15T12:00:00.000Z",
          unlock_date: "2020-01-01T12:00:00.000Z",
          enabled: true,
          group_assignment: false,
          attention_required: false,
          frozen: false,
        },
        {
          id: "999-8888-7777-66",
          name: "Final Exam",
          category: "Exam",
          canvas_id: 987654,
          points: 100.0,
          external_service: "ProctorU",
          external_points: 95,
          due_date: "2020-12-20T12:00:00.000Z",
          unlock_date: "2020-12-01T12:00:00.000Z",
          enabled: true,
          group_assignment: false,
          attention_required: true,
          frozen: false,
        },
      ],
      sections: [
        "fall.2020.excl.101.section.a",
        "fall.2020.excl.101.section.b",
      ],
    },
  ];

  const [searchValue, setSearchValue] = useState("");
  const [selectedAssignmentIds, setSelectedAssignmentIds] = useState<string[]>(
    []
  );
  const [validated, setValidated] = useState(false);

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
    console.log("validating");
    setValidated(true);
  };

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

  return (
    <>
      <Container size="md">
        <Stepper
          pl={50}
          pr={50}
          pb={30}
          active={0}
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
                courseData[0].assignments &&
                courseData[0].assignments.flatMap((x) => [
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
                          courseData[0].assignments?.filter(
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
              <Button component={Link} to="/instructor/migrate" color="gray">
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
    </>
  );
}
