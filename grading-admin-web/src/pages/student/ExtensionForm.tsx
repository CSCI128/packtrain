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
import { calculateNewDueDate, formattedDate } from "../../utils/DateUtil";

export function ExtensionForm() {
  const auth = useAuth();
  const navigate = useNavigate();
  const [searchValue, setSearchValue] = useState("");
  const [latePassView, setLatePassView] = useState<boolean>(true);
  const [latePassesRemaining, setLatePassesRemaining] = useState<boolean>(true);
  const [numDaysRequested, setNumDaysRequested] = useState<number>(1);
  const [selectedAssignmentId, setSelectedAssignmentId] = useState<string>("");

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
        value < 0 || value > 5
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
        value < 0 || value > 5
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

  const getAssignmentDueDate = (assignmentId: string) => {
    return courseData?.course.assignments
      ?.filter((x) => x.id === assignmentId)
      .at(0)?.due_date;
  };

  const postForm = $api.useMutation(
    "post",
    "/student/courses/{course_id}/extensions"
  );

  const submitExtension = (values: typeof extensionForm.values) => {
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

  function CalculatedDate() {
    return (
      <>
        {selectedAssignmentId && (
          <>
            <Text c="gray">
              Original Due Date:{" "}
              <strong>
                {formattedDate(
                  new Date(
                    Date.parse(getAssignmentDueDate(selectedAssignmentId) || "")
                  )
                )}
              </strong>
            </Text>
            <Text>
              <strong>If approved</strong>, the assignment will be due{" "}
              <strong>
                {formattedDate(
                  new Date(
                    calculateNewDueDate(
                      new Date(
                        Date.parse(
                          getAssignmentDueDate(selectedAssignmentId) || ""
                        )
                      ),
                      numDaysRequested
                    )
                  )
                )}
              </strong>
              .
            </Text>
          </>
        )}
      </>
    );
  }

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
          onChange={() => {
            setLatePassView(!latePassView);
            setSearchValue("");
            setSelectedAssignmentId("");
          }}
          data={["Late Pass", "Extension"]}
        />
      </InputWrapper>

      {latePassView ? (
        <form onSubmit={latePassForm.onSubmit(submitLatePass)}>
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
                    value: x.id as string,
                  },
                ])
              }
              key={latePassForm.key("assignmentId")}
              {...latePassForm.getInputProps("assignmentId")}
              onChange={(_value) => setSelectedAssignmentId(_value ?? "")}
            />

            <Text>
              You have <strong>X</strong> late passes remaining. Late passes are
              five, free passes to use over the course of the semester to extend
              your work.
            </Text>

            <Group>
              <NumberInput
                label="Days to extend:"
                defaultValue={1}
                max={5}
                min={1}
                key={latePassForm.key("daysRequested")}
                {...latePassForm.getInputProps("daysRequested")}
                onChange={(value) => {
                  setNumDaysRequested(parseInt(value as string));
                }}
              />

              <Text>
                (<strong>X remaining</strong> after)
              </Text>
            </Group>

            <CalculatedDate />
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
                    value: x.id as string,
                  },
                ])
              }
              key={extensionForm.key("assignmentId")}
              {...extensionForm.getInputProps("assignmentId")}
              onChange={(_value) => setSelectedAssignmentId(_value ?? "")}
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
                max={5}
                min={1}
                key={extensionForm.key("daysRequested")}
                {...extensionForm.getInputProps("daysRequested")}
                onChange={(value) => {
                  setNumDaysRequested(parseInt(value as string));
                }}
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

            <CalculatedDate />
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
