import { Button, Center, Container, Stack } from "@mantine/core";
import { useAuth } from "react-oidc-context";
import { Link, useNavigate } from "react-router-dom";
import { $api, store$ } from "../../../api";

export const SelectClass = ({ close }: { close?: () => void }) => {
  const auth = useAuth();
  const { data, error, isLoading } = $api.useQuery(
    "get",
    auth.isAuthenticated && auth.user?.profile.is_admin
      ? "/admin/courses"
      : "/student/courses"
  );
  const navigate = useNavigate();

  const switchCourse = (id: string, name: string) => {
    store$.id.set(id);
    store$.name.set(name);
    if (close) {
      close();
    }

    if (auth.isAuthenticated && auth.user?.profile.is_admin) {
      navigate("/admin/home");
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
          {data.map((course) => (
            <Button
              key={course.id}
              onClick={() => switchCourse(course.id as string, course.name)}
            >
              {course.name} ({course.term}){" "}
              {store$.id.get() == course.id && <>(selected)</>}
            </Button>
          ))}
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
