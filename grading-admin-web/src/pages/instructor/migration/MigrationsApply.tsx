import {
  Box,
  Button,
  Container,
  Divider,
  Group,
  Stack,
  Stepper,
  Text,
} from "@mantine/core";
import React, { useState } from "react";
import { Link } from "react-router-dom";
import { components } from "../../../lib/api/v1";

export function MigrationsApplyPage() {
  const courseData: components["schemas"]["Course"][] = [
    {
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

  const [selectedAssignmentIds, setSelectedAssignmentIds] = useState<string[]>([
    "999-9999-9999-99",
    "999-8888-7777-66",
  ]);

  return (
    <>
      {/* TODO modal for policy selection */}

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
                        courseData[0].assignments?.filter(
                          (a) => a.id === selectedAssignment
                        )[0]?.name
                      }
                    </Text>
                    <Text>studio1.js</Text>
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
