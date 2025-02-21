import {
  Box,
  Burger,
  Button,
  Divider,
  Drawer,
  Group,
  Menu,
  Modal,
  ScrollArea,
} from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { User } from "oidc-client-ts";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  $api,
  handleLogin,
  handleLogout,
  store$,
  userManager,
} from "../../api";
import { SelectClass } from "../../pages/admin/course/Select";
import classes from "./Navbar.module.scss";

export function Navbar() {
  const { data, error, isLoading } = $api.useQuery("get", "/admin/courses");
  const [drawerOpened, { toggle: toggleDrawer, close: closeDrawer }] =
    useDisclosure(false);
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

  if (isLoading || !data) return "Loading...";

  if (error) return `An error occured: ${error}`;

  return (
    <>
      <Modal opened={opened} onClose={close} title="Select Class">
        <SelectClass close={close} />
      </Modal>

      <Box mb={20}>
        <header className={classes.header}>
          <Group justify="space-between" h="100%">
            <Group h="100%" gap={0} visibleFrom="sm">
              <p onClick={() => navigate("/")}>Grading Admin</p>
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

            <Burger
              opened={opened}
              onClick={toggleDrawer}
              size="sm"
              hiddenFrom="sm"
            />

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
                    <Menu.Item component="a" href="/profile">
                      Profile
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
          </Group>

          {/* TODO finish drawer */}
          <Drawer
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
              <a href="#" className={classes.link}>
                Learn
              </a>
              <a href="#" className={classes.link}>
                Academy
              </a>

              <Divider my="sm" />

              <Group justify="center" grow pb="xl" px="md">
                <Button variant="default">Log in</Button>
              </Group>
            </ScrollArea>
          </Drawer>
        </header>
      </Box>
    </>
  );
}
