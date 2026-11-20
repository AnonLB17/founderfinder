const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

// ====================== EXISTING FUNCTIONS ======================

exports.sendChatPushNotification = functions.firestore
    .document("conversations/{chatId}/messages/{messageId}")
    .onCreate(async (snap, context) => {
        const message = snap.data();
        const chatId = context.params.chatId;

        console.log("New private message detected in chat:", chatId);

        if (!message || !message.recipientId) return null;

        await sendPushNotification(
            message.recipientId,
            message.senderName || "Someone",
            message.text ? message.text.substring(0, 100) : "Sent you a message",
            "PrivateChat",
            { chatId: chatId, type: "chat_message" }
        );
    });

exports.sendCommentPushNotification = functions.firestore
    .document("category/{category}/forum/{forumId}/threads/{threadId}/comments/{commentId}")
    .onCreate(async (snap, context) => {
        const comment = snap.data();
        const { category, forumId, threadId } = context.params;

        console.log("New comment in thread:", threadId);

        if (!comment || !comment.creatorId) return null;

        const threadDoc = await admin.firestore()
            .collection("category")
            .doc(category)
            .collection("forum")
            .doc(forumId)
            .collection("threads")
            .doc(threadId)
            .get();

        const threadOwnerId = threadDoc.data()?.creatorId;

        if (!threadOwnerId || threadOwnerId === comment.creatorId) {
            console.log("No valid thread owner or self-comment");
            return null;
        }

        const title = `New Comment from ${comment.creatorName || "Someone"}`;
        const body = comment.message ? comment.message.substring(0, 100) : "Commented on your thread";

        await sendPushNotification(
            threadOwnerId,
            comment.creatorName || "Someone",
            body,
            "ThreadScreen",
            { 
                threadId: threadId, 
                forumId: forumId,
                category: category,
                type: "new_comment" 
            }
        );
    });

// ====================== NEW: THREAD NOTIFICATION ======================

exports.sendNewThreadPushNotification = functions.firestore
    .document("category/{category}/forum/{forumId}/threads/{threadId}")
    .onCreate(async (snap, context) => {
        const thread = snap.data();
        const { category, forumId, threadId } = context.params;

        console.log(`New thread created in ${category}/${forumId}`);

        if (!thread || !thread.creatorId) return null;

        // Get forum owner
        const forumDoc = await admin.firestore()
            .collection("category")
            .doc(category)
            .collection("forum")
            .doc(forumId)
            .get();

        const forumOwnerId = forumDoc.data()?.creatorId;

        if (!forumOwnerId || forumOwnerId === thread.creatorId) {
            console.log("Skipping notification: self-post or no forum owner");
            return null;
        }

        await sendPushNotification(
            forumOwnerId,
            thread.creatorName || "Someone",
            thread.message ? thread.message.substring(0, 100) : "Created a new thread",
            "ForumTemplate",
            { 
                forumId: forumId,
                threadId: threadId,
                category: category,
                type: "new_thread" 
            }
        );
    });

// NEW: Reply to Comment Notification (using depth)
exports.sendCommentReplyPushNotification = functions.firestore
    .document("category/{category}/forum/{forumId}/threads/{threadId}/comments/{commentId}")
    .onCreate(async (snap, context) => {
        const comment = snap.data();
        const { category, forumId, threadId, commentId } = context.params;

        console.log("New comment/reply detected:", commentId);

        if (!comment || !comment.creatorId) return null;

        // Determine if this is a reply based on depth
        const isReply = (comment.depth || 0) >= 1;

        if (isReply) {
            // It's a reply - notify the parent comment owner
            const parentCommentId = comment.parentId;
            if (!parentCommentId) return null;

            const parentCommentDoc = await admin.firestore()
                .collection("category")
                .doc(category)
                .collection("forum")
                .doc(forumId)
                .collection("threads")
                .doc(threadId)
                .collection("comments")
                .doc(parentCommentId)
                .get();

            const parentOwnerId = parentCommentDoc.data()?.creatorId;

            if (!parentOwnerId || parentOwnerId === comment.creatorId) {
                console.log("No valid parent owner or self-reply");
                return null;
            }

            const title = `${comment.creatorName || "Someone"} Replied to your Comment`;
            const body = comment.message ? comment.message.substring(0, 100) : "Replied to your comment";

            await sendPushNotification(
                parentOwnerId,
                comment.creatorName || "Someone",
                body,
                "ThreadScreen",
                { 
                    threadId: threadId, 
                    forumId: forumId,
                    category: category,
                    commentId: commentId,
                    type: "comment_reply" 
                }
            );
        } else {
            // It's a top-level comment - notify thread owner (existing logic)
            const threadDoc = await admin.firestore()
                .collection("category")
                .doc(category)
                .collection("forum")
                .doc(forumId)
                .collection("threads")
                .doc(threadId)
                .get();

            const threadOwnerId = threadDoc.data()?.creatorId;

            if (!threadOwnerId || threadOwnerId === comment.creatorId) {
                console.log("No valid thread owner or self-comment");
                return null;
            }

            const title = `New Comment from ${comment.creatorName || "Someone"}`;
            const body = comment.message ? comment.message.substring(0, 100) : "Commented on your thread";

            await sendPushNotification(
                threadOwnerId,
                comment.creatorName || "Someone",
                body,
                "ThreadScreen",
                { 
                    threadId: threadId, 
                    forumId: forumId,
                    category: category,
                    type: "new_comment" 
                }
            );
        }
    });

// ====================== HELPER FUNCTION ======================

async function sendPushNotification(recipientId, senderName, body, screen, data) {
    try {
        const userDoc = await admin.firestore()
            .collection("profiles")
            .doc(recipientId)
            .get();

        let fcmToken = userDoc.data()?.fcmToken;

        if (!fcmToken) {
            const userFallback = await admin.firestore()
                .collection("users")
                .doc(recipientId)
                .get();
            fcmToken = userFallback.data()?.fcmToken;
        }

        if (!fcmToken) {
            console.log(`No FCM token found for user: ${recipientId}`);
            return;
        }

        let title = `New Activity from ${senderName}`;

        // Better title based on type
        if (data.type === "comment_reply") {
            title = `New Reply from ${senderName}`;
        } else if (data.type === "new_comment") {
            title = `New Comment from ${senderName}`;
        } else if (data.type === "new_thread") {
            title = `New Thread from ${senderName}`;
        } else if (data.type === "chat_message") {
            title = `New Message from ${senderName}`;
        }

        const payload = {
            token: fcmToken,
            notification: {
                title: title,
                body: body
            },
            data: {
                screen: screen,
                ...data,
                click_action: "FLUTTER_NOTIFICATION_CLICK"
            }
        };

        console.log(`Sending push: "${title}" to ${recipientId}`);

        await admin.messaging().send(payload);
        console.log("✅ Push notification sent successfully");
    } catch (error) {
        console.error("Push notification error:", error);
    }
}