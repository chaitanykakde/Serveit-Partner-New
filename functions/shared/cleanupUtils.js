/**
 * Cleanup Utilities
 * 
 * Provides functions for cleaning up related data when jobs are accepted.
 */

/**
 * Cleanup inbox entries for other providers when job is accepted
 * This removes pending inbox entries for the same bookingId from all other providers
 * 
 * @param {Object} db - Firestore database instance
 * @param {Object} collections - Collection names
 * @param {string} bookingId - The booking ID that was accepted
 * @param {string} acceptedProviderId - The provider ID that accepted the job
 */
async function cleanupInboxForAcceptedJob(db, collections, bookingId, acceptedProviderId) {
  try {
    // Get all inbox entries for this bookingId using collectionGroup query
    const inboxSnapshot = await db
      .collectionGroup("jobs")
      .where("bookingId", "==", bookingId)
      .where("status", "==", "pending")
      .get();
    
    if (inboxSnapshot.empty) {
      console.log(`No pending inbox entries to clean up for booking ${bookingId}`);
      return;
    }
    
    const batch = db.batch();
    let deleteCount = 0;
    
    inboxSnapshot.docs.forEach((doc) => {
      // Extract providerId from path: provider_job_inbox/{providerId}/jobs/{jobId}
      const providerId = doc.ref.parent.parent.id;
      if (providerId !== acceptedProviderId) {
        batch.delete(doc.ref);
        deleteCount++;
      }
    });
    
    if (deleteCount > 0) {
      await batch.commit();
      console.log(`Cleaned up ${deleteCount} inbox entries for booking ${bookingId}`);
    } else {
      console.log(`No inbox entries to clean up (all belong to accepted provider)`);
    }
  } catch (error) {
    console.error(`Error cleaning up inbox for booking ${bookingId}:`, error);
    throw error;
  }
}

module.exports = {
  cleanupInboxForAcceptedJob,
};

