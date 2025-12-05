import { authService } from './authService';

// Campaign Service API
const CAMPAIGN_API_URL = process.env.REACT_APP_CAMPAIGN_API_URL || 'http://localhost:8081/api/v1';

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
    const response = await fetch(`${CAMPAIGN_API_URL}/health`);
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
    const response = await fetch(`${CAMPAIGN_API_URL}/campaigns/${campaignId}/contacts`);
    if (!response.ok) {
      throw new Error(`Failed to fetch contacts: ${response.statusText}`);
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
  }
};

