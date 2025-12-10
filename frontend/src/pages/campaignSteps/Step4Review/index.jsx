import { useState, useEffect } from "react";
import { Mail, Users, Clock, Calendar, Globe, Timer, ChevronRight, RefreshCw } from "lucide-react";
import { campaignApi } from "../../../services/campaignApi";

export default function Step4Review({
  campaignName,
  description,
  uploadedFile,
  emailSubject,
  emailContent,
  campaignSettings,
  campaignId,
  campaignContacts = []
}) {
  const [selectedContactIndex, setSelectedContactIndex] = useState(0);
  const [previewData, setPreviewData] = useState(null);
  const [loadingPreview, setLoadingPreview] = useState(false);
  const [contactPreviews, setContactPreviews] = useState({});

  // Debug: Log contacts when they change
  useEffect(() => {
    console.log("📋 Step4Review - campaignContacts updated:", campaignContacts);
    console.log("📋 Step4Review - campaignContacts length:", campaignContacts.length);
    console.log("📋 Step4Review - campaignId:", campaignId);
    if (campaignContacts.length > 0) {
      console.log("📋 Step4Review - First contact:", campaignContacts[0]);
      console.log("📋 Step4Review - Contact keys:", Object.keys(campaignContacts[0]));
    } else {
      console.warn("⚠️ Step4Review - No contacts found! Campaign ID:", campaignId);
    }
  }, [campaignContacts, campaignId]);

  // Load preview for selected contact
  useEffect(() => {
    if (campaignId && emailSubject && emailContent && campaignContacts.length > 0) {
      loadPreview(selectedContactIndex);
    }
  }, [campaignId, emailSubject, emailContent, selectedContactIndex]);

  // Preload previews for first few contacts
  useEffect(() => {
    if (campaignId && emailSubject && emailContent && campaignContacts.length > 0) {
      const contact = campaignContacts[selectedContactIndex];
      if (contact && !contactPreviews[contact.contactId]) {
        loadPreviewForContact(contact.contactId, selectedContactIndex);
      }
    }
  }, [campaignContacts, campaignId]);

  const loadPreview = async (index) => {
    if (!campaignId || !emailSubject || !emailContent || index >= campaignContacts.length) return;
    
    const contact = campaignContacts[index];
    if (!contact) return;

    // Get contact ID from various possible fields
    const contactId = contact.contactId || contact.id || contact.leadId || contact.lead_id;
    if (!contactId) {
      console.error("Contact has no ID:", contact);
      return;
    }

    // Use cached preview if available
    if (contactPreviews[contactId]) {
      setPreviewData(contactPreviews[contactId]);
      return;
    }

    setLoadingPreview(true);
    try {
      const seed = Date.now();
      const preview = await campaignApi.previewEmail(
        campaignId,
        emailSubject,
        emailContent,
        contactId,
        seed
      );
      setPreviewData(preview);
      setContactPreviews(prev => ({
        ...prev,
        [contactId]: preview
      }));
    } catch (error) {
      console.error("Failed to load preview:", error);
      setPreviewData(null);
    } finally {
      setLoadingPreview(false);
    }
  };

  const loadPreviewForContact = async (contactId, index) => {
    if (!campaignId || !emailSubject || !emailContent) return;
    
    try {
      const seed = Date.now();
      const preview = await campaignApi.previewEmail(
        campaignId,
        emailSubject,
        emailContent,
        contactId,
        seed
      );
      setContactPreviews(prev => ({
        ...prev,
        [contactId]: preview
      }));
    } catch (error) {
      console.error(`Failed to load preview for contact ${contactId}:`, error);
    }
  };

  const refreshPreview = () => {
    if (campaignContacts[selectedContactIndex]) {
      const contact = campaignContacts[selectedContactIndex];
      delete contactPreviews[contact.contactId];
      loadPreview(selectedContactIndex);
    }
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
    return `${h}h ${m}m`;
  };

  const formatDate = (dateStr, timeStr) => {
    if (!dateStr) return "Not set";
    try {
      // dateStr is YYYY-MM-DD, timeStr is HH:mm (in EST/EDT)
      // We just need to display what the user entered, not convert from UTC
      const [year, month, day] = dateStr.split('-').map(Number);
      const [hours, minutes] = (timeStr || "09:00").split(':').map(Number);
      
      // Create a date string in EST format for display
      const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
      const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
      
      // Create date object (will be in local timezone, but we'll format manually)
      const date = new Date(year, month - 1, day, hours, minutes);
      const dayName = dayNames[date.getDay()];
      const monthName = monthNames[month - 1];
      
      // Format time in 12-hour format
      const hour12 = hours % 12 || 12;
      const ampm = hours >= 12 ? 'PM' : 'AM';
      const minutesStr = String(minutes).padStart(2, '0');
      
      return `${dayName}, ${monthName} ${day}, ${year}, ${hour12}:${minutesStr} ${ampm} EST`;
    } catch (error) {
      console.error("Error formatting date:", error);
      // Fallback: just show date and time separately
      return `${dateStr} at ${timeStr || "09:00"} EST`;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Review Campaign</h2>
          <p className="text-sm text-gray-600 mt-1">
            Review your campaign details and preview personalized emails
          </p>
        </div>
      </div>

      {/* Campaign Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <div className="flex items-center gap-2 text-blue-600 mb-2">
            <Users size={20} />
            <span className="text-sm font-medium">Total Recipients</span>
          </div>
          <p className="text-2xl font-bold text-blue-900">{campaignContacts.length}</p>
        </div>
        <div className="bg-green-50 border border-green-200 rounded-lg p-4">
          <div className="flex items-center gap-2 text-green-600 mb-2">
            <Mail size={20} />
            <span className="text-sm font-medium">Email Template</span>
          </div>
          <p className="text-sm text-green-900">
            {emailSubject ? "Ready" : "Not set"}
          </p>
        </div>
        <div className="bg-purple-50 border border-purple-200 rounded-lg p-4">
          <div className="flex items-center gap-2 text-purple-600 mb-2">
            <Timer size={20} />
            <span className="text-sm font-medium">Delay Between</span>
          </div>
          <p className="text-sm text-purple-900">
            {formatDelay(campaignSettings.emailDelayMinutes || 5)}
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Left: Contacts List */}
        <div className="bg-white border border-gray-200 rounded-lg overflow-hidden flex flex-col" style={{ height: '600px' }}>
          <div className="p-4 border-b border-gray-200 bg-gray-50">
            <div className="flex items-center justify-between">
              <h3 className="font-semibold text-gray-800 flex items-center gap-2">
                <Users size={18} />
                Recipients ({campaignContacts.length})
              </h3>
              <button
                onClick={refreshPreview}
                className="text-xs text-blue-600 hover:text-blue-800 flex items-center gap-1"
              >
                <RefreshCw size={14} />
                Refresh
              </button>
            </div>
            <p className="text-xs text-gray-600 mt-1">
              Click a contact to preview their personalized email
            </p>
          </div>
          <div className="flex-1 overflow-y-auto">
            {campaignContacts.length === 0 ? (
              <div className="p-8 text-center text-gray-500">
                <Users size={48} className="mx-auto mb-2 text-gray-300" />
                <p className="mb-2">No contacts loaded</p>
                {campaignId && (
                  <p className="text-xs text-gray-400">
                    Campaign ID: {campaignId}<br/>
                    Make sure contacts were uploaded in Step 1
                  </p>
                )}
              </div>
            ) : (
              <div className="divide-y divide-gray-100">
                {campaignContacts.map((contact, index) => {
                  // Get contact ID from various possible fields
                  const contactId = contact.contactId || contact.id || contact.leadId || contact.lead_id || index;
                  const hasPreview = contactPreviews[contactId];
                  const isSelected = index === selectedContactIndex;
                  
                  // Get email from various possible fields
                  const contactEmail = contact.email || contact.contactEmail || contact.leadEmail || "No email";
                  
                  // Get name from various possible fields
                  const firstName = contact.firstName || contact.contactFirstName || contact.leadFirstName || contact.first_name;
                  const lastName = contact.lastName || contact.contactLastName || contact.leadLastName || contact.last_name;
                  const contactName = firstName && lastName
                    ? `${firstName} ${lastName}`.trim()
                    : firstName 
                      ? firstName
                      : contactEmail !== "No email" 
                        ? contactEmail.split('@')[0]
                        : `Contact ${index + 1}`;
                  
                  // Get company name
                  const companyName = contact.companyName || contact.company || contact.company_name || contact.leadCompany;

                  return (
                    <button
                      key={contactId}
                      onClick={() => setSelectedContactIndex(index)}
                      className={`w-full p-4 text-left hover:bg-blue-50 transition-colors ${
                        isSelected ? 'bg-blue-50 border-l-4 border-blue-600' : ''
                      }`}
                    >
                      <div className="flex items-start justify-between">
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 mb-1">
                            <p className={`font-medium truncate ${
                              isSelected ? 'text-blue-900' : 'text-gray-900'
                            }`}>
                              {contactName}
                            </p>
                            {hasPreview && (
                              <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded">
                                Preview Ready
                              </span>
                            )}
                          </div>
                          <p className="text-sm text-gray-600 truncate">{contactEmail}</p>
                          {companyName && (
                            <p className="text-xs text-gray-500 mt-1 truncate">
                              {companyName}
                            </p>
                          )}
                        </div>
                        <ChevronRight 
                          size={20} 
                          className={`flex-shrink-0 ml-2 ${
                            isSelected ? 'text-blue-600' : 'text-gray-400'
                          }`}
                        />
                      </div>
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* Right: Email Preview */}
        <div className="bg-white border border-gray-200 rounded-lg overflow-hidden flex flex-col" style={{ height: '600px' }}>
          <div className="p-4 border-b border-gray-200 bg-gray-50">
            <h3 className="font-semibold text-gray-800 flex items-center gap-2">
              <Mail size={18} />
              Email Preview
            </h3>
            {previewData?.lead && (
              <p className="text-xs text-gray-600 mt-1">
                Personalized for: {previewData.lead.firstName || ''} {previewData.lead.lastName || ''} 
                {previewData.lead.email ? ` (${previewData.lead.email})` : ''}
              </p>
            )}
          </div>
          <div className="flex-1 overflow-y-auto p-6">
            {loadingPreview ? (
              <div className="flex items-center justify-center h-full">
                <div className="text-center">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-2"></div>
                  <p className="text-sm text-gray-600">Loading preview...</p>
                </div>
              </div>
            ) : previewData ? (
              <div className="space-y-4">
                {/* Email Preview Container */}
                <div className="border border-gray-200 rounded-lg overflow-hidden shadow-sm">
                  {/* Email Header */}
                  <div className="bg-gray-50 border-b border-gray-200 px-4 py-3">
                    <div className="space-y-1">
                      <div className="text-xs text-gray-500">From:</div>
                      <div className="text-sm font-medium text-gray-900">
                        Your Mailbox
                      </div>
                    </div>
                    <div className="mt-2 space-y-1">
                      <div className="text-xs text-gray-500">To:</div>
                      <div className="text-sm text-gray-700">
                        {previewData.lead?.email || "recipient@example.com"}
                      </div>
                    </div>
                    <div className="mt-2 space-y-1">
                      <div className="text-xs text-gray-500">Subject:</div>
                      <div className="text-sm font-medium text-gray-900">
                        {previewData.subject || emailSubject || "(No subject)"}
                      </div>
                    </div>
                  </div>

                  {/* Email Body */}
                  <div className="bg-white p-6">
                    <div 
                      className="prose prose-sm max-w-none"
                      dangerouslySetInnerHTML={{ 
                        __html: previewData.bodyPreview || previewData.body || emailContent || "<p>No content</p>"
                      }}
                    />
                  </div>
                </div>
              </div>
            ) : (
              <div className="flex items-center justify-center h-full text-gray-500">
                <div className="text-center">
                  <Mail size={48} className="mx-auto mb-2 text-gray-300" />
                  <p>Select a contact to preview their email</p>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Campaign Settings Summary */}
      <div className="bg-gray-50 border border-gray-200 rounded-lg p-6">
        <h3 className="font-semibold text-gray-800 mb-4">Campaign Settings</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <div>
            <div className="flex items-center gap-2 text-gray-600 mb-1">
              <Calendar size={16} />
              <span className="text-sm font-medium">Start Date</span>
            </div>
            <p className="text-sm text-gray-900">
              {formatDate(campaignSettings.startDate, campaignSettings.startTime)}
            </p>
          </div>
          <div>
            <div className="flex items-center gap-2 text-gray-600 mb-1">
              <Clock size={16} />
              <span className="text-sm font-medium">Send Window</span>
            </div>
            <p className="text-sm text-gray-900">
              {campaignSettings.startTime || "09:00"} - {campaignSettings.endTime || "17:00"}
            </p>
          </div>
          <div>
            <div className="flex items-center gap-2 text-gray-600 mb-1">
              <Globe size={16} />
              <span className="text-sm font-medium">Timezone</span>
            </div>
            <p className="text-sm text-gray-900">
              EST/EDT
            </p>
          </div>
          <div>
            <div className="flex items-center gap-2 text-gray-600 mb-1">
              <Timer size={16} />
              <span className="text-sm font-medium">Delay Between</span>
            </div>
            <p className="text-sm text-gray-900">
              {formatDelay(campaignSettings.emailDelayMinutes || 5)}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

