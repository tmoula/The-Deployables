import { useState } from "react";
import { Settings, LogOut } from "lucide-react";

export default function Topbar() {
  const [showDropdown, setShowDropdown] = useState(false);
  const [user] = useState({
    name: "John Doe",
    email: "john.doe@techsolutions.com",
    initials: "JD"
  });

  return (
    <div className="bg-white shadow-sm border-b border-gray-200 p-4">
      <div className="flex items-center justify-end">
        <div className="relative">
          <button
            onClick={() => setShowDropdown(!showDropdown)}
            className="w-10 h-10 bg-gradient-to-br from-blue-500 to-purple-600 rounded-full flex items-center justify-center text-white font-semibold text-sm hover:ring-2 hover:ring-blue-300 transition-all shadow-md hover:shadow-lg cursor-pointer aspect-square"
            style={{ borderRadius: '50%' }}
          >
            {user.initials}
          </button>

          {showDropdown && (
            <>
              <div
                className="fixed inset-0 z-10"
                onClick={() => setShowDropdown(false)}
              ></div>
              <div className="absolute right-0 top-full mt-2 w-72 bg-white rounded-lg shadow-lg border border-gray-200 z-20">
                <div className="p-4 border-b border-gray-200">
                  <div className="flex items-center gap-3 mb-2">
                    <div className="w-12 h-12 bg-gradient-to-br from-blue-500 to-purple-600 rounded-full flex items-center justify-center text-white font-semibold aspect-square" style={{ borderRadius: '50%' }}>
                      {user.initials}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900">{user.name}</p>
                      <p className="text-xs text-gray-500 break-words">{user.email}</p>
                    </div>
                  </div>
                </div>
                <div className="py-2">
                  <a
                    href="/settings"
                    className="flex items-center gap-3 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 transition"
                    onClick={() => setShowDropdown(false)}
                  >
                    <Settings size={18} />
                    Settings
                  </a>
                  <button
                    className="flex items-center gap-3 px-4 py-2 text-sm text-red-600 hover:bg-red-50 w-full text-left transition"
                    onClick={() => {
                      setShowDropdown(false);
                      alert("Sign out functionality coming soon");
                    }}
                  >
                    <LogOut size={18} />
                    Sign Out
                  </button>
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
