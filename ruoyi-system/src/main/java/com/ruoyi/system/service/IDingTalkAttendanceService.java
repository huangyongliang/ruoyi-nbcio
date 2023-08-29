package com.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.DingTalkAttendanceRecord;
import com.ruoyi.system.domain.bo.DingTalkAttendanceConfigBo;
import com.ruoyi.system.domain.bo.DingTalkAttendanceQuery;
import com.ruoyi.system.domain.bo.DingTalkAttendanceSyncBo;
import com.ruoyi.system.domain.vo.AttendanceExcelMergeResultVo;
import com.ruoyi.system.domain.vo.DingTalkAttendanceSummaryVo;
import com.ruoyi.system.domain.vo.DingTalkAttendancePersonStatsVo;
import com.ruoyi.system.domain.vo.DingTalkAttendanceTrendVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 钉钉考勤服务
 *
 * @author codex
 */
public interface IDingTalkAttendanceService extends IService<DingTalkAttendanceRecord> {

    TableDataInfo<DingTalkAttendanceRecord> selectPageList(DingTalkAttendanceQuery query, PageQuery pageQuery);

    DingTalkAttendanceRecord selectById(Long recordId);

    DingTalkAttendanceSummaryVo summary(DingTalkAttendanceQuery query);

    DingTalkAttendanceTrendVo trend(DingTalkAttendanceQuery query);

    List<DingTalkAttendancePersonStatsVo> personStats(DingTalkAttendanceQuery query);

    int sync(DingTalkAttendanceSyncBo bo);

    String importExcel(MultipartFile file, boolean updateSupport) throws IOException;

    AttendanceExcelMergeResultVo mergeExcel(MultipartFile[] files) throws IOException;

    DingTalkAttendanceConfigBo getConfig();

    void saveConfig(DingTalkAttendanceConfigBo bo);

}
