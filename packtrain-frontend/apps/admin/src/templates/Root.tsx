import { Box } from "@mantine/core";
import "@mantine/dates/styles.css";
import { Footer } from "@repo/ui/footer/Footer";
import { Navbar } from "@repo/ui/navbar/Navbar";
import { Outlet } from "react-router-dom";

const staticLinks = [
  { href: "/admin/", label: "packtrain" },
  { href: "/admin/profile", label: "Profile" },
];

const links = [
  { href: "/admin/", label: "Course" },
  { href: "/admin/assignments", label: "Assignments" },
  { href: "/admin/members", label: "Members" },
  { href: "/admin/users", label: "Users" },
];

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
