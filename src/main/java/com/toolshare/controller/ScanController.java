package com.toolshare.controller;

import com.toolshare.dto.ApiResponse;
import com.toolshare.dto.scan.ScanBorrowRequest;
import com.toolshare.dto.scan.ScanBorrowResult;
import com.toolshare.dto.scan.ScanReturnRequest;
import com.toolshare.dto.scan.ScanReturnResult;
import com.toolshare.dto.scan.ScanToolBoxResponse;
import com.toolshare.service.ScanService;
import com.toolshare.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scan")
@Tag(name = "扫码借还", description = "通过扫描工具箱编码快速借还工具")
public class ScanController {

    private final ScanService scanService;

    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    @GetMapping("/toolbox/{code}")
    @Operation(summary = "扫描工具箱编码，获取工具箱及工具列表",
            description = "扫码后返回工具箱详情及其中所有工具的状态信息，包括当前借用人和预计归还日期")
    public ApiResponse<ScanToolBoxResponse> scanToolBox(@PathVariable String code) {
        return ApiResponse.success(scanService.getToolBoxByCode(code));
    }

    @PostMapping("/borrow")
    @Operation(summary = "扫码快速借用工具",
            description = "选择工具箱中的多个工具进行批量借用，借用申请直接批准生效（无需审核）")
    public ApiResponse<ScanBorrowResult> borrowTools(@Valid @RequestBody ScanBorrowRequest request) {
        Long requesterId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(scanService.borrowTools(request, requesterId));
    }

    @PostMapping("/return")
    @Operation(summary = "扫码快速归还工具",
            description = "选择工具箱中的多个工具进行批量归还，工具箱管理员、工具所有者或借用人均可操作")
    public ApiResponse<ScanReturnResult> returnTools(@Valid @RequestBody ScanReturnRequest request) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(scanService.returnTools(request, currentUserId));
    }
}
