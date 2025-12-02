// Campaign Service API
const CAMPAIGN_API_URL = process.env.REACT_APP_CAMPAIGN_API_URL || 'http://localhost:8082/api/v1';

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

    const response = await fetch(`${CAMPAIGN_API_URL}/campaigns/upload-csv`, {
      method: 'POST',
      body: formData
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || `Failed to upload CSV: ${response.statusText}`);
    }

    return response.json();
  },

  // Get all campaigns
  async getAllCampaigns() {
    const response = await fetch(`${CAMPAIGN_API_URL}/campaigns`);
    if (!response.ok) {
      throw new Error(`Failed to fetch campaigns: ${response.statusText}`);
    }
    return response.json();
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

  // Generate email using AI service
  async generateEmail(campaignId, contactId, emailRequirements = {}) {
    const response = await fetch(
      `${CAMPAIGN_API_URL}/campaigns/${campaignId}/contacts/${contactId}/generate-email`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(emailRequirements)
      }
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || `Failed to generate email: ${response.statusText}`);
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
  }
};

