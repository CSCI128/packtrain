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
import { useAuth } from "react-oidc-context";
import { useNavigate } from "react-router-dom";
import { store$ } from "../../api";
import { SelectClass } from "../../pages/admin/course/Select";
import classes from "./Navbar.module.scss";

export function Navbar() {
  const [drawerOpened, { toggle: toggleDrawer, close: closeDrawer }] =
    useDisclosure(false);
  const [opened, { open, close }] = useDisclosure(false);
  const navigate = useNavigate();
  const auth = useAuth();

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
              {!auth.user ? (
                <Group visibleFrom="sm">
                  <Button
                    onClick={() => void auth.signinRedirect()}
                    variant="default"
                  >
                    Login
                  </Button>
                </Group>
              ) : (
                <Menu shadow="md" width={200}>
                  <Menu.Target>
                    <p>{auth.user.profile.name}</p>
                  </Menu.Target>

                  <Menu.Dropdown>
                    <Menu.Item component="a" href="/profile">
                      Profile
                    </Menu.Item>

                    <Menu.Divider />

                    <Menu.Item
                      color="red"
                      onClick={() => {
                        void auth.signoutRedirect();
                        store$.id.delete();
                        store$.name.delete();
                      }}
                    >
                      Logout
                    </Menu.Item>
                  </Menu.Dropdown>
                </Menu>
              )}

              {auth.isAuthenticated && (
                <Button variant="default" onClick={open}>
                  {store$.name.get() || "Select Class"}
                </Button>
              )}
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
