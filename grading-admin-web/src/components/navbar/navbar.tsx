import {
  Box,
  Button,
  Center,
  Divider,
  Drawer,
  Group,
  Menu,
  Modal,
  ScrollArea,
  Stack,
} from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { User } from "oidc-client-ts";
import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { $api, store$, userManager } from "../../api";
import classes from "./Navbar.module.scss";

export function Navbar() {
  const { data, error, isLoading } = $api.useQuery("get", "/admin/courses");
  const [drawerOpened, { toggle: toggleDrawer }] = useDisclosure(false);
  const [user, setUser] = useState<User | null>(null);
  const [opened, { open, close }] = useDisclosure(false);
  const navigate = useNavigate();

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

  const switchCourse = (id: string, name: string) => {
    store$.id.set(id);
    store$.name.set(name);
    close();
    navigate("/admin/home");
  };

  if (isLoading || !data) return "Loading...";

  if (error) return `An error occured: ${error}`;

  return (
    <>
      {/* TODO move the modal out of here */}
      <Modal opened={opened} onClose={close} title="Select Class">
        <Center>
          <Stack>
            {data.map((course) => (
              <Button
                key={course.id}
                onClick={() => switchCourse(course.id as string, course.name)}
              >
                {course.name}
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
      </Modal>

      <Box mb={20}>
        <header className={classes.header}>
          <Group justify="space-between" h="100%">
            <Group h="100%" gap={0} visibleFrom="sm">
              <p>Grading Admin</p>
              <a href="/instructor/migrate" className={classes.link}>
                Migrate
              </a>
              <a href="/admin/home" className={classes.link}>
                Course
              </a>
              <a href="/admin/assignments" className={classes.link}>
                Assignments
              </a>
              <a href="/admin/members" className={classes.link}>
                Members
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
                    <p>{user.profile.name}</p>
                  </Menu.Target>

                  <Menu.Dropdown>
                    <Menu.Item>
                      <a href="/profile">Profile</a>
                    </Menu.Item>

                    <Menu.Divider />

                    <Menu.Item color="red" onClick={handleLogout}>
                      Logout
                    </Menu.Item>
                  </Menu.Dropdown>
                </Menu>
              )}

              <Button variant="default" onClick={open}>
                {store$.name.get() || "Select Class"}
              </Button>
            </Group>

            {/* <Burger opened={drawerOpened} onClick={toggleDrawer} hiddenFrom="sm" /> */}
          </Group>
        </header>

        <Drawer
          opened={drawerOpened}
          onClose={toggleDrawer}
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
            {/* <UnstyledButton className={classes.link} onClick={toggleLinks}>
              <Center inline>
                <Box component="span" mr={5}>
                  Features
                </Box>
                <IconChevronDown size={16} color={theme.colors.blue[6]} />
              </Center>
            </UnstyledButton>
            <Collapse in={linksOpened}>{links}</Collapse> */}
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
        </Drawer>
      </Box>
    </>
  );
}
