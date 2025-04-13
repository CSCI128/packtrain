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
  Stack,
  Text,
} from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { store$ } from "@repo/api/store";
import { SelectClass } from "@repo/ui/pages/Select";
import { useAuth } from "react-oidc-context";
import { useNavigate } from "react-router-dom";
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
              <p onClick={() => navigate("/instructor/migrate")}>
                Grading Admin
              </p>
              {auth.user?.profile.is_admin ? (
                <>
                  <a href="/instructor/migrate" className={classes.link}>
                    Migrate
                  </a>
                  <a href="/members" className={classes.link}>
                    Members
                  </a>
                </>
              ) : (
                <></>
              )}
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

              <Button variant="default" onClick={open}>
                {store$.name.get() || "Select Class"}
              </Button>
            </Group>
          </Group>

          <Drawer
            opened={drawerOpened}
            onClose={closeDrawer}
            size="50%"
            padding="md"
            title="Navigation"
            hiddenFrom="sm"
            zIndex={1000000}
          >
            <ScrollArea h="calc(100vh - 80px)" mx="-md">
              <Divider my="sm" />

              <Box ml={20}>
                <p onClick={() => navigate("/")}>Grading Admin</p>
                {auth.user?.profile.is_admin ? (
                  <>
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
                  </>
                ) : (
                  <></>
                )}
              </Box>

              <Divider my="sm" />

              <Group justify="center" grow pb="xl" px="md">
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
                    <Stack>
                      <Text>{auth.user.profile.name}</Text>

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
                    </Stack>
                  </Menu>
                )}

                <Button variant="default" onClick={open}>
                  {store$.name.get() || "Select Class"}
                </Button>
              </Group>
            </ScrollArea>
          </Drawer>
        </header>
      </Box>
    </>
  );
}
