import { BrowserRouter, Routes, Route } from "react-router-dom";
import { createContext, useContext, useState } from "react";
import Sidebar from "./components/Sidebar";
import Topbar from "./components/Topbar";
import PrivateRoute from "./components/PrivateRoute";
import Dashboard from "./pages/Dashboard";
import Leads from "./pages/Leads";
import Campaigns from "./pages/Campaigns";
import Settings from "./pages/Settings";
import Login from "./pages/Login";
import Register from "./pages/Register";
import MasterInbox from "./pages/MasterInbox";
import Mailboxes from "./pages/Mailboxes";


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
            {/* Protected Routes */}
            <Route
              path="/"
              element={
                <PrivateRoute>
                  <>
                    <Topbar />
                    <Dashboard />
                  </>
                </PrivateRoute>
              }
            />
            <Route
              path="/leads"
              element={
                <PrivateRoute>
                  <>
                    <Topbar />
                    <Leads />
                  </>
                </PrivateRoute>
              }
            />
            <Route
              path="/campaigns"
              element={
                <PrivateRoute>
                  <Campaigns />
                </PrivateRoute>
              }
            />
            <Route
              path="/master-inbox"
              element={
                <PrivateRoute>
                  <MasterInbox />
                </PrivateRoute>
              }
            />
            <Route
              path="/mailboxes"
              element={
                <PrivateRoute>
                  <Mailboxes />
                </PrivateRoute>
              }
            />
            <Route
              path="/settings"
              element={
                <PrivateRoute>
                  <>
                    <Topbar />
                    <Settings />
                  </>
                </PrivateRoute>
              }
            />

            {/* Public Routes */}
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
