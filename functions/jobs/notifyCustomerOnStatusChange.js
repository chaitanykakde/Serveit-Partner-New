/**
 * Notify Customer on Status Change
 * Firestore onUpdate trigger on Bookings/{phoneNumber}
 * 
 * Notification function - medium risk
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");

/**
 * Send status update notification to customer
 * Helper function used by notifyCustomerOnStatusChange
 */
async function sendCustomerStatusNotification(db, messaging, bookingData, newStatus, phoneNumber) {
  try {
    // Get customer's FCM token from serveit_users collection
    const userDoc = await db
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
    await db
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

    await messaging.send(message);
    console.log(`Status FCM notification sent to customer ${phoneNumber} for status: ${newStatus}`);
  } catch (error) {
    console.error("Error sending customer status notification:", error);
  }
}

function createNotifyCustomerOnStatusChangeFunction(db, messaging) {
  return functions.firestore
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
              await sendCustomerStatusNotification(db, messaging, afterBooking, afterStatus, phoneNumber);
            }
          }
        } else {
          // Handle single booking structure
          const beforeStatus = beforeData?.status;
          const afterStatus = afterData?.status;

          if (beforeStatus !== afterStatus && afterData.acceptedByProviderId) {
            await sendCustomerStatusNotification(db, messaging, afterData, afterStatus, phoneNumber);
          }
        }
      } catch (error) {
        console.error("Error in notifyCustomerOnStatusChange:", error);
      }

      return null;
    });
}

module.exports = { 
  createNotifyCustomerOnStatusChangeFunction,
  sendCustomerStatusNotification,
};

