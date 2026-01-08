/**
 * Admin Guard
 * 
 * Ensures user has admin privileges
 */

const functions = require("firebase-functions");

/**
 * Require admin role
 * @param {Object} context - Firebase Auth context
 * @throws {functions.https.HttpsError} If not admin
 */
function requireAdmin(context) {
  if (!context || !context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "User must be authenticated"
    );
  }
  
  // Check for admin claim in token
  const isAdmin = context.auth.token.admin === true || 
                  context.auth.token.admin === "true" ||
                  context.auth.token.role === "admin";
  
  if (!isAdmin) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Admin privileges required"
    );
  }
  
  return context.auth;
}

/**
 * Check if user is admin (non-throwing)
 * @param {Object} context - Firebase Auth context
 * @returns {boolean} True if admin
 */
function isAdmin(context) {
  if (!context || !context.auth) {
    return false;
  }
  
  return context.auth.token.admin === true || 
         context.auth.token.admin === "true" ||
         context.auth.token.role === "admin";
}

module.exports = {
  requireAdmin,
  isAdmin
};

