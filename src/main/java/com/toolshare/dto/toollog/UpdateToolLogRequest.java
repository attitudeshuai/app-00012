package com.toolshare.dto.toollog;

import com.toolshare.entity.ToolLogAction;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateToolLogRequest {
    private ToolLogAction action;

    @Size(max = 2000, message = "描述不能超过2000个字符")
    private String description;
}
