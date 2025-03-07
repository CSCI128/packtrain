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
import { Link } from "react-router-dom";

export function ExtensionForm() {
  const auth = useAuth();

  const [originalDate, setOriginalDate] = useState<Date>(new Date());
  const [extensionDays, setExtensionDays] = useState<number>(1);
  const [latePassView, setLatePassView] = useState<boolean>(true);
  const [latePassesRemaining, setLatePassesRemaining] = useState<boolean>(true);

  const extensionForm = useForm({
    mode: "uncontrolled",
    initialValues: {
      courseName: "",
    },
    validate: {
      courseName: (value) =>
        value && value.length < 1
          ? "Course name must have at least 1 character"
          : null,
    },
  });

  const latePassForm = useForm({
    mode: "uncontrolled",
    initialValues: {
      courseName: "",
    },
    validate: {
      courseName: (value) =>
        value && value.length < 1
          ? "Course name must have at least 1 character"
          : null,
    },
  });

  // /admin/courses/{course_id}/members
  // const { data, error, isLoading } = $api.useQuery("get", "/user");

  const submitExtension = (values: typeof extensionForm.values) => {};

  const submitLatePass = (values: typeof latePassForm.values) => {};

  // TODO call user/member endpoint

  // TODO set originalDate

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

            <Group>
              <NumberInput
                label="Days to extend:"
                defaultValue={1}
                onChange={() => setExtensionDays}
                max={5}
                min={1}
              />

              <Text>
                (<strong>X remaining</strong> after)
              </Text>
            </Group>
          </Stack>

          <Group justify="flex-end" mt="md">
            <Button component={Link} to="/" type="submit" color="gray">
              Cancel
            </Button>

            {latePassesRemaining && (
              <Button component={Link} to="/admin/home" type="submit">
                Submit
              </Button>
            )}
          </Group>
        </form>
      ) : (
        <form onSubmit={extensionForm.onSubmit(submitExtension)}>
          <Stack>
            <Select
              label="Assignment"
              placeholder="Pick value"
              data={["Test"]}
            />

            <Text>
              Extensions are <strong>only</strong> for medical, excused or
              extenuating personal circumstances.
            </Text>
            <Text>
              This request will be sent to and reviewed by{" "}
              <strong>Professor TODO</strong>.
            </Text>

            <Group>
              <NumberInput
                label="Days Requested"
                defaultValue={1}
                onChange={() => setExtensionDays}
                max={5}
                min={1}
              />

              <Select
                label="Extension Reason"
                placeholder="Pick value"
                data={[
                  "Tech Issues",
                  "Health-Related",
                  "Family Emergency",
                  "Personal",
                ]}
              />
            </Group>

            <Textarea label="Comments/Explanation" placeholder="Comments" />

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
            <Button component={Link} to="/" type="submit" color="gray">
              Cancel
            </Button>
            <Button component={Link} to="/admin/home" type="submit">
              Submit
            </Button>
          </Group>
        </form>
      )}
    </Container>
  );
}
