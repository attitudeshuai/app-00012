package com.toolshare.dto.scan;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ScanReturnRequest {
    @NotNull(message = "工具箱编码不能为空")
    private String toolBoxCode;

    @NotEmpty(message = "请选择要归还的工具")
    private List<Long> toolIds;

    private String remark;
}
