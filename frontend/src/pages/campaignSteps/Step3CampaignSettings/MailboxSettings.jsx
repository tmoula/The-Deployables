import { Mail, CheckCircle, AlertCircle, Info } from "lucide-react";

export default function MailboxSettings({
  campaignSettings,
  setCampaignSettings
}) {
  // Mock mailboxes - these will be fetched from the mailboxes tab later
  const availableMailboxes = [
    { id: "1", name: "Sales Team Mailbox", email: "sales@company.com", status: "active" },
    { id: "2", name: "Marketing Outreach", email: "marketing@company.com", status: "active" },
    { id: "3", name: "Business Development", email: "bizdev@company.com", status: "active" },
    { id: "4", name: "Partnerships", email: "partners@company.com", status: "paused" },
  ];

  const selectedMailboxIds = campaignSettings.selectedMailboxIds || [];

  const toggleMailboxSelection = (mailboxId) => {
    const currentSelection = selectedMailboxIds || [];
    const newSelection = currentSelection.includes(mailboxId)
      ? currentSelection.filter(id => id !== mailboxId)
      : [...currentSelection, mailboxId];
    
    setCampaignSettings({
      ...campaignSettings,
      selectedMailboxIds: newSelection
    });
  };

  return (
    <div className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Select Mailboxes for This Campaign
        </label>
        <p className="text-xs text-gray-500 mb-4">
          Choose which mailboxes will be used to send emails for this campaign. 
          Emails will be rotated across selected mailboxes.
        </p>

        {/* Mailboxes List */}
        {availableMailboxes.length > 0 ? (
          <div className="space-y-2">
            {availableMailboxes.map((mailbox) => {
              const isSelected = selectedMailboxIds.includes(mailbox.id);
              return (
                <div
                  key={mailbox.id}
                  onClick={() => toggleMailboxSelection(mailbox.id)}
                  className={`flex items-center gap-3 p-4 border-2 rounded-lg cursor-pointer transition-all ${
                    isSelected
                      ? 'border-blue-500 bg-blue-50 shadow-sm'
                      : 'border-gray-200 bg-white hover:border-gray-300 hover:bg-gray-50'
                  }`}
                >
                  {/* Checkbox */}
                  <div className={`flex-shrink-0 w-5 h-5 rounded border-2 flex items-center justify-center transition-all ${
                    isSelected
                      ? 'border-blue-600 bg-blue-600'
                      : 'border-gray-300 bg-white'
                  }`}>
                    {isSelected && (
                      <CheckCircle size={14} className="text-white" />
                    )}
                  </div>

                  {/* Mailbox Icon */}
                  <Mail 
                    size={20} 
                    className={`flex-shrink-0 ${
                      isSelected ? 'text-blue-600' : 'text-gray-400'
                    }`} 
                  />

                  {/* Mailbox Info */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <span className={`text-sm font-medium ${
                        isSelected ? 'text-gray-900' : 'text-gray-700'
                      }`}>
                        {mailbox.name}
                      </span>
                      {mailbox.status === 'active' ? (
                        <CheckCircle size={14} className="text-green-500" />
                      ) : (
                        <AlertCircle size={14} className="text-yellow-500" />
                      )}
                    </div>
                    <span className={`text-xs ${
                      isSelected ? 'text-gray-600' : 'text-gray-500'
                    }`}>
                      {mailbox.email}
                    </span>
                  </div>

                  {/* Status Badge */}
                  <div className={`px-2 py-1 rounded text-xs font-medium ${
                    mailbox.status === 'active'
                      ? 'bg-green-100 text-green-700'
                      : 'bg-yellow-100 text-yellow-700'
                  }`}>
                    {mailbox.status === 'active' ? 'Active' : 'Paused'}
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
            <div className="flex items-start gap-2">
              <AlertCircle size={16} className="text-yellow-600 mt-0.5 flex-shrink-0" />
              <p className="text-xs text-yellow-800">
                No mailboxes available. Add mailboxes in the Mailboxes tab first.
              </p>
            </div>
          </div>
        )}

        {/* Selection Summary */}
        {selectedMailboxIds.length > 0 && (
          <div className="mt-4 pt-4 border-t border-gray-200">
            <div className="flex items-center justify-between bg-blue-50 border border-blue-200 rounded-lg p-3">
              <div className="flex items-center gap-2">
                <CheckCircle size={16} className="text-blue-600" />
                <span className="text-sm font-medium text-gray-700">
                  {selectedMailboxIds.length} {selectedMailboxIds.length === 1 ? 'mailbox' : 'mailboxes'} selected
                </span>
              </div>
              <span className="text-xs text-blue-600 font-medium">
                Emails will rotate across selected mailboxes
              </span>
            </div>
          </div>
        )}
      </div>

      {/* Info Box */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
        <div className="flex items-start gap-2">
          <Info size={16} className="text-blue-600 mt-0.5 flex-shrink-0" />
          <div className="text-xs text-blue-800">
            <p className="font-medium mb-1">About Mailbox Selection:</p>
            <ul className="list-disc list-inside space-y-0.5">
              <li>Select multiple mailboxes to distribute sending load</li>
              <li>Emails will be automatically rotated across selected mailboxes</li>
              <li>Only active mailboxes are recommended for use</li>
              <li>Manage mailboxes in the Mailboxes tab</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}

