package com.toolshare.controller;

import com.toolshare.dto.ApiResponse;
import com.toolshare.dto.PageResponse;
import com.toolshare.dto.notification.NotificationResponse;
import com.toolshare.dto.notification.UnreadCountResponse;
import com.toolshare.service.NotificationService;
import com.toolshare.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "站内消息通知", description = "站内消息通知的查询和管理")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "获取我的通知列表")
    public ApiResponse<PageResponse<NotificationResponse>> getMyNotifications(
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(notificationService.getMyNotifications(userId, read, page, size));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读消息数量")
    public ApiResponse<UnreadCountResponse> getUnreadCount() {
        Long userId = SecurityUtil.getCurrentUserId();
        long count = notificationService.getUnreadCount(userId);
        return ApiResponse.success(new UnreadCountResponse(count));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "标记单条通知为已读")
    public ApiResponse<NotificationResponse> markAsRead(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(notificationService.markAsRead(id, userId));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "标记所有通知为已读")
    public ApiResponse<Void> markAllAsRead() {
        Long userId = SecurityUtil.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ApiResponse.success("全部标记为已读", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除单条通知")
    public ApiResponse<Void> deleteNotification(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        notificationService.deleteNotification(id, userId);
        return ApiResponse.success("删除成功", null);
    }
}
