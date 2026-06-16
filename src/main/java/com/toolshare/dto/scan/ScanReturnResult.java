package com.toolshare.dto.scan;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScanReturnResult {
    private int totalRequested;
    private int successCount;
    private int failCount;
    private List<ReturnResultItem> results;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnResultItem {
        private Long toolId;
        private String toolName;
        private boolean success;
        private String message;
        private Long borrowRequestId;
    }
}
