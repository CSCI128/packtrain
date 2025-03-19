import { Button, Container, Divider, Stepper, Text } from "@mantine/core";
import { Link } from "react-router-dom";

export function MigrationsApplyPage() {
  return (
    <>
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
          Migrations
        </Text>

        <Divider my="sm" />

        <Button
          component={Link}
          to="/instructor/migrate/review"
          variant="filled"
        >
          Review
        </Button>
      </Container>
    </>
  );
}
