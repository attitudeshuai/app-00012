package com.toolshare.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "borrow_requests")
public class BorrowRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tool_id", nullable = false)
    private Long toolId;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "expected_return_date", nullable = false)
    private LocalDate expectedReturnDate;

    @Column(name = "actual_return_date")
    private LocalDate actualReturnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BorrowRequestStatus status = BorrowRequestStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(name = "overdue_notified", nullable = false)
    private boolean overdueNotified = false;

    @Column(name = "due_soon_notified", nullable = false)
    private boolean dueSoonNotified = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
