package com.toolshare.controller;

import com.toolshare.dto.ApiResponse;
import com.toolshare.dto.PageResponse;
import com.toolshare.dto.toolbox.CreateToolBoxRequest;
import com.toolshare.dto.toolbox.ToolBoxResponse;
import com.toolshare.dto.toolbox.UpdateToolBoxRequest;
import com.toolshare.service.ToolBoxService;
import com.toolshare.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/toolboxes")
@Tag(name = "工具箱管理", description = "工具箱的增删改查")
public class ToolBoxController {

    private final ToolBoxService toolBoxService;

    public ToolBoxController(ToolBoxService toolBoxService) {
        this.toolBoxService = toolBoxService;
    }

    @GetMapping
    @Operation(summary = "获取工具箱列表")
    public ApiResponse<PageResponse<ToolBoxResponse>> getAllToolBoxes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.success(toolBoxService.getAllToolBoxes(keyword, isActive, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取工具箱详情")
    public ApiResponse<ToolBoxResponse> getToolBoxById(@PathVariable Long id) {
        return ApiResponse.success(toolBoxService.getToolBoxById(id));
    }

    @PostMapping
    @Operation(summary = "创建工具箱")
    public ApiResponse<ToolBoxResponse> createToolBox(@Valid @RequestBody CreateToolBoxRequest request) {
        Long managerId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(toolBoxService.createToolBox(request, managerId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新工具箱")
    public ApiResponse<ToolBoxResponse> updateToolBox(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateToolBoxRequest request) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(toolBoxService.updateToolBox(id, request, currentUserId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除工具箱")
    public ApiResponse<Void> deleteToolBox(@PathVariable Long id) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        toolBoxService.deleteToolBox(id, currentUserId);
        return ApiResponse.success("删除成功", null);
    }
}
