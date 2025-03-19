import { Container, Divider, Stepper, Text } from "@mantine/core";

export function MigrationsPostPage() {
  return (
    <>
      <Container size="md">
        <Stepper
          pl={50}
          pr={50}
          pb={30}
          active={3}
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
      </Container>
    </>
  );
}
