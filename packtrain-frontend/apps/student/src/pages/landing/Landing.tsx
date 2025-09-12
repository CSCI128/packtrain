import { Button, Container, Text, Title } from "@mantine/core";
import { Enrollment } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { useEffect } from "react";
import { useAuth } from "react-oidc-context";
import { useNavigate } from "react-router-dom";
import { useGetEnrollments } from "../../hooks";
import classes from "./Landing.module.scss";

export const LandingPage = () => {
  const auth = useAuth();
  const navigate = useNavigate();

  const { data: enrollmentInfo } = useGetEnrollments(!!auth.isAuthenticated);

  useEffect(() => {
    if (enrollmentInfo) {
      const enrollment = enrollmentInfo.find(
        (c: Enrollment) => c.id === store$.id.get()
      );
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
              enrollmentInfo.find((c: Enrollment) => c.id === store$.id.get())
                ?.course_role === "instructor" ? (
                <Button
                  className={classes.control}
                  size="lg"
                  onClick={() => (window.location.href = "/instructor/")}
                >
                  Manage Extensions
                </Button>
              ) : enrollmentInfo &&
                enrollmentInfo.find((c: Enrollment) => c.id === store$.id.get())
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
