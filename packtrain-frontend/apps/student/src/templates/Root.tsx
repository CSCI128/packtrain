import { Box } from "@mantine/core";
import "@mantine/dates/styles.css";
import { Footer } from "@repo/ui/footer/Footer";
import { Navbar } from "@repo/ui/navbar/Navbar";
import { Outlet } from "react-router-dom";

const staticLinks = [
  { href: "/requests", label: "Grading Admin" },
  { href: "/profile", label: "Profile" },
];

const links = [{ href: "/requests", label: "Requests" }];

export default function Root() {
  return (
    <>
      <Navbar staticLinks={staticLinks} links={links}></Navbar>
      <Box p={16}>
        <Outlet />
      </Box>
      <Footer />
    </>
  );
}
