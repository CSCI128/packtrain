import { Box } from "@mantine/core";
import "@mantine/dates/styles.css";
import { Footer } from "@repo/ui/components/footer/Footer";
import { Navbar } from "@repo/ui/components/navbar/Navbar";
import { Outlet } from "react-router-dom";

const staticLinks = [
  { href: "/instructor/", label: "packtrain" },
  { href: "/instructor/profile", label: "Profile" },
];

const links = [
  { href: "/instructor/", label: "Approvals" },
  { href: "/instructor/migrate", label: "Migrate" },
  { href: "/instructor/members", label: "Members" },
];

export default function Root() {
  return (
    <>
      <Navbar staticLinks={staticLinks} links={links} />
      <Box p={16}>
        <Outlet />
      </Box>
      <Footer />
    </>
  );
}
