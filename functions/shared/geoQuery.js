/**
 * Geo-Query Utilities
 * 
 * Provides functions for finding providers within a geographic radius.
 */

const { calculateDistance } = require("./distance");

/**
 * Perform geo-query to find providers within radius
 * @param {Object} db - Firestore database instance
 * @param {number} latitude - Center latitude
 * @param {number} longitude - Center longitude
 * @param {string} serviceName - Required service name
 * @param {number} radiusKm - Search radius in kilometers
 * @return {Promise<Array>} Array of potential providers
 */
async function findProvidersWithGeoQuery(db, latitude, longitude, serviceName, radiusKm) {
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

module.exports = {
  findProvidersWithGeoQuery,
};

