package com.toolshare.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OverviewStats {
    private long totalUsers;
    private long totalToolBoxes;
    private long totalTools;
    private long totalBorrowRequests;
    private long totalToolLogs;
    private Map<String, Long> toolsByStatus;
    private Map<String, Long> toolsByCategory;
    private Map<String, Long> borrowRequestsByStatus;
    private long totalOverdueRecords;
    private long unresolvedOverdueRecords;
}
