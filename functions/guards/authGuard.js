/**
 * Authentication Guard
 * 
 * Ensures user is authenticated before proceeding
 */

const functions = require("firebase-functions");

/**
 * Require authentication
 * @param {Object} context - Firebase Auth context
 * @throws {functions.https.HttpsError} If not authenticated
 */
function requireAuth(context) {
  if (!context || !context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "User must be authenticated"
    );
  }
  return context.auth;
}

/**
 * Get authenticated user ID
 * @param {Object} context - Firebase Auth context
 * @returns {string} User ID
 * @throws {functions.https.HttpsError} If not authenticated
 */
function getUserId(context) {
  const auth = requireAuth(context);
  return auth.uid;
}

/**
 * Get authenticated user phone number
 * @param {Object} context - Firebase Auth context
 * @returns {string|null} Phone number or null
 */
function getUserPhone(context) {
  if (!context || !context.auth) {
    return null;
  }
  return context.auth.token.phone_number || null;
}

module.exports = {
  requireAuth,
  getUserId,
  getUserPhone
};

