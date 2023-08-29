package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 钉钉考勤汇总
 *
 * @author codex
 */
@Data
public class DingTalkAttendanceSummaryVo {

    private Long totalCount;

    private Long userCount;

    private Long normalCount;

    private Long exceptionCount;

    private Long lateCount;

    private Long earlyCount;

    private Long absenteeismCount;

    private BigDecimal averageWorkHours;

}
