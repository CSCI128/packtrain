import "@mantine/dates/styles.css";
import { Footer } from "@repo/ui/footer/Footer";
import { Navbar } from "@repo/ui/navbar/Navbar";
import { Outlet } from "react-router-dom";
import "./Root.scss";

const staticLinks = [
  { href: "/admin", label: "Grading Admin" },
  { href: "/admin/profile", label: "Profile" },
];

const links = [
  { href: "/admin", label: "Course" },
  { href: "/admin/assignments", label: "Assignments" },
  { href: "/admin/members", label: "Members" },
  { href: "/admin/users", label: "Users" },
];

export default function Root() {
  return (
    <>
      <Navbar staticLinks={staticLinks} links={links}></Navbar>
      <div className="page-container">
        <Outlet />
      </div>
      <Footer />
    </>
  );
}
