import { Users, Mail, TrendingUp, Target } from "lucide-react";

export default function Dashboard() {
  const stats = [
    { label: "Total Leads", value: "0", icon: Users, color: "bg-blue-500" },
    { label: "Active Campaigns", value: "0", icon: Mail, color: "bg-green-500" },
    { label: "Conversion Rate", value: "0%", icon: TrendingUp, color: "bg-purple-500" },
    { label: "Matches Found", value: "0", icon: Target, color: "bg-orange-500" }
  ];

  return (
    <div className="p-6 bg-gray-50 min-h-screen">
      <h1 className="text-3xl font-bold mb-6 text-gray-800">Dashboard</h1>
      
      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        {stats.map((stat, idx) => (
          <div key={idx} className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-500 text-sm">{stat.label}</p>
                <p className="text-3xl font-bold mt-2">{stat.value}</p>
              </div>
              <div className={`${stat.color} p-3 rounded-lg`}>
                <stat.icon className="text-white" size={24} />
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Recent Activity */}
      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-xl font-semibold mb-4">Recent Activity</h2>
        <p className="text-gray-500">No recent activity to display</p>
      </div>
    </div>
  );
}
