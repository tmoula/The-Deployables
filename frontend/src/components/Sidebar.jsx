import { useState, useEffect } from "react";
import { NavLink, useLocation, useNavigate } from "react-router-dom";
import { LayoutDashboard, Users, Mail, Settings, LogIn, LogOut, ChevronDown, ChevronRight, Inbox, Mailbox } from "lucide-react";
import { authService } from "../services/authService";

export default function Sidebar() {
  const location = useLocation();
  const navigate = useNavigate();
  const [campaignsExpanded, setCampaignsExpanded] = useState(false);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  // Check authentication state on mount and when location changes
  useEffect(() => {
    setIsAuthenticated(authService.isAuthenticated());
  }, [location]);

  // Auto-expand campaigns section if on campaigns-related page
  useEffect(() => {
    if (location.pathname.startsWith('/campaigns') || location.pathname.startsWith('/master-inbox') || location.pathname.startsWith('/mailboxes')) {
      setCampaignsExpanded(true);
    }
  }, [location.pathname]);

  const handleLogout = () => {
    authService.logout();
    navigate('/login');
  };

  const linkStyle = ({ isActive }) =>
    `flex items-center gap-3 p-3 rounded-md cursor-pointer transition-all whitespace-nowrap ${isActive
      ? "bg-blue-600 text-white"
      : "text-gray-300 hover:bg-gray-800 hover:text-white"
    }`;

  const subLinkStyle = ({ isActive }) =>
    `flex items-center gap-2 py-1.5 pl-8 pr-3 rounded-md cursor-pointer transition-all whitespace-nowrap text-sm ${isActive
      ? "bg-blue-500/80 text-white font-medium"
      : "text-gray-400 hover:bg-gray-800/50 hover:text-gray-200"
    }`;

  const isCampaignsActive = location.pathname.startsWith('/campaigns') ||
    location.pathname.startsWith('/master-inbox') ||
    location.pathname.startsWith('/mailboxes');

  return (
    <>
      {/* Sidebar - Always visible */}
      <div
        className="fixed left-0 top-0 bottom-0 bg-gray-900 text-white z-40 w-64 shadow-xl"
      >
        <div className="p-6 h-full flex flex-col">
          <h2 className="text-xl font-bold mb-8 text-center">
            Outreach AI
          </h2>

          <nav className="space-y-2 flex-1">
            <NavLink to="/" className={linkStyle}>
              <LayoutDashboard size={20} className="flex-shrink-0" />
              <span>Home</span>
            </NavLink>

            <NavLink to="/leads" className={linkStyle}>
              <Users size={20} className="flex-shrink-0" />
              <span>Your Future Clients</span>
            </NavLink>

            {/* Collapsible Campaigns Section */}
            <div>
              <button
                onClick={() => setCampaignsExpanded(!campaignsExpanded)}
                className={`w-full flex items-center gap-3 p-3 rounded-md cursor-pointer transition-all whitespace-nowrap ${isCampaignsActive
                    ? "bg-blue-600 text-white"
                    : "text-gray-300 hover:bg-gray-800 hover:text-white"
                  }`}
              >
                <Mail size={20} className="flex-shrink-0" />
                <span className="flex-1 text-left">Campaigns</span>
                {campaignsExpanded ? (
                  <ChevronDown size={16} className="flex-shrink-0" />
                ) : (
                  <ChevronRight size={16} className="flex-shrink-0" />
                )}
              </button>

              {/* Sub-menu items */}
              {campaignsExpanded && (
                <div className="ml-2 mt-1 space-y-0.5 border-l-2 border-gray-700/50 pl-2">
                  <NavLink to="/master-inbox" className={subLinkStyle}>
                    <Inbox size={14} className="flex-shrink-0" />
                    <span className="transition-opacity duration-300 opacity-100">
                      Master Inbox
                    </span>
                  </NavLink>
                  <NavLink to="/mailboxes" className={subLinkStyle}>
                    <Mailbox size={14} className="flex-shrink-0" />
                    <span className="transition-opacity duration-300 opacity-100">
                      Mailboxes
                    </span>
                  </NavLink>
                  <NavLink to="/campaigns" className={subLinkStyle}>
                    <Mail size={14} className="flex-shrink-0" />
                    <span className="transition-opacity duration-300 opacity-100">
                      Campaigns
                    </span>
                  </NavLink>
                </div>
              )}
            </div>

            <NavLink to="/settings" className={linkStyle}>
              <Settings size={20} className="flex-shrink-0" />
              <span>Settings</span>
            </NavLink>
          </nav>

          {/* Login/Logout button at the bottom */}
          <div className="mt-auto pt-4 border-t border-gray-700">
            {isAuthenticated ? (
              <button
                onClick={handleLogout}
                className="flex items-center gap-3 p-3 rounded-md cursor-pointer transition-all whitespace-nowrap text-gray-300 hover:bg-gray-800 hover:text-white w-full"
              >
                <LogOut size={20} className="flex-shrink-0" />
                <span>Logout</span>
              </button>
            ) : (
              <NavLink to="/login" className={linkStyle}>
                <LogIn size={20} className="flex-shrink-0" />
                <span>Login</span>
              </NavLink>
            )}
          </div>
        </div>
      </div>
    </>
  );
}
