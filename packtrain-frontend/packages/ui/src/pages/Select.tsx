import {
  Button,
  Center,
  Checkbox,
  Container,
  Group,
  Stack,
  Text,
} from "@mantine/core";
import { getApiClient } from "@repo/api/index";
import { Enrollment } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { Loading } from "@repo/ui/Loading";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { useAuth } from "react-oidc-context";

export const SelectClass = ({ close }: { close?: () => void }) => {
  const auth = useAuth();
  const [checked, setChecked] = useState(false);

  const { data, error, isLoading, refetch } = useQuery<
    Enrollment[],
    Error,
    Enrollment[]
  >({
    queryKey: ["getEnrollments"],
    queryFn: () =>
      getApiClient()
        .then((client) => client.get_enrollments())
        .then((res) => res.data)
        .catch(() => []),
    enabled: !!auth.isAuthenticated,
  });

  const switchCourse = (id: string, name: string) => {
    store$.id.set(id);
    store$.name.set(name);
    close?.();

    const enrollment = data
      ?.filter((enrollment: Enrollment) => enrollment.id === store$.id.get())
      .at(0);
    if (enrollment !== undefined) {
      if (enrollment.course_role === "owner") {
        window.location.href = "/admin/";
      } else if (enrollment.course_role === "instructor") {
        window.location.href = "/instructor/";
      } else if (enrollment.course_role === "student") {
        window.location.href = "/requests";
      }
    }
  };

  const handleCreateClass = () => {
    if (auth.user?.profile.is_admin) {
      window.location.href = "/admin/create";
    }
    close?.();
  };

  function ClassButton(enrollment: Enrollment) {
    return (
      <Button
        onClick={() => switchCourse(enrollment.id as string, enrollment.name)}
      >
        {enrollment.name} ({enrollment.term}){" "}
        {store$.id.get() == enrollment.id && <>(selected)</>}
        {!enrollment.enabled && <>(inactive)</>}
      </Button>
    );
  }

  if (!data || isLoading) return <Loading />;

  if (error) return <Text>An error occured: {error.message}</Text>;

  return (
    <Container size="sm">
      <Center>
        <Stack>
          {data.length === 0 ? (
            <Center>
              <Text>No enrollments found!</Text>
            </Center>
          ) : (
            <>
              {auth.user?.profile.is_admin ? (
                // admin sees all courses, filterable by active
                <>
                  {data
                    .filter(
                      (enrollment: Enrollment) =>
                        checked ||
                        enrollment.enabled ||
                        // state where course could be selected but disabled
                        store$.id.get() === enrollment.id
                    )
                    .map((enrollment: Enrollment) => (
                      <ClassButton key={enrollment.id} {...enrollment} />
                    ))}
                  <Button color="green" onClick={handleCreateClass}>
                    Create Class
                  </Button>
                  <Group>
                    <Checkbox
                      checked={checked}
                      onChange={(event) => {
                        setChecked(event.currentTarget.checked);
                        refetch();
                      }}
                    />
                    <Text c="gray">Show inactive courses</Text>
                  </Group>
                </>
              ) : (
                // students only see enabled courses
                <>
                  {data
                    .filter((enrollment: Enrollment) => enrollment.enabled)
                    .map((enrollment: Enrollment) => (
                      <ClassButton key={enrollment.id} {...enrollment} />
                    ))}
                </>
              )}
            </>
          )}
        </Stack>
      </Center>
    </Container>
  );
};
