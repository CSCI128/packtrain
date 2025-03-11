import {
  Button,
  Container,
  Divider,
  Group,
  InputWrapper,
  NumberInput,
  SegmentedControl,
  Select,
  Stack,
  Text,
  Textarea,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { useState } from "react";
import { useAuth } from "react-oidc-context";
import { Link, useNavigate } from "react-router-dom";
import { $api, store$ } from "../../api";

export function ExtensionForm() {
  const auth = useAuth();
  const navigate = useNavigate();
  const [searchValue, setSearchValue] = useState("");
  const [originalDate, setOriginalDate] = useState<Date>(new Date());
  const [extensionDays, setExtensionDays] = useState<number>(1);
  const [latePassView, setLatePassView] = useState<boolean>(true);
  const [latePassesRemaining, setLatePassesRemaining] = useState<boolean>(true);

  // TODO validate assignmentId/selected ID in <select>
  const extensionForm = useForm({
    mode: "uncontrolled",
    initialValues: {
      assignmentId: "",
      daysRequested: 1,
      extensionReason: "",
      comments: "",
    },
    validate: {
      daysRequested: (value) =>
        value < 0 && value > 5
          ? "Days requested must be greater than zero and less than or equal to 5."
          : null,
    },
  });

  const latePassForm = useForm({
    mode: "uncontrolled",
    initialValues: {
      assignmentId: "",
      daysRequested: 1,
    },
    validate: {
      daysRequested: (value) =>
        value < 0 && value > 5
          ? "Days requested must be greater than zero and less than or equal to 5."
          : null,
    },
  });

  // TODO might want to send back remaining extensions here
  const {
    data: courseData,
    isLoading,
    error,
  } = $api.useQuery("get", "/student/courses/{course_id}", {
    params: {
      path: {
        course_id: store$.id.get() as string,
      },
    },
  });

  const postForm = $api.useMutation(
    "post",
    "/student/courses/{course_id}/extensions"
  );

  const submitExtension = (values: typeof extensionForm.values) => {
    console.log(values);
    postForm.mutate(
      {
        params: {
          path: {
            course_id: store$.id.get() as string,
          },
        },
        body: {
          user_requester_id: auth.user?.profile.id as string,
          assignment_id: values.assignmentId,
          date_submitted: new Date().toISOString(),
          num_days_requested: values.daysRequested,
          request_type: "extension",
          status: "pending",
          extension: {
            comments: values.comments,
            reason: values.extensionReason,
          },
        },
      },
      {
        onSuccess: () => {
          navigate("/requests");
        },
      }
    );
  };

  const submitLatePass = (values: typeof latePassForm.values) => {
    postForm.mutate(
      {
        params: {
          path: {
            course_id: store$.id.get() as string,
          },
        },
        body: {
          user_requester_id: auth.user?.profile.id as string,
          assignment_id: values.assignmentId,
          date_submitted: new Date().toISOString(),
          num_days_requested: values.daysRequested,
          request_type: "late_pass",
          status: "pending",
        },
      },
      {
        onSuccess: () => {
          navigate("/requests");
        },
      }
    );
  };

  if (isLoading || !courseData) return "Loading...";

  if (error) return `An error occured: ${error}`;

  return (
    <Container size="md">
      <Text size="xl" fw={700}>
        Student Extension Form
      </Text>

      <Divider my="md" />

      <Group>
        <Text size="md">
          <strong>Name</strong>: {auth.user?.profile.name}
        </Text>
        <Text size="md">
          <strong>CWID</strong>: {auth.user?.profile.cwid as string}
        </Text>
      </Group>
      <Text size="md">
        <strong>Date</strong>: {new Date().toLocaleString()}
      </Text>

      <InputWrapper label="Extension Type:">
        <SegmentedControl
          ml={5}
          onChange={() => setLatePassView(!latePassView)}
          // TODO onswitch reset search val/assignment val?
          data={["Late Pass", "Extension"]}
        />
      </InputWrapper>

      {latePassView ? (
        <form onSubmit={latePassForm.onSubmit(submitLatePass)}>
          <Stack>
            <Text>
              You have <strong>X</strong> late passes remaining. Late passes are
              five, free passes to use over the course of the semester to extend
              your work.
            </Text>

            <Select
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
                    value: x.id,
                  },
                ])
              }
              key={latePassForm.key("assignmentId")}
              {...latePassForm.getInputProps("assignmentId")}
            />

            <Group>
              <NumberInput
                label="Days to extend:"
                defaultValue={1}
                onChange={() => setExtensionDays}
                max={5}
                min={1}
                key={latePassForm.key("daysRequested")}
                // {...latePassForm.getInputProps("daysRequested")}
              />

              <Text>
                {/* TODO update extensionDays */}(<strong>X remaining</strong>{" "}
                after)
              </Text>
            </Group>
          </Stack>

          <Group justify="flex-end" mt="md">
            <Button component={Link} to="/requests" color="gray">
              Cancel
            </Button>

            {latePassesRemaining && <Button type="submit">Submit</Button>}
          </Group>
        </form>
      ) : (
        <form onSubmit={extensionForm.onSubmit(submitExtension)}>
          <Stack>
            <Select
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
                    value: x.id,
                  },
                ])
              }
              key={extensionForm.key("assignmentId")}
              {...extensionForm.getInputProps("assignmentId")}
            />
            <Text>
              Extensions are <strong>only</strong> for medical, excused or
              extenuating personal circumstances.
            </Text>
            <Text>
              This request will be sent to and reviewed by{" "}
              <strong>Professor {courseData.professor}</strong>.
            </Text>
            <Group>
              <NumberInput
                label="Days Requested"
                defaultValue={1}
                onChange={() => setExtensionDays}
                max={5}
                min={1}
                key={extensionForm.key("daysRequested")}
                // {...extensionForm.getInputProps("assignmentId")}
              />

              <Select
                label="Extension Reason"
                placeholder="Pick value"
                data={[
                  { label: "Tech Issues", value: "TECH" },
                  { label: "Health-Related", value: "HEALTH" },
                  { label: "Family Emergency", value: "FAMILY" },
                  { label: "Personal", value: "PERSONAL" },
                ]}
                key={extensionForm.key("extensionReason")}
                {...extensionForm.getInputProps("extensionReason")}
              />
            </Group>

            <Textarea
              label="Comments/Explanation"
              placeholder="Comments"
              key={extensionForm.key("comments")}
              {...extensionForm.getInputProps("comments")}
            />
            <Text c="gray">
              Original Due Date:{" "}
              <strong>{originalDate.toLocaleString()}</strong>.
            </Text>
            <Text>
              <strong>If approved</strong>, the assignment will be due{" "}
              <strong>
                {/* TODO update this when extensionDays updates */}
                {new Date(
                  originalDate.getTime() + extensionDays * 86400000
                ).toLocaleString()}
              </strong>
              .
            </Text>
          </Stack>

          <Group justify="flex-end" mt="md">
            <Button component={Link} to="/requests" color="gray">
              Cancel
            </Button>
            <Button type="submit">Submit</Button>
          </Group>
        </form>
      )}
    </Container>
  );
}
