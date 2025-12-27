/**
 * Serveit Firebase Cloud Functions
 * Main Export File - ALL Functions (Production + New)
 *
 * IMPORTANT: Do NOT rename or remove exports - Firebase identifies
 * deployed functions by their export names, not file names.
 *
 * ═══════════════════════════════════════════════════════════
 * SECTION 1: COPIED FROM OLD PROJECT (6 Production Functions)
 * ═══════════════════════════════════════════════════════════
 * These functions are COPIED EXACTLY AS-IS from old project
 * DO NOT MODIFY their export names, triggers, or internal logic
 *
 * ═══════════════════════════════════════════════════════════
 * SECTION 2: NEW PROJECT FUNCTIONS (2 Functions)
 * ═══════════════════════════════════════════════════════════
 * These are functions specific to the new project
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");

// Initialize Firebase Admin SDK (ONCE)
admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

// ═══════════════════════════════════════════════════════════
// SECTION 1: COPIED FROM OLD PROJECT - PRODUCTION FUNCTIONS
// ═══════════════════════════════════════════════════════════
// These functions are PRESERVED with EXACT SAME NAMES
// DO NOT MODIFY their export names or triggers

// Fallback coordinates for Kranti Chowk, Chhatrapati Sambhajinagar, Maharashtra
const FALLBACK_COORDINATES = {
  latitude: 19.8762,
  longitude: 75.3433,
};

// Search radius for geo-query (broad search)
const GEO_QUERY_RADIUS = 200; // kilometers (increased for testing)
// Final filter radius for precise road distance
const FINAL_DISTANCE_LIMIT = 200; // kilometers (increased for testing to allow more providers)

// Google Maps API key - OPTIONAL
// Function will use Haversine distance calculation if API key is not set
// To set this parameter (optional), use one of these methods:
// 1. firebase functions:config:set google.maps_api_key="YOUR_API_KEY"
// 2. Set environment variable GOOGLE_MAPS_API_KEY
// Note: If not set, function will fall back to Haversine distance (less accurate but works)

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

/**
 * Calculate distance between two coordinates using Haversine formula
 * @param {number} lat1 - Latitude of first point
 * @param {number} lon1 - Longitude of first point
 * @param {number} lat2 - Latitude of second point
 * @param {number} lon2 - Longitude of second point
 * @return {number} Distance in kilometers
 */
function calculateDistance(lat1, lon1, lat2, lon2) {
  const R = 6371; // Earth's radius in kilometers
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a =
    Math.sin(dLat/2) * Math.sin(dLat/2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLon/2) * Math.sin(dLon/2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  return R * c;
}

/**
 * Get road distances using Google Maps Distance Matrix API
 * @param {Object} origin - Origin coordinates {lat, lng}
 * @param {Array} destinations - Array of destination coordinates [{lat, lng}]
 * @return {Promise<Array>} Array of distance results
 */
async function getRoadDistances(origin, destinations) {
  const GOOGLE_MAPS_API_KEY = getGoogleMapsApiKey();
  if (!GOOGLE_MAPS_API_KEY) {
    console.warn("Google Maps API key not configured, using Haversine distances");
    console.log(`Origin coordinates: [${origin.lat}, ${origin.lng}]`);

    return destinations.map((dest, index) => {
      const distanceKm = calculateDistance(origin.lat, origin.lng, dest.lat, dest.lng);
      console.log(`Destination ${index + 1}: [${dest.lat}, ${dest.lng}] - Distance: ${distanceKm.toFixed(2)}km`);

      return {
        distance: distanceKm, // Return km directly, not converted to meters
        duration: 0,
        status: "OK",
      };
    });
  }

  const destinationString = destinations.map((d) => `${d.lat},${d.lng}`).join("|");
  const url = `https://maps.googleapis.com/maps/api/distancematrix/json?origins=${origin.lat},${origin.lng}&destinations=${destinationString}&key=${GOOGLE_MAPS_API_KEY}&units=metric&mode=driving`;

  try {
    const response = await axios.get(url);
    if (response.data.status === "OK" && response.data.rows[0]) {
      return response.data.rows[0].elements;
    }
    throw new Error(`Distance Matrix API error: ${response.data.status}`);
  } catch (error) {
    console.error("Error calling Distance Matrix API:", error);
    // Fallback to Haversine calculation
    return destinations.map((dest, index) => {
      const distanceKm = calculateDistance(origin.lat, origin.lng, dest.lat, dest.lng);
      console.log(`Fallback calculation for destination ${index + 1}: ${distanceKm.toFixed(2)}km`);

      return {
        distance: distanceKm, // Return km directly, not converted to meters
        duration: 0,
        status: "OK",
      };
    });
  }
}

/**
 * Perform geo-query to find providers within radius
 * @param {number} latitude - Center latitude
 * @param {number} longitude - Center longitude
 * @param {string} serviceName - Required service name
 * @param {number} radiusKm - Search radius in kilometers
 * @return {Promise<Array>} Array of potential providers
 */
async function findProvidersWithGeoQuery(latitude, longitude, serviceName, radiusKm) {
  // Convert radius to degrees (approximate)
  const radiusDegrees = radiusKm / 111; // 1 degree ≈ 111 km

  const minLat = latitude - radiusDegrees;
  const maxLat = latitude + radiusDegrees;
  const minLng = longitude - radiusDegrees;
  const maxLng = longitude + radiusDegrees;

  // FIXED: Use isVerified (root level) instead of verificationDetails.verified
  // This is more reliable as the mapper always sets isVerified
  const providersQuery = db.collection("partners")
      .where("isVerified", "==", true);

  const providersSnapshot = await providersQuery.get();
  const potentialProviders = [];

  // DIAGNOSTIC: Check total providers (including unverified) for debugging
  const allProvidersSnapshot = await db.collection("partners").limit(10).get();
  console.log(`[DIAGNOSTIC] Total providers in partners collection (first 10): ${allProvidersSnapshot.size}`);
  allProvidersSnapshot.forEach((doc) => {
    const data = doc.data();
    console.log(`[DIAGNOSTIC] Provider ${doc.id}: isVerified=${data.isVerified}, approvalStatus=${data.approvalStatus}, hasLocation=${!!data.locationDetails?.latitude}, hasServices=${!!data.services}`);
  });

  console.log(`[findProvidersWithGeoQuery] Service requested: ${serviceName}`);
  console.log(`[findProvidersWithGeoQuery] Job coordinates: [${latitude}, ${longitude}]`);
  console.log(`[findProvidersWithGeoQuery] Total verified providers in database: ${providersSnapshot.size}`);

  providersSnapshot.forEach((doc) => {
    const providerData = doc.data();

    // FIXED: Check verification status with fallback (support both nested and root level)
    const isVerified = providerData.isVerified ||
                      providerData.verificationDetails?.verified ||
                      false;

    if (!isVerified) {
      console.log(`Provider ${doc.id}: Not verified (isVerified=${providerData.isVerified}, verificationDetails.verified=${providerData.verificationDetails?.verified})`);
      return;
    }

    // Extract coordinates from locationDetails
    const providerLat = parseFloat(providerData.locationDetails?.latitude);
    const providerLng = parseFloat(providerData.locationDetails?.longitude);

    console.log(`Provider ${doc.id}: lat=${providerLat}, lng=${providerLng}, services=${JSON.stringify(providerData.services)}, fcmToken=${providerData.fcmToken ? "Yes" : "No"}`);

    // Check if coordinates exist and are valid
    if (!providerLat || !providerLng) {
      console.log(`Provider ${doc.id}: Missing coordinates`);
      return;
    }

    // FIXED: Improved service matching - case-insensitive and check primary service
    const services = providerData.services || [];
    const primaryService = providerData.selectedMainService || "";
    const serviceMatches = services.some((s) =>
      s && s.toLowerCase() === serviceName.toLowerCase(),
    ) || (primaryService && primaryService.toLowerCase() === serviceName.toLowerCase());

    // Check longitude bounds and service availability
    if (providerLng >= minLng &&
        providerLng <= maxLng &&
        serviceMatches) {
      // Quick distance check using Haversine
      const distance = calculateDistance(
          latitude, longitude,
          providerLat, providerLng,
      );

      console.log(`Provider ${doc.id}: Distance ${distance.toFixed(2)}km from job location`);

      if (distance <= radiusKm) {
        console.log(`Provider ${doc.id}: QUALIFIED - within ${radiusKm}km radius`);
        potentialProviders.push({
          id: doc.id,
          ...providerData,
          latitude: providerLat,
          longitude: providerLng,
          approximateDistance: distance,
        });
      } else {
        console.log(`Provider ${doc.id}: REJECTED - ${distance.toFixed(2)}km exceeds ${radiusKm}km radius`);
      }
    } else {
      if (providerLng < minLng || providerLng > maxLng) {
        console.log(`Provider ${doc.id}: REJECTED - outside longitude bounds [${minLng}, ${maxLng}]`);
      } else if (!serviceMatches) {
        console.log(`Provider ${doc.id}: REJECTED - service mismatch (has: ${JSON.stringify(services)}, requested: ${serviceName})`);
      }
    }
  });

  return potentialProviders;
}

/**
 * Process new booking for job dispatching - REFACTORED for direct query model
 */
async function processNewBooking(bookingData, phoneNumber) {
  const bookingId = bookingData.bookingId;

  console.log(`New booking created: ${bookingId}`, bookingData);

  // Step 1: Get Job's Coordinates from serveit_users collection using document ID
  let jobCoordinates = FALLBACK_COORDINATES;

  try {
    const userDoc = await db.collection("serveit_users").doc(phoneNumber).get();

    if (userDoc.exists) {
      const userData = userDoc.data();
      if (userData.latitude && userData.longitude) {
        jobCoordinates = {
          latitude: userData.latitude,
          longitude: userData.longitude,
        };
        console.log(`Found user coordinates: ${jobCoordinates.latitude}, ${jobCoordinates.longitude}`);
      } else {
        console.log("User found but no coordinates, using fallback");
      }
    } else {
      console.log("User not found, using fallback coordinates");
    }
  } catch (error) {
    console.error("Error fetching user coordinates:", error);
    console.log("Using fallback coordinates due to error");
  }

  // Step 2: Extract service name
  const serviceName = bookingData.serviceName || "General Service";
  console.log(`Service requested: ${serviceName}`);

  // Step 3: Find potential providers using geo-query
  const potentialProviders = await findProvidersWithGeoQuery(
      jobCoordinates.latitude,
      jobCoordinates.longitude,
      serviceName,
      GEO_QUERY_RADIUS,
  );

  if (potentialProviders.length === 0) {
    // Enhanced monitoring: Log zero providers found scenario
    console.error("[MONITORING] Zero providers found for booking", {
      bookingId: bookingId,
      serviceName: serviceName,
      coordinates: [jobCoordinates.latitude, jobCoordinates.longitude],
      radius: GEO_QUERY_RADIUS,
      timestamp: new Date().toISOString(),
      severity: "WARNING",
    });
    console.log("No potential providers found within geo-query radius");
    
    // DIAGNOSTIC: Check if there are any providers at all (even unverified)
    const allProvidersCheck = await db.collection("partners").limit(5).get();
    if (allProvidersCheck.empty) {
      console.error("[DIAGNOSTIC] CRITICAL: No providers exist in partners collection at all!");
    } else {
      console.log(`[DIAGNOSTIC] Found ${allProvidersCheck.size} providers in database (may be unverified)`);
      allProvidersCheck.forEach((doc) => {
        const data = doc.data();
        console.log(`[DIAGNOSTIC] Provider ${doc.id}: isVerified=${data.isVerified}, approvalStatus=${data.approvalStatus || "NOT_SET"}, hasLocation=${!!data.locationDetails?.latitude}, hasServices=${!!data.services}`);
      });
    }
    return;
  }

  console.log(`Found ${potentialProviders.length} potential providers within ${GEO_QUERY_RADIUS}km`);

  // Log provider details for debugging
  potentialProviders.forEach((provider, index) => {
    console.log(`Provider ${index + 1}: ID=${provider.id}, Location=[${provider.latitude}, ${provider.longitude}], FCM=${provider.fcmToken ? "Yes" : "No"}`);
  });

  // Step 4: Get precise road distances using Distance Matrix API
  const destinations = potentialProviders.map((provider) => ({
    lat: provider.latitude,
    lng: provider.longitude,
  }));

  const distanceResults = await getRoadDistances(
      {lat: jobCoordinates.latitude, lng: jobCoordinates.longitude},
      destinations,
  );

  // Step 5: Filter providers within final distance limit and create qualified providers list
  const qualifiedProviders = [];

  for (let i = 0; i < potentialProviders.length && i < distanceResults.length; i++) {
    const provider = potentialProviders[i];
    const distanceResult = distanceResults[i];

    // Check if this is Google Maps API result (has distance.value)
    if (distanceResult.status === "OK" && distanceResult.distance && distanceResult.distance.value) {
      const distanceKm = distanceResult.distance.value / 1000; // Convert meters to km

      console.log(`Provider ${provider.id}: ${distanceKm.toFixed(2)}km distance (Google Maps API)`);

      if (distanceKm <= FINAL_DISTANCE_LIMIT) {
        console.log(`✅ Provider ${provider.id} QUALIFIED - within ${FINAL_DISTANCE_LIMIT}km limit`);
        qualifiedProviders.push({
          id: provider.id,
          fcmToken: provider.fcmToken,
          distance: distanceKm,
          duration: distanceResult.duration ? distanceResult.duration.value : 0,
        });
      } else {
        console.log(`❌ Provider ${provider.id} REJECTED - ${distanceKm.toFixed(2)}km exceeds ${FINAL_DISTANCE_LIMIT}km limit`);
      }
    }
    // Check if this is Haversine result (direct distance value)
    else if (distanceResult.status === "OK" && typeof distanceResult.distance === "number") {
      const distanceKm = distanceResult.distance;

      console.log(`Provider ${provider.id}: ${distanceKm.toFixed(2)}km distance (Haversine)`);

      if (distanceKm <= FINAL_DISTANCE_LIMIT) {
        console.log(`✅ Provider ${provider.id} QUALIFIED - within ${FINAL_DISTANCE_LIMIT}km limit`);
        qualifiedProviders.push({
          id: provider.id,
          fcmToken: provider.fcmToken,
          distance: distanceKm,
          duration: 0, // No duration for Haversine
        });
      } else {
        console.log(`❌ Provider ${provider.id} REJECTED - ${distanceKm.toFixed(2)}km exceeds ${FINAL_DISTANCE_LIMIT}km limit`);
      }
    } else {
      console.log(`⚠️ Provider ${provider.id}: Invalid distance data - ${JSON.stringify(distanceResult)}`);
    }
  }

  if (qualifiedProviders.length === 0) {
    // Enhanced monitoring: Log zero qualified providers after distance filtering
    console.error("[MONITORING] Zero providers qualified after distance filtering", {
      bookingId: bookingId,
      serviceName: serviceName,
      coordinates: [jobCoordinates.latitude, jobCoordinates.longitude],
      potentialProvidersCount: potentialProviders.length,
      finalDistanceLimit: FINAL_DISTANCE_LIMIT,
      timestamp: new Date().toISOString(),
      severity: "WARNING",
    });
    console.log("No providers within final distance criteria");
    return;
  }

  console.log(`${qualifiedProviders.length} providers qualified after distance filtering`);

  // Step 6: Update Booking Document with notified provider IDs
  const notifiedProviderIds = qualifiedProviders.map((provider) => provider.id).filter((id) => id !== undefined);

  try {
    // Update the original booking document (using phone number as document ID)
    const bookingRef = db.collection("Bookings").doc(phoneNumber);

    // Get current document to check if it has multiple bookings
    const currentDoc = await bookingRef.get();
    const currentData = currentDoc.data();

    if (currentData && currentData.bookings && Array.isArray(currentData.bookings)) {
      // Handle multiple bookings structure
      const bookings = currentData.bookings;
      const bookingIndex = bookings.findIndex((b) => b.bookingId === bookingId);

      if (bookingIndex !== -1) {
        // Update specific booking in array
        bookings[bookingIndex].notifiedProviderIds = notifiedProviderIds;
        bookings[bookingIndex].status = "pending";
        bookings[bookingIndex].jobCoordinates = jobCoordinates;

        await bookingRef.update({
          bookings: bookings,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      }
    } else {
      // Single booking structure - add provider notification data to existing booking
      await bookingRef.update({
        notifiedProviderIds: notifiedProviderIds,
        status: "pending",
        jobCoordinates: jobCoordinates,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    }

    console.log(`Updated booking ${bookingId} with ${notifiedProviderIds.length} notified providers`);
  } catch (error) {
    console.error("Error updating booking document:", error);
    return;
  }

  // Step 7: Send Lightweight Wake-up Notifications
  for (const provider of qualifiedProviders) {
    const providerId = provider.id;
    const fcmToken = provider.fcmToken;

    if (!fcmToken) {
      console.log(`No FCM token for provider ${providerId}, skipping notification`);
      continue;
    }

    const message = {
      token: fcmToken,
      notification: {
        title: "New Job Available!",
        body: `${serviceName} service request nearby`,
      },
      data: {
        type: "new_job_alert",
        bookingId: bookingId,
      },
    };

    // Send lightweight notification
    admin.messaging().send(message)
        .then(() => {
          console.log(`Wake-up notification sent to provider ${providerId}`);
        })
        .catch((error) => {
          console.error(`Failed to send notification to provider ${providerId}:`, error);
        });
  }

  // Enhanced monitoring: Log successful job dispatch
  console.log("[MONITORING] Job dispatch completed successfully", {
    bookingId: bookingId,
    serviceName: serviceName,
    providersNotified: notifiedProviderIds.length,
    qualifiedProviders: qualifiedProviders.length,
    potentialProviders: potentialProviders.length,
    timestamp: new Date().toISOString(),
    severity: "INFO",
  });
  console.log(`Job dispatch completed for booking ${bookingId} - ${notifiedProviderIds.length} providers notified`);
}

/**
 * COPIED FROM OLD PROJECT - EXISTING FUNCTION 1: Dispatch Job to Providers
 * Trigger: Firestore onUpdate on Bookings/{phoneNumber}
 * Status: ✅ COPIED EXACTLY AS-IS - DO NOT MODIFY
 */
exports.dispatchJobToProviders = functions.firestore
    .document("Bookings/{phoneNumber}")
    .onUpdate(async (change, context) => {
      try {
        const beforeData = change.before.data();
        const afterData = change.after.data();
        const phoneNumber = context.params.phoneNumber;

        // Check if new booking was added to the array
        const beforeBookings = beforeData?.bookings || [];
        const afterBookings = afterData?.bookings || [];

        if (afterBookings.length <= beforeBookings.length) {
          console.log("No new booking detected");
          return;
        }

        // Get the latest booking (last item in array)
        const latestBooking = afterBookings[afterBookings.length - 1];
        await processNewBooking(latestBooking, phoneNumber);
      } catch (error) {
        console.error("Error in dispatchJobToProviders:", error);
      }
    });

/**
 * COPIED FROM OLD PROJECT - EXISTING FUNCTION 2: Accept Job Request
 * Trigger: HTTPS Callable
 * Status: ✅ COPIED EXACTLY AS-IS - DO NOT MODIFY
 */
exports.acceptJobRequest = functions.https.onCall(async (data, context) => {
  try {
    // Verify authentication
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "User must be authenticated");
    }

    const {bookingId, providerId} = data;

    if (!bookingId || !providerId) {
      throw new functions.https.HttpsError("invalid-argument", "Missing required parameters");
    }

    // Get provider details
    const providerDoc = await admin.firestore()
        .collection("partners")
        .doc(providerId)
        .get();

    if (!providerDoc.exists) {
      throw new functions.https.HttpsError("not-found", "Provider not found");
    }

    const providerData = providerDoc.data();

    // Execute transaction on main Bookings collection - REFACTORED for direct query model
    const result = await admin.firestore().runTransaction(async (transaction) => {
      // Search for the booking across all Bookings documents
      const bookingsSnapshot = await admin.firestore().collection("Bookings").get();
      let bookingRef = null;
      let bookingData = null;
      let phoneNumber = null;

      // Find the document that contains this bookingId
      for (const doc of bookingsSnapshot.docs) {
        const data = doc.data();
        if (data.bookingId === bookingId) {
          bookingRef = doc.ref;
          bookingData = data;
          phoneNumber = doc.id;
          break;
        }
      }

      if (!bookingRef || !bookingData) {
        throw new functions.https.HttpsError("not-found", "Booking not found");
      }

      // Check if job is still available using new status field
      if (bookingData.status !== "pending") {
        throw new functions.https.HttpsError("failed-precondition", "Job has already been accepted by another provider");
      }

      // Verify provider was notified for this job
      if (!bookingData.notifiedProviderIds || !bookingData.notifiedProviderIds.includes(providerId)) {
        throw new functions.https.HttpsError("failed-precondition", "Provider was not notified for this job");
      }

      // Update booking with provider details and new status
      transaction.update(bookingRef, {
        providerId: providerId,
        providerName: providerData.personalDetails?.fullName || providerData.fullName || "Unknown Provider",
        providerMobileNo: providerData.personalDetails?.mobileNo || providerData.mobileNo || "",
        status: "accepted",
        acceptedByProviderId: providerId,
        acceptedAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      console.log(`Updated booking ${bookingId} in document Bookings/${phoneNumber} - accepted by provider ${providerId}`);
      return {success: true, message: "Job accepted successfully"};
    });

    console.log(`Job ${bookingId} accepted by provider ${providerId}`);
    return result;
  } catch (error) {
    console.error("Error in acceptJobRequest:", error);
    throw error;
  }
});

/**
 * COPIED FROM OLD PROJECT - EXISTING FUNCTION 3: Send Verification Notification
 * Trigger: Firestore onUpdate on partners/{partnerId}
 * Status: ✅ COPIED EXACTLY AS-IS - DO NOT MODIFY
 */
exports.sendVerificationNotification = functions.firestore
    .document("partners/{partnerId}")
    .onUpdate(async (change, context) => {
      const before = change.before.data();
      const after = change.after.data();
      const partnerId = context.params.partnerId;

      console.log(`Function triggered for partner: ${partnerId}`);
      console.log("Before data:", JSON.stringify(before, null, 2));
      console.log("After data:", JSON.stringify(after, null, 2));

      // Check if verification status changed - check both isVerified and verificationDetails.verified
      const beforeVerified = before.isVerified || before.verificationDetails?.verified || false;
      const afterVerified = after.isVerified || after.verificationDetails?.verified || false;

      // Check if rejection reason changed (indicates rejection)
      const beforeRejection = before.verificationDetails?.rejectionReason;
      const afterRejection = after.verificationDetails?.rejectionReason;

      // Check if rejected status changed
      const beforeRejected = before.verificationDetails?.rejected || false;
      const afterRejected = after.verificationDetails?.rejected || false;

      console.log(`Verification status: ${beforeVerified} -> ${afterVerified}`);
      console.log(`Rejected status: ${beforeRejected} -> ${afterRejected}`);
      console.log(`Rejection reason: ${beforeRejection} -> ${afterRejection}`);

      let notificationType = null;
      let title = "";
      let body = "";

      // Determine notification type based on current verification state
      if (!beforeRejected && afterRejected) {
        // Application rejected
        notificationType = "VERIFICATION_REJECTED";
        title = "❌ Application Rejected";
        body = `Your application has been rejected. ${afterRejection ? `Reason: ${afterRejection}` : "Please check your documents and resubmit."}`;
        console.log("Detected application rejection");
      } else if (!beforeVerified && afterVerified) {
        // Verification successful
        notificationType = "VERIFICATION_APPROVED";
        title = "🎉 Verification Successful!";
        body = "Congratulations! Your account has been verified successfully. You can now start accepting jobs and providing services.";
        console.log("Detected verification success");
      } else if (beforeVerified && !afterVerified && !afterRejected) {
        // Under review (verified changed from true to false, but not rejected)
        notificationType = "VERIFICATION_PENDING";
        title = "⏳ Application Under Review";
        body = "Your application is currently under review. We will notify you once the verification process is complete.";
        console.log("Detected application under review");
      } else {
        console.log("No significant verification status change detected");
      }

      // Send notification if status changed
      if (notificationType) {
        try {
          console.log(`Attempting to send ${notificationType} notification`);

          // Get user's FCM token from partners collection
          const userDoc = await admin.firestore()
              .collection("partners")
              .doc(partnerId)
              .get();

          const userData = userDoc.data();
          const fcmToken = userData?.fcmToken;

          console.log(`FCM Token found: ${fcmToken ? "Yes" : "No"}`);

          // Store notification in user-specific Firestore path FIRST
          const notificationData = {
            title: title,
            message: body,
            type: notificationType,
            timestamp: Date.now(),
            isRead: false,
            userId: partnerId,
            relatedData: {
              type: notificationType,
              partnerId: partnerId,
              timestamp: Date.now().toString(),
            },
          };

          // Store in partners/{userId}/notifications/
          await admin.firestore()
              .collection("partners")
              .doc(partnerId)
              .collection("notifications")
              .add(notificationData);

          console.log(`Notification stored in Firestore for user ${partnerId}`);

          // Send FCM notification if token exists
          if (fcmToken) {
            const message = {
              token: fcmToken,
              notification: {
                title: title,
                body: body,
              },
              data: {
                type: notificationType,
                partnerId: partnerId,
                timestamp: Date.now().toString(),
              },
              android: {
                notification: {
                  icon: notificationType === "verification_approved" ? "ic_check_circle" : "ic_error",
                  color: notificationType === "verification_approved" ? "#4CAF50" : "#F44336",
                  channelId: "verification_notifications",
                  priority: "high",
                  defaultSound: true,
                  defaultVibrateTimings: true,
                },
              },
            };

            const result = await admin.messaging().send(message);
            console.log(`FCM notification sent successfully to partner ${partnerId}: ${notificationType}`, result);
          } else {
            console.log(`No FCM token found for partner ${partnerId}, but notification stored in Firestore`);
          }
        } catch (error) {
          console.error("Error sending/storing notification:", error);
        }
      }

      return null;
    });

/**
 * COPIED FROM OLD PROJECT - EXISTING FUNCTION 4: Send Job Notification
 * Trigger: Firestore onCreate on jobRequests/{jobId}
 * Status: ✅ COPIED EXACTLY AS-IS - DO NOT MODIFY
 */
exports.sendJobNotification = functions.firestore
    .document("jobRequests/{jobId}")
    .onCreate(async (snap, context) => {
      const jobData = snap.data();
      const jobId = context.params.jobId;

      // Get assigned partner's FCM token
      const partnerId = jobData.assignedPartnerId;
      if (!partnerId) return null;

      try {
        const partnerDoc = await admin.firestore()
            .collection("partners")
            .doc(partnerId)
            .get();

        const partnerData = partnerDoc.data();
        const fcmToken = partnerData?.fcmToken;

        // Store notification in Firestore first
        const notificationData = {
          title: "🔔 New Job Request",
          message: `You have a new ${jobData.serviceType} request from ${jobData.customerName}`,
          type: "INFO",
          timestamp: Date.now(),
          isRead: false,
          userId: partnerId,
          relatedData: {
            type: "job_request",
            jobId: jobId,
            serviceType: jobData.serviceType,
            customerName: jobData.customerName,
            timestamp: Date.now().toString(),
          },
        };

        // Store in partners/{userId}/notifications/
        await admin.firestore()
            .collection("partners")
            .doc(partnerId)
            .collection("notifications")
            .add(notificationData);

        console.log(`Job notification stored in Firestore for user ${partnerId}`);

        if (fcmToken) {
          const message = {
            token: fcmToken,
            notification: {
              title: "🔔 New Job Request",
              body: `You have a new ${jobData.serviceType} request from ${jobData.customerName}`,
            },
            data: {
              type: "job_request",
              jobId: jobId,
              serviceType: jobData.serviceType,
              timestamp: Date.now().toString(),
            },
            android: {
              notification: {
                icon: "ic_work",
                color: "#2196F3",
                channelId: "job_notifications",
                priority: "high",
                defaultSound: true,
                defaultVibrateTimings: true,
              },
            },
          };

          await admin.messaging().send(message);
          console.log(`Job FCM notification sent to partner ${partnerId}`);
        } else {
          console.log(`No FCM token for partner ${partnerId}, but notification stored in Firestore`);
        }
      } catch (error) {
        console.error("Error sending job notification:", error);
      }

      return null;
    });

/**
 * COPIED FROM OLD PROJECT - EXISTING FUNCTION 5: Send Daily Earnings Summary
 * Trigger: Pub/Sub Scheduled (8 PM daily, Asia/Kolkata)
 * Status: ✅ COPIED EXACTLY AS-IS - DO NOT MODIFY
 */
exports.sendDailyEarningsSummary = functions.pubsub
    .schedule("0 20 * * *") // 8 PM daily
    .timeZone("Asia/Kolkata")
    .onRun(async (context) => {
      try {
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        // Get all active partners
        const partnersSnapshot = await admin.firestore()
            .collection("partners")
            .where("isVerified", "==", true)
            .get();

        for (const partnerDoc of partnersSnapshot.docs) {
          const partnerData = partnerDoc.data();
          const partnerId = partnerDoc.id;
          const fcmToken = partnerData.fcmToken;

          if (!fcmToken) continue;

          // Calculate today's earnings
          const earningsSnapshot = await admin.firestore()
              .collection("earnings")
              .where("partnerId", "==", partnerId)
              .where("date", ">=", today)
              .get();

          let totalEarnings = 0;
          let jobsCompleted = 0;

          earningsSnapshot.forEach((doc) => {
            const earning = doc.data();
            totalEarnings += earning.amount || 0;
            jobsCompleted += 1;
          });

          if (jobsCompleted > 0) {
            // Store notification in Firestore first
            const notificationData = {
              title: "💰 Daily Earnings Summary",
              message: `Today you earned ₹${totalEarnings} from ${jobsCompleted} jobs completed!`,
              type: "INFO",
              timestamp: Date.now(),
              isRead: false,
              userId: partnerId,
              relatedData: {
                type: "earnings_summary",
                amount: totalEarnings.toString(),
                jobsCount: jobsCompleted.toString(),
                date: today.toISOString(),
              },
            };

            // Store in partners/{userId}/notifications/
            await admin.firestore()
                .collection("partners")
                .doc(partnerId)
                .collection("notifications")
                .add(notificationData);

            const message = {
              token: fcmToken,
              notification: {
                title: "💰 Daily Earnings Summary",
                body: `Today you earned ₹${totalEarnings} from ${jobsCompleted} jobs completed!`,
              },
              data: {
                type: "earnings_summary",
                amount: totalEarnings.toString(),
                jobsCount: jobsCompleted.toString(),
                date: today.toISOString(),
              },
            };

            await admin.messaging().send(message);
          }
        }

        console.log("Daily earnings summaries sent");
      } catch (error) {
        console.error("Error sending daily summaries:", error);
      }

      return null;
    });

/**
 * Send status update notification to customer
 */
async function sendCustomerStatusNotification(bookingData, newStatus, phoneNumber) {
  try {
    // Get customer's FCM token from serveit_users collection
    const userDoc = await admin.firestore()
        .collection("serveit_users")
        .doc(phoneNumber)
        .get();

    if (!userDoc.exists) {
      console.log(`Customer not found: ${phoneNumber}`);
      return;
    }

    const userData = userDoc.data();
    const fcmToken = userData?.fcmToken;

    if (!fcmToken) {
      console.log(`No FCM token for customer: ${phoneNumber}`);
      return;
    }

    // Prepare notification content based on status
    let title = "";
    let body = "";

    const serviceName = bookingData.serviceName || "Service";
    const providerName = bookingData.providerName || "Service Provider";

    switch (newStatus) {
      case "accepted":
        title = "✅ Order Accepted!";
        body = `${providerName} has accepted your ${serviceName} request. They will arrive soon!`;
        break;
      case "arrived":
        title = "📍 Provider Arrived";
        body = `${providerName} has arrived at your location for ${serviceName}`;
        break;
      case "in_progress":
        title = "🔧 Service Started";
        body = `${providerName} has started working on your ${serviceName}`;
        break;
      case "payment_pending":
        title = "💳 Payment Due";
        body = `${serviceName} completed! Please make payment to ${providerName}`;
        break;
      case "completed":
        title = "🎉 Order Completed!";
        body = `Your ${serviceName} has been completed successfully. Thank you for using our service!`;
        break;
      default:
        console.log(`Unknown status: ${newStatus}`);
        return;
    }

    // Store notification in customer's notifications collection
    const notificationData = {
      title: title,
      message: body,
      type: "ORDER_STATUS_UPDATE",
      timestamp: Date.now(),
      isRead: false,
      userId: phoneNumber,
      relatedData: {
        type: "order_status_update",
        bookingId: bookingData.bookingId,
        status: newStatus,
        providerName: providerName,
        serviceName: serviceName,
        timestamp: Date.now().toString(),
      },
    };

    // Store in serveit_users/{phoneNumber}/notifications/
    await admin.firestore()
        .collection("serveit_users")
        .doc(phoneNumber)
        .collection("notifications")
        .add(notificationData);

    // Send FCM notification
    const message = {
      token: fcmToken,
      notification: {
        title: title,
        body: body,
      },
      data: {
        type: "ORDER_STATUS_UPDATE",
        bookingId: bookingData.bookingId || "",
        status: newStatus,
        timestamp: Date.now().toString(),
      },
    };

    await admin.messaging().send(message);
    console.log(`Status FCM notification sent to customer ${phoneNumber} for status: ${newStatus}`);
  } catch (error) {
    console.error("Error sending customer status notification:", error);
  }
}

/**
 * COPIED FROM OLD PROJECT - EXISTING FUNCTION 6: Notify Customer on Status Change
 * Trigger: Firestore onUpdate on Bookings/{phoneNumber}
 * Status: ✅ COPIED EXACTLY AS-IS - DO NOT MODIFY
 */
exports.notifyCustomerOnStatusChange = functions.firestore
    .document("Bookings/{phoneNumber}")
    .onUpdate(async (change, context) => {
      try {
        const beforeData = change.before.data();
        const afterData = change.after.data();
        const phoneNumber = context.params.phoneNumber;

        console.log(`Status change detected for booking document: ${phoneNumber}`);

        // Check if this is a single booking or array structure
        if (afterData.bookings && Array.isArray(afterData.bookings)) {
          // Handle array structure - check each booking for status changes
          const beforeBookings = beforeData?.bookings || [];
          const afterBookings = afterData.bookings || [];

          for (let i = 0; i < afterBookings.length; i++) {
            const beforeBooking = beforeBookings[i] || {};
            const afterBooking = afterBookings[i];

            const beforeStatus = beforeBooking.status;
            const afterStatus = afterBooking.status;

            // Check if status changed and has a provider assigned
            if (beforeStatus !== afterStatus && afterBooking.acceptedByProviderId) {
              await sendCustomerStatusNotification(afterBooking, afterStatus, phoneNumber);
            }
          }
        } else {
          // Handle single booking structure
          const beforeStatus = beforeData?.status;
          const afterStatus = afterData?.status;

          if (beforeStatus !== afterStatus && afterData.acceptedByProviderId) {
            await sendCustomerStatusNotification(afterData, afterStatus, phoneNumber);
          }
        }
      } catch (error) {
        console.error("Error in notifyCustomerOnStatusChange:", error);
      }

      return null;
    });

// ═══════════════════════════════════════════════════════════
// SECTION 2: NEW PROJECT FUNCTIONS
// ═══════════════════════════════════════════════════════════
// These are functions specific to the new project

/**
 * NEW PROJECT FUNCTION: Send Profile Status Notification
 * Trigger: Firestore onUpdate on providers/{uid}
 * Note: This function watches the providers collection (new project structure)
 */
exports.sendProfileStatusNotification = functions.firestore
    .document("providers/{uid}")
    .onUpdate(async (change, context) => {
      const before = change.before.data();
      const after = change.after.data();
      const uid = context.params.uid;

      // Log the update for debugging
      console.log(`[${uid}] Document updated. Before:`, {
        onboardingStatus: before.onboardingStatus,
        approvalStatus: before.approvalStatus,
      });
      console.log(`[${uid}] After:`, {
        onboardingStatus: after.onboardingStatus,
        approvalStatus: after.approvalStatus,
        hasFcmToken: !!after.fcmToken,
      });

      // Get FCM token from the provider document
      const fcmToken = after.fcmToken;

      if (!fcmToken) {
        console.log(`[${uid}] No FCM token found for provider`);
        return null;
      }

      let notificationTitle = "";
      let notificationBody = "";
      let shouldSend = false;

      // Check if profile was just submitted
      if (before.onboardingStatus !== "SUBMITTED" &&
          after.onboardingStatus === "SUBMITTED") {
        notificationTitle = "Profile Submitted";
        notificationBody = "Your profile is under review. " +
            "We will notify you once the verification is complete.";
        shouldSend = true;
        console.log(`[${uid}] Status changed to SUBMITTED`);
      } else if (before.approvalStatus !== "APPROVED" &&
          after.approvalStatus === "APPROVED") {
        // Check if profile was approved
        notificationTitle = "Profile Approved! 🎉";
        notificationBody = "Congratulations! Your profile has been " +
            "approved. You can now start receiving service requests.";
        shouldSend = true;
        console.log(`[${uid}] Status changed to APPROVED`);
      } else if (before.approvalStatus !== "REJECTED" &&
          after.approvalStatus === "REJECTED") {
        // Check if profile was rejected
        notificationTitle = "Profile Rejected";
        const reason = after.rejectionReason ||
            "Please check your profile for details.";
        notificationBody = `Your profile has been rejected. Reason: ${reason}`;
        shouldSend = true;
        console.log(`[${uid}] Status changed to REJECTED`);
      } else {
        // If no status change, don't send notification
        console.log(`[${uid}] No relevant status change detected`);
        return null;
      }

      if (!shouldSend) {
        return null;
      }

      // Prepare notification message
      const message = {
        notification: {
          title: notificationTitle,
          body: notificationBody,
        },
        data: {
          type: "profile_status_update",
          approvalStatus: after.approvalStatus || "",
          onboardingStatus: after.onboardingStatus || "",
          uid: uid,
        },
        token: fcmToken,
        android: {
          priority: "high",
          notification: {
            channelId: "serveit_partner_notifications",
            priority: "high",
            sound: "default",
          },
        },
      };

      try {
        // Send notification
        const response = await admin.messaging().send(message);
        console.log(`Successfully sent notification to ${uid}:`, response);
        return response;
      } catch (error) {
        console.error(`Error sending notification to ${uid}:`, error);

        // If token is invalid, try to get all tokens from subcollection
        if (error.code === "messaging/invalid-registration-token" ||
            error.code === "messaging/registration-token-not-registered") {
          console.log(`Token invalid, trying to get tokens from ` +
              `subcollection for ${uid}`);

          try {
            const tokensSnapshot = await admin.firestore()
                .collection("providers")
                .doc(uid)
                .collection("fcmTokens")
                .orderBy("createdAt", "desc")
                .limit(5)
                .get();

            if (!tokensSnapshot.empty) {
              // Try the most recent token
              const latestToken = tokensSnapshot.docs[0].data().token;
              message.token = latestToken;

              try {
                const response = await admin.messaging().send(message);
                console.log(`Successfully sent notification using ` +
                    `latest token for ${uid}:`, response);

                // Update main document with latest working token
                await admin.firestore()
                    .collection("providers")
                    .doc(uid)
                    .update({fcmToken: latestToken});

                return response;
              } catch (retryError) {
                console.error(`Failed to send with latest token ` +
                    `for ${uid}:`, retryError);
              }
            }
          } catch (subcollectionError) {
            console.error(`Error accessing token subcollection ` +
                `for ${uid}:`, subcollectionError);
          }
        }

        return null;
      }
    });

/**
 * NEW PROJECT FUNCTION: Send Custom Notification
 * Trigger: HTTPS Callable
 * This can be called manually or from admin panel
 */
exports.sendCustomNotification = functions.https.onCall(
    async (data, context) => {
      // Only allow authenticated admin users
      if (!context.auth || !context.auth.token.admin) {
        throw new functions.https.HttpsError(
            "permission-denied",
            "Only admins can send custom notifications",
        );
      }

      const {uid, title, body} = data;

      if (!uid || !title || !body) {
        throw new functions.https.HttpsError(
            "invalid-argument",
            "uid, title, and body are required",
        );
      }

      try {
        const providerDoc = await admin.firestore()
            .collection("providers")
            .doc(uid)
            .get();

        if (!providerDoc.exists) {
          throw new functions.https.HttpsError(
              "not-found", "Provider not found",
          );
        }

        const providerData = providerDoc.data();
        const fcmToken = providerData.fcmToken;

        if (!fcmToken) {
          throw new functions.https.HttpsError(
              "not-found", "FCM token not found",
          );
        }

        const message = {
          notification: {
            title: title,
            body: body,
          },
          token: fcmToken,
          android: {
            priority: "high",
            notification: {
              channelId: "serveit_partner_notifications",
              priority: "high",
            },
          },
        };

        const response = await admin.messaging().send(message);
        return {success: true, messageId: response};
      } catch (error) {
        console.error("Error sending custom notification:", error);
        throw new functions.https.HttpsError("internal", error.message);
      }
    });
