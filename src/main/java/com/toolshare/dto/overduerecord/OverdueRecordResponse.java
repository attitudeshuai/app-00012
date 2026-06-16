package com.toolshare.dto.overduerecord;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OverdueRecordResponse {
    private Long id;
    private Long borrowRequestId;
    private Long toolId;
    private String toolName;
    private Long requesterId;
    private String requesterName;
    private LocalDate expectedReturnDate;
    private LocalDate overdueDate;
    private Integer overdueDays;
    private Boolean resolved;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
