package com.toolshare.config;

import com.toolshare.entity.*;
import com.toolshare.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class SeedDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ToolBoxRepository toolBoxRepository;
    private final ToolRepository toolRepository;
    private final BorrowRequestRepository borrowRequestRepository;
    private final ToolLogRepository toolLogRepository;
    private final PasswordEncoder passwordEncoder;

    public SeedDataInitializer(UserRepository userRepository,
                               ToolBoxRepository toolBoxRepository,
                               ToolRepository toolRepository,
                               BorrowRequestRepository borrowRequestRepository,
                               ToolLogRepository toolLogRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.toolBoxRepository = toolBoxRepository;
        this.toolRepository = toolRepository;
        this.borrowRequestRepository = borrowRequestRepository;
        this.toolLogRepository = toolLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User user1 = createUser("admin", "admin@toolshare.com", "admin123");
        User user2 = createUser("zhangsan", "zhangsan@example.com", "123456");
        User user3 = createUser("lisi", "lisi@example.com", "123456");

        ToolBox box1 = createToolBox("1号楼工具箱", "1号楼单元门旁", user1.getId(), "BOX001", true);
        ToolBox box2 = createToolBox("2号楼工具箱", "2号楼物业处", user2.getId(), "BOX002", true);
        ToolBox box3 = createToolBox("3号楼工具箱", "3号楼地下室", user1.getId(), "BOX003", false);

        Tool tool1 = createTool(box1.getId(), "电钻", "电动工具", ToolStatus.AVAILABLE, "博世冲击钻，配多种钻头", LocalDate.of(2023, 1, 15), user1.getId());
        Tool tool2 = createTool(box1.getId(), "梯子", "攀爬工具", ToolStatus.AVAILABLE, "3米铝合金折叠梯", LocalDate.of(2022, 6, 20), user1.getId());
        Tool tool3 = createTool(box1.getId(), "扳手套装", "手动工具", ToolStatus.BORROWED, "12件套套筒扳手", LocalDate.of(2023, 3, 10), user2.getId());
        Tool tool4 = createTool(box2.getId(), "螺丝刀套装", "手动工具", ToolStatus.AVAILABLE, "十字一字各规格螺丝刀", LocalDate.of(2023, 5, 1), user2.getId());
        Tool tool5 = createTool(box2.getId(), "万用表", "测量工具", ToolStatus.MAINTENANCE, "数字万用表，功能齐全", LocalDate.of(2022, 11, 25), user3.getId());
        Tool tool6 = createTool(box2.getId(), "电锯", "电动工具", ToolStatus.AVAILABLE, "手持电锯，注意安全使用", LocalDate.of(2023, 2, 14), user3.getId());
        Tool tool7 = createTool(box1.getId(), "锤子", "手动工具", ToolStatus.BROKEN, "羊角锤，锤头松动需维修", LocalDate.of(2021, 8, 30), user1.getId());
        Tool tool8 = createTool(box3.getId(), "工具箱", "收纳工具", ToolStatus.AVAILABLE, "多功能工具箱，包含多种常用工具", LocalDate.of(2023, 4, 20), user1.getId());

        BorrowRequest br1 = createBorrowRequest(tool3.getId(), user3.getId(), LocalDate.now().minusDays(2), LocalDate.now().plusDays(3), null, BorrowRequestStatus.APPROVED, "家里装修需要用一下");
        BorrowRequest br2 = createBorrowRequest(tool2.getId(), user2.getId(), LocalDate.now().plusDays(1), LocalDate.now().plusDays(5), null, BorrowRequestStatus.PENDING, "换灯泡用");
        BorrowRequest br3 = createBorrowRequest(tool1.getId(), user3.getId(), LocalDate.now().minusDays(7), LocalDate.now().minusDays(3), LocalDate.now().minusDays(3), BorrowRequestStatus.RETURNED, "安装窗帘");
        BorrowRequest br4 = createBorrowRequest(tool6.getId(), user1.getId(), LocalDate.now().plusDays(2), LocalDate.now().plusDays(7), null, BorrowRequestStatus.REJECTED, "最近太忙了");

        ToolLog log1 = createToolLog(tool1.getId(), user3.getId(), ToolLogAction.BORROW, "安装窗帘借用");
        ToolLog log2 = createToolLog(tool1.getId(), user3.getId(), ToolLogAction.RETURN, "使用完毕，完好归还");
        ToolLog log3 = createToolLog(tool3.getId(), user2.getId(), ToolLogAction.BORROW, "装修使用");
        ToolLog log4 = createToolLog(tool7.getId(), user1.getId(), ToolLogAction.REPORT, "锤头松动，需要维修");
        ToolLog log5 = createToolLog(tool5.getId(), user3.getId(), ToolLogAction.REPAIR, "定期校准维护");
    }

    private User createUser(String username, String email, String password) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=" + username);
        return userRepository.save(user);
    }

    private ToolBox createToolBox(String name, String location, Long managerId, String code, boolean isActive) {
        ToolBox toolBox = new ToolBox();
        toolBox.setName(name);
        toolBox.setLocation(location);
        toolBox.setManagerId(managerId);
        toolBox.setCode(code);
        toolBox.setIsActive(isActive);
        return toolBoxRepository.save(toolBox);
    }

    private Tool createTool(Long boxId, String name, String category, ToolStatus status,
                            String description, LocalDate purchaseDate, Long ownerId) {
        Tool tool = new Tool();
        tool.setBoxId(boxId);
        tool.setName(name);
        tool.setCategory(category);
        tool.setStatus(status);
        tool.setDescription(description);
        tool.setPurchaseDate(purchaseDate);
        tool.setOwnerId(ownerId);
        return toolRepository.save(tool);
    }

    private BorrowRequest createBorrowRequest(Long toolId, Long requesterId, LocalDate startDate,
                                              LocalDate expectedReturnDate, LocalDate actualReturnDate,
                                              BorrowRequestStatus status, String remark) {
        BorrowRequest br = new BorrowRequest();
        br.setToolId(toolId);
        br.setRequesterId(requesterId);
        br.setStartDate(startDate);
        br.setExpectedReturnDate(expectedReturnDate);
        br.setActualReturnDate(actualReturnDate);
        br.setStatus(status);
        br.setRemark(remark);
        return borrowRequestRepository.save(br);
    }

    private ToolLog createToolLog(Long toolId, Long userId, ToolLogAction action, String description) {
        ToolLog log = new ToolLog();
        log.setToolId(toolId);
        log.setUserId(userId);
        log.setAction(action);
        log.setDescription(description);
        return toolLogRepository.save(log);
    }
}
