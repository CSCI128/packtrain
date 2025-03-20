import {
  Button,
  Container,
  Divider,
  Group,
  Progress,
  Stepper,
  Text,
} from "@mantine/core";
import { useState } from "react";
import { Link } from "react-router-dom";

export function MigrationsPostPage() {
  const [postingFinished, setPostingFinished] = useState(true);

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
          Post Grades
        </Text>

        <Divider my="sm" />

        <Progress my="md" radius="xl" size="lg" value={50} animated />

        <Text size="lg" fw={700}>
          Posting grades..
        </Text>

        <Text size="md" c="gray.6" ta="center">
          Reminder: You must post/publish the grades in Canvas for students to
          see them!
        </Text>

        {postingFinished && (
          <Group justify="flex-end" mt="md">
            <Button color="blue" component={Link} to="/instructor/migrate/load">
              Again
            </Button>

            <Button
              color="blue"
              component={Link}
              to="/instructor/migrate"
              variant="filled"
            >
              Done
            </Button>
          </Group>
        )}
      </Container>
    </>
  );
}
