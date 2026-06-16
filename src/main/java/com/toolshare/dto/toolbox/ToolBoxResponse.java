package com.toolshare.dto.toolbox;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolBoxResponse {
    private Long id;
    private String name;
    private String location;
    private Long managerId;
    private String managerName;
    private String code;
    private String image;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
