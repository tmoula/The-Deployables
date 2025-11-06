// Base URL will change based on environment
const BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8081/api/v1';

export const api = {
  // Health check
  async checkHealth() {
    const response = await fetch(`${BASE_URL}/health`);
    return response.json();
  },

  // Seller Profile
  async setSeller(sellerData) {
    const response = await fetch(`${BASE_URL}/seller`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(sellerData)
    });
    return response.json();
  },

  // Prospects
  async listProspects() {
    const response = await fetch(`${BASE_URL}/prospects`);
    return response.json();
  },

  // Match
  async matchProspects(criteria, limit = 20) {
    const response = await fetch(`${BASE_URL}/match?limit=${limit}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(criteria)
    });
    return response.json();
  }
};
