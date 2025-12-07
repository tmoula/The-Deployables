// Mailbox API service
const AUTH_BASE_URL = process.env.REACT_APP_AUTH_URL || 'http://localhost:8083/api/v1/auth';

// Helper to get auth token from localStorage
const getAuthToken = () => {
    return localStorage.getItem('auth_token');
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
            const text = await response.text();
            try {
                const error = JSON.parse(text);
                throw new Error(error.error || `Failed to fetch mailboxes: ${response.status} ${response.statusText}`);
            } catch (e) {
                throw new Error(`Failed to fetch mailboxes: ${response.status} ${response.statusText} - ${text.substring(0, 100)}`);
            }
        }

        return response.json();
    },

    /**
     * Add a new Gmail mailbox
     */
    async addGmail(mailboxData) {
        const token = getAuthToken();
        const userId = getUserId();
        console.log('🔌 Connecting Mailbox...');
        console.log('🔑 Token:', token ? `Present (${token.substring(0, 10)}...)` : 'MISSING');
        console.log('Pf User ID:', userId);

        const response = await fetch(`${AUTH_BASE_URL}/mailboxes`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`,
                'X-User-Id': userId
            },
            body: JSON.stringify(mailboxData)
        });

        if (!response.ok) {
            const text = await response.text();
            try {
                const error = JSON.parse(text);
                throw new Error(error.error || 'Failed to add mailbox');
            } catch (e) {
                throw new Error(`Failed to add mailbox: ${response.status} ${response.statusText} - ${text.substring(0, 100)}`);
            }
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
    },

    /**
     * Get all emails for Master Inbox
     */
    async getAllEmails() {
        const response = await fetch(`${AUTH_BASE_URL}/inbox/all`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getAuthToken()}`,
                'X-User-Id': getUserId()
            }
        });

        if (!response.ok) {
            throw new Error(`Failed to fetch emails: ${response.statusText}`);
        }

        return response.json();
    }
};
