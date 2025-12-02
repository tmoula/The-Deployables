// Authentication Service - Handles all auth-related API calls
const AUTH_BASE_URL = process.env.REACT_APP_AUTH_URL || 'http://localhost:8083/api/v1/auth';

// Debug logging in development
if (process.env.NODE_ENV === 'development') {
  console.log('🔐 Auth Service URL:', AUTH_BASE_URL);
}

// Token management
const TOKEN_KEY = 'auth_token';
const USER_EMAIL_KEY = 'user_email';

export const authService = {
  /**
   * Register a new user
   * @param {string} email - User's email address
   * @param {string} password - User's password (min 8 characters)
   * @param {string} firstName - User's first name
   * @param {string} lastName - User's last name
   * @returns {Promise<Object>} User object
   */
  async register(email, password, firstName, lastName) {
    try {
      const response = await fetch(`${AUTH_BASE_URL}/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          email,
          password,
          firstName,
          lastName,
        }),
      });

      if (!response.ok) {
        const errorText = await response.text();
        if (response.status === 400) {
          throw new Error('Invalid registration data. Please check your inputs.');
        }
        throw new Error(`Registration failed: ${errorText || response.statusText}`);
      }

      const user = await response.json();
      console.log('✅ Registration successful:', user.email);
      return user;
    } catch (error) {
      if (error.message.includes('Failed to fetch') || error.message.includes('NetworkError')) {
        throw new Error(`Cannot connect to auth service at ${AUTH_BASE_URL}. Make sure the backend is running on http://localhost:8083`);
      }
      throw error;
    }
  },

  /**
   * Login with email and password
   * @param {string} email - User's email address
   * @param {string} password - User's password
   * @returns {Promise<Object>} Auth token response
   */
  async login(email, password) {
    try {
      const response = await fetch(`${AUTH_BASE_URL}/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          email,
          password,
        }),
      });

      if (!response.ok) {
        const errorText = await response.text();
        if (response.status === 401 || response.status === 400) {
          throw new Error('Invalid email or password');
        }
        throw new Error(`Login failed: ${errorText || response.statusText}`);
      }

      const authResponse = await response.json();

      // Store token and user details
      this.setToken(authResponse.accessToken);
      this.setUserEmail(authResponse.email);
      this.setUserName(authResponse.firstName, authResponse.lastName);

      console.log('✅ Login successful:', authResponse.email);
      return authResponse;
    } catch (error) {
      if (error.message.includes('Failed to fetch') || error.message.includes('NetworkError')) {
        throw new Error(`Cannot connect to auth service at ${AUTH_BASE_URL}. Make sure the backend is running on http://localhost:8083`);
      }
      throw error;
    }
  },

  /**
   * Validate the current token
   * @returns {Promise<boolean>} True if token is valid
   */
  async validateToken() {
    const token = this.getToken();
    if (!token) {
      return false;
    }

    try {
      const response = await fetch(`${AUTH_BASE_URL}/validate`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        return false;
      }

      const isValid = await response.json();
      return isValid;
    } catch (error) {
      console.error('Token validation error:', error);
      return false;
    }
  },

  /**
   * Verify user's email with a code
   * @param {string} email - User's email address
   * @param {string} code - Verification code
   * @returns {Promise<boolean>} True if verification is successful
   */
  async verify(email, code) {
    try {
      const response = await fetch(`${AUTH_BASE_URL}/verify`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, code }),
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Verification failed');
      }

      console.log('✅ Email verification successful for:', email);
      return true;
    } catch (error) {
      if (error.message.includes('Failed to fetch') || error.message.includes('NetworkError')) {
        throw new Error(`Cannot connect to auth service at ${AUTH_BASE_URL}. Make sure the backend is running on http://localhost:8083`);
      }
      throw error;
    }
  },

  /**
   * Logout the current user
   */
  logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_EMAIL_KEY);
    localStorage.removeItem('user_name');
    console.log('🔓 User logged out');
  },

  /**
   * Store authentication token
   * @param {string} token - JWT token
   */
  setToken(token) {
    localStorage.setItem(TOKEN_KEY, token);
  },

  /**
   * Get stored authentication token
   * @returns {string|null} JWT token or null
   */
  getToken() {
    return localStorage.getItem(TOKEN_KEY);
  },

  /**
   * Store user email
   * @param {string} email - User's email
   */
  setUserEmail(email) {
    localStorage.setItem(USER_EMAIL_KEY, email);
  },

  /**
   * Get stored user email
   * @returns {string|null} User's email or null
   */
  getUserEmail() {
    return localStorage.getItem(USER_EMAIL_KEY);
  },

  /**
   * Store user name
   * @param {string} firstName 
   * @param {string} lastName 
   */
  setUserName(firstName, lastName) {
    if (firstName && lastName) {
      localStorage.setItem('user_name', `${firstName} ${lastName}`);
    }
  },

  /**
   * Get stored user name
   * @returns {string|null} User's full name or null
   */
  getUserName() {
    return localStorage.getItem('user_name');
  },

  /**
   * Check if user is authenticated
   * @returns {boolean} True if user has a token
   */
  isAuthenticated() {
    return !!this.getToken();
  },
};
