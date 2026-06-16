package com.toolshare.service;

import com.toolshare.dto.PageResponse;
import com.toolshare.dto.notification.NotificationResponse;
import com.toolshare.entity.Notification;
import com.toolshare.entity.NotificationType;
import com.toolshare.exception.ResourceNotFoundException;
import com.toolshare.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public PageResponse<NotificationResponse> getMyNotifications(Long userId, Boolean read, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Notification> notificationPage;

        if (read != null) {
            notificationPage = notificationRepository.findByUserIdAndRead(userId, read, pageable);
        } else {
            notificationPage = notificationRepository.findByUserId(userId, pageable);
        }

        Page<NotificationResponse> responsePage = notificationPage.map(this::toResponse);
        return PageResponse.from(responsePage);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndRead(userId, false);
    }

    @Transactional
    public NotificationResponse markAsRead(Long id, Long userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("通知不存在"));

        if (!notification.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("通知不存在");
        }

        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        return toResponse(saved);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        Pageable pageable = PageRequest.of(0, 1000);
        Page<Notification> unreadPage = notificationRepository.findByUserIdAndRead(userId, false, pageable);

        while (!unreadPage.isEmpty()) {
            for (Notification notification : unreadPage.getContent()) {
                notification.setRead(true);
            }
            notificationRepository.saveAll(unreadPage.getContent());

            if (unreadPage.hasNext()) {
                unreadPage = notificationRepository.findByUserIdAndRead(userId, false,
                        PageRequest.of(unreadPage.getNumber() + 1, 1000));
            } else {
                break;
            }
        }
    }

    @Transactional
    public void deleteNotification(Long id, Long userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("通知不存在"));

        if (!notification.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("通知不存在");
        }

        notificationRepository.delete(notification);
    }

    @Transactional
    public void createNotification(Long userId, NotificationType type, String title, String content, Long relatedId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedId(relatedId);
        notification.setRead(false);
        notificationRepository.save(notification);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getRelatedId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
