package com.toolshare.controller;

import com.toolshare.dto.ApiResponse;
import com.toolshare.dto.PageResponse;
import com.toolshare.dto.tooleview.CreateToolReviewRequest;
import com.toolshare.dto.tooleview.ToolReviewResponse;
import com.toolshare.service.ToolReviewService;
import com.toolshare.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "工具评价管理", description = "工具评价的增删改查")
public class ToolReviewController {

    private final ToolReviewService toolReviewService;

    public ToolReviewController(ToolReviewService toolReviewService) {
        this.toolReviewService = toolReviewService;
    }

    @PostMapping
    @Operation(summary = "创建工具评价")
    public ApiResponse<ToolReviewResponse> createReview(@Valid @RequestBody CreateToolReviewRequest request) {
        Long reviewerId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(toolReviewService.createReview(request, reviewerId));
    }

    @GetMapping("/tool/{toolId}")
    @Operation(summary = "获取工具的评价列表")
    public ApiResponse<PageResponse<ToolReviewResponse>> getReviewsByToolId(
            @PathVariable Long toolId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(toolReviewService.getReviewsByToolId(toolId, page, size));
    }

    @GetMapping("/mine")
    @Operation(summary = "获取我的评价列表")
    public ApiResponse<PageResponse<ToolReviewResponse>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long reviewerId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(toolReviewService.getMyReviews(reviewerId, page, size));
    }

    @GetMapping("/borrowrequest/{borrowRequestId}")
    @Operation(summary = "根据借用申请ID获取评价")
    public ApiResponse<ToolReviewResponse> getReviewByBorrowRequestId(@PathVariable Long borrowRequestId) {
        return ApiResponse.success(toolReviewService.getReviewByBorrowRequestId(borrowRequestId));
    }

    @GetMapping("/tool/{toolId}/average")
    @Operation(summary = "获取工具的平均评分")
    public ApiResponse<Double> getAverageRatingByToolId(@PathVariable Long toolId) {
        return ApiResponse.success(toolReviewService.getAverageRatingByToolId(toolId));
    }

    @GetMapping("/tool/{toolId}/count")
    @Operation(summary = "获取工具的评价数量")
    public ApiResponse<Long> getReviewCountByToolId(@PathVariable Long toolId) {
        return ApiResponse.success(toolReviewService.getReviewCountByToolId(toolId));
    }

    @GetMapping("/borrowrequest/{borrowRequestId}/exists")
    @Operation(summary = "检查借用申请是否已评价")
    public ApiResponse<Boolean> hasReviewed(@PathVariable Long borrowRequestId) {
        return ApiResponse.success(toolReviewService.hasReviewed(borrowRequestId));
    }
}
