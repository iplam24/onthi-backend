package com.onthi.v_edu.social.service;

import com.onthi.v_edu.social.entity.Notification;
import com.onthi.v_edu.social.repository.NotificationRepository;
import com.onthi.v_edu.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public void createNotification(User user, String title, String message, String type, String targetUrl) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setTargetUrl(targetUrl);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    public Page<Notification> getMyNotifications(User user, Pageable pageable) {
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(user.getId(), pageable);
    }

    @Override
    public long getUnreadCount(User user) {
        return notificationRepository.countByUser_IdAndReadFalse(user.getId());
    }

    @Override
    public void markAsRead(Integer notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Override
    public void markAllAsRead(User user) {
        Page<Notification> unread = notificationRepository.findByUser_IdOrderByCreatedAtDesc(user.getId(), Pageable.unpaged());
        unread.forEach(n -> {
            if (!n.isRead()) {
                n.setRead(true);
            }
        });
        notificationRepository.saveAll(unread);
    }
}
