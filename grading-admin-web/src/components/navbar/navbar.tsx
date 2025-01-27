import { Box, Button, Group, Menu } from "@mantine/core";
import { User } from "oidc-client-ts";
import { useEffect, useState } from "react";
import { userManager } from "../../api";
import classes from "./Navbar.module.css";

export function Navbar() {
  // TODO mobile responsiveness drawer
  // const [drawerOpened, { toggle: toggleDrawer }] = useDisclosure(false);

  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    const handleCallback = async () => {
      try {
        // TODO this may or may not be necessary, we can probably just signinRedirectCallback();
        // Check if the URL contains an authorization response (e.g., ?code=)
        const url = new URL(window.location.href);
        if (url.searchParams.has("code") || url.searchParams.has("state")) {
          await userManager.signinRedirectCallback();
          // Remove query parameters from the URL after processing
          window.history.replaceState({}, document.title, url.pathname);
        }
        const user = await userManager.getUser();
        setUser(user);

        // TODO grab bearer token from here and set for middleware/future requests

        // console.log(user);
      } catch (error) {
        // TODO handle the error properly
        console.error("Error handling callback:", error);
      }
    };

    handleCallback();
  }, []);

  const handleLogin = async () => {
    await userManager.signinRedirect();
  };

  const handleLogout = async () => {
    await userManager.signoutRedirect();
  };

  return (
    <Box pb={120}>
      <header className={classes.header}>
        <Group justify="space-between" h="100%">
          <Group h="100%" gap={0} visibleFrom="sm">
            <p>Grading Admin</p>
            <a href="/admin/assignments" className={classes.link}>
              Assignments
            </a>
            <a href="/admin/users" className={classes.link}>
              Users
            </a>
          </Group>

          <Group>
            {!user ? (
              <Group visibleFrom="sm">
                <Button onClick={handleLogin} variant="default">
                  Login
                </Button>
              </Group>
            ) : (
              <Menu shadow="md" width={200}>
                <Menu.Target>
                  <p>{user.profile.sub}</p>
                </Menu.Target>

                <Menu.Dropdown>
                  <Menu.Item>Profile</Menu.Item>

                  <Menu.Divider />

                  <Menu.Item color="red" onClick={handleLogout}>
                    Logout
                  </Menu.Item>
                </Menu.Dropdown>
              </Menu>
            )}

            {/* TODO class state here */}
            <p>CSCI128</p>
          </Group>

          {/* <Burger opened={drawerOpened} onClick={toggleDrawer} hiddenFrom="sm" /> */}
        </Group>
      </header>

      {/* <Drawer
        opened={drawerOpened}
        onClose={closeDrawer}
        size="100%"
        padding="md"
        title="Navigation"
        hiddenFrom="sm"
        zIndex={1000000}
        >
          <ScrollArea h="calc(100vh - 80px" mx="-md">
            <Divider my="sm" />

            <a href="#" className={classes.link}>
              Home
            </a>
            <UnstyledButton className={classes.link} onClick={toggleLinks}>
              <Center inline>
                <Box component="span" mr={5}>
                  Features
                </Box>
                <IconChevronDown size={16} color={theme.colors.blue[6]} />
              </Center>
            </UnstyledButton>
            <Collapse in={linksOpened}>{links}</Collapse>
            <a href="#" className={classes.link}>
              Learn
            </a>
            <a href="#" className={classes.link}>
              Academy
            </a>

            <Divider my="sm" />

            <Group justify="center" grow pb="xl" px="md">
              <Button variant="default">Log in</Button>
              <Button>Sign up</Button>
            </Group>
          </ScrollArea>
        </Drawer> */}
    </Box>
  );
}
