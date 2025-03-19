import {
  Button,
  Container,
  Divider,
  Group,
  NumberInput,
  Select,
  Stack,
  Stepper,
  Text,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { useState } from "react";
import { Link } from "react-router-dom";

export function MigrationsLoadPage() {
  let courseData: any[] = [];

  const [searchValue, setSearchValue] = useState("");

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

  const submitLoadAssignmentss = (values: typeof form.values) => {
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

        <form onSubmit={form.onSubmit(submitLoadAssignmentss)}>
          <Stack>
            <Select
              withAsterisk
              searchable
              searchValue={searchValue}
              onSearchChange={setSearchValue}
              label="Assignment"
              placeholder="Pick value"
              data={
                courseData.course.assignments &&
                courseData.course.assignments.flatMap((x) => [
                  {
                    label: x.name,
                    value: x.id as string,
                  },
                ])
              }
              key={form.key("assignmentId")}
              {...form.getInputProps("assignmentId")}
              onChange={(_value, _) => {
                // setSelectedAssignmentId(_value ?? "");
                // latePassForm.setFieldValue("assignmentId", _value ?? "");
              }}
            />

            <Text>
              You have <strong>X</strong> late passes remaining. Late passes are
              five, free passes to use over the course of the semester to extend
              your work.
            </Text>

            <Group>
              <NumberInput
                withAsterisk
                label="Days to extend:"
                defaultValue={1}
                max={5}
                min={1}
                key={form.key("daysRequested")}
                {...form.getInputProps("daysRequested")}
                onChange={(value) => {
                  //   setNumDaysRequested(parseInt(value as string));
                }}
              />

              <Text>
                (<strong>X remaining</strong> after)
              </Text>
            </Group>
          </Stack>

          <Group justify="flex-end" mt="md">
            <Button component={Link} to="/requests" color="gray">
              Cancel
            </Button>

            <Button type="submit">Submit</Button>
          </Group>
        </form>

        <Button
          component={Link}
          to="/instructor/migrate/apply"
          variant="filled"
        >
          Apply
        </Button>
      </Container>
    </>
  );
}
