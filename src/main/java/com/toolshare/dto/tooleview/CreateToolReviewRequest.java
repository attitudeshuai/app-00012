package com.toolshare.dto.tooleview;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateToolReviewRequest {

    @NotNull(message = "借用申请ID不能为空")
    private Long borrowRequestId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分不能低于1分")
    @Max(value = 5, message = "评分不能高于5分")
    private Integer rating;

    private String comment;
}
