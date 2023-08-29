package com.ruoyi.system.domain.bo;

import lombok.Data;

/**
 * 钉钉考勤配置
 *
 * @author codex
 */
@Data
public class DingTalkAttendanceConfigBo {

    /**
     * 是否启用钉钉考勤同步
     */
    private Boolean enabled;

    /**
     * 企业内部应用 AppKey
     */
    private String appKey;

    /**
     * 企业内部应用 AppSecret
     */
    private String appSecret;

    /**
     * 默认同步的钉钉用户ID，逗号或换行分隔
     */
    private String userIds;

    /**
     * AppSecret 是否已配置
     */
    private Boolean secretConfigured;

}
