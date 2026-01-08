/**
 * Dispatch Job to Providers
 * 
 * Firestore onUpdate trigger on Bookings/{phoneNumber}
 * Dispatches new bookings to qualified providers
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { findProvidersWithGeoQuery } = require("../shared/geoUtils");
const { getRoadDistances } = require("../shared/distanceUtils");

function createDispatchJobToProvidersFunction(db, messaging, config, collections) {
  /**
   * Process new booking for job dispatching
   */
  async function processNewBooking(bookingData, phoneNumber) {
    const bookingId = bookingData.bookingId;
    const geoConstants = config.getGeoConstants();
    const googleMapsApiKey = config.getGoogleMapsApiKey();

    console.log(`New booking created: ${bookingId}`, bookingData);

    // Step 1: Get Job's Coordinates from serveit_users collection
    let jobCoordinates = geoConstants.fallbackCoordinates;

    try {
      const userDoc = await db.collection(collections.serveitUsers).doc(phoneNumber).get();

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
      db,
      collections,
      jobCoordinates.latitude,
      jobCoordinates.longitude,
      serviceName,
      geoConstants.geoQueryRadius,
    );

    if (potentialProviders.length === 0) {
      console.error("[MONITORING] Zero providers found for booking", {
        bookingId: bookingId,
        serviceName: serviceName,
        coordinates: [jobCoordinates.latitude, jobCoordinates.longitude],
        radius: geoConstants.geoQueryRadius,
        timestamp: new Date().toISOString(),
        severity: "WARNING",
      });
      console.log("No potential providers found within geo-query radius");
      return;
    }

    console.log(`Found ${potentialProviders.length} potential providers within ${geoConstants.geoQueryRadius}km`);

    // Step 4: Get precise road distances
    const destinations = potentialProviders.map((provider) => ({
      lat: provider.latitude,
      lng: provider.longitude,
    }));

    const distanceResults = await getRoadDistances(
      googleMapsApiKey,
      {lat: jobCoordinates.latitude, lng: jobCoordinates.longitude},
      destinations,
    );

    // Step 5: Filter providers within final distance limit
    const qualifiedProviders = [];

    for (let i = 0; i < potentialProviders.length && i < distanceResults.length; i++) {
      const provider = potentialProviders[i];
      const distanceResult = distanceResults[i];

      if (distanceResult.status === "OK" && distanceResult.distance && distanceResult.distance.value) {
        const distanceKm = distanceResult.distance.value / 1000;

        if (distanceKm <= geoConstants.finalDistanceLimit) {
          qualifiedProviders.push({
            id: provider.id,
            fcmToken: provider.fcmToken,
            distance: distanceKm,
            duration: distanceResult.duration ? distanceResult.duration.value : 0,
          });
        }
      } else if (distanceResult.status === "OK" && typeof distanceResult.distance === "number") {
        const distanceKm = distanceResult.distance;

        if (distanceKm <= geoConstants.finalDistanceLimit) {
          qualifiedProviders.push({
            id: provider.id,
            fcmToken: provider.fcmToken,
            distance: distanceKm,
            duration: 0,
          });
        }
      }
    }

    if (qualifiedProviders.length === 0) {
      console.error("[MONITORING] Zero providers qualified after distance filtering", {
        bookingId: bookingId,
        serviceName: serviceName,
        coordinates: [jobCoordinates.latitude, jobCoordinates.longitude],
        potentialProvidersCount: potentialProviders.length,
        finalDistanceLimit: geoConstants.finalDistanceLimit,
        timestamp: new Date().toISOString(),
        severity: "WARNING",
      });
      return;
    }

    console.log(`${qualifiedProviders.length} providers qualified after distance filtering`);

    // Step 6: Update Booking Document with notified provider IDs
    const notifiedProviderIds = qualifiedProviders.map((provider) => provider.id).filter((id) => id !== undefined);

    try {
      const bookingRef = db.collection(collections.bookings).doc(phoneNumber);
      const currentDoc = await bookingRef.get();
      const currentData = currentDoc.data();

      if (currentData && currentData.bookings && Array.isArray(currentData.bookings)) {
        const bookings = currentData.bookings;
        const bookingIndex = bookings.findIndex((b) => b.bookingId === bookingId);

        if (bookingIndex !== -1) {
          bookings[bookingIndex].notifiedProviderIds = notifiedProviderIds;
          bookings[bookingIndex].status = "pending";
          bookings[bookingIndex].jobCoordinates = jobCoordinates;

          await bookingRef.update({
            bookings: bookings,
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          });
        }
      } else {
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
        new Date(Date.now() + 30 * 60 * 1000)
      );
      
      let bookingIndex = -1;
      if (currentData && currentData.bookings && Array.isArray(currentData.bookings)) {
        bookingIndex = currentData.bookings.findIndex((b) => b.bookingId === bookingId);
      }
      
      for (const provider of qualifiedProviders) {
        const inboxRef = db
          .collection(collections.jobInbox)
          .doc(provider.id)
          .collection("jobs")
          .doc(bookingId);
        
        inboxBatch.set(inboxRef, {
          bookingId: bookingId,
          customerPhone: phoneNumber,
          bookingDocPath: `${collections.bookings}/${phoneNumber}`,
          bookingIndex: bookingIndex >= 0 ? bookingIndex : 0,
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

      messaging.send(message)
        .then(() => {
          console.log(`Wake-up notification sent to provider ${providerId}`);
        })
        .catch((error) => {
          console.error(`Failed to send notification to provider ${providerId}:`, error);
        });
    }

    console.log(`Job dispatch completed for booking ${bookingId} - ${notifiedProviderIds.length} providers notified`);
  }

  return functions.firestore
    .document(`${collections.bookings}/{phoneNumber}`)
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
}

module.exports = { createDispatchJobToProvidersFunction };

