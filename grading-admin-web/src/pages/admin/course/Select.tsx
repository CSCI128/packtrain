import { Button, Center, Container, Stack } from "@mantine/core";
import { Link, useNavigate } from "react-router-dom";
import { $api, store$ } from "../../../api";

export const SelectClass = ({ close }: { close?: () => void }) => {
  const { data, error, isLoading } = $api.useQuery("get", "/admin/courses");
  const navigate = useNavigate();

  const switchCourse = (id: string, name: string) => {
    store$.id.set(id);
    store$.name.set(name);
    if (close) {
      close();
    }
    navigate("/admin/home");
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
              {course.name} ({course.term})
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
