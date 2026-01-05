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
    
    // Step 6.5: Create Inbox Entries for Qualified Providers
    const inboxBatch = db.batch();
    const expiresAt = admin.firestore.Timestamp.fromDate(
      new Date(Date.now() + 30 * 60 * 1000) // 30 min expiry
    );
    
    // Get bookingIndex for array format, or -1 for single booking format
    let bookingIndex = -1;
    if (currentData && currentData.bookings && Array.isArray(currentData.bookings)) {
      bookingIndex = currentData.bookings.findIndex((b) => b.bookingId === bookingId);
    }
    
    for (const provider of qualifiedProviders) {
      const inboxRef = db
        .collection("provider_job_inbox")
        .doc(provider.id)
        .collection("jobs")
        .doc(bookingId);
      
      inboxBatch.set(inboxRef, {
        bookingId: bookingId,
        customerPhone: phoneNumber,
        bookingDocPath: `Bookings/${phoneNumber}`,
        bookingIndex: bookingIndex >= 0 ? bookingIndex : 0, // Use 0 for single booking format
        serviceName: serviceName,
        priceSnapshot: bookingData.totalPrice || 0,
        status: "pending",
        distanceKm: provider.distance || 0,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        expiresAt: expiresAt,
      });
    }
    
    await inboxBatch.commit();
    console.log(`Created inbox entries for ${qualifiedProviders.length} providers`);
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
 * Cleanup inbox entries for other providers when job is accepted
 * This removes pending inbox entries for the same bookingId from all other providers
 */
async function cleanupInboxForAcceptedJob(bookingId, acceptedProviderId) {
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

/**
 * COPIED FROM OLD PROJECT - EXISTING FUNCTION 2: Accept Job Request
 * Trigger: HTTPS Callable
 * Status: ✅ OPTIMIZED - Uses inbox for O(1) lookup
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

    // Execute transaction - OPTIMIZED: Use inbox for O(1) lookup
    const result = await admin.firestore().runTransaction(async (transaction) => {
      // STEP 1: Read inbox entry (O(1) lookup)
      const inboxRef = db
        .collection("provider_job_inbox")
        .doc(providerId)
        .collection("jobs")
        .doc(bookingId);
      
      const inboxDoc = await transaction.get(inboxRef);
      
      if (!inboxDoc.exists) {
        throw new functions.https.HttpsError("not-found", "Job not found in your inbox");
      }
      
      const inboxData = inboxDoc.data();
      const customerPhone = inboxData.customerPhone;
      const bookingIndex = inboxData.bookingIndex;
      
      // STEP 2: Read EXACT booking document (direct access)
      const bookingRef = db.collection("Bookings").doc(customerPhone);
      const bookingDoc = await transaction.get(bookingRef);
      
      if (!bookingDoc.exists) {
        throw new functions.https.HttpsError("not-found", "Booking document not found");
      }
      
      const bookingData = bookingDoc.data();
      const bookingsArray = bookingData.bookings || [];
      
      // Handle both array and single booking formats
      let targetBooking;
      if (bookingsArray.length > 0 && Array.isArray(bookingsArray)) {
        // Array format
        if (bookingIndex >= bookingsArray.length) {
          throw new functions.https.HttpsError("not-found", "Booking index out of range");
        }
        targetBooking = bookingsArray[bookingIndex];
      } else {
        // Single booking format (legacy)
        targetBooking = bookingData;
      }
      
      // STEP 3: Validate (from SOURCE OF TRUTH)
      if (targetBooking.bookingId !== bookingId) {
        throw new functions.https.HttpsError("failed-precondition", "Booking ID mismatch");
      }
      
      if (targetBooking.status !== "pending") {
        throw new functions.https.HttpsError("failed-precondition", "Job has already been accepted by another provider");
      }
      
      if (!targetBooking.notifiedProviderIds || !targetBooking.notifiedProviderIds.includes(providerId)) {
        throw new functions.https.HttpsError("failed-precondition", "Provider was not notified for this job");
      }
      
      // STEP 4: Update booking (SOURCE OF TRUTH)
      // NOTE: Cannot use FieldValue.serverTimestamp() inside arrays, so we use Timestamp.now()
      const acceptedAtTimestamp = admin.firestore.Timestamp.now();
      
      if (bookingsArray.length > 0 && Array.isArray(bookingsArray)) {
        // Array format: Update specific booking in array
        const updatedBookings = [...bookingsArray];
        updatedBookings[bookingIndex] = {
          ...targetBooking,
          providerId: providerId,
          providerName: providerData.personalDetails?.fullName || providerData.fullName || "Unknown Provider",
          providerMobileNo: providerData.personalDetails?.mobileNo || providerData.mobileNo || "",
          status: "accepted",
          bookingStatus: "accepted", // Keep both fields in sync
          acceptedByProviderId: providerId,
          acceptedAt: acceptedAtTimestamp, // Use Timestamp.now() instead of FieldValue.serverTimestamp()
        };
        
        transaction.update(bookingRef, {
          bookings: updatedBookings,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(), // This is OK at document level
        });
      } else {
        // Single booking format (legacy)
        transaction.update(bookingRef, {
          providerId: providerId,
          providerName: providerData.personalDetails?.fullName || providerData.fullName || "Unknown Provider",
          providerMobileNo: providerData.personalDetails?.mobileNo || providerData.mobileNo || "",
          status: "accepted",
          bookingStatus: "accepted", // Keep both fields in sync
          acceptedByProviderId: providerId,
          acceptedAt: admin.firestore.FieldValue.serverTimestamp(), // This is OK for single booking format
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      }
      
      // STEP 5: Update inbox entry
      transaction.update(inboxRef, {
        status: "accepted",
      });
      
      console.log(`Updated booking ${bookingId} in document Bookings/${customerPhone} - accepted by provider ${providerId}`);
      return {success: true, message: "Job accepted successfully"};
    });
    
    // STEP 6: Cleanup inbox entries for other providers (outside transaction)
    // This is done after transaction to avoid transaction size limits
    try {
      await cleanupInboxForAcceptedJob(bookingId, providerId);
    } catch (cleanupError) {
      console.error("Error cleaning up inbox entries:", cleanupError);
      // Don't fail the accept operation if cleanup fails
    }

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

/**
 * Sync inbox status when booking status changes
 * This keeps inbox UI in sync with booking status, but Bookings remains authoritative
 */
exports.syncInboxStatus = functions.firestore
  .document("Bookings/{phoneNumber}")
  .onUpdate(async (change, context) => {
    try {
      const afterData = change.after.data();
      const bookingsArray = afterData.bookings || [];
      
      // For each booking that has a providerId (accepted job)
      for (let i = 0; i < bookingsArray.length; i++) {
        const booking = bookingsArray[i];
        if (booking.providerId && booking.bookingId) {
          // Update inbox entry status
          const inboxRef = db
            .collection("provider_job_inbox")
            .doc(booking.providerId)
            .collection("jobs")
            .doc(booking.bookingId);
          
          await inboxRef.update({
            status: booking.status,
          }).catch((error) => {
            // Inbox entry might not exist (deleted/expired), ignore
            console.log(`Inbox entry not found for booking ${booking.bookingId}, ignoring sync`);
          });
        }
      }
      
      // Also handle single booking format (legacy)
      if (!Array.isArray(bookingsArray) && afterData.providerId && afterData.bookingId) {
        const inboxRef = db
          .collection("provider_job_inbox")
          .doc(afterData.providerId)
          .collection("jobs")
          .doc(afterData.bookingId);
        
        await inboxRef.update({
          status: afterData.status,
        }).catch((error) => {
          console.log(`Inbox entry not found for booking ${afterData.bookingId}, ignoring sync`);
        });
      }
    } catch (error) {
      console.error("Error syncing inbox status:", error);
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

/**
 * PAYOUT SYSTEM - MONTHLY SETTLEMENT AGGREGATION
 * Scheduled function to calculate monthly earnings summaries
 * Runs automatically on the 1st of each month at 2 AM IST
 */
exports.aggregateMonthlySettlements = functions
  .runWith({
    timeoutSeconds: 540, // 9 minutes (max allowed)
    memory: '1GB'
  })
  .pubsub.schedule('0 2 1 * *') // Run at 2 AM on the 1st of every month
  .timeZone('Asia/Kolkata')
  .onRun(async (context) => {
    const db = admin.firestore();
    const batch = db.batch();

    try {
      // Calculate target month (previous month)
      const now = new Date();
      const targetDate = new Date(now.getFullYear(), now.getMonth() - 1, 1);
      const yearMonth = `${targetDate.getFullYear()}-${String(targetDate.getMonth() + 1).padStart(2, '0')}`;

      console.log(`Starting monthly settlement aggregation for ${yearMonth}`);

      // Get all completed bookings for the target month
      const startOfMonth = new Date(targetDate.getFullYear(), targetDate.getMonth(), 1);
      const endOfMonth = new Date(targetDate.getFullYear(), targetDate.getMonth() + 1, 0, 23, 59, 59);

      const bookingsQuery = db.collection('bookings')
        .where('status', '==', 'COMPLETED')
        .where('completedAt', '>=', admin.firestore.Timestamp.fromDate(startOfMonth))
        .where('completedAt', '<=', admin.firestore.Timestamp.fromDate(endOfMonth));

      const bookingsSnapshot = await bookingsQuery.get();

      if (bookingsSnapshot.empty) {
        console.log(`No completed bookings found for ${yearMonth}`);
        return null;
      }

      // Group bookings by partner
      const partnerBookings = new Map();

      bookingsSnapshot.forEach(doc => {
        const booking = { bookingId: doc.id, ...doc.data() };
        const partnerId = booking.partnerId;

        if (!partnerBookings.has(partnerId)) {
          partnerBookings.set(partnerId, []);
        }
        partnerBookings.get(partnerId).push(booking);
      });

      console.log(`Processing settlements for ${partnerBookings.size} partners`);

      // Process each partner's bookings
      const settlementPromises = Array.from(partnerBookings.entries()).map(async ([partnerId, bookings]) => {
        try {
          await processPartnerSettlement(partnerId, bookings, yearMonth, batch);
        } catch (error) {
          console.error(`Error processing settlement for partner ${partnerId}:`, error);
        }
      });

      await Promise.all(settlementPromises);

      // Commit all batch writes
      await batch.commit();

      console.log(`Successfully processed monthly settlements for ${yearMonth}`);
      return { success: true, partnersProcessed: partnerBookings.size, yearMonth };

    } catch (error) {
      console.error('Error in monthly settlement aggregation:', error);
      throw error;
    }
  });

/**
 * Process settlement for a single partner
 */
async function processPartnerSettlement(partnerId, bookings, yearMonth, batch) {
  const db = admin.firestore();

  // Calculate settlement totals
  const totalEarnings = bookings.reduce((sum, booking) => sum + booking.amount, 0);
  const platformFees = bookings.reduce((sum, booking) => sum + (booking.platformFee || 0), 0);
  const partnerShare = bookings.reduce((sum, booking) => {
    // Use partnerEarning if available, otherwise calculate from amount - platformFee
    return sum + (booking.partnerEarning || (booking.amount - (booking.platformFee || 0)));
  }, 0);

  const completedJobs = bookings.length;

  // Check if settlement already exists
  const existingSettlementQuery = db.collection('monthlySettlements')
    .where('partnerId', '==', partnerId)
    .where('yearMonth', '==', yearMonth)
    .limit(1);

  const existingSettlementSnapshot = await existingSettlementQuery.get();

  const settlementData = {
    partnerId,
    yearMonth,
    totalEarnings,
    platformFees,
    partnerShare,
    completedJobs,
    paidAmount: 0, // Initially 0, updated when payouts are made
    pendingAmount: partnerShare, // Initially equals partner share
    settlementStatus: 'READY', // Ready for payout requests
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  };

  if (!existingSettlementSnapshot.empty) {
    // Update existing settlement
    const existingDoc = existingSettlementSnapshot.docs[0];
    const existingData = existingDoc.data();

    // Preserve paidAmount and pendingAmount from existing settlement
    const updatedData = {
      ...settlementData,
      paidAmount: existingData.paidAmount,
      pendingAmount: settlementData.partnerShare - existingData.paidAmount,
      settlementStatus: existingData.settlementStatus, // Preserve status
      createdAt: existingData.createdAt // Preserve original creation date
    };

    batch.update(existingDoc.ref, updatedData);
    console.log(`Updated settlement for partner ${partnerId} (${yearMonth}): ₹${partnerShare}`);
  } else {
    // Create new settlement
    const newSettlementRef = db.collection('monthlySettlements').doc();
    batch.set(newSettlementRef, {
      settlementId: newSettlementRef.id,
      ...settlementData
    });
    console.log(`Created settlement for partner ${partnerId} (${yearMonth}): ₹${partnerShare}`);
  }
}

/**
 * Manual trigger function for testing/admin purposes
 * Can be called via HTTP to recalculate settlements for specific periods
 */
exports.recalculateSettlements = functions.https.onCall(async (data, context) => {
  // For now, allow any authenticated user (implement admin role check)
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Authentication required');
  }

  const db = admin.firestore();
  const { yearMonth, partnerId } = data || {};

  try {
    if (partnerId && yearMonth) {
      // Recalculate for specific partner and month
      await recalculatePartnerSettlement(partnerId, yearMonth);
      return { success: true, message: `Recalculated settlement for partner ${partnerId} (${yearMonth})` };
    } else if (yearMonth) {
      // Recalculate for all partners in specific month
      await aggregateMonthlySettlementsForMonth(yearMonth);
      return { success: true, message: `Recalculated settlements for ${yearMonth}` };
    } else {
      throw new functions.https.HttpsError('invalid-argument', 'Specify yearMonth and optionally partnerId');
    }
  } catch (error) {
    console.error('Error recalculating settlements:', error);
    throw new functions.https.HttpsError('internal', 'Failed to recalculate settlements');
  }
});

/**
 * Recalculate settlement for a specific partner and month
 */
async function recalculatePartnerSettlement(partnerId, yearMonth) {
  const db = admin.firestore();

  // Parse yearMonth (format: "2024-01")
  const [year, month] = yearMonth.split('-').map(Number);
  const startOfMonth = new Date(year, month - 1, 1);
  const endOfMonth = new Date(year, month, 0, 23, 59, 59);

  // Get all completed bookings for this partner in the month
  const bookingsQuery = db.collection('bookings')
    .where('partnerId', '==', partnerId)
    .where('status', '==', 'COMPLETED')
    .where('completedAt', '>=', admin.firestore.Timestamp.fromDate(startOfMonth))
    .where('completedAt', '<=', admin.firestore.Timestamp.fromDate(endOfMonth));

  const bookingsSnapshot = await bookingsQuery.get();
  const bookings = bookingsSnapshot.docs.map(doc => ({
    bookingId: doc.id,
    ...doc.data()
  }));

  if (bookings.length > 0) {
    const batch = db.batch();
    await processPartnerSettlement(partnerId, bookings, yearMonth, batch);
    await batch.commit();
  }
}

/**
 * Aggregate settlements for all partners in a specific month
 */
async function aggregateMonthlySettlementsForMonth(yearMonth) {
  const db = admin.firestore();

  // Parse yearMonth
  const [year, month] = yearMonth.split('-').map(Number);
  const startOfMonth = new Date(year, month - 1, 1);
  const endOfMonth = new Date(year, month, 0, 23, 59, 59);

  // Get all completed bookings for the month
  const bookingsQuery = db.collection('bookings')
    .where('status', '==', 'COMPLETED')
    .where('completedAt', '>=', admin.firestore.Timestamp.fromDate(startOfMonth))
    .where('completedAt', '<=', admin.firestore.Timestamp.fromDate(endOfMonth));

  const bookingsSnapshot = await bookingsQuery.get();
  const batch = db.batch();

  // Group by partner and process
  const partnerBookings = new Map();

  bookingsSnapshot.forEach(doc => {
    const booking = { bookingId: doc.id, ...doc.data() };
    const partnerId = booking.partnerId;

    if (!partnerBookings.has(partnerId)) {
      partnerBookings.set(partnerId, []);
    }
    partnerBookings.get(partnerId).push(booking);
  });

  const settlementPromises = Array.from(partnerBookings.entries()).map(async ([partnerId, bookings]) => {
    await processPartnerSettlement(partnerId, bookings, yearMonth, batch);
  });

  await Promise.all(settlementPromises);
  await batch.commit();
}

/**
 * ADMIN PAYOUT DASHBOARD API - Get Payout Requests
 * HTTPS Callable function for admin dashboard to fetch payout requests
 */
exports.getPayoutRequests = functions.https.onCall(async (data, context) => {
  // TODO: Implement admin role verification
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Authentication required');
  }

  const db = admin.firestore();
  const { status, limit = 50, offset } = data || {};

  try {
    let query = db.collection('payoutRequests')
      .orderBy('requestedAt', 'desc')
      .limit(limit);

    if (status) {
      query = query.where('requestStatus', '==', status);
    }

    if (offset) {
      // For pagination, you'd typically use a document ID as offset
      query = query.startAfter(offset);
    }

    const snapshot = await query.get();
    const requests = [];

    for (const doc of snapshot.docs) {
      const requestData = doc.data();

      // Get bank account details
      let bankAccount = null;
      if (requestData.bankAccountId) {
        try {
          const bankAccountDoc = await db.collection('bankAccounts')
            .doc(requestData.bankAccountId)
            .get();
          if (bankAccountDoc.exists) {
            bankAccount = { id: bankAccountDoc.id, ...bankAccountDoc.data() };
          }
        } catch (error) {
          console.error('Error fetching bank account:', error);
        }
      }

      // Get settlement details
      let settlement = null;
      if (requestData.settlementId) {
        try {
          const settlementDoc = await db.collection('monthlySettlements')
            .doc(requestData.settlementId)
            .get();
          if (settlementDoc.exists) {
            settlement = { id: settlementDoc.id, ...settlementDoc.data() };
          }
        } catch (error) {
          console.error('Error fetching settlement:', error);
        }
      }

      // Get partner details
      let partner = null;
      try {
        const partnerDoc = await db.collection('partners')
          .doc(requestData.partnerId)
          .get();
        if (partnerDoc.exists) {
          const partnerData = partnerDoc.data();
          partner = {
            id: partnerDoc.id,
            fullName: partnerData.personalDetails?.fullName || partnerData.fullName || 'Unknown',
            mobileNo: partnerData.personalDetails?.mobileNo || partnerData.mobileNo || '',
            email: partnerData.email || ''
          };
        }
      } catch (error) {
        console.error('Error fetching partner:', error);
      }

      requests.push({
        id: doc.id,
        ...requestData,
        bankAccount,
        settlement,
        partner,
        requestedAt: requestData.requestedAt?.toDate?.() || requestData.requestedAt,
        processedAt: requestData.processedAt?.toDate?.() || requestData.processedAt
      });
    }

    return {
      success: true,
      requests,
      total: requests.length
    };
  } catch (error) {
    console.error('Error fetching payout requests:', error);
    throw new functions.https.HttpsError('internal', 'Failed to fetch payout requests');
  }
});

/**
 * ADMIN PAYOUT DASHBOARD API - Approve Payout Request
 * HTTPS Callable function for admin to approve payout requests
 */
exports.approvePayoutRequest = functions.https.onCall(async (data, context) => {
  // TODO: Implement admin role verification
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Authentication required');
  }

  const db = admin.firestore();
  const { requestId, notes } = data;

  if (!requestId) {
    throw new functions.https.HttpsError('invalid-argument', 'requestId is required');
  }

  try {
    await db.runTransaction(async (transaction) => {
      // Get the payout request
      const requestRef = db.collection('payoutRequests').doc(requestId);
      const requestDoc = await transaction.get(requestRef);

      if (!requestDoc.exists) {
        throw new functions.https.HttpsError('not-found', 'Payout request not found');
      }

      const requestData = requestDoc.data();

      if (requestData.requestStatus !== 'PENDING') {
        throw new functions.https.HttpsError('failed-precondition', 'Request is not in PENDING status');
      }

      // Update payout request status
      transaction.update(requestRef, {
        requestStatus: 'APPROVED',
        processedAt: admin.firestore.FieldValue.serverTimestamp(),
        notes: notes || '',
        processedBy: context.auth.uid
      });

      // Update settlement paid amount
      if (requestData.settlementId) {
        const settlementRef = db.collection('monthlySettlements').doc(requestData.settlementId);
        const settlementDoc = await transaction.get(settlementRef);

        if (settlementDoc.exists) {
          const settlementData = settlementDoc.data();
          const newPaidAmount = (settlementData.paidAmount || 0) + requestData.requestedAmount;
          const newPendingAmount = settlementData.partnerShare - newPaidAmount;

          transaction.update(settlementRef, {
            paidAmount: newPaidAmount,
            pendingAmount: newPendingAmount,
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
          });
        }
      }

      // Create payout transaction record
      const transactionRef = db.collection('payoutTransactions').doc();
      transaction.set(transactionRef, {
        transactionId: transactionRef.id,
        partnerId: requestData.partnerId,
        payoutRequestId: requestId,
        amount: requestData.requestedAmount,
        bankAccountId: requestData.bankAccountId,
        paymentMethod: 'BANK_TRANSFER',
        status: 'PENDING', // Will be updated by payment processor
        processedAt: admin.firestore.FieldValue.serverTimestamp(),
        fees: 0, // Bank transfer fees
        notes: notes || 'Approved by admin'
      });
    });

    // Send notification to partner
    await sendPayoutStatusNotification(requestId, 'APPROVED');

    return { success: true, message: 'Payout request approved successfully' };
  } catch (error) {
    console.error('Error approving payout request:', error);
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }
    throw new functions.https.HttpsError('internal', 'Failed to approve payout request');
  }
});

/**
 * ADMIN PAYOUT DASHBOARD API - Reject Payout Request
 * HTTPS Callable function for admin to reject payout requests
 */
exports.rejectPayoutRequest = functions.https.onCall(async (data, context) => {
  // TODO: Implement admin role verification
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Authentication required');
  }

  const db = admin.firestore();
  const { requestId, reason, notes } = data;

  if (!requestId) {
    throw new functions.https.HttpsError('invalid-argument', 'requestId is required');
  }

  if (!reason) {
    throw new functions.https.HttpsError('invalid-argument', 'reason is required');
  }

  try {
    // Update payout request status
    await db.collection('payoutRequests').doc(requestId).update({
      requestStatus: 'REJECTED',
      processedAt: admin.firestore.FieldValue.serverTimestamp(),
      failureReason: reason,
      notes: notes || '',
      processedBy: context.auth.uid
    });

    // Send notification to partner
    await sendPayoutStatusNotification(requestId, 'REJECTED', reason);

    return { success: true, message: 'Payout request rejected successfully' };
  } catch (error) {
    console.error('Error rejecting payout request:', error);
    throw new functions.https.HttpsError('internal', 'Failed to reject payout request');
  }
});

/**
 * ADMIN PAYOUT DASHBOARD API - Get Payout Statistics
 * HTTPS Callable function for admin dashboard statistics
 */
exports.getPayoutStatistics = functions.https.onCall(async (data, context) => {
  // TODO: Implement admin role verification
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Authentication required');
  }

  const db = admin.firestore();

  try {
    // Get counts by status
    const statusCounts = {};
    const statusSnapshot = await db.collection('payoutRequests')
      .select('requestStatus')
      .get();

    statusSnapshot.forEach(doc => {
      const status = doc.data().requestStatus;
      statusCounts[status] = (statusCounts[status] || 0) + 1;
    });

    // Get total amounts by status
    const amountStats = {};
    const amountSnapshot = await db.collection('payoutRequests').get();

    amountSnapshot.forEach(doc => {
      const data = doc.data();
      const status = data.requestStatus;
      const amount = data.requestedAmount || 0;

      if (!amountStats[status]) {
        amountStats[status] = { count: 0, total: 0 };
      }
      amountStats[status].count += 1;
      amountStats[status].total += amount;
    });

    // Get recent activity (last 30 days)
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

    const recentSnapshot = await db.collection('payoutRequests')
      .where('requestedAt', '>=', admin.firestore.Timestamp.fromDate(thirtyDaysAgo))
      .orderBy('requestedAt', 'desc')
      .limit(10)
      .get();

    const recentActivity = recentSnapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
      requestedAt: doc.data().requestedAt?.toDate?.() || doc.data().requestedAt
    }));

    return {
      success: true,
      statistics: {
        statusCounts,
        amountStats,
        recentActivity,
        totalRequests: amountSnapshot.size
      }
    };
  } catch (error) {
    console.error('Error fetching payout statistics:', error);
    throw new functions.https.HttpsError('internal', 'Failed to fetch payout statistics');
  }
});

/**
 * Send payout status notification to partner
 */
async function sendPayoutStatusNotification(requestId, status, reason = '', transactionId = null) {
  try {
    const db = admin.firestore();

    let requestData = null;
    let partnerId = null;

    if (transactionId) {
      // Get data from transaction
      const transactionDoc = await db.collection('payoutTransactions').doc(transactionId).get();
      if (transactionDoc.exists) {
        const transactionData = transactionDoc.data();
        partnerId = transactionData.partnerId;
        requestData = { amount: transactionData.amount };
      }
    } else if (requestId) {
      // Get data from payout request
      const requestDoc = await db.collection('payoutRequests').doc(requestId).get();
      if (requestDoc.exists) {
        requestData = requestDoc.data();
        partnerId = requestData.partnerId;
      }
    }

    if (!partnerId) return;

    // Get partner FCM token
    const partnerDoc = await db.collection('partners').doc(partnerId).get();
    if (!partnerDoc.exists) return;

    const partnerData = partnerDoc.data();
    const fcmToken = partnerData.fcmToken;
    if (!fcmToken) return;

    let title = '';
    let body = '';

    const amount = requestData?.requestedAmount || requestData?.amount || 0;

    switch (status) {
      case 'APPROVED':
        title = '💰 Payout Approved!';
        body = `Your payout request for ₹${amount} has been approved. Payment will be processed within 3-5 business days.`;
        break;
      case 'REJECTED':
        title = '❌ Payout Rejected';
        body = `Your payout request has been rejected. Reason: ${reason}`;
        break;
      case 'COMPLETED':
        title = '✅ Payment Completed!';
        body = `Your payout of ₹${amount} has been completed successfully. Cash payment processed.`;
        break;
      case 'FAILED':
        title = '⚠️ Payment Failed';
        body = `There was an issue processing your payout. Reason: ${reason}`;
        break;
    }

    // Send FCM notification
    const message = {
      notification: {
        title: title,
        body: body
      },
        data: {
          type: 'payout_status_update',
          requestId: requestId,
          transactionId: transactionId,
          status: status,
          amount: (requestData?.requestedAmount || requestData?.amount || 0).toString()
        },
      token: fcmToken,
      android: {
        priority: 'high',
        notification: {
          channelId: 'payout_notifications',
          priority: 'high'
        }
      }
    };

    await admin.messaging().send(message);

    // Store notification in Firestore
    await db.collection('partners').doc(partnerId)
      .collection('notifications').add({
        title: title,
        message: body,
        type: 'PAYOUT_STATUS_UPDATE',
        timestamp: Date.now(),
        isRead: false,
            relatedData: {
              requestId: requestId,
              transactionId: transactionId,
              status: status,
              amount: requestData?.requestedAmount || requestData?.amount || 0
            }
      });

    console.log(`Payout notification sent to partner ${partnerId}: ${status}`);
  } catch (error) {
    console.error('Error sending payout notification:', error);
  }
}

/**
 * ADMIN PAYOUT DASHBOARD API - Complete Payout Transaction
 * HTTPS Callable function for admin to mark payout as completed (cash paid)
 */
exports.completePayout = functions.https.onCall(async (data, context) => {
  // TODO: Implement admin role verification
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Authentication required');
  }

  const db = admin.firestore();
  const { transactionId, paymentMethod = 'CASH', notes } = data;

  if (!transactionId) {
    throw new functions.https.HttpsError('invalid-argument', 'transactionId is required');
  }

  try {
    await db.runTransaction(async (transaction) => {
      // Get the payout transaction
      const transactionRef = db.collection('payoutTransactions').doc(transactionId);
      const transactionDoc = await transaction.get(transactionRef);

      if (!transactionDoc.exists) {
        throw new functions.https.HttpsError('not-found', 'Transaction not found');
      }

      const transactionData = transactionDoc.data();

      if (transactionData.status === 'COMPLETED') {
        throw new functions.https.HttpsError('failed-precondition', 'Transaction is already completed');
      }

      // Update transaction status
      transaction.update(transactionRef, {
        status: 'COMPLETED',
        paymentMethod: paymentMethod,
        completedAt: admin.firestore.FieldValue.serverTimestamp(),
        completedBy: context.auth.uid,
        notes: notes || 'Payment completed successfully'
      });

      // Update payout request status if exists
      if (transactionData.payoutRequestId) {
        const requestRef = db.collection('payoutRequests').doc(transactionData.payoutRequestId);
        transaction.update(requestRef, {
          requestStatus: 'COMPLETED',
          processedAt: admin.firestore.FieldValue.serverTimestamp()
        });
      }

      // Update settlement paid amount
      if (transactionData.settlementId) {
        const settlementRef = db.collection('monthlySettlements').doc(transactionData.settlementId);
        const settlementDoc = await transaction.get(settlementRef);

        if (settlementDoc.exists) {
          const settlementData = settlementDoc.data();
          const newPaidAmount = (settlementData.paidAmount || 0) + transactionData.amount;
          const newPendingAmount = settlementData.partnerShare - newPaidAmount;

          transaction.update(settlementRef, {
            paidAmount: newPaidAmount,
            pendingAmount: newPendingAmount,
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
          });
        }
      }
    });

    // Generate receipt automatically
    try {
      await generatePaymentReceipt({
        transactionId: transactionId,
        partnerId: context.auth.uid
      });
    } catch (receiptError) {
      console.error('Error generating receipt:', receiptError);
      // Don't fail the payout completion if receipt generation fails
    }

    // Send notification to partner
    await sendPayoutStatusNotification(null, 'COMPLETED', '', transactionId);

    return {
      success: true,
      message: 'Payout completed successfully',
      transactionId: transactionId
    };

  } catch (error) {
    console.error('Error completing payout:', error);
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }
    throw new functions.https.HttpsError('internal', 'Failed to complete payout');
  }
});

/**
 * ADMIN PAYOUT DASHBOARD API - Get Pending Payout Transactions
 * HTTPS Callable function to get transactions that need completion
 */
exports.getPendingPayoutTransactions = functions.https.onCall(async (data, context) => {
  // TODO: Implement admin role verification
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Authentication required');
  }

  const db = admin.firestore();
  const { limit = 50, status = 'PENDING' } = data || {};

  try {
    const transactionsRef = db.collection('payoutTransactions')
      .where('status', '==', status)
      .orderBy('processedAt', 'desc')
      .limit(limit);

    const snapshot = await transactionsRef.get();
    const transactions = [];

    for (const doc of snapshot.docs) {
      const transactionData = doc.data();

      // Get related data
      let partner = null;
      let payoutRequest = null;

      try {
        // Get partner details
        const partnerDoc = await db.collection('partners').doc(transactionData.partnerId).get();
        if (partnerDoc.exists) {
          const partnerData = partnerDoc.data();
          partner = {
            id: partnerDoc.id,
            fullName: partnerData.personalDetails?.fullName || partnerData.fullName || 'Unknown',
            mobileNo: partnerData.personalDetails?.mobileNo || partnerData.mobileNo || '',
            email: partnerData.email || ''
          };
        }

        // Get payout request details if exists
        if (transactionData.payoutRequestId) {
          const requestDoc = await db.collection('payoutRequests').doc(transactionData.payoutRequestId).get();
          if (requestDoc.exists) {
            payoutRequest = {
              id: requestDoc.id,
              ...requestDoc.data(),
              requestedAt: requestDoc.data().requestedAt?.toDate?.() || requestDoc.data().requestedAt
            };
          }
        }
      } catch (error) {
        console.error('Error fetching related data:', error);
      }

      transactions.push({
        id: doc.id,
        ...transactionData,
        partner,
        payoutRequest,
        processedAt: transactionData.processedAt?.toDate?.() || transactionData.processedAt,
        completedAt: transactionData.completedAt?.toDate?.() || transactionData.completedAt
      });
    }

    return {
      success: true,
      transactions,
      total: transactions.length
    };

  } catch (error) {
    console.error('Error fetching pending transactions:', error);
    throw new functions.https.HttpsError('internal', 'Failed to fetch pending transactions');
  }
});

/**
 * PAYMENT RECEIPT GENERATION - Generate PDF Receipt
 * HTTPS Callable function to generate and download payment receipts
 */
exports.generatePaymentReceipt = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Authentication required');
  }

  const { transactionId } = data;

  if (!transactionId) {
    throw new functions.https.HttpsError('invalid-argument', 'transactionId is required');
  }

  try {
    const db = admin.firestore();

    // Get transaction details
    const transactionDoc = await db.collection('payoutTransactions').doc(transactionId).get();
    if (!transactionDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'Transaction not found');
    }

    const transactionData = transactionDoc.data();

    // Verify ownership (user can only access their own transactions)
    if (transactionData.partnerId !== context.auth.uid) {
      throw new functions.https.HttpsError('permission-denied', 'Access denied');
    }

    // Get related data
    const [payoutRequestDoc, bankAccountDoc, partnerDoc] = await Promise.all([
      db.collection('payoutRequests').doc(transactionData.payoutRequestId).get(),
      db.collection('bankAccounts').doc(transactionData.bankAccountId).get(),
      db.collection('partners').doc(transactionData.partnerId).get()
    ]);

    const payoutRequest = payoutRequestDoc.exists ? payoutRequestDoc.data() : null;
    const bankAccount = bankAccountDoc.exists ? bankAccountDoc.data() : null;
    const partner = partnerDoc.exists ? partnerDoc.data() : null;

    // Generate PDF receipt
    const pdfBuffer = await generateReceiptPDF({
      transaction: { id: transactionDoc.id, ...transactionData },
      payoutRequest: payoutRequest ? { id: payoutRequestDoc.id, ...payoutRequest } : null,
      bankAccount: bankAccount ? { id: bankAccountDoc.id, ...bankAccount } : null,
      partner: partner ? { id: partnerDoc.id, ...partner } : null
    });

    // Upload to Cloud Storage
    const bucket = admin.storage().bucket();
    const fileName = `receipts/${transactionData.partnerId}/${transactionId}.pdf`;
    const file = bucket.file(fileName);

    await file.save(pdfBuffer, {
      metadata: {
        contentType: 'application/pdf',
        metadata: {
          transactionId: transactionId,
          partnerId: transactionData.partnerId,
          amount: transactionData.amount.toString(),
          generatedAt: new Date().toISOString()
        }
      }
    });

    // Make file publicly accessible for download
    await file.makePublic();

    // Get download URL
    const [url] = await file.getSignedUrl({
      action: 'read',
      expires: Date.now() + (365 * 24 * 60 * 60 * 1000) // 1 year
    });

    // Update transaction with receipt URL
    await db.collection('payoutTransactions').doc(transactionId).update({
      receiptUrl: url,
      receiptGeneratedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return {
      success: true,
      receiptUrl: url,
      fileName: `${transactionId}.pdf`
    };

  } catch (error) {
    console.error('Error generating receipt:', error);
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }
    throw new functions.https.HttpsError('internal', 'Failed to generate receipt');
  }
});

/**
 * Generate PDF receipt using HTML template
 */
async function generateReceiptPDF(data) {
  const { PDFDocument, rgb } = require('pdf-lib');

  // Create a new PDF document
  const pdfDoc = await PDFDocument.create();
  const page = pdfDoc.addPage();
  const { width, height } = page.getSize();

  // Set up fonts and colors
  const fontSize = 12;
  const titleFontSize = 18;
  const blueColor = rgb(0.1, 0.4, 0.8);
  const grayColor = rgb(0.5, 0.5, 0.5);

  // Helper function to draw text
  let yPosition = height - 50;

  function drawText(text, x = 50, size = fontSize, color = rgb(0, 0, 0)) {
    page.drawText(text, { x, y: yPosition, size, color });
    yPosition -= size + 5;
  }

  function drawTitle(text) {
    drawText(text, 50, titleFontSize, blueColor);
    yPosition -= 10;
  }

  function drawSection(text) {
    drawText(text, 50, 14, blueColor);
    yPosition -= 5;
  }

  // Header
  drawTitle('💰 Payment Receipt');
  drawText(`Transaction ID: ${data.transaction.id}`, 50, 10, grayColor);
  drawText(`Date: ${new Date(data.transaction.processedAt?.toDate?.() || data.transaction.processedAt).toLocaleDateString('en-IN')}`, 50, 10, grayColor);
  yPosition -= 20;

  // Partner Information
  drawSection('Partner Information');
  drawText(`Name: ${data.partner?.personalDetails?.fullName || data.partner?.fullName || 'N/A'}`);
  drawText(`Partner ID: ${data.partner?.id}`);
  drawText(`Mobile: ${data.partner?.personalDetails?.mobileNo || data.partner?.mobileNo || 'N/A'}`);
  yPosition -= 15;

  // Payment Details
  drawSection('Payment Details');
  drawText(`Amount Paid: ₹${data.transaction.amount?.toLocaleString('en-IN') || '0'}`);
  drawText(`Payment Method: ${data.transaction.paymentMethod || 'Bank Transfer'}`);
  drawText(`Transaction Status: ${data.transaction.status || 'Completed'}`);
  drawText(`Processing Fees: ₹${data.transaction.fees?.toLocaleString('en-IN') || '0'}`);

  if (data.transaction.transactionRef) {
    drawText(`Reference Number: ${data.transaction.transactionRef}`);
  }
  yPosition -= 15;

  // Bank Account Details
  if (data.bankAccount) {
    drawSection('Bank Account Details');
    drawText(`Account Holder: ${data.bankAccount.accountHolderName}`);
    drawText(`Bank Name: ${data.bankAccount.bankName}`);
    drawText(`Account Number: ****${data.bankAccount.accountNumber?.slice(-4) || '****'}`);
    drawText(`IFSC Code: ${data.bankAccount.ifscCode}`);
  }
  yPosition -= 15;

  // Settlement Information
  if (data.payoutRequest) {
    drawSection('Settlement Information');
    drawText(`Settlement Period: ${data.payoutRequest.settlementId || 'N/A'}`);
    drawText(`Requested Amount: ₹${data.payoutRequest.requestedAmount?.toLocaleString('en-IN') || '0'}`);
    drawText(`Request Date: ${new Date(data.payoutRequest.requestedAt?.toDate?.() || data.payoutRequest.requestedAt).toLocaleDateString('en-IN')}`);

    if (data.payoutRequest.processedAt) {
      drawText(`Processed Date: ${new Date(data.payoutRequest.processedAt?.toDate?.() || data.payoutRequest.processedAt).toLocaleDateString('en-IN')}`);
    }
  }
  yPosition -= 30;

  // Footer
  drawText('Thank you for your service!', 50, 10, grayColor);
  drawText('Generated by Serveit Partner App', 50, 8, grayColor);
  drawText(`Generated on: ${new Date().toLocaleString('en-IN')}`, 50, 8, grayColor);

  // Add border
  page.drawRectangle({
    x: 20,
    y: 20,
    width: width - 40,
    height: height - 40,
    borderColor: grayColor,
    borderWidth: 1
  });

  // Serialize the PDF
  const pdfBytes = await pdfDoc.save();
  return Buffer.from(pdfBytes);
}

/**
 * PAYMENT RECEIPT GENERATION - Get Transaction Details
 * HTTPS Callable function to get transaction details for receipt generation
 */
exports.getTransactionDetails = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Authentication required');
  }

  const { transactionId } = data;

  if (!transactionId) {
    throw new functions.https.HttpsError('invalid-argument', 'transactionId is required');
  }

  try {
    const db = admin.firestore();

    // Get transaction details
    const transactionDoc = await db.collection('payoutTransactions').doc(transactionId).get();
    if (!transactionDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'Transaction not found');
    }

    const transactionData = transactionDoc.data();

    // Verify ownership
    if (transactionData.partnerId !== context.auth.uid) {
      throw new functions.https.HttpsError('permission-denied', 'Access denied');
    }

    // Get related data
    const [payoutRequestDoc, bankAccountDoc, partnerDoc] = await Promise.all([
      transactionData.payoutRequestId ? db.collection('payoutRequests').doc(transactionData.payoutRequestId).get() : Promise.resolve(null),
      transactionData.bankAccountId ? db.collection('bankAccounts').doc(transactionData.bankAccountId).get() : Promise.resolve(null),
      db.collection('partners').doc(transactionData.partnerId).get()
    ]);

    const result = {
      transaction: {
        id: transactionDoc.id,
        ...transactionData,
        processedAt: transactionData.processedAt?.toDate?.() || transactionData.processedAt
      }
    };

    if (payoutRequestDoc?.exists) {
      result.payoutRequest = {
        id: payoutRequestDoc.id,
        ...payoutRequestDoc.data(),
        requestedAt: payoutRequestDoc.data().requestedAt?.toDate?.() || payoutRequestDoc.data().requestedAt,
        processedAt: payoutRequestDoc.data().processedAt?.toDate?.() || payoutRequestDoc.data().processedAt
      };
    }

    if (bankAccountDoc?.exists) {
      result.bankAccount = {
        id: bankAccountDoc.id,
        ...bankAccountDoc.data()
      };
    }

    if (partnerDoc?.exists) {
      const partnerData = partnerDoc.data();
      result.partner = {
        id: partnerDoc.id,
        fullName: partnerData.personalDetails?.fullName || partnerData.fullName || 'Unknown',
        mobileNo: partnerData.personalDetails?.mobileNo || partnerData.mobileNo || '',
        email: partnerData.email || ''
      };
    }

    return result;

  } catch (error) {
    console.error('Error getting transaction details:', error);
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }
    throw new functions.https.HttpsError('internal', 'Failed to get transaction details');
  }
});

// ═══════════════════════════════════════════════════════════
// AGORA VOICE CALLING FUNCTIONS - PRODUCTION READY
// ═══════════════════════════════════════════════════════════
// These functions handle secure Agora token generation and call management

/**
 * Serveit Firebase Cloud Functions
 *
 * Secure backend for Agora token generation and booking management
 *
 * Environment Variables Required:
 * - AGORA_APP_ID: Your Agora App ID
 * - AGORA_APP_CERTIFICATE: Your Agora App Certificate (NEVER expose to client)
 */

const { RtcTokenBuilder, RtcRole } = require("agora-access-token");

/**
 * Generate Agora RTC Token for Secure Audio Calling
 *
 * Security Features:
 * - Requires authenticated user
 * - Validates booking ownership
 * - Validates booking status (must be CONFIRMED)
 * - Generates unique channel per booking
 * - Token expires in 10 minutes
 * - Never exposes app certificate
 *
 * @param {Object} data - { bookingId: string }
 * @param {Object} context - Firebase Auth context
 * @returns {Object} { success, token, channelName, uid, error }
 */
exports.generateAgoraToken = functions.https.onCall(async (data, context) => {
  const startTime = Date.now();

  try {
    // ═══════════════════════════════════════════════════════════════
    // STEP 1: Authentication Check
    // ═══════════════════════════════════════════════════════════════
    if (!context.auth) {
      console.error("❌ Unauthenticated call attempt");
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated to generate call token"
      );
    }

    const userId = context.auth.uid;
    const userPhone = context.auth.token.phone_number || null;

    console.log(`🔐 Auth verified - User: ${userId}, Phone: ${userPhone}`);

  // ═══════════════════════════════════════════════════════════════
  // STEP 2: Input Validation
  // ═══════════════════════════════════════════════════════════════
  const { bookingId, userMobile } = data;

  if (!bookingId || typeof bookingId !== "string" || bookingId.trim() === "") {
    console.error("❌ Invalid bookingId:", bookingId);
    throw new functions.https.HttpsError(
      "invalid-argument",
      "bookingId is required and must be a non-empty string"
    );
  }
  if (!userMobile || typeof userMobile !== "string" || userMobile.trim() === "") {
    console.error("❌ Invalid userMobile:", userMobile);
    throw new functions.https.HttpsError(
      "invalid-argument",
      "userMobile is required and must be a non-empty string"
    );
  }

  console.log(`📋 Generating token for booking: ${bookingId}`);
  console.log(`👤 Using resolved userMobile: ${userMobile}`);

  // ═══════════════════════════════════════════════════════════════
  // STEP 3: Fetch Booking from Firestore (Match App Logic Exactly)
  // ═══════════════════════════════════════════════════════════════

  console.log(`📂 Reading Firestore document: Bookings/${userMobile}`);

  // Use the provided userMobile instead of auth phone
  const userBookingDoc = await db.collection("Bookings")
    .doc(userMobile)
    .get();

    if (!userBookingDoc.exists) {
      console.error(`❌ No bookings document found for user: ${userMobile}`);
      console.error(`💡 Make sure the document ID in Firestore matches: ${userMobile}`);
      throw new functions.https.HttpsError(
        "not-found",
        `No bookings found. Document ID should be: ${userMobile}`
      );
    }

    // Get bookings array - matches HistoryFragment line 183
    const bookingsArray = userBookingDoc.data().bookings || [];
    console.log(`📊 Found ${bookingsArray.length} bookings for user`);

    if (bookingsArray.length === 0) {
      console.error(`❌ Bookings array is empty`);
      throw new functions.https.HttpsError(
        "not-found",
        "No bookings in your account"
      );
    }

    // Find the specific booking by bookingId - matches HistoryFragment line 240
    const booking = bookingsArray.find(b => b.bookingId === bookingId);

    if (!booking) {
      console.error(`❌ Booking ${bookingId} not found in user's ${bookingsArray.length} bookings`);
      console.error(`📋 Available booking IDs:`, bookingsArray.map(b => b.bookingId).join(", "));
      throw new functions.https.HttpsError(
        "not-found",
        `Booking ID "${bookingId}" not found. Available IDs: ${bookingsArray.map(b => b.bookingId).slice(0, 3).join(", ")}`
      );
    }

    console.log(`✅ Found booking: ${booking.serviceName || "Unknown Service"}`);
    console.log(`📦 Booking data:`, JSON.stringify({
      bookingId: booking.bookingId,
      serviceName: booking.serviceName,
      bookingStatus: booking.bookingStatus,
      providerName: booking.providerName || "Not set"
    }));

    // Validate booking status - matches HistoryFragment line 243
    // App uses "bookingStatus" field
    const bookingStatus = (booking.bookingStatus || "pending").toLowerCase();

    console.log(`🔍 Booking status: "${bookingStatus}"`);

    // Allow calling for: accepted, arrived, in_progress, payment_pending
    const allowedStatuses = ["accepted", "arrived", "in_progress", "payment_pending"];

    if (!allowedStatuses.includes(bookingStatus)) {
      console.error(`❌ Invalid booking status for calling: "${bookingStatus}"`);
      console.error(`✅ Allowed statuses:`, allowedStatuses.join(", "));
      throw new functions.https.HttpsError(
        "failed-precondition",
        `Cannot call with status "${bookingStatus}". Booking must be accepted first. Allowed: ${allowedStatuses.join(", ")}`
      );
    }

    console.log(`✅ Booking validated - Status: ${bookingStatus}`);

    // ═══════════════════════════════════════════════════════════════
    // STEP 4: Generate Agora Token
    // ═══════════════════════════════════════════════════════════════

    // Read Agora credentials from Firebase Functions config
    const appId = functions.config().agora?.app_id || process.env.AGORA_APP_ID;
    const appCertificate = functions.config().agora?.app_certificate || process.env.AGORA_APP_CERTIFICATE;

    if (!appId || !appCertificate) {
      console.error("❌ Agora credentials not configured in Firebase Functions config");
      console.error("💡 Set with: firebase functions:config:set agora.app_id=\"YOUR_APP_ID\"");
      console.error("💡 Set with: firebase functions:config:set agora.app_certificate=\"YOUR_CERTIFICATE\"");
      throw new functions.https.HttpsError(
        "internal",
        "Agora credentials not configured. Please contact administrator."
      );
    }

    // Generate unique channel name for this booking
    const channelName = `serveit_booking_${bookingId}`;

    // Generate unique UID for this user (hash of phone number)
    const uid = generateNumericUid(userPhone);

    // Token expires in 10 minutes (600 seconds)
    const expirationTimeInSeconds = 600;
    const currentTimestamp = Math.floor(Date.now() / 1000);
    const privilegeExpiredTs = currentTimestamp + expirationTimeInSeconds;

    // Build RTC token with PUBLISHER role (can send and receive audio)
    const token = RtcTokenBuilder.buildTokenWithUid(
      appId,
      appCertificate,
      channelName,
      uid,
      RtcRole.PUBLISHER,
      privilegeExpiredTs
    );

    console.log(`✅ Token generated successfully`);
    console.log(`   Channel: ${channelName}`);
    console.log(`   UID: ${uid}`);
    console.log(`   Expires: ${new Date(privilegeExpiredTs * 1000).toISOString()}`);

    // ═══════════════════════════════════════════════════════════════
    // STEP 5: Log Call Initiation (Optional Analytics)
    // ═══════════════════════════════════════════════════════════════

    await db.collection("CallLogs").add({
      bookingId: bookingId,
      userId: userId,
      userPhone: userPhone,
      channelName: channelName,
      uid: uid,
      tokenGeneratedAt: admin.firestore.FieldValue.serverTimestamp(),
      expiresAt: admin.firestore.Timestamp.fromMillis(privilegeExpiredTs * 1000),
      status: "initiated"
    });

    const executionTime = Date.now() - startTime;
    console.log(`⏱️  Execution time: ${executionTime}ms`);

    // ═══════════════════════════════════════════════════════════════
    // STEP 6: Return Success Response
    // ═══════════════════════════════════════════════════════════════

    return {
      success: true,
      token: token,
      channelName: channelName,
      uid: uid,
      appId: appId, // Include App ID for Android initialization
      expiresIn: expirationTimeInSeconds,
      expiresAt: privilegeExpiredTs * 1000,
      message: "Token generated successfully"
    };

  } catch (error) {
    console.error("❌ Error in generateAgoraToken:", error);

    // Re-throw HttpsError as-is
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }

    // Wrap unexpected errors
    throw new functions.https.HttpsError(
      "internal",
      `Failed to generate token: ${error.message}`
    );
  }
});

/**
 * Generate numeric UID from phone number
 * Agora requires numeric UID for better performance
 *
 * @param {string} phoneNumber - User's phone number
 * @returns {number} - Numeric UID (0 to 2^32-1)
 */
function generateNumericUid(phoneNumber) {
  if (!phoneNumber) {
    return Math.floor(Math.random() * 1000000);
  }

  // Remove all non-numeric characters
  const numericOnly = phoneNumber.replace(/\D/g, "");

  // Take last 9 digits and convert to number
  // This ensures UID fits in 32-bit integer
  const uid = parseInt(numericOnly.slice(-9)) || Math.floor(Math.random() * 1000000);

  return uid;
}

/**
 * Update Booking Status
 *
 * Called when provider accepts booking
 * Updates status from PENDING → ACCEPTED
 *
 * @param {Object} data - { bookingId: string, providerId: string }
 * @param {Object} context - Firebase Auth context
 */
exports.acceptBooking = functions.https.onCall(async (data, context) => {
  try {
    // Auth check
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "Must be authenticated");
    }

    const { bookingId, providerId } = data;

    if (!bookingId || !providerId) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "bookingId and providerId are required"
      );
    }

    console.log(`📞 Provider ${providerId} accepting booking ${bookingId}`);

    // Find booking and update status
    const bookingsQuery = await db.collection("Bookings").get();

    for (const doc of bookingsQuery.docs) {
      const bookingsArray = doc.data().bookings || [];
      const bookingIndex = bookingsArray.findIndex(b => b.bookingId === bookingId);

      if (bookingIndex !== -1) {
        bookingsArray[bookingIndex].bookingStatus = "accepted";
        bookingsArray[bookingIndex].providerId = providerId;
        bookingsArray[bookingIndex].acceptedAt = admin.firestore.FieldValue.serverTimestamp();
        bookingsArray[bookingIndex].updatedAt = admin.firestore.FieldValue.serverTimestamp();

        await doc.ref.update({
          bookings: bookingsArray,
          lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        });

        console.log(`✅ Booking ${bookingId} status updated to ACCEPTED`);

        return {
          success: true,
          message: "Booking accepted successfully"
        };
      }
    }

    throw new functions.https.HttpsError("not-found", "Booking not found");

  } catch (error) {
    console.error("❌ Error accepting booking:", error);
    throw error instanceof functions.https.HttpsError
      ? error
      : new functions.https.HttpsError("internal", error.message);
  }
});

/**
 * End Call and Log Duration
 *
 * Called when call ends
 * Updates call log with duration and status
 */
exports.endCall = functions.https.onCall(async (data, context) => {
  try {
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "Must be authenticated");
    }

    const { bookingId, duration, endReason } = data;

    console.log(`📞 Ending call for booking ${bookingId}, Duration: ${duration}s`);

    // Update call log
    const callLogsQuery = await db.collection("CallLogs")
      .where("bookingId", "==", bookingId)
      .orderBy("tokenGeneratedAt", "desc")
      .limit(1)
      .get();

    if (!callLogsQuery.empty) {
      const callLogDoc = callLogsQuery.docs[0];
      await callLogDoc.ref.update({
        status: "completed",
        endedAt: admin.firestore.FieldValue.serverTimestamp(),
        durationSeconds: duration || 0,
        endReason: endReason || "user_hangup"
      });
    }

    return { success: true };

  } catch (error) {
    console.error("❌ Error ending call:", error);
    throw error instanceof functions.https.HttpsError
      ? error
      : new functions.https.HttpsError("internal", error.message);
  }
});

/**
 * Test Function - Check Booking Setup
 *
 * Helps debug booking configuration
 * Call this to verify your booking structure
 *
 * Usage: checkBooking({ bookingId: "your_booking_id" })
 */
exports.checkBooking = functions.https.onCall(async (data, context) => {
  try {
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "Must be authenticated");
    }

    const { bookingId } = data;
    const userPhone = context.auth.token.phone_number;

    if (!userPhone) {
      return {
        success: false,
        error: "No phone number in auth token",
        hint: "Make sure you're logged in with phone auth"
      };
    }

    // Format phone number exactly like the app
    const formattedPhone = userPhone.startsWith("+91") ? userPhone : `+91${userPhone}`;
    console.log(`🔍 Checking booking ${bookingId || "ALL"} for user ${formattedPhone}`);

    const userBookingDoc = await db.collection("Bookings")
      .doc(formattedPhone)
      .get();

    if (!userBookingDoc.exists) {
      return {
        success: false,
        error: "No bookings document found",
        phone: formattedPhone,
        hint: `Create a document in Firestore: Bookings/${formattedPhone}`
      };
    }

    const bookingsArray = userBookingDoc.data().bookings || [];

    // If no bookingId provided, return all bookings
    if (!bookingId) {
      return {
        success: true,
        message: `Found ${bookingsArray.length} bookings`,
        phone: formattedPhone,
        totalBookings: bookingsArray.length,
        bookings: bookingsArray.map(b => ({
          bookingId: b.bookingId,
          serviceName: b.serviceName,
          status: b.bookingStatus || "Unknown",
          providerName: b.providerName || "Not set",
          canMakeCall: ["accepted", "arrived", "in_progress", "payment_pending"].includes(
            (b.bookingStatus || "").toLowerCase()
          )
        }))
      };
    }

    // Find specific booking
    const booking = bookingsArray.find(b => b.bookingId === bookingId);

    if (!booking) {
      return {
        success: false,
        error: "Booking not found",
        phone: formattedPhone,
        requestedBookingId: bookingId,
        totalBookings: bookingsArray.length,
        availableBookingIds: bookingsArray.map(b => b.bookingId),
        hint: "Check if the bookingId matches exactly (case-sensitive)"
      };
    }

    const bookingStatus = (booking.bookingStatus || "pending").toLowerCase();
    const canMakeCall = ["accepted", "arrived", "in_progress", "payment_pending"].includes(bookingStatus);

    return {
      success: true,
      message: "✅ Booking found!",
      phone: formattedPhone,
      booking: {
        bookingId: booking.bookingId,
        serviceName: booking.serviceName,
        status: booking.bookingStatus || "Unknown",
        providerName: booking.providerName || "Not set",
        providerPhone: booking.providerMobile || "9322067937",
        totalPrice: booking.totalPrice || 0,
        createdAt: booking.createdAt || null,
        canMakeCall: canMakeCall,
        callAllowedStatuses: ["accepted", "arrived", "in_progress", "payment_pending"],
        currentStatus: bookingStatus
      },
      verdict: canMakeCall
        ? "✅ This booking CAN make calls"
        : `❌ Cannot call with status "${bookingStatus}". Change status to: accepted/arrived/in_progress`
    };

  } catch (error) {
    console.error("❌ Error checking booking:", error);
    return {
      success: false,
      error: error.message,
      stack: error.stack
    };
  }
});

/**
 * Update Booking with Provider Info
 *
 * For testing - adds provider details to booking
 */
exports.updateBookingProvider = functions.https.onCall(async (data, context) => {
  try {
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "Must be authenticated");
    }

    const { bookingId, providerPhone } = data;
    const userPhone = context.auth.token.phone_number;

    if (!userPhone || !bookingId) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "userPhone and bookingId required"
      );
    }

    const userBookingDoc = await db.collection("Bookings")
      .doc(userPhone)
      .get();

    if (!userBookingDoc.exists) {
      throw new functions.https.HttpsError("not-found", "No bookings found");
    }

    const bookingsArray = userBookingDoc.data().bookings || [];
    const bookingIndex = bookingsArray.findIndex(b => b.bookingId === bookingId);

    if (bookingIndex === -1) {
      throw new functions.https.HttpsError("not-found", "Booking not found");
    }

    // Update booking with provider info
    bookingsArray[bookingIndex].providerPhone = providerPhone || "9322067937";
    bookingsArray[bookingIndex].providerName = "Service Provider";
    bookingsArray[bookingIndex].providerRating = 4.5;
    bookingsArray[bookingIndex].updatedAt = admin.firestore.FieldValue.serverTimestamp();

    await db.collection("Bookings")
      .doc(userPhone)
      .update({
        bookings: bookingsArray,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp()
      });

    console.log(`✅ Updated booking ${bookingId} with provider info`);

    return {
      success: true,
      message: "Provider info updated",
      providerPhone: providerPhone || "9322067937"
    };

  } catch (error) {
    console.error("❌ Error updating booking:", error);
    throw error instanceof functions.https.HttpsError
      ? error
      : new functions.https.HttpsError("internal", error.message);
  }
});

/**
 * Validate Call Permission
 *
 * Checks if a user can make a call for a specific booking
 * Returns booking details and call permission status
 */
exports.validateCallPermission = functions.https.onCall(async (data, context) => {
  console.log("🔍 validateCallPermission invoked");
  console.log("📍 Function: validateCallPermission");
  console.log("📍 Region:", process.env.FUNCTION_REGION || "us-central1");
  console.log("🛡️ AppCheck present:", !!context.app?.appId);

  try {
    if (!context.auth) {
      console.log("❌ AUTH FAILED: No authentication context");
      throw new functions.https.HttpsError("unauthenticated", "Must be authenticated");
    }

    console.log("✅ AUTH SUCCESS: uid =", context.auth.uid);
    console.log("📞 Caller phone:", context.auth.token.phone_number);

    const { bookingId, userMobile, callerRole } = data;
    console.log("📥 INPUT DATA:", JSON.stringify({ bookingId, userMobile, callerRole }));

    if (!bookingId || !userMobile || !callerRole) {
      console.log("❌ VALIDATION FAILED: Missing required fields");
      throw new functions.https.HttpsError(
        "invalid-argument",
        "bookingId, userMobile, and callerRole are required"
      );
    }

    // Step 1: Read ONLY the user's booking document
    console.log("📂 STEP 1: Reading Firestore document");
    console.log("🎯 Target path: Bookings/" + userMobile);

    const bookingDoc = await db.collection("Bookings").doc(userMobile).get();

    console.log("📄 Document exists:", bookingDoc.exists);

    if (!bookingDoc.exists) {
      console.log("❌ STEP 1 FAILED: User booking document not found");
      throw new functions.https.HttpsError(
        "not-found",
        "User booking document not found"
      );
    }

    console.log("✅ STEP 1 SUCCESS: Document found");

    // Step 2: Find booking inside bookings[]
    console.log("🔎 STEP 2: Searching for booking in array");
    const bookings = bookingDoc.data().bookings || [];
    console.log("📊 Bookings array length:", bookings.length);

    const booking = bookings.find(b => b.bookingId === bookingId);
    console.log("🎫 Booking found:", !!booking);

    if (!booking) {
      console.log("❌ STEP 2 FAILED: Booking not found in array");
      console.log("🎯 Searched bookingId:", bookingId);
      console.log("📋 Available bookingIds:", bookings.map(b => b.bookingId));
      throw new functions.https.HttpsError(
        "not-found",
        "Booking not found"
      );
    }

    console.log("✅ STEP 2 SUCCESS: Booking found");
    console.log("📦 Booking data:", JSON.stringify(booking));

    // Step 3: Role-based validation
    console.log("🔐 STEP 3: Role-based validation");
    console.log("👤 Caller role:", callerRole);

    if (callerRole === "PROVIDER") {
      console.log("🔗 Checking provider assignment");
      console.log("🎯 Expected providerId:", booking.providerId);
      console.log("🎯 Actual caller uid:", context.auth.uid);

      if (booking.providerId !== context.auth.uid) {
        console.log("❌ STEP 3 FAILED: Provider not assigned to booking");
        throw new functions.https.HttpsError(
          "permission-denied",
          "Provider not assigned to this booking"
        );
      }
      console.log("✅ STEP 3 SUCCESS: Provider authorized");
    } else if (callerRole === "USER") {
      console.log("👤 Validating USER role permissions");
      console.log("🎯 Booking providerId exists:", !!booking.providerId);

      if (!booking.providerId) {
        console.log("❌ STEP 3 FAILED: Booking has no assigned provider");
        throw new functions.https.HttpsError(
          "failed-precondition",
          "Booking must have an assigned provider to initiate calls"
        );
      }
      console.log("✅ STEP 3 SUCCESS: User authorized to call assigned provider");
    } else {
      console.log("❌ STEP 3 FAILED: Invalid callerRole");
      throw new functions.https.HttpsError(
        "invalid-argument",
        "callerRole must be either 'USER' or 'PROVIDER'"
      );
    }

    // Step 4: Booking status validation (role-specific)
    console.log("📋 STEP 4: Booking status validation");
    console.log("📊 Current status:", booking.bookingStatus);

    let allowedStatuses;
    if (callerRole === "PROVIDER") {
      allowedStatuses = ["accepted", "arrived", "in_progress", "completed"];
    } else if (callerRole === "USER") {
      allowedStatuses = ["accepted", "arrived", "in_progress", "payment_pending"];
    }

    console.log("✅ Allowed statuses for", callerRole + ":", allowedStatuses);

    if (!allowedStatuses.includes(booking.bookingStatus)) {
      console.log("❌ STEP 4 FAILED: Invalid booking status");
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Call not allowed for this booking status"
      );
    }

    console.log("✅ STEP 4 SUCCESS: Status validated");

    // Step 5: Return success + booking data for call setup
    console.log("🎉 FINAL RESULT: CALL PERMISSION GRANTED");
    console.log("📞 Booking ready for call setup:", booking.bookingId);

    return {
      allowed: true,
      booking
    };

  } catch (error) {
    console.error("💥 EXCEPTION in validateCallPermission:", error);
    console.error("❌ Error type:", error.constructor.name);
    console.error("❌ Error message:", error.message);

    if (error instanceof functions.https.HttpsError) {
      console.error("🚫 HttpsError code:", error.code);
      console.error("🚫 HttpsError details:", error.details);
    }

    throw error instanceof functions.https.HttpsError
      ? error
      : new functions.https.HttpsError("internal", error.message);
  }
});
