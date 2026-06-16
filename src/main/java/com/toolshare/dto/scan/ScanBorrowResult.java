package com.toolshare.dto.scan;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScanBorrowResult {
    private int totalRequested;
    private int successCount;
    private int failCount;
    private List<BorrowResultItem> results;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BorrowResultItem {
        private Long toolId;
        private String toolName;
        private boolean success;
        private String message;
        private Long borrowRequestId;
    }
}
