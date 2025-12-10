import { Clock, Info, AlertCircle } from "lucide-react";
import { useState } from "react";

export default function EmailDelaySettings({
  campaignSettings,
  setCampaignSettings
}) {
  const [showInfo, setShowInfo] = useState(false);
  
  // Get delay in minutes (default 5 minutes minimum)
  const delayMinutes = campaignSettings.emailDelayMinutes || 5;
  
  // Calculate hours and minutes from total minutes
  const hours = Math.floor(delayMinutes / 60);
  const remainingMinutes = delayMinutes % 60;

  const handleDelayChange = (newHours, newMinutes) => {
    const totalMinutes = (newHours * 60) + newMinutes;
    // Enforce minimum of 5 minutes
    const finalMinutes = Math.max(5, totalMinutes);
    
    setCampaignSettings({
      ...campaignSettings,
      emailDelayMinutes: finalMinutes
    });
  };

  const formatDelay = (minutes) => {
    if (minutes < 60) {
      return `${minutes} minute${minutes !== 1 ? 's' : ''}`;
    }
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    if (m === 0) {
      return `${h} hour${h !== 1 ? 's' : ''}`;
    }
    return `${h} hour${h !== 1 ? 's' : ''} ${m} minute${m !== 1 ? 's' : ''}`;
  };

  return (
    <div className="space-y-4">
      <div className="bg-gradient-to-br from-blue-50 to-indigo-50 border border-blue-200 rounded-xl p-6 shadow-sm">
        <div className="flex items-center gap-2 mb-4">
          <div className="w-10 h-10 rounded-lg bg-blue-600 flex items-center justify-center">
            <Clock className="text-white" size={20} />
          </div>
          <div className="flex-1">
            <h4 className="text-base font-semibold text-gray-800">
              Delay Between Emails
            </h4>
            <p className="text-xs text-gray-600">
              Minimum 5 minutes required to avoid rate limiting
            </p>
          </div>
          <div className="relative">
            <Info
              size={16}
              className="text-gray-400 cursor-help"
              onMouseEnter={() => setShowInfo(true)}
              onMouseLeave={() => setShowInfo(false)}
            />
            {showInfo && (
              <div className="absolute right-0 top-6 w-72 bg-gray-800 text-white text-xs rounded-lg p-3 z-10 shadow-lg">
                <p className="mb-2">
                  <strong>Why delay between emails?</strong>
                </p>
                <ul className="list-disc list-inside space-y-1 text-gray-300">
                  <li>Prevents email provider rate limiting</li>
                  <li>Reduces risk of spam detection</li>
                  <li>Maintains sender reputation</li>
                  <li>Minimum 5 minutes ensures safe sending</li>
                </ul>
              </div>
            )}
          </div>
        </div>

        {/* Current Delay Display */}
        <div className="bg-white rounded-lg p-4 border border-blue-200 mb-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-700">Current Delay</p>
              <p className="text-2xl font-bold text-blue-600 mt-1">
                {formatDelay(delayMinutes)}
              </p>
            </div>
            {delayMinutes < 5 && (
              <div className="flex items-center gap-2 text-red-600 bg-red-50 px-3 py-2 rounded-lg">
                <AlertCircle size={16} />
                <span className="text-xs font-medium">Minimum 5 minutes</span>
              </div>
            )}
          </div>
        </div>

        {/* Time Input Controls */}
        <div className="grid grid-cols-2 gap-4">
          {/* Hours */}
          <div className="space-y-2">
            <label className="block text-sm font-medium text-gray-700">
              Hours
            </label>
            <div className="relative">
              <input
                type="number"
                min="0"
                max="23"
                value={hours}
                onChange={(e) => {
                  const newHours = parseInt(e.target.value) || 0;
                  handleDelayChange(Math.max(0, Math.min(23, newHours)), remainingMinutes);
                }}
                className="w-full border-2 border-gray-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm font-medium bg-white"
              />
            </div>
          </div>

          {/* Minutes */}
          <div className="space-y-2">
            <label className="block text-sm font-medium text-gray-700">
              Minutes
            </label>
            <div className="relative">
              <input
                type="number"
                min="0"
                max="59"
                value={remainingMinutes}
                onChange={(e) => {
                  let newMinutes = parseInt(e.target.value) || 0;
                  newMinutes = Math.max(0, Math.min(59, newMinutes));
                  
                  // Ensure total is at least 5 minutes
                  const total = (hours * 60) + newMinutes;
                  if (total < 5 && hours === 0) {
                    newMinutes = 5;
                  }
                  
                  handleDelayChange(hours, newMinutes);
                }}
                className="w-full border-2 border-gray-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm font-medium bg-white"
              />
            </div>
          </div>
        </div>

        {/* Quick Presets */}
        <div className="mt-4 pt-4 border-t border-blue-200">
          <p className="text-xs font-medium text-gray-600 mb-2">Quick Presets:</p>
          <div className="flex flex-wrap gap-2">
            {[5, 10, 15, 30, 60].map((presetMinutes) => (
              <button
                key={presetMinutes}
                type="button"
                onClick={() => {
                  setCampaignSettings({
                    ...campaignSettings,
                    emailDelayMinutes: presetMinutes
                  });
                }}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                  delayMinutes === presetMinutes
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                {presetMinutes === 60 ? '1 hour' : `${presetMinutes} min`}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Best Practices Info */}
      <div className="bg-gradient-to-r from-blue-50 to-indigo-50 border-l-4 border-blue-500 rounded-lg p-4">
        <div className="flex items-start gap-3">
          <Info className="text-blue-600 flex-shrink-0 mt-0.5" size={18} />
          <div className="flex-1">
            <p className="text-sm font-semibold text-gray-800 mb-1">
              Recommended Delays
            </p>
            <ul className="text-xs text-gray-700 space-y-1 list-disc list-inside">
              <li><strong>5-10 minutes:</strong> Safe for most email providers (Gmail, Outlook)</li>
              <li><strong>15-30 minutes:</strong> Conservative approach, best for new accounts</li>
              <li><strong>1+ hour:</strong> Maximum safety for high-volume campaigns</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}

