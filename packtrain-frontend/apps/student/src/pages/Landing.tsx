import { Button, Container, Text, Title } from "@mantine/core";
import { useAuth } from "react-oidc-context";
import { useNavigate } from "react-router-dom";
import classes from "./Landing.module.scss";

export const LandingPage = () => {
  const auth = useAuth();
  const navigate = useNavigate();

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
          <div className={classes.controls}>
            <Button
              className={classes.control}
              size="lg"
              onClick={() => navigate("/requests")}
            >
              Request Extensions
            </Button>
          </div>
        )}
      </div>
    </Container>
  );
};
