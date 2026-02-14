package com.example.gestionvol.service;

import com.example.gestionvol.entities.Checkout;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * NotificationService: Manage user notifications for booking confirmations and updates
 * 
 * Features:
 * - Track unread notification count
 * - Store notification data (bookingId, status, timestamp)
 * - Persist notifications to JSON or database
 * - Support for real-time notification badges
 */
public class NotificationService {

    private static final Map<Integer, List<Notification>> userNotifications = new HashMap<>();
    private static final Map<Integer, Integer> unreadCounts = new HashMap<>();

    public static class Notification {
        public int notificationId;
        public int bookingId;
        public String status;          // "pending", "approved", "rejected", "cancelled"
        public LocalDateTime timestamp;
        public String pdfPath;
        public boolean isRead;

        public Notification(int bookingId, String status, String pdfPath) {
            this.bookingId = bookingId;
            this.status = status;
            this.pdfPath = pdfPath;
            this.timestamp = LocalDateTime.now();
            this.isRead = false;
        }

        @Override
        public String toString() {
            return "Notification{" +
                    "bookingId=" + bookingId +
                    ", status='" + status + '\'' +
                    ", timestamp=" + timestamp +
                    ", pdfPath='" + pdfPath + '\'' +
                    ", isRead=" + isRead +
                    '}';
        }
    }

    /**
     * Add notification for a user (called when admin accepts/rejects booking)
     */
    public static void addNotification(int userId, int bookingId, String status, String pdfPath) {
        userNotifications.putIfAbsent(userId, new CopyOnWriteArrayList<>());
        
        Notification notification = new Notification(bookingId, status, pdfPath);
        userNotifications.get(userId).add(notification);
        
        // Increment unread count
        unreadCounts.put(userId, unreadCounts.getOrDefault(userId, 0) + 1);
        
        System.out.println("🔔 Notification added for user " + userId + ": " + notification);
    }

    /**
     * Get all notifications for a user
     */
    public static List<Notification> getUserNotifications(int userId) {
        return userNotifications.getOrDefault(userId, new ArrayList<>());
    }

    /**
     * Get unread notification count for a user
     */
    public static int getUnreadCount(int userId) {
        return unreadCounts.getOrDefault(userId, 0);
    }

    /**
     * Mark a notification as read
     */
    public static void markAsRead(int userId, int notificationId) {
        List<Notification> notifications = userNotifications.get(userId);
        if (notifications != null && notificationId < notifications.size()) {
            Notification notif = notifications.get(notificationId);
            if (!notif.isRead) {
                notif.isRead = true;
                unreadCounts.put(userId, Math.max(0, unreadCounts.getOrDefault(userId, 1) - 1));
                System.out.println("✅ Notification marked as read: user=" + userId + ", notif=" + notificationId);
            }
        }
    }

    /**
     * Mark all notifications as read
     */
    public static void markAllAsRead(int userId) {
        List<Notification> notifications = userNotifications.get(userId);
        if (notifications != null) {
            long unreadCount = notifications.stream().filter(n -> !n.isRead).count();
            notifications.forEach(n -> n.isRead = true);
            unreadCounts.put(userId, 0);
            System.out.println("✅ All " + unreadCount + " notifications marked as read for user " + userId);
        }
    }

    /**
     * Delete a notification
     */
    public static void deleteNotification(int userId, int notificationId) {
        List<Notification> notifications = userNotifications.get(userId);
        if (notifications != null && notificationId < notifications.size()) {
            Notification removed = notifications.remove(notificationId);
            if (!removed.isRead) {
                unreadCounts.put(userId, Math.max(0, unreadCounts.getOrDefault(userId, 1) - 1));
            }
            System.out.println("🗑️ Notification deleted: user=" + userId + ", notif=" + notificationId);
        }
    }

    /**
     * Check if user has unread notifications
     */
    public static boolean hasUnreadNotifications(int userId) {
        return getUnreadCount(userId) > 0;
    }

    /**
     * Process booking acceptance (to be called by admin)
     */
    public static void onBookingAccepted(int userId, int bookingId, String pdfPath) {
        addNotification(userId, bookingId, "approved", pdfPath);
    }

    /**
     * Process booking rejection (to be called by admin)
     */
    public static void onBookingRejected(int userId, int bookingId) {
        addNotification(userId, bookingId, "rejected", null);
    }

    /**
     * Clear all notifications for a user
     */
    public static void clearAllNotifications(int userId) {
        List<Notification> notifications = userNotifications.get(userId);
        if (notifications != null) {
            notifications.clear();
            unreadCounts.put(userId, 0);
            System.out.println("🗑️ All notifications cleared for user " + userId);
        }
    }
}
