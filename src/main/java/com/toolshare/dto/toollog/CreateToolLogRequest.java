package com.toolshare.dto.toollog;

import com.toolshare.entity.ToolLogAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateToolLogRequest {
    @NotNull(message = "工具ID不能为空")
    private Long toolId;

    @NotNull(message = "操作类型不能为空")
    private ToolLogAction action;

    @Size(max = 2000, message = "描述不能超过2000个字符")
    private String description;
}
