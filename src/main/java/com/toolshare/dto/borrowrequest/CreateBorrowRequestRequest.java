package com.toolshare.dto.borrowrequest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateBorrowRequestRequest {
    @NotNull(message = "工具ID不能为空")
    private Long toolId;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "预计归还日期不能为空")
    private LocalDate expectedReturnDate;

    @Size(max = 2000, message = "备注不能超过2000个字符")
    private String remark;
}
