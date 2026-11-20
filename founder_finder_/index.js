const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendChatPushNotification = functions.firestore
    .document("conversations/{chatId}/messages/{messageId}")
    .onCreate(async (snap, context) => {
        const message = snap.data();
        const chatId = context.params.chatId;

        console.log("New message detected in chat:", chatId);

        if (!message || !message.recipientId) {
            console.log("Missing recipientId");
            return null;
        }

        const userDoc = await admin.firestore()
            .collection("users")
            .doc(message.recipientId)
            .get();

        const fcmToken = userDoc.data()?.fcmToken;

        if (!fcmToken) {
            console.log("No FCM token found for recipient:", message.recipientId);
            return null;
        }

        const payload = {
            token: fcmToken,
            notification: {
                title: `New Message from ${message.senderName || "Someone"}`,
                body: message.text ? message.text.substring(0, 100) : "Sent you a message"
            },
            data: {
                screen: "PrivateChat",
                chatId: chatId,
                senderId: message.senderId || "",
                type: "chat_message"
            }
        };

        try {
            const response = await admin.messaging().send(payload);
            console.log("Push notification sent successfully. Response:", response);
        } catch (error) {
            console.error("Error sending push:", error);
        }
    });