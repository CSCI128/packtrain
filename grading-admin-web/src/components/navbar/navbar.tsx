import {
  Box,
  Button,
  Center,
  Collapse,
  Divider,
  Drawer,
  Group,
  Menu,
  Modal,
  ScrollArea,
  Text,
  UnstyledButton,
} from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { User } from "oidc-client-ts";
import { useEffect, useState } from "react";
import { FaChevronDown } from "react-icons/fa";
import { useNavigate } from "react-router-dom";
import { $api, store$, userManager } from "../../api";
import { SelectClass } from "../../pages/admin/course/Select";
import classes from "./Navbar.module.scss";

export function Navbar() {
  const { data, error, isLoading } = $api.useQuery("get", "/admin/courses");
  const [drawerOpened, { toggle: toggleDrawer, close: closeDrawer }] =
    useDisclosure(false);
  const [linksOpened, { toggle: toggleLinks }] = useDisclosure(false);
  const [user, setUser] = useState<User | null>(null);
  const [opened, { open, close }] = useDisclosure(false);
  const navigate = useNavigate();

  const mockdata = [
    {
      // icon: IconCode,
      title: "Open source",
      description: "This Pokémon’s cry is very loud and distracting",
    },
    {
      // icon: IconCoin,
      title: "Free for everyone",
      description: "The fluid of Smeargle’s tail secretions changes",
    },
    {
      // icon: IconBook,
      title: "Documentation",
      description: "Yanma is capable of seeing 360 degrees without",
    },
    {
      // icon: IconFingerprint,
      title: "Security",
      description: "The shell’s rounded shape and the grooves on its.",
    },
    {
      // icon: IconChartPie3,
      title: "Analytics",
      description: "This Pokémon uses its flying ability to quickly chase",
    },
    {
      // icon: IconNotification,
      title: "Notifications",
      description: "Combusken battles with the intensely hot flames it spews",
    },
  ];

  const links = mockdata.map((item) => (
    <UnstyledButton className={classes.subLink} key={item.title}>
      <Group wrap="nowrap" align="flex-start">
        {/* <ThemeIcon size={34} variant="default" radius="md">
          <item.icon size={22} color={theme.colors.blue[6]} />
        </ThemeIcon> */}
        <div>
          <Text size="sm" fw={500}>
            {item.title}
          </Text>
          <Text size="xs" c="dimmed">
            {item.description}
          </Text>
        </div>
      </Group>
    </UnstyledButton>
  ));

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

  // useEffect(() => {
  //   // can't find course, delete state tracked course
  //   if (data === undefined) {
  //     store$.id.delete();
  //     store$.name.delete();
  //   }
  // }, []);

  if (isLoading || !data) return "Loading...";

  if (error) return `An error occured: ${error}`;

  return (
    <>
      {/* TODO move the modal out of here */}
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
                <FaChevronDown size={16} color="blue" />
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
        </Drawer>
      </Box>
    </>
  );
}
