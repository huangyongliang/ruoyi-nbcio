package com.ruoyi.system.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 考勤Excel合并结果
 *
 * @author codex
 */
@Data
@AllArgsConstructor
public class AttendanceExcelMergeResultVo {

    private String fileName;

    private String contentType;

    private byte[] data;

}
