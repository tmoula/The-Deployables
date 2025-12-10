import { Clock, Info } from "lucide-react";
import { useState, useEffect } from "react";
import DatePicker from "react-datepicker";
import { format, parse, setHours, setMinutes, startOfDay } from "date-fns";
import "react-datepicker/dist/react-datepicker.css";

export default function SendTimeSettings({
  campaignSettings,
  setCampaignSettings
}) {
  // Helper to create a date with specific time
  const createTimeDate = (timeString) => {
    try {
      const [hours, minutes] = timeString.split(':').map(Number);
      return setMinutes(setHours(startOfDay(new Date()), hours), minutes);
    } catch {
      return setMinutes(setHours(startOfDay(new Date()), 9), 0);
    }
  };

  // Parse existing times or use defaults
  const defaultStartTime = campaignSettings.startTime || "09:00";
  const defaultEndTime = campaignSettings.endTime || "17:00";

  const [localStartTime, setLocalStartTime] = useState(createTimeDate(defaultStartTime));
  const [localEndTime, setLocalEndTime] = useState(createTimeDate(defaultEndTime));

  // Update local times when campaignSettings change
  useEffect(() => {
    if (campaignSettings.startTime) {
      setLocalStartTime(createTimeDate(campaignSettings.startTime));
    }
  }, [campaignSettings.startTime]);

  useEffect(() => {
    if (campaignSettings.endTime) {
      setLocalEndTime(createTimeDate(campaignSettings.endTime));
    }
  }, [campaignSettings.endTime]);

  const handleStartTimeChange = (date) => {
    if (date) {
      setLocalStartTime(date);
      const timeString = format(date, "HH:mm");
      setCampaignSettings({
        ...campaignSettings,
        startTime: timeString
      });
      // If start time is after end time, update end time
      if (date > localEndTime) {
        const newEndTime = setMinutes(setHours(date, date.getHours() + 1), date.getMinutes());
        setLocalEndTime(newEndTime);
        setCampaignSettings({
          ...campaignSettings,
          startTime: timeString,
          endTime: format(newEndTime, "HH:mm")
        });
      }
    }
  };

  const handleEndTimeChange = (date) => {
    if (date) {
      setLocalEndTime(date);
      const timeString = format(date, "HH:mm");
      setCampaignSettings({
        ...campaignSettings,
        endTime: timeString
      });
      // If end time is before start time, update start time
      if (date < localStartTime) {
        const newStartTime = setMinutes(setHours(date, date.getHours() - 1), date.getMinutes());
        setLocalStartTime(newStartTime);
        setCampaignSettings({
          ...campaignSettings,
          startTime: format(newStartTime, "HH:mm"),
          endTime: timeString
        });
      }
    }
  };

  // Format time for display
  const formatTimeForDisplay = (timeString) => {
    try {
      const time = parse(timeString, "HH:mm", new Date());
      return format(time, "h:mm aa");
    } catch {
      return timeString;
    }
  };

  // Create min/max time constraints (full day range)
  const minTime = startOfDay(new Date());
  const maxTime = setMinutes(setHours(startOfDay(new Date()), 23), 59);

  return (
    <div className="space-y-6">
      {/* Time Range Selector */}
      <div className="bg-gradient-to-br from-blue-50 to-indigo-50 border border-blue-200 rounded-xl p-6 shadow-sm">
        <div className="flex items-center gap-2 mb-4">
          <div className="w-10 h-10 rounded-lg bg-blue-600 flex items-center justify-center">
            <Clock className="text-white" size={20} />
          </div>
          <div>
            <h4 className="text-base font-semibold text-gray-800">
              Send Time Window
            </h4>
              <p className="text-xs text-gray-600">
              Set the time range when emails can be sent (EST/EDT)
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Start Time */}
          <div className="space-y-2">
            <label className="block text-sm font-medium text-gray-700">
              Start Time
            </label>
            <div className="relative">
              <DatePicker
                selected={localStartTime}
                onChange={handleStartTimeChange}
                showTimeSelect
                showTimeSelectOnly
                timeIntervals={15}
                timeCaption="Time"
                dateFormat="h:mm aa"
                className="w-full border-2 border-gray-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm font-medium bg-white shadow-sm transition-all"
                placeholderText="Select start time"
                wrapperClassName="w-full"
                minTime={minTime}
                maxTime={maxTime}
              />
              <div className="absolute right-3 top-1/2 transform -translate-y-1/2 pointer-events-none">
                <Clock size={18} className="text-gray-400" />
              </div>
            </div>
            {campaignSettings.startTime && (
              <p className="text-xs text-gray-500">
                Starts at {formatTimeForDisplay(campaignSettings.startTime)}
              </p>
            )}
          </div>

          {/* End Time */}
          <div className="space-y-2">
            <label className="block text-sm font-medium text-gray-700">
              End Time
            </label>
            <div className="relative">
              <DatePicker
                selected={localEndTime}
                onChange={handleEndTimeChange}
                showTimeSelect
                showTimeSelectOnly
                timeIntervals={15}
                timeCaption="Time"
                dateFormat="h:mm aa"
                className="w-full border-2 border-gray-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm font-medium bg-white shadow-sm transition-all"
                placeholderText="Select end time"
                wrapperClassName="w-full"
                minTime={minTime}
                maxTime={maxTime}
              />
              <div className="absolute right-3 top-1/2 transform -translate-y-1/2 pointer-events-none">
                <Clock size={18} className="text-gray-400" />
              </div>
            </div>
            {campaignSettings.endTime && (
              <p className="text-xs text-gray-500">
                Ends at {formatTimeForDisplay(campaignSettings.endTime)}
              </p>
            )}
          </div>
        </div>

        {/* Time Range Display */}
        {campaignSettings.startTime && campaignSettings.endTime && (
          <div className="mt-6 pt-4 border-t border-blue-200">
            <div className="flex items-center justify-between bg-white rounded-lg p-4 border border-blue-200 shadow-sm">
              <div className="flex items-center gap-2">
                <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse"></div>
                <span className="text-sm font-medium text-gray-700">
                  Active Window
                </span>
              </div>
              <span className="text-sm font-semibold text-blue-600">
                {formatTimeForDisplay(campaignSettings.startTime)} - {formatTimeForDisplay(campaignSettings.endTime)}
              </span>
            </div>
          </div>
        )}
      </div>

      {/* Best Practices Info */}
      <div className="bg-gradient-to-r from-blue-50 to-indigo-50 border-l-4 border-blue-500 rounded-lg p-4">
        <div className="flex items-start gap-3">
          <Info className="text-blue-600 flex-shrink-0 mt-0.5" size={18} />
          <div className="flex-1">
            <p className="text-sm font-semibold text-gray-800 mb-1">
              Optimal Send Times
            </p>
            <ul className="text-xs text-gray-700 space-y-1 list-disc list-inside">
              <li>Morning window: 9:00 AM - 11:00 AM (highest open rates)</li>
              <li>Afternoon window: 2:00 PM - 4:00 PM (good engagement)</li>
              <li>Avoid early mornings (before 8 AM) and late evenings (after 6 PM)</li>
              <li>Consider your target audience's timezone when setting windows</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
