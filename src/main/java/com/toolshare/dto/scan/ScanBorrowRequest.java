package com.toolshare.dto.scan;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ScanBorrowRequest {
    @NotNull(message = "工具箱编码不能为空")
    private String toolBoxCode;

    @NotEmpty(message = "请选择要借用的工具")
    private List<Long> toolIds;

    @NotNull(message = "预计归还日期不能为空")
    private LocalDate expectedReturnDate;

    private String remark;
}
