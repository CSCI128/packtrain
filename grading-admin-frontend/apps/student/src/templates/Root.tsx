import "@mantine/dates/styles.css";
import { Navbar } from "@repo/ui/navbar/Navbar";
import { Outlet } from "react-router-dom";
import "./Root.scss";

const staticLinks = [
  { href: "/requests", label: "Grading Admin" },
  { href: "/profile", label: "Profile" },
];

const links = [{ href: "/requests", label: "Requests" }];

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
