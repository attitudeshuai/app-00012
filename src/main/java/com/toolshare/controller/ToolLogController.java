package com.toolshare.controller;

import com.toolshare.dto.ApiResponse;
import com.toolshare.dto.PageResponse;
import com.toolshare.dto.toollog.CreateToolLogRequest;
import com.toolshare.dto.toollog.ToolLogResponse;
import com.toolshare.dto.toollog.UpdateToolLogRequest;
import com.toolshare.entity.ToolLogAction;
import com.toolshare.service.ToolLogService;
import com.toolshare.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/toollogs")
@Tag(name = "使用日志管理", description = "工具使用日志的增删改查")
public class ToolLogController {

    private final ToolLogService toolLogService;

    public ToolLogController(ToolLogService toolLogService) {
        this.toolLogService = toolLogService;
    }

    @GetMapping
    @Operation(summary = "获取使用日志列表")
    public ApiResponse<PageResponse<ToolLogResponse>> getAllToolLogs(
            @RequestParam(required = false) Long toolId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) ToolLogAction action,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.success(toolLogService.getAllToolLogs(
                toolId, userId, action, startTime, endTime, page, size, sortBy, sortDir));
    }

    @GetMapping("/mine")
    @Operation(summary = "获取我的使用日志")
    public ApiResponse<PageResponse<ToolLogResponse>> getMyToolLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(toolLogService.getMyToolLogs(userId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取使用日志详情")
    public ApiResponse<ToolLogResponse> getToolLogById(@PathVariable Long id) {
        return ApiResponse.success(toolLogService.getToolLogById(id));
    }

    @PostMapping
    @Operation(summary = "创建使用日志")
    public ApiResponse<ToolLogResponse> createToolLog(@Valid @RequestBody CreateToolLogRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(toolLogService.createToolLog(request, userId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新使用日志")
    public ApiResponse<ToolLogResponse> updateToolLog(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateToolLogRequest request) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(toolLogService.updateToolLog(id, request, currentUserId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除使用日志")
    public ApiResponse<Void> deleteToolLog(@PathVariable Long id) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        toolLogService.deleteToolLog(id, currentUserId);
        return ApiResponse.success("删除成功", null);
    }
}
