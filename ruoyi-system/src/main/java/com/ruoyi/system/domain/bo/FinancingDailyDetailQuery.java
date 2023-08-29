package com.ruoyi.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 融资团队每日统计明细查询条件
 *
 * @author codex
 */
@Data
public class FinancingDailyDetailQuery {

    /**
     * 姓名
     */
    private String employeeName;

    /**
     * 钉钉用户ID
     */
    private String dingUserId;

    /**
     * 部门
     */
    private String deptName;

    /**
     * 班次
     */
    private String shiftName;

    /**
     * 打卡结果关键字
     */
    private String resultKeyword;

    /**
     * 查询开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date beginDate;

    /**
     * 查询结束日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endDate;

}
