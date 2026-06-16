package com.toolshare.controller;

import com.toolshare.dto.ApiResponse;
import com.toolshare.dto.PageResponse;
import com.toolshare.dto.tool.CreateToolRequest;
import com.toolshare.dto.tool.ToolResponse;
import com.toolshare.dto.tool.UpdateToolRequest;
import com.toolshare.dto.tool.UpdateToolStatusRequest;
import com.toolshare.entity.ToolStatus;
import com.toolshare.service.ToolService;
import com.toolshare.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tools")
@Tag(name = "工具管理", description = "工具的增删改查")
public class ToolController {

    private final ToolService toolService;

    public ToolController(ToolService toolService) {
        this.toolService = toolService;
    }

    @GetMapping
    @Operation(summary = "获取工具列表")
    public ApiResponse<PageResponse<ToolResponse>> getAllTools(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ToolStatus status,
            @RequestParam(required = false) Long boxId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.success(toolService.getAllTools(keyword, category, status, boxId, page, size, sortBy, sortDir));
    }

    @GetMapping("/mine")
    @Operation(summary = "获取我发布的工具")
    public ApiResponse<PageResponse<ToolResponse>> getMyTools(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long ownerId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(toolService.getMyTools(ownerId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取工具详情")
    public ApiResponse<ToolResponse> getToolById(@PathVariable Long id) {
        return ApiResponse.success(toolService.getToolById(id));
    }

    @PostMapping
    @Operation(summary = "创建工具")
    public ApiResponse<ToolResponse> createTool(@Valid @RequestBody CreateToolRequest request) {
        Long ownerId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(toolService.createTool(request, ownerId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新工具")
    public ApiResponse<ToolResponse> updateTool(@PathVariable Long id,
                                                 @Valid @RequestBody UpdateToolRequest request) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(toolService.updateTool(id, request, currentUserId));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "修改工具状态")
    public ApiResponse<ToolResponse> updateToolStatus(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateToolStatusRequest request) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(toolService.updateToolStatus(id, request, currentUserId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除工具")
    public ApiResponse<Void> deleteTool(@PathVariable Long id) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        toolService.deleteTool(id, currentUserId);
        return ApiResponse.success("删除成功", null);
    }
}
