package com.ruoyi.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 钉钉考勤同步请求
 *
 * @author codex
 */
@Data
public class DingTalkAttendanceSyncBo {

    /**
     * 同步开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date checkDateFrom;

    /**
     * 同步结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date checkDateTo;

    /**
     * 钉钉用户ID，逗号或换行分隔；为空时使用系统配置
     */
    private String userIds;

    /**
     * 临时 AppKey；为空时使用系统配置
     */
    private String appKey;

    /**
     * 临时 AppSecret；为空时使用系统配置
     */
    private String appSecret;

    /**
     * 是否把本次凭证和用户范围保存到系统配置
     */
    private Boolean saveConfig;

}
