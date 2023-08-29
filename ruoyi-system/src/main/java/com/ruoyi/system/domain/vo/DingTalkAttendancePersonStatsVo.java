package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 钉钉考勤个人统计
 *
 * @author codex
 */
@Data
public class DingTalkAttendancePersonStatsVo {

    private String dingUserId;

    private String userName;

    private String deptName;

    private BigDecimal workDays = BigDecimal.ZERO;

    private BigDecimal leaveDays = BigDecimal.ZERO;

    private BigDecimal restDays = BigDecimal.ZERO;

    private BigDecimal absenteeismDays = BigDecimal.ZERO;

    private BigDecimal workMinutes = BigDecimal.ZERO;

    private BigDecimal overtimeHours = BigDecimal.ZERO;

    private BigDecimal businessTripHours = BigDecimal.ZERO;

    private BigDecimal outsideHours = BigDecimal.ZERO;

    private Integer lateCount = 0;

    private Integer earlyCount = 0;

    private Integer missingCardCount = 0;

    private Integer exceptionCount = 0;

    private Integer recordCount = 0;

}
