package com.toolshare.service;

import com.toolshare.dto.scan.ScanBorrowRequest;
import com.toolshare.dto.scan.ScanBorrowResult;
import com.toolshare.dto.scan.ScanReturnRequest;
import com.toolshare.dto.scan.ScanReturnResult;
import com.toolshare.dto.scan.ScanToolBoxResponse;
import com.toolshare.dto.scan.ScanToolItem;
import com.toolshare.entity.BorrowRequest;
import com.toolshare.entity.BorrowRequestStatus;
import com.toolshare.entity.NotificationType;
import com.toolshare.entity.OverdueRecord;
import com.toolshare.entity.Tool;
import com.toolshare.entity.ToolBox;
import com.toolshare.entity.ToolLogAction;
import com.toolshare.entity.ToolStatus;
import com.toolshare.entity.User;
import com.toolshare.exception.BadRequestException;
import com.toolshare.exception.ResourceNotFoundException;
import com.toolshare.repository.BorrowRequestRepository;
import com.toolshare.repository.OverdueRecordRepository;
import com.toolshare.repository.ToolBoxRepository;
import com.toolshare.repository.ToolRepository;
import com.toolshare.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ScanService {

    private final ToolBoxRepository toolBoxRepository;
    private final ToolRepository toolRepository;
    private final UserRepository userRepository;
    private final BorrowRequestRepository borrowRequestRepository;
    private final ToolLogService toolLogService;
    private final NotificationService notificationService;
    private final OverdueRecordRepository overdueRecordRepository;

    public ScanService(ToolBoxRepository toolBoxRepository,
                       ToolRepository toolRepository,
                       UserRepository userRepository,
                       BorrowRequestRepository borrowRequestRepository,
                       ToolLogService toolLogService,
                       NotificationService notificationService,
                       OverdueRecordRepository overdueRecordRepository) {
        this.toolBoxRepository = toolBoxRepository;
        this.toolRepository = toolRepository;
        this.userRepository = userRepository;
        this.borrowRequestRepository = borrowRequestRepository;
        this.toolLogService = toolLogService;
        this.notificationService = notificationService;
        this.overdueRecordRepository = overdueRecordRepository;
    }

    public ScanToolBoxResponse getToolBoxByCode(String code) {
        ToolBox toolBox = toolBoxRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("工具箱不存在，编码：" + code));

        if (!Boolean.TRUE.equals(toolBox.getIsActive())) {
            throw new BadRequestException("工具箱已停用");
        }

        List<Tool> tools = toolRepository.findByBoxId(toolBox.getId());
        List<Long> toolIds = tools.stream().map(Tool::getId).collect(Collectors.toList());

        Map<Long, BorrowRequest> activeBorrowMap = new HashMap<>();
        if (!toolIds.isEmpty()) {
            List<BorrowRequest> activeBorrows = borrowRequestRepository
                    .findActiveBorrowsByToolIds(toolIds, BorrowRequestStatus.APPROVED);
            for (BorrowRequest br : activeBorrows) {
                activeBorrowMap.put(br.getToolId(), br);
            }
        }

        Map<Long, String> userNameMap = new HashMap<>();
        List<Long> userIds = new ArrayList<>();
        for (Tool tool : tools) {
            userIds.add(tool.getOwnerId());
        }
        for (BorrowRequest br : activeBorrowMap.values()) {
            userIds.add(br.getRequesterId());
        }
        if (!userIds.isEmpty()) {
            userRepository.findAllById(userIds).forEach(u -> userNameMap.put(u.getId(), u.getUsername()));
        }

        List<ScanToolItem> toolItems = new ArrayList<>();
        for (Tool tool : tools) {
            ScanToolItem item = new ScanToolItem();
            item.setId(tool.getId());
            item.setName(tool.getName());
            item.setCategory(tool.getCategory());
            item.setStatus(tool.getStatus());
            item.setDescription(tool.getDescription());
            item.setImage(tool.getImage());
            item.setPurchaseDate(tool.getPurchaseDate());
            item.setOwnerId(tool.getOwnerId());
            item.setOwnerName(userNameMap.get(tool.getOwnerId()));

            BorrowRequest activeBorrow = activeBorrowMap.get(tool.getId());
            if (activeBorrow != null) {
                item.setCurrentBorrowerId(activeBorrow.getRequesterId());
                item.setCurrentBorrowerName(userNameMap.get(activeBorrow.getRequesterId()));
                item.setExpectedReturnDate(activeBorrow.getExpectedReturnDate());
            }

            toolItems.add(item);
        }

        ScanToolBoxResponse response = new ScanToolBoxResponse();
        response.setId(toolBox.getId());
        response.setName(toolBox.getName());
        response.setLocation(toolBox.getLocation());
        response.setCode(toolBox.getCode());
        response.setImage(toolBox.getImage());
        response.setManagerId(toolBox.getManagerId());
        response.setManagerName(userNameMap.get(toolBox.getManagerId()));
        response.setTools(toolItems);

        return response;
    }

    @Transactional
    public ScanBorrowResult borrowTools(ScanBorrowRequest request, Long requesterId) {
        ToolBox toolBox = toolBoxRepository.findByCode(request.getToolBoxCode())
                .orElseThrow(() -> new ResourceNotFoundException("工具箱不存在，编码：" + request.getToolBoxCode()));

        if (!Boolean.TRUE.equals(toolBox.getIsActive())) {
            throw new BadRequestException("工具箱已停用");
        }

        if (request.getExpectedReturnDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("预计归还日期不能早于今天");
        }

        User requester = userRepository.findById(requesterId).orElse(null);
        String requesterName = requester != null ? requester.getUsername() : "未知用户";

        List<Long> toolIds = request.getToolIds();
        List<Tool> tools = toolRepository.findAllById(toolIds);
        Map<Long, Tool> toolMap = tools.stream().collect(Collectors.toMap(Tool::getId, t -> t));

        List<ScanBorrowResult.BorrowResultItem> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (Long toolId : toolIds) {
            Tool tool = toolMap.get(toolId);
            ScanBorrowResult.BorrowResultItem item = new ScanBorrowResult.BorrowResultItem();
            item.setToolId(toolId);

            if (tool == null) {
                item.setSuccess(false);
                item.setMessage("工具不存在");
                item.setToolName("未知工具");
                failCount++;
                results.add(item);
                continue;
            }

            item.setToolName(tool.getName());

            if (!tool.getBoxId().equals(toolBox.getId())) {
                item.setSuccess(false);
                item.setMessage("工具不在此工具箱中");
                failCount++;
                results.add(item);
                continue;
            }

            if (tool.getStatus() != ToolStatus.AVAILABLE) {
                item.setSuccess(false);
                String statusMsg = switch (tool.getStatus()) {
                    case BORROWED -> "工具已被借出";
                    case MAINTENANCE -> "工具正在维护中";
                    case BROKEN -> "工具已损坏";
                    default -> "工具当前不可借用";
                };
                item.setMessage(statusMsg);
                failCount++;
                results.add(item);
                continue;
            }

            BorrowRequest borrowRequest = new BorrowRequest();
            borrowRequest.setToolId(toolId);
            borrowRequest.setRequesterId(requesterId);
            borrowRequest.setStartDate(LocalDate.now());
            borrowRequest.setExpectedReturnDate(request.getExpectedReturnDate());
            borrowRequest.setRemark(request.getRemark());
            borrowRequest.setStatus(BorrowRequestStatus.APPROVED);
            BorrowRequest savedRequest = borrowRequestRepository.save(borrowRequest);

            tool.setStatus(ToolStatus.BORROWED);
            toolRepository.save(tool);

            toolLogService.createLogInternal(toolId, requesterId, ToolLogAction.BORROW, "扫码快速借用");

            notificationService.createNotification(
                    tool.getOwnerId(),
                    NotificationType.NEW_BORROW_REQUEST,
                    "工具已被借出",
                    requesterName + " 通过扫码借用了您的工具「" + tool.getName() + "」",
                    savedRequest.getId()
            );

            item.setSuccess(true);
            item.setMessage("借用成功");
            item.setBorrowRequestId(savedRequest.getId());
            successCount++;
            results.add(item);
        }

        ScanBorrowResult result = new ScanBorrowResult();
        result.setTotalRequested(toolIds.size());
        result.setSuccessCount(successCount);
        result.setFailCount(failCount);
        result.setResults(results);

        return result;
    }

    @Transactional
    public ScanReturnResult returnTools(ScanReturnRequest request, Long currentUserId) {
        ToolBox toolBox = toolBoxRepository.findByCode(request.getToolBoxCode())
                .orElseThrow(() -> new ResourceNotFoundException("工具箱不存在，编码：" + request.getToolBoxCode()));

        if (!Boolean.TRUE.equals(toolBox.getIsActive())) {
            throw new BadRequestException("工具箱已停用");
        }

        boolean isManager = toolBox.getManagerId().equals(currentUserId);

        List<Long> toolIds = request.getToolIds();
        List<Tool> tools = toolRepository.findAllById(toolIds);
        Map<Long, Tool> toolMap = tools.stream().collect(Collectors.toMap(Tool::getId, t -> t));

        List<BorrowRequest> activeBorrows = borrowRequestRepository
                .findActiveBorrowsByToolIds(toolIds, BorrowRequestStatus.APPROVED);
        Map<Long, BorrowRequest> borrowMap = activeBorrows.stream()
                .collect(Collectors.toMap(BorrowRequest::getToolId, br -> br));

        Map<Long, String> userNameMap = new HashMap<>();
        for (BorrowRequest br : activeBorrows) {
            userNameMap.put(br.getRequesterId(), "");
        }
        if (!userNameMap.isEmpty()) {
            userRepository.findAllById(userNameMap.keySet())
                    .forEach(u -> userNameMap.put(u.getId(), u.getUsername()));
        }

        List<ScanReturnResult.ReturnResultItem> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (Long toolId : toolIds) {
            Tool tool = toolMap.get(toolId);
            ScanReturnResult.ReturnResultItem item = new ScanReturnResult.ReturnResultItem();
            item.setToolId(toolId);

            if (tool == null) {
                item.setSuccess(false);
                item.setMessage("工具不存在");
                item.setToolName("未知工具");
                failCount++;
                results.add(item);
                continue;
            }

            item.setToolName(tool.getName());

            if (!tool.getBoxId().equals(toolBox.getId())) {
                item.setSuccess(false);
                item.setMessage("工具不在此工具箱中");
                failCount++;
                results.add(item);
                continue;
            }

            BorrowRequest borrowRequest = borrowMap.get(toolId);
            if (borrowRequest == null) {
                item.setSuccess(false);
                item.setMessage("该工具没有未归还的借用记录");
                failCount++;
                results.add(item);
                continue;
            }

            boolean isBorrower = borrowRequest.getRequesterId().equals(currentUserId);
            boolean isOwner = tool.getOwnerId().equals(currentUserId);
            if (!isManager && !isBorrower && !isOwner) {
                item.setSuccess(false);
                item.setMessage("无权归还此工具（需工具箱管理员、工具所有者或借用人操作）");
                failCount++;
                results.add(item);
                continue;
            }

            borrowRequest.setStatus(BorrowRequestStatus.RETURNED);
            borrowRequest.setActualReturnDate(LocalDate.now());
            borrowRequestRepository.save(borrowRequest);

            tool.setStatus(ToolStatus.AVAILABLE);
            toolRepository.save(tool);

            toolLogService.createLogInternal(toolId, currentUserId, ToolLogAction.RETURN, "扫码快速归还");

            String borrowerName = userNameMap.getOrDefault(borrowRequest.getRequesterId(), "用户");
            notificationService.createNotification(
                    borrowRequest.getRequesterId(),
                    NotificationType.BORROW_RETURNED,
                    "工具归还已确认",
                    "您借用的工具「" + tool.getName() + "」归还已确认，感谢使用！",
                    borrowRequest.getId()
            );

            if (!tool.getOwnerId().equals(borrowRequest.getRequesterId())) {
                notificationService.createNotification(
                        tool.getOwnerId(),
                        NotificationType.BORROW_RETURNED,
                        "工具已归还",
                        borrowerName + " 已归还工具「" + tool.getName() + "」",
                        borrowRequest.getId()
                );
            }

            overdueRecordRepository.findByBorrowRequestId(borrowRequest.getId()).ifPresent(record -> {
                if (!record.isResolved()) {
                    record.setResolved(true);
                    record.setResolvedAt(LocalDateTime.now());
                    overdueRecordRepository.save(record);
                }
            });

            item.setSuccess(true);
            item.setMessage("归还成功");
            item.setBorrowRequestId(borrowRequest.getId());
            successCount++;
            results.add(item);
        }

        ScanReturnResult result = new ScanReturnResult();
        result.setTotalRequested(toolIds.size());
        result.setSuccessCount(successCount);
        result.setFailCount(failCount);
        result.setResults(results);

        return result;
    }
}
