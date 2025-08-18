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
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { useAuth } from "react-oidc-context";

export const SelectClass = ({ close }: { close?: () => void }) => {
  const auth = useAuth();
  const [checked, setChecked] = useState(true);

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
        .catch((err) => {
          console.log(err);
          return [];
        }),
    enabled: !!auth.isAuthenticated,
  });

  const switchCourse = (id: string, name: string) => {
    store$.id.set(id);
    store$.name.set(name);
    if (close) {
      close();
    }

    const enrollment = data?.filter((c) => c.id === store$.id.get()).at(0);
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
      </Button>
    );
  }

  if (!data || isLoading) return "Loading...";

  if (error) return `An error occured: ${error}`;

  return (
    <Container size="sm">
      <Center>
        <Stack>
          {/* students can only see enabled courses, admin sees all */}
          {auth.user?.profile.is_admin ? (
            <>
              {data.map((enrollment: Enrollment) => (
                <ClassButton key={enrollment.id} {...enrollment} />
              ))}
              <Button color="green" onClick={handleCreateClass}>
                Create Class
              </Button>
              <Center>
                <Group>
                  <Checkbox
                    checked={checked}
                    onChange={(event) => {
                      setChecked(event.currentTarget.checked);
                      refetch();
                    }}
                  />
                  <Text c="gray">Show only active courses</Text>
                </Group>
              </Center>
            </>
          ) : (
            <>
              {data
                .filter((enrollment: Enrollment) => enrollment.enabled)
                .map((enrollment: Enrollment) => (
                  <ClassButton key={enrollment.id} {...enrollment} />
                ))}
            </>
          )}
        </Stack>
      </Center>
    </Container>
  );
};
