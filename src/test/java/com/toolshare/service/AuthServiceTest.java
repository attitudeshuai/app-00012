package com.toolshare.service;

import com.toolshare.dto.ApiResponse;
import com.toolshare.dto.auth.RegisterRequest;
import com.toolshare.dto.auth.UserResponse;
import com.toolshare.entity.User;
import com.toolshare.exception.BadRequestException;
import com.toolshare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private com.toolshare.config.JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
    }

    @Test
    void register_WhenUsernameExists_ShouldThrowException() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> {
            authService.register(registerRequest);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_WhenEmailExists_ShouldThrowException() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> {
            authService.register(registerRequest);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getCurrentUser_WhenUserExists_ShouldReturnUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = authService.getCurrentUser(1L);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    void getCurrentUser_WhenUserNotFound_ShouldThrowException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            authService.getCurrentUser(999L);
        });
    }
}
