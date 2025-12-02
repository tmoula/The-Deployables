// Mailbox API service
const AUTH_BASE_URL = process.env.REACT_APP_AUTH_URL || 'http://localhost:8083/api/v1/auth';

// Helper to get auth token from localStorage
const getAuthToken = () => {
    return localStorage.getItem('token');
};

// Helper to get user ID (temporary - in production this comes from JWT)
const getUserId = () => {
    return localStorage.getItem('userId') || '1';
};

export const mailboxApi = {
    /**
     * Get all mailboxes for the current user
     */
    async getMailboxes() {
        const response = await fetch(`${AUTH_BASE_URL}/mailboxes`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getAuthToken()}`,
                'X-User-Id': getUserId()
            }
        });

        if (!response.ok) {
            throw new Error(`Failed to fetch mailboxes: ${response.statusText}`);
        }

        return response.json();
    },

    /**
     * Add a new Gmail mailbox
     */
    async addGmail(mailboxData) {
        const response = await fetch(`${AUTH_BASE_URL}/mailboxes`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getAuthToken()}`,
                'X-User-Id': getUserId()
            },
            body: JSON.stringify(mailboxData)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Failed to add mailbox');
        }

        return response.json();
    },

    /**
     * Remove a mailbox
     */
    async removeMailbox(id) {
        const response = await fetch(`${AUTH_BASE_URL}/mailboxes/${id}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${getAuthToken()}`,
                'X-User-Id': getUserId()
            }
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Failed to remove mailbox');
        }
    },

    /**
     * Update mailbox status (pause/resume)
     */
    async updateStatus(id, status) {
        const response = await fetch(`${AUTH_BASE_URL}/mailboxes/${id}/status`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getAuthToken()}`,
                'X-User-Id': getUserId()
            },
            body: JSON.stringify({ status })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Failed to update status');
        }

        return response.json();
    },

    /**
     * Test mailbox connection
     */
    async testConnection(id) {
        const response = await fetch(`${AUTH_BASE_URL}/mailboxes/${id}/test`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${getAuthToken()}`,
                'X-User-Id': getUserId()
            }
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Failed to test connection');
        }

        return response.json();
    }
};
