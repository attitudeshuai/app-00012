package com.toolshare.dto.borrowrequest;

import com.toolshare.entity.BorrowRequestStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateBorrowRequestRequest {
    private LocalDate startDate;
    private LocalDate expectedReturnDate;
    private LocalDate actualReturnDate;
    private BorrowRequestStatus status;

    @Size(max = 2000, message = "备注不能超过2000个字符")
    private String remark;
}
