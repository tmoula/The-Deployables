import { authService } from './authService';

// Campaign Service API
// Use the shared REACT_APP_API_URL (which is /api/v1 in prod) to ensure relative paths work through Ingress
const CAMPAIGN_API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8081/api/v1';

function getUserEmailHeaders(extraHeaders = {}) {
  const email = authService.getUserEmail();
  console.log('📧 getUserEmailHeaders - Retrieved email from authService:', email);
  const base = email ? { 'X-User-Email': email } : {};
  const headers = { ...base, ...extraHeaders };
  console.log('📧 getUserEmailHeaders - Final headers:', headers);
  return headers;
}

export const campaignApi = {
  // Health check
  async checkHealth() {
    // Check specific Campaign Service health endpoint
    const response = await fetch(`${CAMPAIGN_API_URL}/campaigns/health`);
    return response.json();
  },

  // Upload CSV file
  async uploadCsv(file, campaignName, description) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('campaignName', campaignName); // Required
    if (description) formData.append('description', description);

    const headers = getUserEmailHeaders();
    console.log('📤 Upload CSV - URL:', `${CAMPAIGN_API_URL}/campaigns/upload-csv`);
    console.log('📤 Upload CSV - Headers:', headers);
    console.log('📤 Upload CSV - Campaign Name:', campaignName);
    console.log('📤 Upload CSV - File:', file?.name, 'Size:', file?.size);

    const response = await fetch(`${CAMPAIGN_API_URL}/campaigns/upload-csv`, {
      method: 'POST',
      headers: headers, // browser will set correct Content-Type for FormData
      body: formData
    });

    console.log('📤 Upload CSV - Response status:', response.status);

    if (!response.ok) {
      const error = await response.json().catch(() => ({ error: response.statusText }));
      console.error('📤 Upload CSV - Error:', error);
      throw new Error(error.error || `Failed to upload CSV: ${response.statusText}`);
    }

    const data = await response.json();
    console.log('📤 Upload CSV - Success! Response:', data);
    return data;
  },

  // Get all campaigns
  async getAllCampaigns() {
    const headers = getUserEmailHeaders();
    console.log('📋 Fetching campaigns with headers:', headers);
    console.log('📋 Campaign API URL:', `${CAMPAIGN_API_URL}/campaigns`);

    const response = await fetch(`${CAMPAIGN_API_URL}/campaigns`, {
      headers: headers
    });

    console.log('📋 Campaigns response status:', response.status);

    if (!response.ok) {
      const errorText = await response.text();
      console.error('📋 Failed to fetch campaigns:', errorText);
      throw new Error(`Failed to fetch campaigns: ${response.statusText} - ${errorText}`);
    }

    const data = await response.json();
    console.log('📋 Received campaigns:', data);
    return data;
  },

  // Get campaign by ID
  async getCampaign(id) {
    const response = await fetch(`${CAMPAIGN_API_URL}/campaigns/${id}`);
    if (!response.ok) {
      throw new Error(`Failed to fetch campaign: ${response.statusText}`);
    }
    return response.json();
  },

  // Get campaign leads
  async getCampaignLeads(campaignId) {
    const response = await fetch(`${CAMPAIGN_API_URL}/campaigns/${campaignId}/leads`);
    if (!response.ok) {
      throw new Error(`Failed to fetch leads: ${response.statusText}`);
    }
    return response.json();
  },

  // Update campaign status
  async updateCampaignStatus(campaignId, status) {
    const response = await fetch(`${CAMPAIGN_API_URL}/campaigns/${campaignId}/status`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status })
    });

    if (!response.ok) {
      throw new Error(`Failed to update campaign status: ${response.statusText}`);
    }
    return response.json();
  },

  // Get contacts for a campaign
  async getCampaignContacts(campaignId) {
    const headers = getUserEmailHeaders({
      'Content-Type': 'application/json'
    });
    console.log('📋 Fetching campaign contacts for campaign:', campaignId);
    const response = await fetch(`${CAMPAIGN_API_URL}/campaigns/${campaignId}/contacts`, {
      method: 'GET',
      headers: headers
    });
    if (!response.ok) {
      const errorBody = await response.json().catch(() => ({ error: response.statusText }));
      console.error('❌ Failed to fetch contacts:', errorBody);
      throw new Error(`Failed to fetch contacts: ${errorBody.error || response.statusText}`);
    }
    const data = await response.json();
    console.log('✅ Fetched contacts:', data);
    return data;
  },

  // Get merge fields (CSV columns) for a campaign
  async getMergeFields(campaignId) {
    const response = await fetch(`${CAMPAIGN_API_URL}/campaigns/${campaignId}/merge-fields`);
    if (!response.ok) {
      throw new Error(`Failed to fetch merge fields: ${response.statusText}`);
    }
    return response.json();
  },

  // Preview email with sample lead data
  async previewEmail(campaignId, subject, body, leadId = null, spintaxSeed = null) {
    const url = new URL(`${CAMPAIGN_API_URL}/campaigns/${campaignId}/preview-email`);
    if (leadId) {
      url.searchParams.append('leadId', leadId);
    }
    // Pass spintax seed for rotation - each preview refresh gets different spintax selections
    // Ensure seed is an integer (backend expects Long, not decimal)
    if (spintaxSeed !== null) {
      const integerSeed = Math.floor(spintaxSeed);
      url.searchParams.append('spintaxSeed', integerSeed.toString());
    }

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...getUserEmailHeaders()
      },
      body: JSON.stringify({ subject, body })
    });

    if (!response.ok) {
      let errorMessage = `Failed to preview email: ${response.statusText}`;
      try {
        const error = await response.json();
        errorMessage = error.error || errorMessage;
        console.error('Preview email error response:', error);
      } catch (e) {
        const errorText = await response.text();
        console.error('Preview email error (non-JSON):', errorText);
        errorMessage = errorText || errorMessage;
      }
      throw new Error(errorMessage);
    }

    return response.json();
  },

  // Delete a campaign
  async deleteCampaign(campaignId) {
    const headers = getUserEmailHeaders();
    console.log('🗑️ Deleting campaign:', campaignId, 'with headers:', headers);

    const response = await fetch(`${CAMPAIGN_API_URL}/campaigns/${campaignId}`, {
      method: 'DELETE',
      headers: headers
    });

    console.log('🗑️ Delete response status:', response.status);

    if (!response.ok && response.status !== 204) {
      const error = await response.json().catch(() => ({ error: response.statusText }));
      console.error('🗑️ Delete error:', error);
      throw new Error(error.error || `Failed to delete campaign: ${response.statusText}`);
    }

    console.log('🗑️ Campaign deleted successfully');
  },

  // Save campaign email (subject and body)
  async saveCampaignEmail(campaignId, emailSubject, emailBody) {
    const headers = getUserEmailHeaders({
      'Content-Type': 'application/json'
    });

    console.log('💾 Saving email for campaign:', campaignId);

    const response = await fetch(`${CAMPAIGN_API_URL}/campaigns/${campaignId}/email`, {
      method: 'PUT',
      headers: headers,
      body: JSON.stringify({ emailSubject, emailBody })
    });

    console.log('💾 Save email response status:', response.status);

    if (!response.ok) {
      const error = await response.json().catch(() => ({ error: response.statusText }));
      console.error('💾 Save email error:', error);
      throw new Error(error.error || `Failed to save email: ${response.statusText}`);
    }

    const data = await response.json();
    console.log('💾 Email saved successfully:', data);
    return data;
  },

  // Schedule campaign with start date and mailboxes
  async scheduleCampaign(campaignId, scheduleSettings) {
    const headers = getUserEmailHeaders({
      'Content-Type': 'application/json'
    });

    console.log('📅 Scheduling campaign:', campaignId, scheduleSettings);

    const response = await fetch(`${CAMPAIGN_API_URL}/campaigns/${campaignId}/schedule`, {
      method: 'PUT',
      headers: headers,
      body: JSON.stringify(scheduleSettings)
    });

    console.log('📅 Schedule campaign response status:', response.status);

    if (!response.ok) {
      const error = await response.json().catch(() => ({ error: response.statusText }));
      console.error('📅 Schedule campaign error:', error);
      throw new Error(error.error || `Failed to schedule campaign: ${response.statusText}`);
    }

    const data = await response.json();
    console.log('📅 Campaign scheduled successfully:', data);
    return data;
  },

  async getCampaignStats(campaignId) {
    const headers = getUserEmailHeaders();
    const response = await fetch(`${CAMPAIGN_API_URL}/campaigns/${campaignId}/stats`, {
      headers: headers
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({ error: response.statusText }));
      throw new Error(error.error || `Failed to fetch campaign stats: ${response.statusText}`);
    }

    return response.json();
  },
};

