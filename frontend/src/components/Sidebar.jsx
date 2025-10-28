import { NavLink } from "react-router-dom";
import { LayoutDashboard, Users, Mail, Settings, LogIn } from "lucide-react";

export default function Sidebar() {
  const linkStyle = ({ isActive }) =>
    `flex items-center gap-3 p-2 rounded-md cursor-pointer transition ${
      isActive
        ? "bg-blue-600 text-white"
        : "text-gray-300 hover:bg-gray-800 hover:text-white"
    }`;

  return (
    <div className="bg-gray-900 text-white w-56 min-h-screen p-6">
      <h2 className="text-xl font-bold mb-8 text-center">Outreach AI</h2>

      <nav className="space-y-3">
        <NavLink to="/" className={linkStyle}>
          <LayoutDashboard size={20} /> Dashboard
        </NavLink>

        <NavLink to="/leads" className={linkStyle}>
          <Users size={20} /> Leads
        </NavLink>

        <NavLink to="/campaigns" className={linkStyle}>
          <Mail size={20} /> Campaigns
        </NavLink>

        <NavLink to="/settings" className={linkStyle}>
          <Settings size={20} /> Settings
        </NavLink>

        <NavLink to="/login" className={linkStyle}>
          <LogIn size={20} /> Login
        </NavLink>
      </nav>
    </div>
  );
}
