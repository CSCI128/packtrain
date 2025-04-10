import "@mantine/dates/styles.css";
import "@repo/ui/Root.scss";
import { Outlet } from "react-router-dom";
import { Navbar } from "../components/Navbar";

export default function Root() {
  return (
    <>
      <Navbar></Navbar>
      <div className="page-container">
        <Outlet />
      </div>
    </>
  );
}
