package com.toolshare.controller;

import com.toolshare.dto.ApiResponse;
import com.toolshare.dto.stats.OverviewStats;
import com.toolshare.dto.stats.TrendStats;
import com.toolshare.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/stats")
@Tag(name = "统计管理", description = "数据统计与趋势分析")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/overview")
    @Operation(summary = "总览统计")
    public ApiResponse<OverviewStats> getOverviewStats() {
        return ApiResponse.success(statsService.getOverviewStats());
    }

    @GetMapping("/trend")
    @Operation(summary = "趋势统计")
    public ApiResponse<TrendStats> getTrendStats(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return ApiResponse.success(statsService.getTrendStats(startDate, endDate));
    }
}
