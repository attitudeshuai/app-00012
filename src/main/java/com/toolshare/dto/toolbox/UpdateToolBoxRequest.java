package com.toolshare.dto.toolbox;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateToolBoxRequest {
    @Size(max = 100, message = "工具箱名称不能超过100个字符")
    private String name;

    @Size(max = 200, message = "位置描述不能超过200个字符")
    private String location;

    @Size(max = 50, message = "取箱码不能超过50个字符")
    private String code;

    @Size(max = 500, message = "图片链接不能超过500个字符")
    private String image;

    private Boolean isActive;
}
