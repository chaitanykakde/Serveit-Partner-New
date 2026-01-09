/**
 * Distance Calculation Utilities
 * 
 * Provides functions for calculating distances between coordinates
 * using Haversine formula and Google Maps Distance Matrix API.
 */

const axios = require("axios");
const { getGoogleMapsApiKey } = require("./constants");

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

module.exports = {
  calculateDistance,
  getRoadDistances,
};

