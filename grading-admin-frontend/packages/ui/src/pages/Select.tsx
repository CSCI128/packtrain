import { Button, Center, Container, Stack, Text } from "@mantine/core";
import { getApiClient } from "@repo/api/index";
import { Course, CourseSlim } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "react-oidc-context";
import { Link, useNavigate } from "react-router-dom";

export const SelectClass = ({ close }: { close?: () => void }) => {
  const auth = useAuth();

  const { data, error, isLoading } = useQuery({
    queryKey: ["getCourses"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          auth.isAuthenticated && auth.user?.profile.is_admin
            ? client.get_courses()
            : client.get_courses_student()
        )
        .then((res) => res.data)
        .catch((err) => console.log(err)),
    enabled: !!auth.isAuthenticated,
  });

  const { data: enrollmentInfo } = useQuery({
    queryKey: ["getEnrollments"],
    queryFn: () =>
      getApiClient()
        .then((client) => client.get_enrollments())
        .then((res) => res.data)
        .catch((err) => console.log(err)),
    enabled: !!auth.isAuthenticated,
  });

  const navigate = useNavigate();

  const switchCourse = (id: string, name: string) => {
    store$.id.set(id);
    store$.name.set(name);
    if (close) {
      close();
    }

    if (enrollmentInfo) {
      let enrollment = enrollmentInfo
        .filter((c) => c.id === store$.id.get())
        .at(0);
      if (enrollment !== undefined) {
        if (enrollment.course_role === "owner") {
          navigate("/admin", { replace: true });
          window.location.href = "/admin"; // TODO this is cursed
        } else if (enrollment.course_role === "instructor") {
          navigate("/instructor", { replace: true });
          window.location.href = "/instructor"; // TODO this is cursed
        } else if (enrollment.course_role === "student") {
          navigate("/requests", { replace: true });
          window.location.href = "/requests"; // TODO this is cursed
        }
      }
    }
  };

  if (isLoading || !data) return "Loading...";

  if (error) return `An error occured: ${error}`;

  return (
    <Container size="sm">
      <Center>
        <Stack>
          {data.length === 0 ? (
            <Text>No courses were found!</Text>
          ) : (
            data.map((course: Course | CourseSlim) => (
              <Button
                key={course.id}
                onClick={() => switchCourse(course.id as string, course.name)}
              >
                {course.name} ({course.term}){" "}
                {store$.id.get() == course.id && <>(selected)</>}
              </Button>
            ))
          )}
          {auth.user?.profile.is_admin ? (
            <Button
              color="green"
              component={Link}
              to="/admin/create"
              onClick={close}
            >
              Create Class
            </Button>
          ) : (
            <></>
          )}
        </Stack>
      </Center>
    </Container>
  );
};
