/**
 * Job Validators
 * 
 * Validation utilities for job-related operations
 */

const functions = require("firebase-functions");

/**
 * Validate booking ID
 */
function validateBookingId(bookingId) {
  if (!bookingId || typeof bookingId !== "string" || bookingId.trim() === "") {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "bookingId is required and must be a non-empty string"
    );
  }
}

/**
 * Validate provider ID
 */
function validateProviderId(providerId) {
  if (!providerId || typeof providerId !== "string" || providerId.trim() === "") {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "providerId is required and must be a non-empty string"
    );
  }
}

/**
 * Validate booking status for operations
 */
function validateBookingStatus(booking, allowedStatuses) {
  const status = (booking.bookingStatus || booking.status || "pending").toLowerCase();
  
  if (!allowedStatuses.includes(status)) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      `Operation not allowed for booking status "${status}". Allowed: ${allowedStatuses.join(", ")}`
    );
  }
  
  return status;
}

module.exports = {
  validateBookingId,
  validateProviderId,
  validateBookingStatus
};

