import { BrowserRouter, Routes, Route, useLocation } from "react-router-dom";
import { createContext, useContext, useState, useEffect } from "react";
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
import TourGuide, { shouldRunTour } from "./components/TourGuide";
import { TourProvider, useTour } from "./contexts/TourContext";


// Create context for sidebar state
const SidebarContext = createContext();

export const useSidebar = () => useContext(SidebarContext);

function AppContent() {
  const location = useLocation();
  const { runTour, startTour, stopTour } = useTour();
  const [tourStarted, setTourStarted] = useState(false);

  // Check if tour should run on dashboard (first time user)
  useEffect(() => {
    // Only show tour on dashboard for authenticated users
    if (location.pathname === '/' && shouldRunTour() && !tourStarted) {
      // Small delay to ensure page is fully rendered
      const timer = setTimeout(() => {
        startTour();
        setTourStarted(true);
      }, 1000);
      return () => clearTimeout(timer);
    }
  }, [location.pathname, tourStarted, startTour]);

  // Listen for manual tour trigger (from Settings)
  useEffect(() => {
    const handleTriggerTour = () => {
      if (location.pathname === '/') {
        startTour();
      }
    };

    window.addEventListener('triggerTour', handleTriggerTour);
    return () => window.removeEventListener('triggerTour', handleTriggerTour);
  }, [location.pathname, startTour]);

  const handleTourComplete = () => {
    stopTour();
  };

  return (
    <SidebarContext.Provider value={{}}>
      <div className="w-full">
        <Sidebar />
        {/* Main content area - offset by sidebar width (256px = 64 * 4px) */}
        <div className="ml-64 min-h-screen">
          {/* Tour Guide - Shows on first login */}
          <TourGuide run={runTour} onComplete={handleTourComplete} />
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
      <TourProvider>
        <AppContent />
      </TourProvider>
    </BrowserRouter>
  );
}
