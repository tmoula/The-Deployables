import { BrowserRouter, Routes, Route } from "react-router-dom";
import { createContext, useContext, useState } from "react";
import Sidebar from "./components/Sidebar";
import Topbar from "./components/Topbar";
import Dashboard from "./pages/Dashboard";
import Leads from "./pages/Leads";
import Campaigns from "./pages/Campaigns";
import Settings from "./pages/Settings";
import Login from "./pages/Login";
import Register from "./pages/Register";


// Create context for sidebar state
const SidebarContext = createContext();

export const useSidebar = () => useContext(SidebarContext);

function AppContent() {
  return (
    <SidebarContext.Provider value={{}}>
      <div className="w-full">
        <Sidebar />
        {/* Main content area - offset by sidebar width (256px = 64 * 4px) */}
        <div className="ml-64 min-h-screen">
          <Routes>
            <Route path="/" element={<><Topbar /><Dashboard /></>} />
            <Route path="/leads" element={<><Topbar /><Leads /></>} />
            <Route path="/campaigns" element={<Campaigns />} />
            <Route path="/settings" element={<><Topbar /><Settings /></>} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
          </Routes>
        </div>
      </div>
    </SidebarContext.Provider>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AppContent />
    </BrowserRouter>
  );
}
