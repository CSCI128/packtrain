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
import { getApiClient } from "@repo/api/index";
import { Assignment, LateRequest, StudentInformation } from "@repo/api/openapi";
import { store$ } from "@repo/api/store.js";
import { calculateNewDueDate, formattedDate } from "@repo/ui/DateUtil";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { useAuth } from "react-oidc-context";
import { Link, useNavigate } from "react-router-dom";

export function ExtensionForm() {
  const auth = useAuth();
  const navigate = useNavigate();
  const [searchValue, setSearchValue] = useState("");
  const [latePassView, setLatePassView] = useState<boolean>(true);
  const [numDaysRequested, setNumDaysRequested] = useState<number>(1);
  const [selectedAssignmentId, setSelectedAssignmentId] = useState<string>("");

  const extensionForm = useForm({
    mode: "uncontrolled",
    initialValues: {
      assignmentId: "",
      daysRequested: numDaysRequested,
      extensionReason: "",
      comments: "",
    },
    validate: {
      assignmentId: (value) =>
        value === "" ? "You must select an assignment!" : null,
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
      daysRequested: numDaysRequested,
    },
    validate: {
      assignmentId: (value) =>
        value === "" ? "You must select an assignment!" : null,
      daysRequested: (value) =>
        value < 0 || value > 5
          ? "Days requested must be greater than zero and less than or equal to 5."
          : null,
    },
  });

  const {
    data: courseData,
    error: courseError,
    isLoading: courseIsLoading,
  } = useQuery<StudentInformation | null>({
    queryKey: ["getCourse"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_course_information_student({
            course_id: store$.id.get() as string,
          })
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });

  const getAssignmentDueDate = (assignmentId: string) => {
    return courseData?.course.assignments
      ?.filter((x: Assignment) => x.id === assignmentId)
      .at(0)?.due_date;
  };

  const { data, error, isLoading } = useQuery<LateRequest[] | null>({
    queryKey: ["getExtensions"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_all_extensions({
            course_id: store$.id.get() as string,
          })
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });

  const postForm = useMutation({
    mutationKey: ["createExtensionRequest"],
    mutationFn: ({ body }: { body: LateRequest }) =>
      getApiClient()
        .then((client) =>
          client.create_extension_request(
            {
              course_id: store$.id.get() as string,
            },
            body
          )
        )
        .then((res) => res.data)
        .catch((err) => console.log(err)),
  });

  const submitExtension = (values: typeof extensionForm.values) => {
    postForm.mutate(
      {
        body: {
          user_requester_id: auth.user?.profile.id as string,
          assignment_id: values.assignmentId,
          date_submitted: new Date().toISOString(),
          num_days_requested: numDaysRequested,
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
        body: {
          user_requester_id: auth.user?.profile.id as string,
          assignment_id: values.assignmentId,
          date_submitted: new Date().toISOString(),
          num_days_requested: numDaysRequested,
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

  if (isLoading || !courseData || courseIsLoading || !data) return "Loading...";

  if (error || courseError) return `An error occured: ${error}`;

  const LATE_PASSES_ALLOWED =
    courseData.course.late_request_config.total_late_passes_allowed;

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
        <strong>Date</strong>:{" "}
        {new Date().toLocaleDateString("en-US", {
          year: "numeric",
          month: "2-digit",
          day: "2-digit",
        })}
      </Text>

      <InputWrapper label="Extension Type:">
        <SegmentedControl
          ml={5}
          onChange={() => {
            setLatePassView(!latePassView);
            setSearchValue("");
            setSelectedAssignmentId("");
            extensionForm.setFieldValue("assignmentId", "");
            latePassForm.setFieldValue("assignmentId", "");
          }}
          data={["Late Pass", "Extension"]}
        />
      </InputWrapper>

      {latePassView ? (
        <form onSubmit={latePassForm.onSubmit(submitLatePass)}>
          <Stack gap="xs">
            {(courseData.late_passes_used ?? 0) >= LATE_PASSES_ALLOWED ? (
              <>
                <Text size="md" mt={20} ta="center" c="red.9" fw={700}>
                  You have no late passes remaining!
                </Text>

                <Text ta="center" c="gray.7">
                  Please contact your instructor if you think this is a mistake.
                </Text>
              </>
            ) : (
              <>
                <Select
                  withAsterisk
                  searchable
                  searchValue={searchValue}
                  onSearchChange={setSearchValue}
                  label="Assignment"
                  placeholder="Pick value"
                  data={
                    courseData.course.assignments &&
                    courseData.course.assignments
                      .filter(
                        (assignment) =>
                          !data.some((d) => d.assignment_id === assignment.id)
                      )
                      .map((assignment) => ({
                        label: assignment.name,
                        value: assignment.id as string,
                      }))
                  }
                  key={latePassForm.key("assignmentId")}
                  {...latePassForm.getInputProps("assignmentId")}
                  onChange={(_value, _) => {
                    setSelectedAssignmentId(_value ?? "");
                    latePassForm.setFieldValue("assignmentId", _value ?? "");
                  }}
                />

                {courseData.course.assignments?.length === 0 && (
                  <>
                    <Text size="md" mt={20} ta="center" c="red.9" fw={700}>
                      There are no loaded/upcoming assignments!
                    </Text>

                    <Text ta="center" c="gray.7">
                      Please contact your instructor if you think this is a
                      mistake.
                    </Text>
                  </>
                )}

                <Text>
                  You have{" "}
                  <strong>
                    {LATE_PASSES_ALLOWED - (courseData.late_passes_used ?? 0)}
                  </strong>{" "}
                  late passes remaining. Late passes are five, free passes to
                  use over the course of the semester to extend your work.
                </Text>

                <Group>
                  <NumberInput
                    withAsterisk
                    label="Days to extend:"
                    defaultValue={1}
                    max={
                      LATE_PASSES_ALLOWED - (courseData.late_passes_used ?? 0)
                    }
                    min={1}
                    key={latePassForm.key("daysRequested")}
                    {...latePassForm.getInputProps("daysRequested")}
                    onChange={(value) => {
                      setNumDaysRequested(parseInt(value as string));
                    }}
                  />

                  <Text>
                    (
                    <strong>
                      {LATE_PASSES_ALLOWED -
                        (courseData.late_passes_used ?? 0) -
                        numDaysRequested}{" "}
                      remaining
                    </strong>{" "}
                    after)
                  </Text>
                </Group>

                <CalculatedDate />
              </>
            )}
          </Stack>

          <Group justify="flex-end" mt="md">
            <Button
              variant="light"
              component={Link}
              to="/requests"
              color="gray"
            >
              Cancel
            </Button>

            {selectedAssignmentId &&
              (courseData.late_passes_used ?? 0) < LATE_PASSES_ALLOWED && (
                <Button type="submit">Submit</Button>
              )}
          </Group>
        </form>
      ) : (
        <form onSubmit={extensionForm.onSubmit(submitExtension)}>
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
                courseData.course.assignments
                  .filter(
                    (assignment) =>
                      !data.some((d) => d.assignment_id === assignment.id)
                  )
                  .map((assignment) => ({
                    label: assignment.name,
                    value: assignment.id as string,
                  }))
              }
              key={extensionForm.key("assignmentId")}
              {...extensionForm.getInputProps("assignmentId")}
              onChange={(_value, _) => {
                setSelectedAssignmentId(_value ?? "");
                extensionForm.setFieldValue("assignmentId", _value ?? "");
              }}
            />

            {courseData.course.assignments?.length === 0 && (
              <>
                <Text size="md" mt={20} ta="center" c="red.9" fw={700}>
                  There are no loaded/upcoming assignments!
                </Text>

                <Text ta="center" c="gray.7">
                  Please contact your instructor if you think this is a mistake.
                </Text>
              </>
            )}

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
                withAsterisk
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
                withAsterisk
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
              withAsterisk
              label="Comments/Explanation"
              placeholder="Some comments.."
              key={extensionForm.key("comments")}
              {...extensionForm.getInputProps("comments")}
            />

            <CalculatedDate />
          </Stack>

          <Group justify="flex-end" mt="md">
            <Button
              variant="light"
              component={Link}
              to="/requests"
              color="gray"
            >
              Cancel
            </Button>
            {selectedAssignmentId && <Button type="submit">Submit</Button>}
          </Group>
        </form>
      )}
    </Container>
  );
}
