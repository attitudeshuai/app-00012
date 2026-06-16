package com.toolshare.controller;

import com.toolshare.dto.ApiResponse;
import com.toolshare.dto.UploadResponse;
import com.toolshare.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@Tag(name = "文件管理", description = "文件上传和下载接口")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(value = "/upload/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传头像图片")
    public ApiResponse<UploadResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String fileUrl = fileStorageService.storeFile(file, "avatars");
        UploadResponse response = new UploadResponse(fileUrl, file.getOriginalFilename(), file.getSize());
        return ApiResponse.success(response);
    }

    @PostMapping(value = "/upload/tool", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传工具图片")
    public ApiResponse<UploadResponse> uploadToolImage(@RequestParam("file") MultipartFile file) {
        String fileUrl = fileStorageService.storeFile(file, "tools");
        UploadResponse response = new UploadResponse(fileUrl, file.getOriginalFilename(), file.getSize());
        return ApiResponse.success(response);
    }

    @PostMapping(value = "/upload/toolbox", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传工具箱图片")
    public ApiResponse<UploadResponse> uploadToolBoxImage(@RequestParam("file") MultipartFile file) {
        String fileUrl = fileStorageService.storeFile(file, "toolboxes");
        UploadResponse response = new UploadResponse(fileUrl, file.getOriginalFilename(), file.getSize());
        return ApiResponse.success(response);
    }

    @GetMapping("/download/{subDir}/{fileName:.+}")
    @Operation(summary = "下载文件")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String subDir,
            @PathVariable String fileName) {
        Resource resource = fileStorageService.loadFileAsResource(subDir + "/" + fileName);
        String contentType = "application/octet-stream";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
