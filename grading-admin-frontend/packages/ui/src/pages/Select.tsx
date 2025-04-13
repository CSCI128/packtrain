import { Button, Center, Container, Stack } from "@mantine/core";
import { getApiClient } from "@repo/api/index";
import { store$ } from "@repo/api/store";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "react-oidc-context";
import { Link, useNavigate } from "react-router-dom";
import { components } from "../lib/api/v1";

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

  const navigate = useNavigate();

  const switchCourse = (id: string, name: string) => {
    store$.id.set(id);
    store$.name.set(name);
    if (close) {
      close();
    }

    if (auth.isAuthenticated && auth.user?.profile.is_admin) {
      navigate("/admin", { replace: true });
      window.location.href = "/admin"; // TODO this is cursed
    } else {
      navigate("/requests");
    }
  };

  if (isLoading || !data) return "Loading...";

  if (error) return `An error occured: ${error}`;

  return (
    <Container size="sm">
      <Center>
        <Stack>
          {data.map(
            (
              course:
                | components["schemas"]["Course"]
                | components["schemas"]["CourseSlim"]
            ) => (
              <Button
                key={course.id}
                onClick={() => switchCourse(course.id as string, course.name)}
              >
                {course.name} ({course.term}){" "}
                {store$.id.get() == course.id && <>(selected)</>}
              </Button>
            )
          )}
          <Button
            color="green"
            component={Link}
            to="/admin/create"
            onClick={close}
          >
            Create Class
          </Button>
        </Stack>
      </Center>
    </Container>
  );
};
