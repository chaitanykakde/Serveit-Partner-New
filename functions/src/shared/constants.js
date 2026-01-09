/**
 * Shared Constants for Serveit Partner Backend
 * 
 * These constants are used across multiple Cloud Functions.
 * Environment-specific values should be configured via Firebase Functions config.
 */

const functions = require("firebase-functions");

// Fallback coordinates for Kranti Chowk, Chhatrapati Sambhajinagar, Maharashtra
// TODO: Move to Firebase Functions config for environment-specific values
const FALLBACK_COORDINATES = {
  latitude: 19.8762,
  longitude: 75.3433,
};

// Search radius for geo-query (broad search)
const GEO_QUERY_RADIUS = 200; // kilometers (increased for testing)
// Final filter radius for precise road distance
const FINAL_DISTANCE_LIMIT = 200; // kilometers (increased for testing to allow more providers)

// Helper function to get API key with backward compatibility
function getGoogleMapsApiKey() {
  // Try environment variable first (for new params system)
  if (process.env.GOOGLE_MAPS_API_KEY) {
    return process.env.GOOGLE_MAPS_API_KEY;
  }
  // Fallback to old config for backward compatibility
  try {
    const configValue = functions.config().google?.maps_api_key;
    if (configValue) {
      return configValue;
    }
  } catch (e) {
    // Config not available, continue
  }
  return "";
}

module.exports = {
  FALLBACK_COORDINATES,
  GEO_QUERY_RADIUS,
  FINAL_DISTANCE_LIMIT,
  getGoogleMapsApiKey,
};

