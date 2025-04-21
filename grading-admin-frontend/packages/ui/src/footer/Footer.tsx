import { Center, Text } from "@mantine/core";
import classes from "./Footer.module.scss";

export function Footer() {
  return (
    <div className={classes.footer}>
      <div className={classes.inner}>
        <Center>
          <Text c="dimmed">
            Copyright (c) 2025 The Packtrain Team &bull;{" "}
            <a href="https://github.com/CSCI128/packtrain">GitHub</a>
          </Text>
        </Center>
      </div>
    </div>
  );
}
