/**
 * Environment Configuration
 * 
 * Centralized configuration for DEV/STAGE/PROD environments
 * Uses Firebase Functions config with environment variable fallbacks
 */

const functions = require("firebase-functions");

/**
 * Get environment (DEV, STAGE, PROD)
 */
function getEnvironment() {
  // Check environment variable first
  if (process.env.ENVIRONMENT) {
    return process.env.ENVIRONMENT.toUpperCase();
  }
  
  // Check Firebase Functions config
  try {
    const env = functions.config().env?.environment;
    if (env) {
      return env.toUpperCase();
    }
  } catch (e) {
    // Config not available
  }
  
  // Default to DEV
  return "DEV";
}

/**
 * Check if running in production
 */
function isProduction() {
  return getEnvironment() === "PRODUCTION" || getEnvironment() === "PROD";
}

/**
 * Check if running in development
 */
function isDevelopment() {
  return getEnvironment() === "DEV" || getEnvironment() === "DEVELOPMENT";
}

/**
 * Get Google Maps API key
 */
function getGoogleMapsApiKey() {
  // Try environment variable first
  if (process.env.GOOGLE_MAPS_API_KEY) {
    return process.env.GOOGLE_MAPS_API_KEY;
  }
  
  // Fallback to Firebase Functions config
  try {
    const configValue = functions.config().google?.maps_api_key;
    if (configValue) {
      return configValue;
    }
  } catch (e) {
    // Config not available
  }
  
  return "";
}

/**
 * Get Agora credentials
 */
function getAgoraCredentials() {
  const appId = process.env.AGORA_APP_ID || functions.config().agora?.app_id;
  const appCertificate = process.env.AGORA_APP_CERTIFICATE || functions.config().agora?.app_certificate;
  
  return {
    appId: appId || "",
    appCertificate: appCertificate || ""
  };
}

/**
 * Get collection names (allows environment-specific overrides)
 */
function getCollectionNames() {
  return {
    bookings: "Bookings",
    partners: "partners",
    providers: "providers",
    serveitUsers: "serveit_users",
    jobInbox: "provider_job_inbox",
    callLogs: "CallLogs",
    activeCalls: "ActiveCalls",
    earnings: "earnings",
    payoutRequests: "payoutRequests",
    payoutTransactions: "payoutTransactions",
    monthlySettlements: "monthlySettlements",
    bankAccounts: "bankAccounts"
  };
}

/**
 * Get geo-query constants
 */
function getGeoConstants() {
  return {
    fallbackCoordinates: {
      latitude: 19.8762,
      longitude: 75.3433
    },
    geoQueryRadius: 200, // kilometers
    finalDistanceLimit: 200 // kilometers
  };
}

/**
 * Get timezone
 */
function getTimezone() {
  return "Asia/Kolkata";
}

module.exports = {
  getEnvironment,
  isProduction,
  isDevelopment,
  getGoogleMapsApiKey,
  getAgoraCredentials,
  getCollectionNames,
  getGeoConstants,
  getTimezone
};

