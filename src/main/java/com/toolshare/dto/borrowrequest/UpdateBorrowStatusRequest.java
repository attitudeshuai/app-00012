package com.toolshare.dto.borrowrequest;

import com.toolshare.entity.BorrowRequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateBorrowStatusRequest {
    @NotNull(message = "状态不能为空")
    private BorrowRequestStatus status;
}
