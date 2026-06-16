package com.toolshare.dto.scan;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScanToolBoxResponse {
    private Long id;
    private String name;
    private String location;
    private String code;
    private String image;
    private Long managerId;
    private String managerName;
    private List<ScanToolItem> tools;
    private LocalDateTime scannedAt = LocalDateTime.now();
}
