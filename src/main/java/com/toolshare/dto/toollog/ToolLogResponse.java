package com.toolshare.dto.toollog;

import com.toolshare.entity.ToolLogAction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolLogResponse {
    private Long id;
    private Long toolId;
    private String toolName;
    private Long userId;
    private String userName;
    private ToolLogAction action;
    private String description;
    private LocalDateTime createdAt;
}
