import { Button, Container, Text, Title } from "@mantine/core";
import { getApiClient } from "@repo/api/index";
import { store$ } from "@repo/api/store";
import { useQuery } from "@tanstack/react-query";
import { useEffect } from "react";
import { useAuth } from "react-oidc-context";
import { useNavigate } from "react-router-dom";
import classes from "./Landing.module.scss";

export const LandingPage = () => {
  const auth = useAuth();
  const navigate = useNavigate();

  const { data: enrollmentInfo } = useQuery({
    queryKey: ["getEnrollments"],
    queryFn: () =>
      getApiClient()
        .then((client) => client.get_enrollments())
        .then((res) => res.data)
        .catch((err) => console.log(err)),
    enabled: !!auth.isAuthenticated,
  });

  useEffect(() => {
    if (enrollmentInfo) {
      const enrollment = enrollmentInfo
        .filter((c) => c.id === store$.id.get())
        .at(0);
      if (enrollment !== undefined) {
        if (enrollment.course_role === "owner") {
          window.location.href = "/admin/";
        } else if (enrollment.course_role === "instructor") {
          window.location.href = "/instructor/";
        }
      }
    }
  }, [enrollmentInfo]);

  return (
    <Container className={classes.wrapper} size="75%">
      <div className={classes.inner}>
        <Title className={classes.title}>
          <Text component="span" className={classes.highlight} inherit>
            packtrain
          </Text>
        </Title>

        <Container p={0} size={600}>
          <Text size="lg" c="dimmed" className={classes.description}>
            Software for grading and student extension management.
          </Text>
        </Container>

        {!auth.user ? (
          <div className={classes.controls}>
            <Button
              className={classes.control}
              size="lg"
              onClick={() => void auth.signinRedirect()}
            >
              Login
            </Button>
          </div>
        ) : (
          <>
            <div className={classes.controls}>
              {enrollmentInfo &&
              enrollmentInfo.filter((c) => c.id === store$.id.get()).at(0)
                ?.course_role === "instructor" ? (
                <Button
                  className={classes.control}
                  size="lg"
                  onClick={() => (window.location.href = "/instructor/")}
                >
                  Manage Extensions
                </Button>
              ) : enrollmentInfo &&
                enrollmentInfo.filter((c) => c.id === store$.id.get()).at(0)
                  ?.course_role === "owner" ? (
                <Button
                  className={classes.control}
                  size="lg"
                  onClick={() => (window.location.href = "/admin/")}
                >
                  Manage Course
                </Button>
              ) : (
                <Button
                  className={classes.control}
                  size="lg"
                  onClick={() => navigate("/requests")}
                >
                  Request Extensions
                </Button>
              )}
            </div>
          </>
        )}
      </div>
    </Container>
  );
};
