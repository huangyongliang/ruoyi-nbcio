package com.ruoyi.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 钉钉考勤查询条件
 *
 * @author codex
 */
@Data
public class DingTalkAttendanceQuery {

    /**
     * 钉钉用户ID
     */
    private String dingUserId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 打卡类型
     */
    private String checkType;

    /**
     * 时间结果
     */
    private String timeResult;

    /**
     * 查询开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date beginTime;

    /**
     * 查询结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

}
