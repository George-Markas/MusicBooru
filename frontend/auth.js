const AUTH_CONFIG = {
    baseUrl: 'http://localhost:80',
    timeout: 10000,
    maxAttempts: 5
};

// Input validation
function validateCredentials(username, password) {
    if (!username || username.length < 3 || username.length > 50) {
        return 'Username must be 3-50 characters';
    }
    if (!/^[a-zA-Z0-9._@-]+$/.test(username)) {
        return 'Username contains invalid characters';
    }
    if (!password || password.length < 8) {
        return 'Password must be at least 8 characters';
    }
    return null;
}

// Check if JWT is valid and not expired
function isValidToken(token) {
    if (!token) return false;

    try {
        const parts = token.split('.');
        if (parts.length !== 3) return false;

        const payload = JSON.parse(atob(parts[1]));
        const now = Math.floor(Date.now() / 1000);

        return !payload.exp || payload.exp > now;
    } catch (e) {
        return false;
    }
}

// Simple rate limiting
function checkRateLimit() {
    const attempts = JSON.parse(sessionStorage.getItem('loginAttempts') || '[]');
    const fiveMinutesAgo = Date.now() - (5 * 60 * 1000);
    const recentAttempts = attempts.filter(time => time > fiveMinutesAgo);

    return recentAttempts.length < AUTH_CONFIG.maxAttempts;
}

function recordFailedAttempt() {
    const attempts = JSON.parse(sessionStorage.getItem('loginAttempts') || '[]');
    attempts.push(Date.now());
    sessionStorage.setItem('loginAttempts', JSON.stringify(attempts));
}

// Main login function
async function login(username, password) {
    // Validate inputs
    /*
    const validationError = validateCredentials(username, password);
    if (validationError) {
        return { success: false, error: validationError };
    }
    */

    // // Check rate limit
    // if (!checkRateLimit()) {
    //     return { success: false, error: 'Too many attempts. Try again in 5 minutes.' };
    // }

    try {
        // Create request with timeout
        const controller = new AbortController();
        setTimeout(() => controller.abort(), AUTH_CONFIG.timeout);

        const response = await fetch(`${AUTH_CONFIG.baseUrl}/api/auth/authenticate`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                username: username.trim(),
                password: password
            }),
            signal: controller.signal
        });

        if (!response.ok) {
            recordFailedAttempt();
            console.log(response.status);
            switch (response.status) {
                case 401:
                    return { success: false, error: 'Invalid username or password' };
                case 429:
                    return { success: false, error: 'Too many attempts. Please wait.' };
                default:
                    return { success: false, error: 'Login failed. Please try again.' };
            }
        }

        const data = await response.json();

        if (!data.token || !isValidToken(data.token)) {
            return { success: false, error: 'Invalid response from server' };
        }

        // Store token
        sessionStorage.setItem('authToken', data.token);
        sessionStorage.removeItem('loginAttempts'); // Clear failed attempts

        console.log("Successfully logged in (Not)!");

        return { success: true, user: data.user };

    } catch (error) {
        recordFailedAttempt();

        if (error.name === 'AbortError') {
            return { success: false, error: 'Request timeout' };
        }

        return { success: false, error: 'Connection failed. Please try again.' };
    }
}

// Get stored token (with validation)
function getAuthToken() {
    const token = sessionStorage.getItem('authToken');

    if (!isValidToken(token)) {
        sessionStorage.removeItem('authToken');
        return null;
    }

    return token;
}

// Check if user is logged in
function isLoggedIn() {
    return !!getAuthToken();
}

// Logout function
function logout() {
    sessionStorage.removeItem('authToken');
    sessionStorage.removeItem('loginAttempts');
}

// Helper to make authenticated requests
async function fetchWithAuth(url, options = {}) {
    const token = getAuthToken();

    if (!token) {
        throw new Error('Not authenticated');
    }

    return fetch(url, {
        ...options,
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
            ...options.headers
        }
    });
}