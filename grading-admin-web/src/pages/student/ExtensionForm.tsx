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
import { useAuth } from "react-oidc-context";
import { Link } from "react-router-dom";

export function ExtensionForm() {
  const auth = useAuth();

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

      {/* <form onSubmit={latePassForm.onSubmit(submitLatePass)}></form> */}

      <InputWrapper label="Extension Type:">
        <SegmentedControl
          onChange={() => console.log("hello")}
          data={["Late Pass", "Extension"]}
        />
      </InputWrapper>

      <form onSubmit={extensionForm.onSubmit(submitExtension)}>
        <Stack>
          <Select label="Assignment" placeholder="Pick value" data={["Test"]} />

          <Text>
            Extensions are <strong>only</strong> for medical, excused or
            extenuating personal circumstances.
          </Text>
          <Text>
            This request will be sent to and reviewed by{" "}
            <strong>Professor TODO</strong>.
          </Text>

          <Group>
            <NumberInput label="Days Requested" defaultValue={1} />

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

          <Textarea label="Comments/Explanation" placeholder="Comments.." />

          <Text c="gray">
            Original Due Date: <strong>TODO ORIGINAL DUE DATE</strong>.
          </Text>

          <Text>
            <strong>If approved</strong>, the assignment will be due TODO NEW
            DUE DATE.
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
    </Container>
  );
}
