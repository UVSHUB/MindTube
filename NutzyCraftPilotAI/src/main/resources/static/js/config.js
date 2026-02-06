// API Configuration
// Change this URL when deploying to different environments
const API_BASE_URL = 'http://209.97.161.131';
const AI_BASE_URL = 'http://209.97.161.131';

// Helper function to build API URLs
function getApiUrl(endpoint) {
    // Remove leading slash if present
    const cleanEndpoint = endpoint.startsWith('/') ? endpoint.slice(1) : endpoint;
    return `${API_BASE_URL}/${cleanEndpoint}`;
}

function getAiUrl(endpoint) {
    const cleanEndpoint = endpoint.startsWith('/') ? endpoint.slice(1) : endpoint;
    return `${AI_BASE_URL}/${cleanEndpoint}`;
}
