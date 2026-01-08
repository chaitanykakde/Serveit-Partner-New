/**
 * Ownership Guard
 * 
 * Validates resource ownership
 */

const functions = require("firebase-functions");

/**
 * Require resource ownership
 * @param {string} resourceOwnerId - Owner ID from resource
 * @param {string} userId - Current user ID
 * @param {string} resourceName - Name of resource for error message
 * @throws {functions.https.HttpsError} If user doesn't own resource
 */
function requireOwnership(resourceOwnerId, userId, resourceName = "resource") {
  if (resourceOwnerId !== userId) {
    throw new functions.https.HttpsError(
      "permission-denied",
      `Access denied. You do not own this ${resourceName}.`
    );
  }
}

/**
 * Check if user owns resource (non-throwing)
 * @param {string} resourceOwnerId - Owner ID from resource
 * @param {string} userId - Current user ID
 * @returns {boolean} True if user owns resource
 */
function isOwner(resourceOwnerId, userId) {
  return resourceOwnerId === userId;
}

module.exports = {
  requireOwnership,
  isOwner
};

