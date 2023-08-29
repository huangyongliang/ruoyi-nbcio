package com.ruoyi.web.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.system.domain.DingTalkAttendanceRecord;
import com.ruoyi.system.domain.bo.DingTalkAttendanceConfigBo;
import com.ruoyi.system.domain.bo.DingTalkAttendanceQuery;
import com.ruoyi.system.domain.bo.DingTalkAttendanceSyncBo;
import com.ruoyi.system.domain.vo.AttendanceExcelMergeResultVo;
import com.ruoyi.system.domain.vo.DingTalkAttendanceSummaryVo;
import com.ruoyi.system.domain.vo.DingTalkAttendancePersonStatsVo;
import com.ruoyi.system.domain.vo.DingTalkAttendanceTrendVo;
import com.ruoyi.system.service.IDingTalkAttendanceService;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 钉钉考勤同步与报表
 *
 * @author codex
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/dingtalkAttendance")
public class DingTalkAttendanceController extends BaseController {

    private final IDingTalkAttendanceService dingTalkAttendanceService;

    /**
     * 查询钉钉考勤记录列表
     */
    @SaCheckPermission("system:dingtalkAttendance:list")
    @GetMapping("/list")
    public TableDataInfo<DingTalkAttendanceRecord> list(DingTalkAttendanceQuery query, PageQuery pageQuery) {
        return dingTalkAttendanceService.selectPageList(query, pageQuery);
    }

    /**
     * 查询钉钉考勤记录详情
     */
    @SaCheckPermission("system:dingtalkAttendance:list")
    @GetMapping("/{recordId}")
    public R<DingTalkAttendanceRecord> getInfo(@PathVariable Long recordId) {
        return R.ok(dingTalkAttendanceService.selectById(recordId));
    }

    /**
     * 查询钉钉考勤汇总
     */
    @SaCheckPermission("system:dingtalkAttendance:list")
    @GetMapping("/summary")
    public R<DingTalkAttendanceSummaryVo> summary(DingTalkAttendanceQuery query) {
        return R.ok(dingTalkAttendanceService.summary(query));
    }

    /**
     * 查询钉钉考勤趋势
     */
    @SaCheckPermission("system:dingtalkAttendance:list")
    @GetMapping("/trend")
    public R<DingTalkAttendanceTrendVo> trend(DingTalkAttendanceQuery query) {
        return R.ok(dingTalkAttendanceService.trend(query));
    }

    /**
     * 查询钉钉考勤个人统计
     */
    @SaCheckPermission("system:dingtalkAttendance:list")
    @GetMapping("/personStats")
    public R<List<DingTalkAttendancePersonStatsVo>> personStats(DingTalkAttendanceQuery query) {
        return R.ok(dingTalkAttendanceService.personStats(query));
    }

    /**
     * 获取钉钉考勤配置
     */
    @SaCheckPermission("system:dingtalkAttendance:list")
    @GetMapping("/config")
    public R<DingTalkAttendanceConfigBo> getConfig() {
        return R.ok(dingTalkAttendanceService.getConfig());
    }

    /**
     * 保存钉钉考勤配置
     */
    @SaCheckPermission("system:dingtalkAttendance:config")
    @Log(title = "钉钉考勤配置", businessType = BusinessType.UPDATE)
    @PutMapping("/config")
    public R<Void> saveConfig(@RequestBody DingTalkAttendanceConfigBo bo) {
        dingTalkAttendanceService.saveConfig(bo);
        return R.ok();
    }

    /**
     * 同步钉钉考勤数据
     */
    @SaCheckPermission("system:dingtalkAttendance:sync")
    @Log(title = "钉钉考勤同步", businessType = BusinessType.IMPORT)
    @PostMapping("/sync")
    public R<Integer> sync(@RequestBody DingTalkAttendanceSyncBo bo) {
        int count = dingTalkAttendanceService.sync(bo);
        return R.ok("同步完成", count);
    }

    /**
     * 导入钉钉考勤历史Excel
     */
    @SaCheckPermission("system:dingtalkAttendance:import")
    @Log(title = "钉钉考勤历史导入", businessType = BusinessType.IMPORT)
    @PostMapping(value = "/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Void> importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws IOException {
        return R.ok(dingTalkAttendanceService.importExcel(file, updateSupport));
    }

    /**
     * 合并钉钉考勤每日统计Excel
     */
    @SaCheckPermission("system:dingtalkAttendance:import")
    @Log(title = "钉钉考勤Excel合并", businessType = BusinessType.EXPORT)
    @PostMapping(value = "/mergeExcel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void mergeExcel(@RequestPart("files") MultipartFile[] files, HttpServletResponse response) throws IOException {
        AttendanceExcelMergeResultVo result = dingTalkAttendanceService.mergeExcel(files);
        response.setContentType(result.getContentType());
        FileUtils.setAttachmentResponseHeader(response, result.getFileName());
        response.getOutputStream().write(result.getData());
    }

}
