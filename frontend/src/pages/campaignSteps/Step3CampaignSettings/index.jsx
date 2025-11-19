import { Clock, Globe, Mail, Calendar } from "lucide-react";
import CampaignTiming from "./CampaignTiming";
import TimezoneSettings from "./TimezoneSettings";
import SendTimeSettings from "./SendTimeSettings";
import MailboxSettings from "./MailboxSettings";

export default function Step3CampaignSettings({
  campaignSettings,
  setCampaignSettings
}) {
  return (
    <div className="space-y-8">
      <div>
        <h3 className="text-lg font-semibold text-gray-800 mb-6">
          Campaign Settings
        </h3>
        <p className="text-sm text-gray-600 mb-6">
          Configure when and how your campaign will run
        </p>
      </div>

      {/* Campaign Timing Section */}
      <div className="border border-gray-200 rounded-lg p-6">
        <div className="flex items-center gap-2 mb-4">
          <Calendar className="text-blue-600" size={20} />
          <h4 className="text-base font-semibold text-gray-800">
            Campaign Timing
          </h4>
        </div>
        <CampaignTiming
          campaignSettings={campaignSettings}
          setCampaignSettings={setCampaignSettings}
        />
      </div>

      {/* Timezone Section */}
      <div className="border border-gray-200 rounded-lg p-6">
        <div className="flex items-center gap-2 mb-4">
          <Globe className="text-blue-600" size={20} />
          <h4 className="text-base font-semibold text-gray-800">
            Timezone
          </h4>
        </div>
        <TimezoneSettings
          campaignSettings={campaignSettings}
          setCampaignSettings={setCampaignSettings}
        />
      </div>

      {/* Send Time Section */}
      <div className="border border-gray-200 rounded-lg p-6">
        <div className="flex items-center gap-2 mb-4">
          <Clock className="text-blue-600" size={20} />
          <h4 className="text-base font-semibold text-gray-800">
            Send Time
          </h4>
        </div>
        <SendTimeSettings
          campaignSettings={campaignSettings}
          setCampaignSettings={setCampaignSettings}
        />
      </div>

      {/* Mailbox Settings Section */}
      <div className="border border-gray-200 rounded-lg p-6">
        <div className="flex items-center gap-2 mb-4">
          <Mail className="text-blue-600" size={20} />
          <h4 className="text-base font-semibold text-gray-800">
            Mailboxes
          </h4>
        </div>
        <MailboxSettings
          campaignSettings={campaignSettings}
          setCampaignSettings={setCampaignSettings}
        />
      </div>
    </div>
  );
}

