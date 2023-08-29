package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 钉钉考勤打卡记录 dingtalk_attendance_record
 *
 * @author codex
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dingtalk_attendance_record")
public class DingTalkAttendanceRecord extends BaseEntity {

    /**
     * 本地记录ID
     */
    @TableId(value = "record_id")
    private Long recordId;

    /**
     * 钉钉打卡记录ID
     */
    private String dingRecordId;

    /**
     * 钉钉业务ID
     */
    private String bizId;

    /**
     * 钉钉企业ID
     */
    private String corpId;

    /**
     * 钉钉用户ID
     */
    private String dingUserId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 工作日
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date workDate;

    /**
     * 实际打卡时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date checkTime;

    /**
     * 排班打卡时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date baseCheckTime;

    /**
     * 用户打卡时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date userCheckTime;

    /**
     * 打卡类型 OnDuty/OffDuty
     */
    private String checkType;

    /**
     * 打卡来源
     */
    private String sourceType;

    /**
     * 关联的每日统计明细ID
     */
    private Long dailyDetailId;

    /**
     * 来源文件名
     */
    private String sourceFile;

    /**
     * 加班小时，来自关联每日统计的“加班-审批单统计”
     */
    @TableField(exist = false)
    private BigDecimal overtimeHours;

    /**
     * 加班审批统计原始文本
     */
    @TableField(exist = false)
    private String overtimeApprovalStats;

    /**
     * 导入批次号
     */
    private String importBatchNo;

    /**
     * 时间结果
     */
    private String timeResult;

    /**
     * 位置结果
     */
    private String locationResult;

    /**
     * 定位方式
     */
    private String locationMethod;

    /**
     * 考勤组ID
     */
    private String groupId;

    /**
     * 排班计划ID
     */
    private String planId;

    /**
     * 审批实例ID
     */
    private String procInstId;

    /**
     * 审批单ID
     */
    private String approveId;

    /**
     * 打卡设备ID
     */
    private String deviceId;

    /**
     * 打卡地址
     */
    private String userAddress;

    /**
     * 经度
     */
    private BigDecimal userLongitude;

    /**
     * 纬度
     */
    private BigDecimal userLatitude;

    /**
     * 钉钉原始返回
     */
    private String rawData;

}
