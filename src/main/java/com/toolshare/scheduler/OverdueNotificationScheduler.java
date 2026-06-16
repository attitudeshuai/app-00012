package com.toolshare.scheduler;

import com.toolshare.entity.BorrowRequest;
import com.toolshare.entity.BorrowRequestStatus;
import com.toolshare.entity.NotificationType;
import com.toolshare.entity.OverdueRecord;
import com.toolshare.entity.Tool;
import com.toolshare.repository.BorrowRequestRepository;
import com.toolshare.repository.OverdueRecordRepository;
import com.toolshare.repository.ToolRepository;
import com.toolshare.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class OverdueNotificationScheduler {

    private static final int DUE_SOON_DAYS = 3;

    private final BorrowRequestRepository borrowRequestRepository;
    private final OverdueRecordRepository overdueRecordRepository;
    private final ToolRepository toolRepository;
    private final NotificationService notificationService;

    public OverdueNotificationScheduler(BorrowRequestRepository borrowRequestRepository,
                                        OverdueRecordRepository overdueRecordRepository,
                                        ToolRepository toolRepository,
                                        NotificationService notificationService) {
        this.borrowRequestRepository = borrowRequestRepository;
        this.overdueRecordRepository = overdueRecordRepository;
        this.toolRepository = toolRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void checkBorrows() {
        LocalDate today = LocalDate.now();
        checkDueSoonBorrows(today);
        checkOverdueBorrows(today);
        resolveReturnedOverdueRecords();
    }

    private void checkDueSoonBorrows(LocalDate today) {
        LocalDate fromDate = today.plusDays(1);
        LocalDate toDate = today.plusDays(DUE_SOON_DAYS);
        Pageable pageable = PageRequest.of(0, 100);

        Page<BorrowRequest> dueSoonPage = borrowRequestRepository.findDueSoonBorrows(
                BorrowRequestStatus.APPROVED, fromDate, toDate, pageable);

        while (!dueSoonPage.isEmpty()) {
            List<BorrowRequest> toUpdate = new ArrayList<>();
            for (BorrowRequest borrowRequest : dueSoonPage.getContent()) {
                if (!borrowRequest.isDueSoonNotified()) {
                    Tool tool = toolRepository.findById(borrowRequest.getToolId()).orElse(null);
                    String toolName = tool != null ? tool.getName() : "未知工具";
                    long daysLeft = ChronoUnit.DAYS.between(today, borrowRequest.getExpectedReturnDate());
                    notificationService.createNotification(
                            borrowRequest.getRequesterId(),
                            NotificationType.BORROW_DUE_SOON,
                            "工具即将到期",
                            "您借用的工具「" + toolName + "」还有 " + daysLeft + " 天到期，请按时归还。",
                            borrowRequest.getId()
                    );
                    if (tool != null) {
                        notificationService.createNotification(
                                tool.getOwnerId(),
                                NotificationType.BORROW_DUE_SOON,
                                "工具即将到期",
                                "工具「" + toolName + "」还有 " + daysLeft + " 天到期，请关注借用者归还情况。",
                                borrowRequest.getId()
                        );
                    }
                    borrowRequest.setDueSoonNotified(true);
                    toUpdate.add(borrowRequest);
                }
            }
            if (!toUpdate.isEmpty()) {
                borrowRequestRepository.saveAll(toUpdate);
            }

            if (dueSoonPage.hasNext()) {
                dueSoonPage = borrowRequestRepository.findDueSoonBorrows(
                        BorrowRequestStatus.APPROVED, fromDate, toDate,
                        PageRequest.of(dueSoonPage.getNumber() + 1, 100));
            } else {
                break;
            }
        }
    }

    private void checkOverdueBorrows(LocalDate today) {
        Pageable pageable = PageRequest.of(0, 100);

        Page<BorrowRequest> overduePage = borrowRequestRepository.findOverdueBorrows(
                BorrowRequestStatus.APPROVED, today, pageable);

        while (!overduePage.isEmpty()) {
            List<BorrowRequest> toUpdate = new ArrayList<>();
            for (BorrowRequest borrowRequest : overduePage.getContent()) {
                Tool tool = toolRepository.findById(borrowRequest.getToolId()).orElse(null);
                String toolName = tool != null ? tool.getName() : "未知工具";
                int overdueDays = (int) ChronoUnit.DAYS.between(borrowRequest.getExpectedReturnDate(), today);

                if (!borrowRequest.isOverdueNotified()) {
                    notificationService.createNotification(
                            borrowRequest.getRequesterId(),
                            NotificationType.BORROW_OVERDUE,
                            "工具借用已逾期",
                            "您借用的工具「" + toolName + "」已超过预计归还日期 " + overdueDays + " 天，请尽快归还。",
                            borrowRequest.getId()
                    );
                    if (tool != null) {
                        notificationService.createNotification(
                                tool.getOwnerId(),
                                NotificationType.BORROW_OVERDUE,
                                "工具借用已逾期",
                                "工具「" + toolName + "」已被逾期 " + overdueDays + " 天，请联系借用者催还。",
                                borrowRequest.getId()
                        );
                    }
                    borrowRequest.setOverdueNotified(true);
                    toUpdate.add(borrowRequest);
                }

                createOrUpdateOverdueRecord(borrowRequest, today, overdueDays);
            }
            if (!toUpdate.isEmpty()) {
                borrowRequestRepository.saveAll(toUpdate);
            }

            if (overduePage.hasNext()) {
                overduePage = borrowRequestRepository.findOverdueBorrows(
                        BorrowRequestStatus.APPROVED, today,
                        PageRequest.of(overduePage.getNumber() + 1, 100));
            } else {
                break;
            }
        }
    }

    private void createOrUpdateOverdueRecord(BorrowRequest borrowRequest, LocalDate today, int overdueDays) {
        Optional<OverdueRecord> existingOpt = overdueRecordRepository.findByBorrowRequestId(borrowRequest.getId());
        OverdueRecord record;

        if (existingOpt.isPresent()) {
            record = existingOpt.get();
            record.setOverdueDays(overdueDays);
        } else {
            record = new OverdueRecord();
            record.setBorrowRequestId(borrowRequest.getId());
            record.setToolId(borrowRequest.getToolId());
            record.setRequesterId(borrowRequest.getRequesterId());
            record.setExpectedReturnDate(borrowRequest.getExpectedReturnDate());
            record.setOverdueDate(borrowRequest.getExpectedReturnDate().plusDays(1));
            record.setOverdueDays(overdueDays);
            record.setResolved(false);
        }
        overdueRecordRepository.save(record);
    }

    private void resolveReturnedOverdueRecords() {
        Pageable pageable = PageRequest.of(0, 100);
        Page<OverdueRecord> unresolvedPage = overdueRecordRepository.findByResolved(false, pageable);

        while (!unresolvedPage.isEmpty()) {
            List<OverdueRecord> toUpdate = new ArrayList<>();
            for (OverdueRecord record : unresolvedPage.getContent()) {
                Optional<BorrowRequest> borrowRequestOpt = borrowRequestRepository.findById(record.getBorrowRequestId());
                if (borrowRequestOpt.isPresent()) {
                    BorrowRequest borrowRequest = borrowRequestOpt.get();
                    if (borrowRequest.getActualReturnDate() != null) {
                        record.setResolved(true);
                        record.setResolvedAt(LocalDateTime.now());
                        toUpdate.add(record);
                    }
                }
            }
            if (!toUpdate.isEmpty()) {
                overdueRecordRepository.saveAll(toUpdate);
            }

            if (unresolvedPage.hasNext()) {
                unresolvedPage = overdueRecordRepository.findByResolved(false,
                        PageRequest.of(unresolvedPage.getNumber() + 1, 100));
            } else {
                break;
            }
        }
    }
}
