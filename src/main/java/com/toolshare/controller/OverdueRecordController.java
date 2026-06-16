package com.toolshare.controller;

import com.toolshare.dto.ApiResponse;
import com.toolshare.dto.PageResponse;
import com.toolshare.dto.overduerecord.OverdueRecordResponse;
import com.toolshare.service.OverdueRecordService;
import com.toolshare.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/overdue-records")
@Tag(name = "逾期记录管理", description = "逾期记录的查询和处理")
public class OverdueRecordController {

    private final OverdueRecordService overdueRecordService;

    public OverdueRecordController(OverdueRecordService overdueRecordService) {
        this.overdueRecordService = overdueRecordService;
    }

    @GetMapping
    @Operation(summary = "获取逾期记录列表")
    public ApiResponse<PageResponse<OverdueRecordResponse>> getAllOverdueRecords(
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(required = false) Long requesterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(overdueRecordService.getAllOverdueRecords(
                resolved, requesterId, page, size, sortBy, sortDir, currentUserId));
    }

    @GetMapping("/mine")
    @Operation(summary = "获取我的逾期记录")
    public ApiResponse<PageResponse<OverdueRecordResponse>> getMyOverdueRecords(
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long requesterId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(overdueRecordService.getMyOverdueRecords(requesterId, resolved, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取逾期记录详情")
    public ApiResponse<OverdueRecordResponse> getOverdueRecordById(@PathVariable Long id) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(overdueRecordService.getOverdueRecordById(id, currentUserId));
    }

    @PatchMapping("/{id}/resolve")
    @Operation(summary = "标记逾期记录为已处理")
    public ApiResponse<OverdueRecordResponse> resolveOverdueRecord(@PathVariable Long id) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(overdueRecordService.resolveOverdueRecord(id, currentUserId));
    }

    @GetMapping("/stats/unresolved-count")
    @Operation(summary = "获取未处理逾期数量")
    public ApiResponse<Long> getUnresolvedCount() {
        return ApiResponse.success(overdueRecordService.getUnresolvedCount());
    }
}
