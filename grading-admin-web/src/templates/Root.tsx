import { Outlet } from "react-router-dom";
import { Navbar } from "../components/navbar/navbar";
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
