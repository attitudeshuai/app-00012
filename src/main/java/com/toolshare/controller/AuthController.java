package com.toolshare.controller;

import com.toolshare.dto.ApiResponse;
import com.toolshare.dto.auth.LoginResponse;
import com.toolshare.dto.auth.LoginRequest;
import com.toolshare.dto.auth.RegisterRequest;
import com.toolshare.dto.auth.UpdateUserRequest;
import com.toolshare.dto.auth.UserResponse;
import com.toolshare.service.AuthService;
import com.toolshare.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "用户注册、登录、个人信息管理")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前登录用户信息")
    public ApiResponse<UserResponse> getCurrentUser() {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(authService.getCurrentUser(userId));
    }

    @PutMapping("/me")
    @Operation(summary = "更新个人信息")
    public ApiResponse<UserResponse> updateCurrentUser(@Valid @RequestBody UpdateUserRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(authService.updateCurrentUser(userId, request));
    }
}
