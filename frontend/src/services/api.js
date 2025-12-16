const isProd = process.env.NODE_ENV === 'production';

// Campaign Service (campaign-svc)
const CAMPAIGN_BASE_URL = isProd
  ? (process.env.REACT_APP_CAMPAIGN_API_URL || '/api/v1/campaigns')
  : (process.env.REACT_APP_CAMPAIGN_API_URL || 'http://localhost:8081/api/v1/campaigns');

// Lead Service (lead-svc)
const LEAD_BASE_URL = isProd
  ? (process.env.REACT_APP_LEAD_API_URL || '/api/v1')
  : (process.env.REACT_APP_LEAD_API_URL || 'http://localhost:8084/api/v1');
// Debug: Log the API URLs being used (check browser console)
if (process.env.NODE_ENV === 'development') {
  console.log('🔗 Campaign API Base URL:', CAMPAIGN_BASE_URL);
  console.log('🔗 Lead API Base URL:', LEAD_BASE_URL);
  console.log('📡 Campaign service should be running on: http://localhost:8081');
  console.log('📡 Lead service should be running on: http://localhost:8084');
}

export const api = {
  // Health check (campaign service)
  async checkHealth() {
    try {
      const response = await fetch(`${CAMPAIGN_BASE_URL}/health`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' }
      });
      if (!response.ok) {
        throw new Error(`Backend health check failed: ${response.status} ${response.statusText}`);
      }
      return response.json();
    } catch (error) {
      if (error.message.includes('Failed to fetch') || error.message.includes('NetworkError')) {
        throw new Error(`Cannot connect to campaign service at ${CAMPAIGN_BASE_URL}. Make sure it's running on http://localhost:8081`);
      }
      throw error;
    }
  },

  // Seller Profile (lead service)
  async setSeller(sellerData) {
    try {
      const headers = { 'Content-Type': 'application/json' };
      const userEmail = this.getUserEmail();
      if (userEmail) {
        headers['X-User-Email'] = userEmail;
      }

      const response = await fetch(`${LEAD_BASE_URL}/seller`, {
        method: 'PUT',
        headers: headers,
        body: JSON.stringify(sellerData)
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to set seller profile: ${response.status} ${response.statusText} - ${errorText}`);
      }
      return response.json();
    } catch (error) {
      if (error.message.includes('Failed to fetch') || error.message.includes('NetworkError')) {
        throw new Error(`Cannot connect to lead service at ${LEAD_BASE_URL}. Make sure it's running on http://localhost:8084`);
      }
      throw error;
    }
  },

  // Get Seller Profile (lead service)
  async getSeller() {
    try {
      const headers = { 'Content-Type': 'application/json' };
      const userEmail = this.getUserEmail();
      if (userEmail) {
        headers['X-User-Email'] = userEmail;
      }

      const response = await fetch(`${LEAD_BASE_URL}/seller`, {
        method: 'GET',
        headers: headers
      });
      if (!response.ok) {
        if (response.status === 404) {
          return null; // Seller profile not set yet
        }
        const errorText = await response.text();
        throw new Error(`Failed to get seller profile: ${response.status} ${response.statusText} - ${errorText}`);
      }
      return response.json();
    } catch (error) {
      if (error.message.includes('Failed to fetch') || error.message.includes('NetworkError')) {
        throw new Error(`Cannot connect to lead service at ${LEAD_BASE_URL}. Make sure it's running on http://localhost:8084`);
      }
      throw error;
    }
  },

  // Prospects (lead service)
  async listProspects() {
    try {
      const response = await fetch(`${LEAD_BASE_URL}/prospects`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' }
      });
      if (!response.ok) {
        throw new Error(`Failed to list prospects: ${response.status} ${response.statusText}`);
      }
      return response.json();
    } catch (error) {
      if (error.message.includes('Failed to fetch') || error.message.includes('NetworkError')) {
        throw new Error(`Cannot connect to lead service at ${LEAD_BASE_URL}. Make sure it's running on http://localhost:8084`);
      }
      throw error;
    }
  },

  async createProspect(prospectData) {
    try {
      const response = await fetch(`${LEAD_BASE_URL}/prospects`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(prospectData)
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to create prospect: ${response.status} ${response.statusText} - ${errorText}`);
      }
      return response.json();
    } catch (error) {
      if (error.message.includes('Failed to fetch') || error.message.includes('NetworkError')) {
        throw new Error(`Cannot connect to lead service at ${LEAD_BASE_URL}. Make sure it's running on http://localhost:8084`);
      }
      throw error;
    }
  },

  // Match (lead service) - Returns batch_id for async processing
  async matchProspects(criteria, limit = 5) {
    try {
      const headers = { 'Content-Type': 'application/json' };

      // Add user email header if available
      const userEmail = this.getUserEmail();
      if (userEmail) {
        headers['X-User-Email'] = userEmail;
      }

      const response = await fetch(`${LEAD_BASE_URL}/match?limit=${limit}`, {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(criteria)
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to match prospects: ${response.status} ${response.statusText} - ${errorText}`);
      }
      return response.json();
    } catch (error) {
      if (error.message.includes('Failed to fetch') || error.message.includes('NetworkError')) {
        throw new Error(`Cannot connect to lead service at ${LEAD_BASE_URL}. Make sure it's running on http://localhost:8084`);
      }
      throw error;
    }
  },

  // Get batch status
  async getBatchStatus(batchId) {
    try {
      const headers = {};
      const userEmail = this.getUserEmail();
      if (userEmail) {
        headers['X-User-Email'] = userEmail;
      }

      const response = await fetch(`${LEAD_BASE_URL}/lead-batches/${batchId}`, {
        method: 'GET',
        headers: headers
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to get batch status: ${response.status} ${response.statusText} - ${errorText}`);
      }
      return response.json();
    } catch (error) {
      if (error.message.includes('Failed to fetch') || error.message.includes('NetworkError')) {
        throw new Error(`Cannot connect to lead service at ${LEAD_BASE_URL}. Make sure it's running on http://localhost:8084`);
      }
      throw error;
    }
  },

  // Get leads for a batch
  async getBatchLeads(batchId) {
    try {
      const headers = {};
      const userEmail = this.getUserEmail();
      if (userEmail) {
        headers['X-User-Email'] = userEmail;
      }

      const response = await fetch(`${LEAD_BASE_URL}/lead-batches/${batchId}/leads`, {
        method: 'GET',
        headers: headers
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to get batch leads: ${response.status} ${response.statusText} - ${errorText}`);
      }
      return response.json();
    } catch (error) {
      if (error.message.includes('Failed to fetch') || error.message.includes('NetworkError')) {
        throw new Error(`Cannot connect to lead service at ${LEAD_BASE_URL}. Make sure it's running on http://localhost:8084`);
      }
      throw error;
    }
  },

  // Helper to get user email from auth service
  getUserEmail() {
    try {
      // Get email from localStorage (set by authService)
      return localStorage.getItem('user_email');
    } catch (error) {
      console.error('Error getting user email:', error);
      return null;
    }
  }
};
