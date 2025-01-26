import {
    Box,
    Burger,
    Button,
    Group
  } from '@mantine/core';
  import { useDisclosure } from '@mantine/hooks';
  import classes from './Navbar.module.css';
  
  export function Navbar() {
    const [drawerOpened, { toggle: toggleDrawer }] = useDisclosure(false);
    
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
  
            <Group visibleFrom="sm">
              <Button variant="default">Login</Button>
              <Button>Sign Up</Button>
            </Group>
  
            <Burger opened={drawerOpened} onClick={toggleDrawer} hiddenFrom="sm" />
          </Group>
        </header>
      </Box>
    );
  }