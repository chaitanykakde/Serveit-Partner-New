/**
 * Geo Query Utilities
 * 
 * Consolidated geo-query and distance calculation functions
 */

const { calculateDistance } = require("./distanceUtils");

/**
 * Perform geo-query to find providers within radius
 * @param {Object} db - Firestore database instance
 * @param {Object} collections - Collection names
 * @param {number} latitude - Center latitude
 * @param {number} longitude - Center longitude
 * @param {string} serviceName - Required service name
 * @param {number} radiusKm - Search radius in kilometers
 * @return {Promise<Array>} Array of potential providers
 */
async function findProvidersWithGeoQuery(db, collections, latitude, longitude, serviceName, radiusKm) {
  // Convert radius to degrees (approximate)
  const radiusDegrees = radiusKm / 111; // 1 degree ≈ 111 km

  const minLat = latitude - radiusDegrees;
  const maxLat = latitude + radiusDegrees;
  const minLng = longitude - radiusDegrees;
  const maxLng = longitude + radiusDegrees;

  // Query ALL providers (can't query nested verificationDetails.status in Firestore)
  // We'll filter client-side using the same logic as sendVerificationNotification
  const allProvidersSnapshot = await db.collection(collections.partners).get();
  const potentialProviders = [];

  console.log(`[findProvidersWithGeoQuery] Service requested: ${serviceName}`);
  console.log(`[findProvidersWithGeoQuery] Job coordinates: [${latitude}, ${longitude}]`);
  console.log(`[findProvidersWithGeoQuery] Total providers in database: ${allProvidersSnapshot.size}`);

  // DIAGNOSTIC: Log first 10 providers for debugging
  let diagnosticCount = 0;
  allProvidersSnapshot.forEach((doc) => {
    if (diagnosticCount < 10) {
      const data = doc.data();
      const status = data?.verificationDetails?.status;
      console.log(`[DIAGNOSTIC] Provider ${doc.id}: verificationDetails.status=${status}, hasLocation=${!!data.locationDetails?.latitude}, hasServices=${!!data.services}`);
      diagnosticCount++;
    }
  });

  // Count verified providers
  let verifiedCount = 0;
  allProvidersSnapshot.forEach((doc) => {
    const data = doc.data();
    // Use same check as sendVerificationNotification.js (lines 24-27)
    const status = data?.verificationDetails?.status;
    if (status === "verified") {
      verifiedCount++;
    }
  });
  console.log(`[findProvidersWithGeoQuery] Total verified providers: ${verifiedCount}`);

  allProvidersSnapshot.forEach((doc) => {
    const providerData = doc.data();

    // Use same verification check as sendVerificationNotification.js (lines 24-27)
    const status = providerData?.verificationDetails?.status;
    if (status !== "verified") {
      console.log(`Provider ${doc.id}: Not verified (verificationDetails.status=${status})`);
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

    // Improved service matching - case-insensitive and check primary service
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

module.exports = {
  findProvidersWithGeoQuery
};

