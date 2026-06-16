package com.toolshare.dto.tool;

import com.toolshare.entity.ToolStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateToolRequest {
    @NotNull(message = "工具箱ID不能为空")
    private Long boxId;

    @NotBlank(message = "工具名称不能为空")
    @Size(max = 100, message = "工具名称不能超过100个字符")
    private String name;

    @Size(max = 50, message = "类别不能超过50个字符")
    private String category;

    @Size(max = 2000, message = "描述不能超过2000个字符")
    private String description;

    @Size(max = 500, message = "图片链接不能超过500个字符")
    private String image;

    private LocalDate purchaseDate;
}
