package com.toolshare.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendStats {
    private String startDate;
    private String endDate;
    private List<Map<String, Object>> borrowRequestsByDate;
    private List<Map<String, Object>> toolLogsByAction;
}
