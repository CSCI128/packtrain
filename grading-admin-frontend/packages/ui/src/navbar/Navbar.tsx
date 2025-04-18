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
  UnstyledButton,
} from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { store$ } from "@repo/api/store";
import { SelectClass } from "@repo/ui/pages/Select";
import { IconChevronDown } from "@tabler/icons-react";
import { useAuth } from "react-oidc-context";
import { useNavigate } from "react-router-dom";
import classes from "./Navbar.module.scss";

type Link = {
  href: string;
  label: string;
};

interface NavbarProps {
  staticLinks: Link[];
  links: Link[];
}

export function Navbar({ staticLinks, links }: NavbarProps) {
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
              <p onClick={() => navigate(staticLinks[0]?.href as string)}>
                packtrain
              </p>

              {links.map((link) => (
                <a key={link.href} href={link.href} className={classes.link}>
                  {link.label}
                </a>
              ))}
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
                    <UnstyledButton>
                      <Group gap={7}>
                        <Text fw={500} size="sm" lh={1} mr={3}>
                          {auth.user.profile.name}
                        </Text>
                        <IconChevronDown size={12} stroke={1.5} />
                      </Group>
                    </UnstyledButton>
                  </Menu.Target>

                  <Menu.Dropdown>
                    <Menu.Item component="a" href={staticLinks[1]?.href}>
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
                <p onClick={() => navigate(staticLinks[0]?.href as string)}>
                  Grading Admin
                </p>
                {links.map((link) => (
                  <a key={link.href} href={link.href} className={classes.link}>
                    {link.label}
                  </a>
                ))}
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
                        <Menu.Item component="a" href={staticLinks[1]?.href}>
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
