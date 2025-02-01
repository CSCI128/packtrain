import { observable } from "@legendapp/state";
import { Box, Button, Group, Menu, Modal } from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { User } from "oidc-client-ts";
import { useEffect, useState } from "react";
import { userManager } from "../../api";
import classes from "./Navbar.module.scss";

interface Course {
  id: string;
  name: string;
}

const store$ = observable<Course>();

export function Navbar() {
  // TODO mobile responsiveness drawer
  // const [drawerOpened, { toggle: toggleDrawer }] = useDisclosure(false);

  const [user, setUser] = useState<User | null>(null);
  const [opened, { open, close }] = useDisclosure(false);

  useEffect(() => {
    const handleCallback = async () => {
      try {
        const url = new URL(window.location.href);
        if (url.searchParams.has("code") || url.searchParams.has("state")) {
          await userManager.signinRedirectCallback();
          window.history.replaceState({}, document.title, url.pathname);
        }
        const user = await userManager.getUser();
        setUser(user);
      } catch (error) {
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

  const setActiveCourse = () => {
    store$.name.set("CSCI128");
    close();
  };

  return (
    <>
      <Modal opened={opened} onClose={close} title="Select Class">
        {/* TODO get all courses and map */}
        <Button onClick={setActiveCourse}>CSCI128</Button>
        <br />
        <Button>Create Class</Button>
      </Modal>

      <Box mb={20}>
        <header className={classes.header}>
          <Group justify="space-between" h="100%">
            <Group h="100%" gap={0} visibleFrom="sm">
              <p>Grading Admin</p>
              <a href="/admin/migrate" className={classes.link}>
                Migrate
              </a>
              <a href="/admin/home" className={classes.link}>
                Course
              </a>
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
                    <Menu.Item>
                      <a href="/profile" className={classes.link}>
                        Profile
                      </a>
                    </Menu.Item>

                    <Menu.Divider />

                    <Menu.Item color="red" onClick={handleLogout}>
                      Logout
                    </Menu.Item>
                  </Menu.Dropdown>
                </Menu>
              )}

              {/* TODO class state here */}
              {/* <p>CSCI128</p> */}
              <Button variant="default" onClick={open}>
                {/* CSCI128 */}
                {store$.name.get()}
              </Button>
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
    </>
  );
}
