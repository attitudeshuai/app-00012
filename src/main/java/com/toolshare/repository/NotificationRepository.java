package com.toolshare.repository;

import com.toolshare.entity.Notification;
import com.toolshare.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserId(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndRead(Long userId, boolean read, Pageable pageable);

    long countByUserIdAndRead(Long userId, boolean read);

    List<Notification> findByUserIdAndTypeAndRelatedIdAndRead(Long userId, NotificationType type, Long relatedId, boolean read);
}
