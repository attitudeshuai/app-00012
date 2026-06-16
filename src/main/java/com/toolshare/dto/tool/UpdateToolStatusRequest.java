package com.toolshare.dto.tool;

import com.toolshare.entity.ToolStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateToolStatusRequest {
    @NotNull(message = "状态不能为空")
    private ToolStatus status;
}
