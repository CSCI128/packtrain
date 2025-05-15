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
import { Course, CourseSlim, Enrollment } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { useAuth } from "react-oidc-context";

export const SelectClass = ({ close }: { close?: () => void }) => {
  const auth = useAuth();
  const [checked, setChecked] = useState(true);

  const { data, error, isLoading, refetch } = useQuery<
    Course[] | CourseSlim[],
    Error,
    Course[] | CourseSlim[],
    [string, { onlyActive: boolean }]
  >({
    queryKey: ["getCourses", { onlyActive: checked }],
    queryFn: async ({
      queryKey,
    }: {
      queryKey: [string, { onlyActive: boolean }];
    }) => {
      const [, { onlyActive }] = queryKey;
      const client = await getApiClient();

      const res =
        auth.isAuthenticated && auth.user?.profile.is_admin
          ? await client.owner_get_courses({ onlyActive })
          : await client.get_courses_student();

      return res.data;
    },
    enabled: auth.isAuthenticated,
  });

  const { data: enrollmentInfo } = useQuery<Enrollment[]>({
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

    if (enrollmentInfo) {
      const enrollment = enrollmentInfo
        .filter((c) => c.id === store$.id.get())
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
    }
  };

  const handleCreateClass = () => {
    if (auth.user?.profile.is_admin) {
      window.location.href = "/admin/create";
    }
    close?.();
  };

  if (isLoading || !data) return "Loading...";

  if (error) return `An error occured: ${error}`;

  return (
    <Container size="sm">
      <Center>
        <Stack>
          {data
            // admins can see disabled courses (toggleable), students can't
            .filter((x) => !auth.user?.profile.is_admin && x.enabled)
            .map((course: Course | CourseSlim) => (
              <Button
                key={course.id}
                onClick={() => switchCourse(course.id as string, course.name)}
              >
                {course.name} ({course.term}){" "}
                {store$.id.get() == course.id && <>(selected)</>}
              </Button>
            ))}
          {auth.user?.profile.is_admin ? (
            <>
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
            <></>
          )}
        </Stack>
      </Center>
    </Container>
  );
};
