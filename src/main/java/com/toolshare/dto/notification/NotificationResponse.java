package com.toolshare.dto.notification;

import com.toolshare.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private Long userId;
    private NotificationType type;
    private String title;
    private String content;
    private Long relatedId;
    private boolean read;
    private LocalDateTime createdAt;
}
