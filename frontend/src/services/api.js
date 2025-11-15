// Base URL - Backend runs on port 8081
const BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8081/api/v1';

// Debug: Log the API URL being used (check browser console)
if (process.env.NODE_ENV === 'development') {
  console.log('🔗 API Base URL:', BASE_URL);
  console.log('📡 Backend should be running on: http://localhost:8081');
}

export const api = {
  // Health check
  async checkHealth() {
    try {
      const response = await fetch(`${BASE_URL}/health`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' }
      });
      if (!response.ok) {
        throw new Error(`Backend health check failed: ${response.status} ${response.statusText}`);
      }
      return response.json();
    } catch (error) {
      if (error.message.includes('Failed to fetch') || error.message.includes('NetworkError')) {
        throw new Error(`Cannot connect to backend at ${BASE_URL}. Make sure the backend is running on http://localhost:8081`);
      }
      throw error;
    }
  },

  // Seller Profile
  async setSeller(sellerData) {
    try {
      const response = await fetch(`${BASE_URL}/seller`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(sellerData)
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to set seller profile: ${response.status} ${response.statusText} - ${errorText}`);
      }
      return response.json();
    } catch (error) {
      if (error.message.includes('Failed to fetch') || error.message.includes('NetworkError')) {
        throw new Error(`Cannot connect to backend at ${BASE_URL}. Make sure the backend is running on http://localhost:8081`);
      }
      throw error;
    }
  },

  // Prospects
  async listProspects() {
    const response = await fetch(`${BASE_URL}/prospects`);
    return response.json();
  },

  async createProspect(prospectData) {
    const response = await fetch(`${BASE_URL}/prospects`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(prospectData)
    });
    if (!response.ok) {
      throw new Error(`Failed to create prospect: ${response.statusText}`);
    }
    return response.json();
  },

  // Match
  async matchProspects(criteria, limit = 20) {
    try {
      const response = await fetch(`${BASE_URL}/match?limit=${limit}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(criteria)
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to match prospects: ${response.status} ${response.statusText} - ${errorText}`);
      }
      return response.json();
    } catch (error) {
      if (error.message.includes('Failed to fetch') || error.message.includes('NetworkError')) {
        throw new Error(`Cannot connect to backend at ${BASE_URL}. Make sure the backend is running on http://localhost:8081`);
      }
      throw error;
    }
  }
};
