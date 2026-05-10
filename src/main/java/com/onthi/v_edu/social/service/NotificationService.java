package com.onthi.v_edu.social.service;

import com.onthi.v_edu.social.entity.Notification;
import com.onthi.v_edu.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    void createNotification(User user, String title, String message, String type, String targetUrl);
    Page<Notification> getMyNotifications(User user, Pageable pageable);
    long getUnreadCount(User user);
    void markAsRead(Integer notificationId);
    void markAllAsRead(User user);
}
