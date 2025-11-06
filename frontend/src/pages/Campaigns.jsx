import { Mail, Play, Pause, CheckCircle } from "lucide-react";

export default function Campaigns() {
  const campaigns = [
    { id: 1, name: "Q4 Outreach", status: "draft", sent: 0, opened: 0, replied: 0 },
  ];

  const getStatusColor = (status) => {
    switch(status) {
      case 'running': return 'bg-green-100 text-green-800';
      case 'paused': return 'bg-yellow-100 text-yellow-800';
      case 'completed': return 'bg-blue-100 text-blue-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <div className="p-6 bg-gray-50 min-h-screen">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-800">Campaigns</h1>
        <button className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg flex items-center gap-2 transition">
          <Mail size={20} />
          New Campaign
        </button>
      </div>

      <div className="grid gap-4">
        {campaigns.length === 0 ? (
          <div className="bg-white rounded-lg shadow p-8 text-center text-gray-500">
            No campaigns yet. Create your first campaign to get started.
          </div>
        ) : (
          campaigns.map((campaign) => (
            <div key={campaign.id} className="bg-white rounded-lg shadow p-6">
              <div className="flex justify-between items-start mb-4">
                <div>
                  <h3 className="text-xl font-semibold">{campaign.name}</h3>
                  <span className={`inline-block mt-2 px-3 py-1 rounded-full text-sm ${getStatusColor(campaign.status)}`}>
                    {campaign.status}
                  </span>
                </div>
                <div className="flex gap-2">
                  <button className="p-2 hover:bg-gray-100 rounded">
                    <Play size={20} className="text-green-600" />
                  </button>
                  <button className="p-2 hover:bg-gray-100 rounded">
                    <Pause size={20} className="text-orange-600" />
                  </button>
                </div>
              </div>
              
              <div className="grid grid-cols-3 gap-4 mt-4">
                <div>
                  <p className="text-gray-500 text-sm">Sent</p>
                  <p className="text-2xl font-bold">{campaign.sent}</p>
                </div>
                <div>
                  <p className="text-gray-500 text-sm">Opened</p>
                  <p className="text-2xl font-bold">{campaign.opened}</p>
                </div>
                <div>
                  <p className="text-gray-500 text-sm">Replied</p>
                  <p className="text-2xl font-bold">{campaign.replied}</p>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}