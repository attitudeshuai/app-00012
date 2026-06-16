package com.toolshare.controller;

import com.toolshare.dto.ApiResponse;
import com.toolshare.dto.PageResponse;
import com.toolshare.dto.borrowrequest.BorrowRequestResponse;
import com.toolshare.dto.borrowrequest.CreateBorrowRequestRequest;
import com.toolshare.dto.borrowrequest.UpdateBorrowRequestRequest;
import com.toolshare.dto.borrowrequest.UpdateBorrowStatusRequest;
import com.toolshare.entity.BorrowRequestStatus;
import com.toolshare.service.BorrowRequestService;
import com.toolshare.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/borrowrequests")
@Tag(name = "借用申请管理", description = "借用申请的增删改查")
public class BorrowRequestController {

    private final BorrowRequestService borrowRequestService;

    public BorrowRequestController(BorrowRequestService borrowRequestService) {
        this.borrowRequestService = borrowRequestService;
    }

    @GetMapping
    @Operation(summary = "获取借用申请列表")
    public ApiResponse<PageResponse<BorrowRequestResponse>> getAllBorrowRequests(
            @RequestParam(required = false) BorrowRequestStatus status,
            @RequestParam(required = false) Long requesterId,
            @RequestParam(required = false) Long toolId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.success(borrowRequestService.getAllBorrowRequests(
                status, requesterId, toolId, startDate, endDate, page, size, sortBy, sortDir));
    }

    @GetMapping("/mine")
    @Operation(summary = "获取我的借用申请")
    public ApiResponse<PageResponse<BorrowRequestResponse>> getMyBorrowRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long requesterId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(borrowRequestService.getMyBorrowRequests(requesterId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取借用申请详情")
    public ApiResponse<BorrowRequestResponse> getBorrowRequestById(@PathVariable Long id) {
        return ApiResponse.success(borrowRequestService.getBorrowRequestById(id));
    }

    @PostMapping
    @Operation(summary = "创建借用申请")
    public ApiResponse<BorrowRequestResponse> createBorrowRequest(@Valid @RequestBody CreateBorrowRequestRequest request) {
        Long requesterId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(borrowRequestService.createBorrowRequest(request, requesterId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新借用申请")
    public ApiResponse<BorrowRequestResponse> updateBorrowRequest(@PathVariable Long id,
                                                                   @Valid @RequestBody UpdateBorrowRequestRequest request) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(borrowRequestService.updateBorrowRequest(id, request, currentUserId));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "修改借用申请状态")
    public ApiResponse<BorrowRequestResponse> updateBorrowStatus(@PathVariable Long id,
                                                                  @Valid @RequestBody UpdateBorrowStatusRequest request) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(borrowRequestService.updateBorrowStatus(id, request, currentUserId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除借用申请")
    public ApiResponse<Void> deleteBorrowRequest(@PathVariable Long id) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        borrowRequestService.deleteBorrowRequest(id, currentUserId);
        return ApiResponse.success("删除成功", null);
    }
}
