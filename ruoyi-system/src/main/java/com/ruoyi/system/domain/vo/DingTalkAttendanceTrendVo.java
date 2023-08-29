package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 钉钉考勤趋势
 *
 * @author codex
 */
@Data
public class DingTalkAttendanceTrendVo {

    private List<String> dates = new ArrayList<>();

    private List<Long> totalCounts = new ArrayList<>();

    private List<Long> normalCounts = new ArrayList<>();

    private List<Long> exceptionCounts = new ArrayList<>();

    private List<Long> lateCounts = new ArrayList<>();

    private List<Long> earlyCounts = new ArrayList<>();

    private List<DingTalkAttendanceChartItemVo> resultDistribution = new ArrayList<>();

}
