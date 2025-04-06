import {
  Button,
  Container,
  Divider,
  Group,
  Stepper,
  Tabs,
  Text,
} from "@mantine/core";
import { useState } from "react";
import { Link } from "react-router-dom";
import { components } from "../../../lib/api/v1";

export function MigrationsReviewPage() {
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

  const [selectedAssignmentIds, setSelectedAssignmentIds] = useState<string[]>([
    "999-9999-9999-99",
    "999-8888-7777-66",
  ]);

  return (
    <>
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

        <Tabs defaultValue={selectedAssignmentIds.at(0)}>
          <Tabs.List>
            {selectedAssignmentIds.map((assignment) => (
              <Tabs.Tab value={assignment}>
                {
                  courseData[0].assignments?.filter(
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

            <Button
              color="blue"
              component={Link}
              to="/instructor/migrate/post"
              variant="filled"
            >
              Post
            </Button>
          </Group>
        </Group>
      </Container>
    </>
  );
}
