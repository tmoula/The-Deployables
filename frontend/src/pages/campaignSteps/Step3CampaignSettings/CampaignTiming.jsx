import { Calendar, Info } from "lucide-react";
import { useState } from "react";

export default function CampaignTiming({
  campaignSettings,
  setCampaignSettings
}) {
  const [showInfo, setShowInfo] = useState(false);

  return (
    <div className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Campaign Start Date
        </label>
        <input
          type="date"
          value={campaignSettings.startDate || ""}
          onChange={(e) =>
            setCampaignSettings({
              ...campaignSettings,
              startDate: e.target.value
            })
          }
          className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Campaign End Date
        </label>
        <input
          type="date"
          value={campaignSettings.endDate || ""}
          onChange={(e) =>
            setCampaignSettings({
              ...campaignSettings,
              endDate: e.target.value
            })
          }
          min={campaignSettings.startDate || ""}
          className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      <div>
        <div className="flex items-center gap-2 mb-2">
          <label className="block text-sm font-medium text-gray-700">
            Active Days of Week
          </label>
          <div className="relative">
            <Info
              size={14}
              className="text-gray-400 cursor-help"
              onMouseEnter={() => setShowInfo(true)}
              onMouseLeave={() => setShowInfo(false)}
            />
            {showInfo && (
              <div className="absolute left-0 top-6 w-64 bg-gray-800 text-white text-xs rounded-lg p-2 z-10 shadow-lg">
                Select which days of the week emails should be sent. Emails will only be sent on selected days.
              </div>
            )}
          </div>
        </div>
        <div className="grid grid-cols-7 gap-2">
          {[
            { key: 'monday', label: 'Mon' },
            { key: 'tuesday', label: 'Tue' },
            { key: 'wednesday', label: 'Wed' },
            { key: 'thursday', label: 'Thu' },
            { key: 'friday', label: 'Fri' },
            { key: 'saturday', label: 'Sat' },
            { key: 'sunday', label: 'Sun' }
          ].map((day) => (
            <button
              key={day.key}
              type="button"
              onClick={() => {
                const activeDays = campaignSettings.activeDays || [];
                const newActiveDays = activeDays.includes(day.key)
                  ? activeDays.filter(d => d !== day.key)
                  : [...activeDays, day.key];
                setCampaignSettings({
                  ...campaignSettings,
                  activeDays: newActiveDays
                });
              }}
              className={`px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                (campaignSettings.activeDays || []).includes(day.key)
                  ? 'bg-blue-600 text-white hover:bg-blue-700'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              {day.label}
            </button>
          ))}
        </div>
      </div>

      <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
        <p className="text-xs text-blue-800">
          <strong>Tip:</strong> Select specific days to avoid sending emails on weekends or holidays. 
          Emails will only be sent on the days you select.
        </p>
      </div>
    </div>
  );
}

