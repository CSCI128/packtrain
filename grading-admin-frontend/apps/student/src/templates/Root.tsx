import "@mantine/dates/styles.css";
import { Outlet } from "react-router-dom";
import { Navbar } from "../components/Navbar";
import "./Root.scss";

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
