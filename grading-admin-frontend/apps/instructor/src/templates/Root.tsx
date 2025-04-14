import "@mantine/dates/styles.css";
import { Navbar } from "@repo/ui/navbar/Navbar";
import { Outlet } from "react-router-dom";
import "./Root.scss";

const staticLinks = [
  { href: "/instructor", label: "Grading Admin" },
  { href: "/instructor/profile", label: "Profile" },
];

const links = [
  { href: "/instructor/migrate", label: "Migrate" },
  { href: "/instructor/members", label: "Members" },
];

export default function Root() {
  return (
    <>
      <Navbar staticLinks={staticLinks} links={links}></Navbar>
      <div className="page-container">
        <Outlet />
      </div>
    </>
  );
}
