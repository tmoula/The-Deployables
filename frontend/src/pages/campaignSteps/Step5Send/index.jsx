import { useState, useEffect } from "react";
import { Send, Mail, CheckCircle, XCircle, Clock, Users, AlertCircle, RefreshCw } from "lucide-react";
import { campaignApi } from "../../../services/campaignApi";

export default function Step5Send({
  campaignName,
  campaignId,
  campaignContacts = [],
  campaignSettings,
  emailSubject,
  emailContent,
  isEditing = false,
  onSaveCampaign,
  onSendComplete
}) {
  const [sending, setSending] = useState(false);
  const [sendStatus, setSendStatus] = useState(null); // 'pending', 'sending', 'sent', 'failed'
  const [recipientStatuses, setRecipientStatuses] = useState({});
  const [sendProgress, setSendProgress] = useState({ sent: 0, total: 0 });

  useEffect(() => {
    // Initialize recipient statuses
    if (campaignContacts.length > 0) {
      const initialStatuses = {};
      campaignContacts.forEach(contact => {
        const contactId = contact.contactId || contact.id;
        initialStatuses[contactId] = {
          status: 'pending',
          email: contact.email || contact.contactEmail || 'No email',
          name: contact.firstName || contact.contactFirstName 
            ? `${contact.firstName || contact.contactFirstName} ${contact.lastName || contact.contactLastName || ""}`.trim()
            : (contact.email || contact.contactEmail || '').split('@')[0],
          error: null
        };
      });
      setRecipientStatuses(initialStatuses);
      setSendProgress({ sent: 0, total: campaignContacts.length });
    }
  }, [campaignContacts]);

  const handleSendCampaign = async () => {
    // If no campaign ID and we have a save function, save the campaign first
    let finalCampaignId = campaignId;
    
    if (!finalCampaignId && onSaveCampaign) {
      try {
        setSending(true);
        setSendStatus('sending');
        finalCampaignId = await onSaveCampaign();
        if (!finalCampaignId) {
          alert("Failed to save campaign. Please try again.");
          setSending(false);
          setSendStatus(null);
          return;
        }
      } catch (error) {
        console.error("Failed to save campaign:", error);
        alert(`Failed to save campaign: ${error.message}`);
        setSending(false);
        setSendStatus(null);
        return;
      }
    }

    if (!finalCampaignId) {
      alert("Campaign ID is missing. Please go back and save the campaign first.");
      return;
    }

    if (campaignContacts.length === 0) {
      alert("No recipients found. Please go back and upload contacts.");
      return;
    }

    if (!campaignSettings.selectedMailboxIds || campaignSettings.selectedMailboxIds.length === 0) {
      alert("No mailboxes selected. Please go back to Campaign Settings and select at least one mailbox.");
      return;
    }

    const confirmed = window.confirm(
      `Are you sure you want to send this campaign to ${campaignContacts.length} recipients?\n\n` +
      `This will start sending emails according to your schedule and delay settings.`
    );

    if (!confirmed) {
      if (!campaignId) {
        setSending(false);
        setSendStatus(null);
      }
      return;
    }

    if (!sending) {
      setSending(true);
    }
    setSendStatus('sending');

    try {
      // Save email templates if not already saved
      if (emailSubject || emailContent) {
        try {
          await campaignApi.saveCampaignEmail(finalCampaignId, emailSubject, emailContent);
          console.log('✅ Email templates saved');
        } catch (error) {
          console.error("Warning: Failed to save email templates:", error);
          // Continue anyway
        }
      }

      // Convert EST to UTC for startDate
      let startDateUTC = null;
      if (campaignSettings.startDate) {
        const dateStr = campaignSettings.startDate; // YYYY-MM-DD
        const timeStr = campaignSettings.startTime || "09:00"; // HH:mm
        
        const [year, month, day] = dateStr.split('-').map(Number);
        const [hours, minutes] = timeStr.split(':').map(Number);
        const monthIndex = month - 1;
        const isDST = (monthIndex >= 2 && monthIndex <= 9) || 
                     (monthIndex === 2 && day >= 14) || 
                     (monthIndex === 10 && day <= 7);
        const estOffsetHours = isDST ? -4 : -5;
        const utcHours = hours - estOffsetHours;
        
        let utcDay = day, utcMonth = monthIndex, utcYear = year, finalHours = utcHours;
        if (finalHours >= 24) {
          finalHours -= 24;
          utcDay++;
          const daysInMonth = new Date(year, month, 0).getDate();
          if (utcDay > daysInMonth) {
            utcDay = 1;
            utcMonth++;
            if (utcMonth >= 12) {
              utcMonth = 0;
              utcYear++;
            }
          }
        } else if (finalHours < 0) {
          finalHours += 24;
          utcDay--;
          if (utcDay < 1) {
            utcMonth--;
            if (utcMonth < 0) {
              utcMonth = 11;
              utcYear--;
            }
            const daysInPrevMonth = new Date(utcYear, utcMonth + 1, 0).getDate();
            utcDay = daysInPrevMonth;
          }
        }
        
        const utcDate = new Date(Date.UTC(utcYear, utcMonth, utcDay, finalHours, minutes, 0));
        startDateUTC = utcDate.toISOString();
      } else {
        // Default to current time
        startDateUTC = new Date().toISOString();
      }

      // Schedule the campaign (this triggers the scheduler which will execute it)
      await campaignApi.scheduleCampaign(finalCampaignId, {
        startDate: startDateUTC,
        selectedMailboxIds: campaignSettings.selectedMailboxIds || [],
        emailDelayMinutes: campaignSettings.emailDelayMinutes || 5
      });

      // Update statuses - simulate progression (in real implementation, you'd poll the API)
      setSendStatus('sent');
      
      // Mark all as queued (they'll be sent according to schedule)
      const updatedStatuses = { ...recipientStatuses };
      Object.keys(updatedStatuses).forEach(contactId => {
        if (updatedStatuses[contactId].status === 'pending') {
          updatedStatuses[contactId].status = 'queued';
        }
      });
      setRecipientStatuses(updatedStatuses);

      alert(`Campaign "${campaignName}" has been scheduled successfully!\n\n` +
            `Emails will be sent according to your schedule and delay settings.`);

      if (onSendComplete) {
        onSendComplete();
      }
    } catch (error) {
      console.error("Failed to send campaign:", error);
      setSendStatus('failed');
      alert(`Failed to send campaign: ${error.message}`);
    } finally {
      setSending(false);
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'sent':
        return <CheckCircle size={16} className="text-green-600" />;
      case 'failed':
        return <XCircle size={16} className="text-red-600" />;
      case 'queued':
        return <Clock size={16} className="text-blue-600" />;
      case 'sending':
        return <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>;
      default:
        return <Clock size={16} className="text-gray-400" />;
    }
  };

  const getStatusText = (status) => {
    switch (status) {
      case 'sent':
        return 'Sent';
      case 'failed':
        return 'Failed';
      case 'queued':
        return 'Queued';
      case 'sending':
        return 'Sending...';
      default:
        return 'Pending';
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'sent':
        return 'bg-green-50 text-green-700 border-green-200';
      case 'failed':
        return 'bg-red-50 text-red-700 border-red-200';
      case 'queued':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'sending':
        return 'bg-yellow-50 text-yellow-700 border-yellow-200';
      default:
        return 'bg-gray-50 text-gray-700 border-gray-200';
    }
  };

  const pendingCount = Object.values(recipientStatuses).filter(r => r.status === 'pending').length;
  const queuedCount = Object.values(recipientStatuses).filter(r => r.status === 'queued').length;
  const sentCount = Object.values(recipientStatuses).filter(r => r.status === 'sent').length;
  const failedCount = Object.values(recipientStatuses).filter(r => r.status === 'failed').length;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-2xl font-bold text-gray-900">Send Campaign</h2>
        <p className="text-sm text-gray-600 mt-1">
          Review recipients and trigger the campaign to start sending
        </p>
      </div>

      {/* Campaign Summary */}
      <div className="bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-200 rounded-lg p-6">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-semibold text-gray-900">{campaignName}</h3>
            <p className="text-sm text-gray-600 mt-1">
              {campaignContacts.length} recipient{campaignContacts.length !== 1 ? 's' : ''} ready to receive emails
            </p>
          </div>
          <div className="flex items-center gap-4">
            {sendStatus === 'sent' && (
              <div className="flex items-center gap-2 text-green-700 bg-white px-4 py-2 rounded-lg border border-green-200">
                <CheckCircle size={20} />
                <span className="font-medium">Campaign Scheduled</span>
              </div>
            )}
            <button
              onClick={handleSendCampaign}
              disabled={sending || sendStatus === 'sent'}
              className={`px-6 py-3 rounded-lg font-medium transition-all flex items-center gap-2 ${
                sending || sendStatus === 'sent'
                  ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                  : 'bg-blue-600 hover:bg-blue-700 text-white shadow-lg hover:shadow-xl'
              }`}
            >
              {sending ? (
                <>
                  <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                  <span>Scheduling...</span>
                </>
              ) : sendStatus === 'sent' ? (
                <>
                  <CheckCircle size={20} />
                  <span>Scheduled</span>
                </>
              ) : (
                <>
                  <Send size={20} />
                  <span>Schedule & Send Campaign</span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Status Summary */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white border border-gray-200 rounded-lg p-4">
          <div className="flex items-center gap-2 text-gray-600 mb-1">
            <Clock size={16} />
            <span className="text-sm font-medium">Pending</span>
          </div>
          <p className="text-2xl font-bold text-gray-900">{pendingCount}</p>
        </div>
        <div className="bg-white border border-blue-200 rounded-lg p-4">
          <div className="flex items-center gap-2 text-blue-600 mb-1">
            <Mail size={16} />
            <span className="text-sm font-medium">Queued</span>
          </div>
          <p className="text-2xl font-bold text-blue-900">{queuedCount}</p>
        </div>
        <div className="bg-white border border-green-200 rounded-lg p-4">
          <div className="flex items-center gap-2 text-green-600 mb-1">
            <CheckCircle size={16} />
            <span className="text-sm font-medium">Sent</span>
          </div>
          <p className="text-2xl font-bold text-green-900">{sentCount}</p>
        </div>
        <div className="bg-white border border-red-200 rounded-lg p-4">
          <div className="flex items-center gap-2 text-red-600 mb-1">
            <XCircle size={16} />
            <span className="text-sm font-medium">Failed</span>
          </div>
          <p className="text-2xl font-bold text-red-900">{failedCount}</p>
        </div>
      </div>

      {/* Recipients List */}
      <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
        <div className="p-4 border-b border-gray-200 bg-gray-50">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold text-gray-800 flex items-center gap-2">
              <Users size={18} />
              Recipients ({campaignContacts.length})
            </h3>
          </div>
        </div>
        <div className="max-h-96 overflow-y-auto">
          {campaignContacts.length === 0 ? (
            <div className="p-8 text-center text-gray-500">
              <Users size={48} className="mx-auto mb-2 text-gray-300" />
              <p>No recipients found</p>
            </div>
          ) : (
            <div className="divide-y divide-gray-100">
              {campaignContacts.map((contact, index) => {
                const contactId = contact.contactId || contact.id || index;
                const recipientStatus = recipientStatuses[contactId] || {
                  status: 'pending',
                  email: contact.email || contact.contactEmail || 'No email',
                  name: contact.firstName || contact.contactFirstName 
                    ? `${contact.firstName || contact.contactFirstName} ${contact.lastName || contact.contactLastName || ""}`.trim()
                    : (contact.email || contact.contactEmail || '').split('@')[0],
                  error: null
                };

                return (
                  <div
                    key={contactId}
                    className="p-4 hover:bg-gray-50 transition-colors"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-3">
                          {getStatusIcon(recipientStatus.status)}
                          <div className="flex-1 min-w-0">
                            <p className="font-medium text-gray-900 truncate">
                              {recipientStatus.name}
                            </p>
                            <p className="text-sm text-gray-600 truncate">
                              {recipientStatus.email}
                            </p>
                            {recipientStatus.error && (
                              <p className="text-xs text-red-600 mt-1">
                                {recipientStatus.error}
                              </p>
                            )}
                          </div>
                        </div>
                      </div>
                      <div className="ml-4">
                        <span className={`px-3 py-1 rounded-full text-xs font-medium border ${getStatusColor(recipientStatus.status)}`}>
                          {getStatusText(recipientStatus.status)}
                        </span>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* Info Box */}
      {sendStatus === 'sent' && (
        <div className="bg-blue-50 border-l-4 border-blue-500 rounded-lg p-4">
          <div className="flex items-start gap-3">
            <AlertCircle className="text-blue-600 flex-shrink-0 mt-0.5" size={20} />
            <div>
              <h4 className="font-semibold text-blue-900 mb-1">Campaign Scheduled Successfully!</h4>
              <p className="text-sm text-blue-800">
                Your campaign has been scheduled and will start sending emails according to your settings:
              </p>
              <ul className="text-sm text-blue-800 mt-2 list-disc list-inside space-y-1">
                <li>Start date: {campaignSettings.startDate ? new Date(campaignSettings.startDate).toLocaleString() : 'Immediately'}</li>
                <li>Delay between emails: {campaignSettings.emailDelayMinutes || 5} minutes</li>
                <li>Send window: {campaignSettings.startTime || "09:00"} - {campaignSettings.endTime || "17:00"}</li>
              </ul>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

