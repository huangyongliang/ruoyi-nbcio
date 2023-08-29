package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 考勤每日统计明细 financing_daily_detail
 *
 * @author codex
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("financing_daily_detail")
public class FinancingDailyDetail extends BaseEntity {

    /**
     * 明细ID
     */
    @TableId(value = "detail_id")
    private Long detailId;

    /**
     * 来源文件名
     */
    private String sourceFile;

    /**
     * 导入批次号
     */
    private String importBatchNo;

    /**
     * 报表开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date reportStartDate;

    /**
     * 报表结束日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date reportEndDate;

    /**
     * 报表生成时间
     */
    private String reportGeneratedTime;

    /**
     * 姓名
     */
    private String employeeName;

    /**
     * 考勤组
     */
    private String attendanceGroup;

    /**
     * 部门
     */
    private String deptName;

    /**
     * 工号
     */
    private String employeeNo;

    /**
     * 职位
     */
    private String positionName;

    /**
     * 钉钉用户ID
     */
    private String dingUserId;

    /**
     * 日期文本
     */
    private String attendanceDateLabel;

    /**
     * 考勤日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date attendanceDate;

    /**
     * workDate毫秒值
     */
    private Long workDateMillis;

    /**
     * 班次
     */
    private String shiftName;

    private String on1Time;
    private String on1Result;
    private String off1Time;
    private String off1Result;
    private String on2Time;
    private String on2Result;
    private String off2Time;
    private String off2Result;
    private String on3Time;
    private String on3Result;
    private String off3Time;
    private String off3Result;

    /**
     * 关联的审批单
     */
    private String relatedApproval;

    private BigDecimal attendanceDays;
    private BigDecimal restDays;
    private BigDecimal workDuration;
    private Integer lateCount;
    private BigDecimal lateDuration;
    private Integer seriousLateCount;
    private BigDecimal seriousLateDuration;
    private Integer absenteeLateCount;
    private Integer earlyCount;
    private BigDecimal earlyDuration;
    private Integer onMissingCount;
    private Integer offMissingCount;
    private BigDecimal absenteeismDays;
    private BigDecimal businessTripDuration;
    private BigDecimal outsideDuration;
    private String overtimeApprovalStats;
    private BigDecimal overtimeDurationRule;

}
