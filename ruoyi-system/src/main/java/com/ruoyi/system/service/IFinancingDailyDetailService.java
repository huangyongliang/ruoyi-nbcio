package com.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.FinancingDailyDetail;
import com.ruoyi.system.domain.bo.FinancingDailyDetailQuery;
import com.ruoyi.system.domain.vo.AttendanceExcelMergeResultVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 考勤每日统计明细服务
 *
 * @author codex
 */
public interface IFinancingDailyDetailService extends IService<FinancingDailyDetail> {

    TableDataInfo<FinancingDailyDetail> selectPageList(FinancingDailyDetailQuery query, PageQuery pageQuery);

    FinancingDailyDetail selectById(Long detailId);

    String importData(MultipartFile file, boolean updateSupport) throws IOException;

    AttendanceExcelMergeResultVo mergeDailyStatistics(MultipartFile[] files) throws IOException;

}
