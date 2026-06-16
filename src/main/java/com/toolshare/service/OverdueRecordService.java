package com.toolshare.service;

import com.toolshare.dto.PageResponse;
import com.toolshare.dto.overduerecord.OverdueRecordResponse;
import com.toolshare.entity.BorrowRequest;
import com.toolshare.entity.OverdueRecord;
import com.toolshare.entity.Tool;
import com.toolshare.entity.User;
import com.toolshare.exception.BadRequestException;
import com.toolshare.exception.ResourceNotFoundException;
import com.toolshare.repository.BorrowRequestRepository;
import com.toolshare.repository.OverdueRecordRepository;
import com.toolshare.repository.ToolRepository;
import com.toolshare.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OverdueRecordService {

    private final OverdueRecordRepository overdueRecordRepository;
    private final BorrowRequestRepository borrowRequestRepository;
    private final ToolRepository toolRepository;
    private final UserRepository userRepository;

    public OverdueRecordService(OverdueRecordRepository overdueRecordRepository,
                                BorrowRequestRepository borrowRequestRepository,
                                ToolRepository toolRepository,
                                UserRepository userRepository) {
        this.overdueRecordRepository = overdueRecordRepository;
        this.borrowRequestRepository = borrowRequestRepository;
        this.toolRepository = toolRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public OverdueRecord createOrUpdateOverdueRecord(BorrowRequest borrowRequest) {
        if (borrowRequest.getActualReturnDate() != null) {
            throw new BadRequestException("该借用申请已归还，无需记录逾期");
        }

        LocalDate today = LocalDate.now();
        if (!borrowRequest.getExpectedReturnDate().isBefore(today)) {
            throw new BadRequestException("该借用申请尚未逾期");
        }

        Optional<OverdueRecord> existingOpt = overdueRecordRepository.findByBorrowRequestId(borrowRequest.getId());
        OverdueRecord record;

        if (existingOpt.isPresent()) {
            record = existingOpt.get();
            int overdueDays = (int) ChronoUnit.DAYS.between(borrowRequest.getExpectedReturnDate(), today);
            record.setOverdueDays(overdueDays);
        } else {
            record = new OverdueRecord();
            record.setBorrowRequestId(borrowRequest.getId());
            record.setToolId(borrowRequest.getToolId());
            record.setRequesterId(borrowRequest.getRequesterId());
            record.setExpectedReturnDate(borrowRequest.getExpectedReturnDate());
            record.setOverdueDate(borrowRequest.getExpectedReturnDate().plusDays(1));
            int overdueDays = (int) ChronoUnit.DAYS.between(borrowRequest.getExpectedReturnDate(), today);
            record.setOverdueDays(overdueDays);
            record.setResolved(false);
        }

        return overdueRecordRepository.save(record);
    }

    @Transactional
    public OverdueRecordResponse resolveOverdueRecord(Long id, Long currentUserId) {
        OverdueRecord record = overdueRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("逾期记录不存在"));

        if (!record.getRequesterId().equals(currentUserId)) {
            throw new BadRequestException("无权处理此逾期记录");
        }

        if (record.isResolved()) {
            throw new BadRequestException("该逾期记录已处理");
        }

        BorrowRequest borrowRequest = borrowRequestRepository.findById(record.getBorrowRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("借用申请不存在"));

        if (borrowRequest.getActualReturnDate() == null) {
            throw new BadRequestException("工具尚未归还，无法标记逾期记录为已处理");
        }

        record.setResolved(true);
        record.setResolvedAt(LocalDateTime.now());
        OverdueRecord saved = overdueRecordRepository.save(record);
        return toResponse(saved);
    }

    public PageResponse<OverdueRecordResponse> getAllOverdueRecords(Boolean resolved, Long requesterId,
                                                                    int page, int size, String sortBy, String sortDir,
                                                                    Long currentUserId) {
        if (requesterId != null && !requesterId.equals(currentUserId)) {
            throw new BadRequestException("无权查看他人的逾期记录");
        }
        Long filteredRequesterId = currentUserId;

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<OverdueRecord> recordPage;
        if (resolved != null && filteredRequesterId != null) {
            recordPage = overdueRecordRepository.findByRequesterIdAndResolved(filteredRequesterId, resolved, pageable);
        } else if (resolved != null) {
            recordPage = overdueRecordRepository.findByResolved(resolved, pageable);
        } else if (filteredRequesterId != null) {
            recordPage = overdueRecordRepository.findByRequesterId(filteredRequesterId, pageable);
        } else {
            recordPage = overdueRecordRepository.findAll(pageable);
        }

        List<OverdueRecordResponse> responseList = toResponseList(recordPage.getContent());
        return PageResponse.of(responseList, recordPage.getTotalElements(), recordPage.getNumber(), recordPage.getSize());
    }

    public OverdueRecordResponse getOverdueRecordById(Long id, Long currentUserId) {
        OverdueRecord record = overdueRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("逾期记录不存在"));

        if (!record.getRequesterId().equals(currentUserId)) {
            throw new BadRequestException("无权查看此逾期记录");
        }

        return toResponse(record);
    }

    public PageResponse<OverdueRecordResponse> getMyOverdueRecords(Long requesterId, Boolean resolved, int page, int size) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<OverdueRecord> recordPage;
        if (resolved != null) {
            recordPage = overdueRecordRepository.findByRequesterIdAndResolved(requesterId, resolved, pageable);
        } else {
            recordPage = overdueRecordRepository.findByRequesterId(requesterId, pageable);
        }

        List<OverdueRecordResponse> responseList = toResponseList(recordPage.getContent());
        return PageResponse.of(responseList, recordPage.getTotalElements(), recordPage.getNumber(), recordPage.getSize());
    }

    public long getUnresolvedCount() {
        return overdueRecordRepository.countByResolved(false);
    }

    public long getTotalOverdueCount() {
        return overdueRecordRepository.count();
    }

    public long getOverdueCountByDateRange(LocalDate startDate, LocalDate endDate) {
        return overdueRecordRepository.countByOverdueDateBetween(startDate, endDate);
    }

    private List<OverdueRecordResponse> toResponseList(List<OverdueRecord> records) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> toolIds = records.stream().map(OverdueRecord::getToolId).collect(Collectors.toSet());
        Set<Long> requesterIds = records.stream().map(OverdueRecord::getRequesterId).collect(Collectors.toSet());

        Map<Long, String> toolNameMap = new HashMap<>();
        if (!toolIds.isEmpty()) {
            toolRepository.findAllById(toolIds).forEach(t -> toolNameMap.put(t.getId(), t.getName()));
        }
        Map<Long, String> requesterNameMap = new HashMap<>();
        if (!requesterIds.isEmpty()) {
            userRepository.findAllById(requesterIds).forEach(u -> requesterNameMap.put(u.getId(), u.getUsername()));
        }

        List<OverdueRecordResponse> responses = new ArrayList<>();
        for (OverdueRecord record : records) {
            OverdueRecordResponse response = new OverdueRecordResponse();
            response.setId(record.getId());
            response.setBorrowRequestId(record.getBorrowRequestId());
            response.setToolId(record.getToolId());
            response.setToolName(toolNameMap.get(record.getToolId()));
            response.setRequesterId(record.getRequesterId());
            response.setRequesterName(requesterNameMap.get(record.getRequesterId()));
            response.setExpectedReturnDate(record.getExpectedReturnDate());
            response.setOverdueDate(record.getOverdueDate());
            response.setOverdueDays(record.getOverdueDays());
            response.setResolved(record.isResolved());
            response.setResolvedAt(record.getResolvedAt());
            response.setCreatedAt(record.getCreatedAt());
            response.setUpdatedAt(record.getUpdatedAt());
            responses.add(response);
        }
        return responses;
    }

    private OverdueRecordResponse toResponse(OverdueRecord record) {
        OverdueRecordResponse response = new OverdueRecordResponse();
        response.setId(record.getId());
        response.setBorrowRequestId(record.getBorrowRequestId());
        response.setToolId(record.getToolId());
        response.setRequesterId(record.getRequesterId());
        response.setExpectedReturnDate(record.getExpectedReturnDate());
        response.setOverdueDate(record.getOverdueDate());
        response.setOverdueDays(record.getOverdueDays());
        response.setResolved(record.isResolved());
        response.setResolvedAt(record.getResolvedAt());
        response.setCreatedAt(record.getCreatedAt());
        response.setUpdatedAt(record.getUpdatedAt());

        toolRepository.findById(record.getToolId()).ifPresent(tool ->
                response.setToolName(tool.getName())
        );
        userRepository.findById(record.getRequesterId()).ifPresent(user ->
                response.setRequesterName(user.getUsername())
        );

        return response;
    }
}
