package com.toolshare.dto.borrowrequest;

import com.toolshare.entity.BorrowRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BorrowRequestResponse {
    private Long id;
    private Long toolId;
    private String toolName;
    private Long requesterId;
    private String requesterName;
    private LocalDate startDate;
    private LocalDate expectedReturnDate;
    private LocalDate actualReturnDate;
    private BorrowRequestStatus status;
    private String remark;
    private LocalDateTime createdAt;
    private Boolean hasReviewed;
    private Boolean isOverdue;
    private Integer overdueDays;
    private Boolean isDueSoon;
}
