package com.ruoyi.system.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.client.DingTalkAttendanceClient;
import com.ruoyi.system.domain.DingTalkAttendanceRecord;
import com.ruoyi.system.domain.FinancingDailyDetail;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.domain.bo.DingTalkAttendanceConfigBo;
import com.ruoyi.system.domain.bo.DingTalkAttendanceQuery;
import com.ruoyi.system.domain.bo.DingTalkAttendanceSyncBo;
import com.ruoyi.system.domain.vo.AttendanceExcelMergeResultVo;
import com.ruoyi.system.domain.vo.DingTalkAttendanceChartItemVo;
import com.ruoyi.system.domain.vo.DingTalkAttendancePersonStatsVo;
import com.ruoyi.system.domain.vo.DingTalkAttendanceSummaryVo;
import com.ruoyi.system.domain.vo.DingTalkAttendanceTrendVo;
import com.ruoyi.system.mapper.DingTalkAttendanceRecordMapper;
import com.ruoyi.system.mapper.SysConfigMapper;
import com.ruoyi.system.service.IFinancingDailyDetailService;
import com.ruoyi.system.service.IDingTalkAttendanceService;
import com.ruoyi.system.service.ISysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 钉钉考勤服务实现
 *
 * @author codex
 */
@RequiredArgsConstructor
@Service
public class DingTalkAttendanceServiceImpl extends ServiceImpl<DingTalkAttendanceRecordMapper, DingTalkAttendanceRecord>
    implements IDingTalkAttendanceService {

    private static final String CONFIG_ENABLED = "dingtalk.attendance.enabled";
    private static final String CONFIG_APP_KEY = "dingtalk.attendance.appKey";
    private static final String CONFIG_APP_SECRET = "dingtalk.attendance.appSecret";
    private static final String CONFIG_USER_IDS = "dingtalk.attendance.userIds";
    private static final int USER_BATCH_SIZE = 50;
    private static final long SEVEN_DAYS_MILLIS = 7L * 24 * 60 * 60 * 1000;
    private static final BigDecimal STANDARD_WORK_MINUTES = BigDecimal.valueOf(480);
    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);
    private static final Pattern OVERTIME_APPROVAL_NUMBER_PATTERN = Pattern.compile("-?\\d+(?:\\.\\d+)?");

    private final DingTalkAttendanceRecordMapper baseMapper;
    private final DingTalkAttendanceClient dingTalkAttendanceClient;
    private final IFinancingDailyDetailService financingDailyDetailService;
    private final ISysConfigService sysConfigService;
    private final SysConfigMapper sysConfigMapper;

    @Override
    public TableDataInfo<DingTalkAttendanceRecord> selectPageList(DingTalkAttendanceQuery query, PageQuery pageQuery) {
        LambdaQueryWrapper<DingTalkAttendanceRecord> lqw = buildQueryWrapper(query)
            .orderByDesc(DingTalkAttendanceRecord::getCheckTime);
        Page<DingTalkAttendanceRecord> page = baseMapper.selectPage(pageQuery.build(), lqw);
        enrichOvertimeFromDetails(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public DingTalkAttendanceRecord selectById(Long recordId) {
        if (recordId == null) {
            return null;
        }
        DingTalkAttendanceRecord record = baseMapper.selectById(recordId);
        if (record != null) {
            enrichOvertimeFromDetails(Arrays.asList(record));
        }
        return record;
    }

    @Override
    public DingTalkAttendanceSummaryVo summary(DingTalkAttendanceQuery query) {
        List<DingTalkAttendanceRecord> records = baseMapper.selectList(buildQueryWrapper(defaultQuery(query)));
        DingTalkAttendanceSummaryVo summary = new DingTalkAttendanceSummaryVo();
        summary.setTotalCount((long) records.size());

        Set<String> userIds = new HashSet<>();
        long normalCount = 0;
        long lateCount = 0;
        long earlyCount = 0;
        long absenteeismCount = 0;
        long exceptionCount = 0;

        for (DingTalkAttendanceRecord record : records) {
            if (StringUtils.isNotBlank(record.getDingUserId())) {
                userIds.add(record.getDingUserId());
            }
            if (isNormal(record)) {
                normalCount++;
            } else {
                exceptionCount++;
            }
            if (isLate(record.getTimeResult())) {
                lateCount++;
            }
            if ("Early".equals(record.getTimeResult())) {
                earlyCount++;
            }
            if ("Absenteeism".equals(record.getTimeResult())) {
                absenteeismCount++;
            }
        }
        summary.setUserCount((long) userIds.size());
        summary.setNormalCount(normalCount);
        summary.setLateCount(lateCount);
        summary.setEarlyCount(earlyCount);
        summary.setAbsenteeismCount(absenteeismCount);
        summary.setExceptionCount(exceptionCount);
        summary.setAverageWorkHours(calculateAverageWorkHours(records));
        return summary;
    }

    @Override
    public DingTalkAttendanceTrendVo trend(DingTalkAttendanceQuery query) {
        DingTalkAttendanceQuery actualQuery = defaultQuery(query);
        List<DingTalkAttendanceRecord> records = baseMapper.selectList(buildQueryWrapper(actualQuery));
        List<String> days = buildDays(actualQuery.getBeginTime(), actualQuery.getEndTime());

        Map<String, Long> totalMap = initDayMap(days);
        Map<String, Long> normalMap = initDayMap(days);
        Map<String, Long> exceptionMap = initDayMap(days);
        Map<String, Long> lateMap = initDayMap(days);
        Map<String, Long> earlyMap = initDayMap(days);
        Map<String, Long> distributionMap = new LinkedHashMap<>();

        for (DingTalkAttendanceRecord record : records) {
            Date date = record.getWorkDate() == null ? record.getCheckTime() : record.getWorkDate();
            if (date == null) {
                continue;
            }
            String day = DateUtils.dateTime(date);
            if (!totalMap.containsKey(day)) {
                continue;
            }
            addOne(totalMap, day);
            if (isNormal(record)) {
                addOne(normalMap, day);
            } else {
                addOne(exceptionMap, day);
            }
            if (isLate(record.getTimeResult())) {
                addOne(lateMap, day);
            }
            if ("Early".equals(record.getTimeResult())) {
                addOne(earlyMap, day);
            }
            String resultLabel = resultLabel(record);
            distributionMap.put(resultLabel, distributionMap.getOrDefault(resultLabel, 0L) + 1);
        }

        DingTalkAttendanceTrendVo trend = new DingTalkAttendanceTrendVo();
        trend.setDates(days);
        trend.setTotalCounts(new ArrayList<>(totalMap.values()));
        trend.setNormalCounts(new ArrayList<>(normalMap.values()));
        trend.setExceptionCounts(new ArrayList<>(exceptionMap.values()));
        trend.setLateCounts(new ArrayList<>(lateMap.values()));
        trend.setEarlyCounts(new ArrayList<>(earlyMap.values()));
        for (Map.Entry<String, Long> entry : distributionMap.entrySet()) {
            trend.getResultDistribution().add(new DingTalkAttendanceChartItemVo(entry.getKey(), entry.getValue()));
        }
        return trend;
    }

    @Override
    public List<DingTalkAttendancePersonStatsVo> personStats(DingTalkAttendanceQuery query) {
        DingTalkAttendanceQuery actualQuery = defaultQuery(query);
        Map<String, DingTalkAttendancePersonStatsVo> statsMap = new LinkedHashMap<>();
        Set<String> detailDayKeys = new HashSet<>();

        List<FinancingDailyDetail> details = financingDailyDetailService.list(buildDetailQueryWrapper(actualQuery));
        for (FinancingDailyDetail detail : details) {
            DingTalkAttendancePersonStatsVo stats = personStats(statsMap, detail.getDingUserId(), detail.getEmployeeName());
            stats.setUserName(firstNotBlank(stats.getUserName(), detail.getEmployeeName()));
            stats.setDeptName(firstNotBlank(stats.getDeptName(), detail.getDeptName()));
            addDetailStats(stats, detail);
            if (StringUtils.isNotBlank(detail.getDingUserId()) && detail.getAttendanceDate() != null) {
                detailDayKeys.add(personDayKey(detail.getDingUserId(), detail.getAttendanceDate()));
            }
        }

        List<DingTalkAttendanceRecord> records = baseMapper.selectList(buildQueryWrapper(actualQuery));
        Map<String, RawPersonDayStats> rawDayMap = new LinkedHashMap<>();
        for (DingTalkAttendanceRecord record : records) {
            if (StringUtils.isBlank(record.getDingUserId())) {
                continue;
            }
            Date workDate = record.getWorkDate() == null ? record.getCheckTime() : record.getWorkDate();
            if (workDate == null || detailDayKeys.contains(personDayKey(record.getDingUserId(), workDate))) {
                continue;
            }
            String dayKey = personDayKey(record.getDingUserId(), workDate);
            RawPersonDayStats dayStats = rawDayMap.computeIfAbsent(dayKey, key -> new RawPersonDayStats(record.getDingUserId(), record.getUserName(), workDate));
            dayStats.accept(record);
        }
        for (RawPersonDayStats dayStats : rawDayMap.values()) {
            DingTalkAttendancePersonStatsVo stats = personStats(statsMap, dayStats.dingUserId, dayStats.userName);
            stats.setUserName(firstNotBlank(stats.getUserName(), dayStats.userName));
            addRawDayStats(stats, dayStats);
        }

        List<DingTalkAttendancePersonStatsVo> list = new ArrayList<>(statsMap.values());
        list.sort((left, right) -> firstNotBlank(left.getUserName(), left.getDingUserId())
            .compareTo(firstNotBlank(right.getUserName(), right.getDingUserId())));
        return list;
    }

    @Override
    public int sync(DingTalkAttendanceSyncBo bo) {
        if (bo == null || bo.getCheckDateFrom() == null || bo.getCheckDateTo() == null) {
            throw new ServiceException("请选择同步时间范围");
        }
        if (bo.getCheckDateFrom().after(bo.getCheckDateTo())) {
            throw new ServiceException("同步开始时间不能晚于结束时间");
        }

        DingTalkAttendanceConfigBo config = getConfig();
        if (Boolean.FALSE.equals(config.getEnabled()) && !Boolean.TRUE.equals(bo.getSaveConfig())) {
            throw new ServiceException("钉钉考勤同步未启用");
        }
        String appKey = firstNotBlank(bo.getAppKey(), config.getAppKey());
        String appSecret = firstNotBlank(bo.getAppSecret(), sysConfigService.selectConfigByKey(CONFIG_APP_SECRET));
        String userIdsValue = firstNotBlank(bo.getUserIds(), config.getUserIds());
        if (StringUtils.isBlank(appKey) || StringUtils.isBlank(appSecret)) {
            throw new ServiceException("请先配置钉钉 AppKey 和 AppSecret");
        }
        List<String> userIds = parseUserIds(userIdsValue);
        if (userIds.isEmpty()) {
            throw new ServiceException("请先配置需要同步的钉钉用户ID");
        }
        if (Boolean.TRUE.equals(bo.getSaveConfig())) {
            DingTalkAttendanceConfigBo saveBo = new DingTalkAttendanceConfigBo();
            saveBo.setEnabled(true);
            saveBo.setAppKey(appKey);
            saveBo.setAppSecret(appSecret);
            saveBo.setUserIds(userIdsValue);
            saveConfig(saveBo);
        }

        String accessToken = dingTalkAttendanceClient.getAccessToken(appKey, appSecret);
        int savedCount = 0;
        List<List<String>> userBatches = splitList(userIds, USER_BATCH_SIZE);
        Date chunkStart = bo.getCheckDateFrom();
        while (!chunkStart.after(bo.getCheckDateTo())) {
            Date chunkEnd = new Date(Math.min(bo.getCheckDateTo().getTime(), chunkStart.getTime() + SEVEN_DAYS_MILLIS - 1000));
            for (List<String> userBatch : userBatches) {
                JSONArray records = dingTalkAttendanceClient.listRecord(
                    accessToken,
                    userBatch,
                    DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, chunkStart),
                    DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, chunkEnd)
                );
                for (int i = 0; i < records.size(); i++) {
                    DingTalkAttendanceRecord record = toRecord(records.getJSONObject(i));
                    saveOrUpdateRecord(record);
                    savedCount++;
                }
            }
            chunkStart = new Date(chunkEnd.getTime() + 1000);
        }
        return savedCount;
    }

    @Override
    public String importExcel(MultipartFile file, boolean updateSupport) throws IOException {
        return financingDailyDetailService.importData(file, updateSupport);
    }

    @Override
    public AttendanceExcelMergeResultVo mergeExcel(MultipartFile[] files) throws IOException {
        return financingDailyDetailService.mergeDailyStatistics(files);
    }

    @Override
    public DingTalkAttendanceConfigBo getConfig() {
        DingTalkAttendanceConfigBo config = new DingTalkAttendanceConfigBo();
        config.setEnabled(!"false".equalsIgnoreCase(sysConfigService.selectConfigByKey(CONFIG_ENABLED)));
        config.setAppKey(sysConfigService.selectConfigByKey(CONFIG_APP_KEY));
        String appSecret = sysConfigService.selectConfigByKey(CONFIG_APP_SECRET);
        config.setSecretConfigured(StringUtils.isNotBlank(appSecret));
        config.setAppSecret("");
        config.setUserIds(sysConfigService.selectConfigByKey(CONFIG_USER_IDS));
        return config;
    }

    @Override
    public void saveConfig(DingTalkAttendanceConfigBo bo) {
        if (bo == null) {
            return;
        }
        saveConfigValue(CONFIG_ENABLED, "钉钉考勤同步开关", String.valueOf(!Boolean.FALSE.equals(bo.getEnabled())));
        if (StringUtils.isNotBlank(bo.getAppKey())) {
            saveConfigValue(CONFIG_APP_KEY, "钉钉考勤AppKey", bo.getAppKey().trim());
        }
        if (StringUtils.isNotBlank(bo.getAppSecret())) {
            saveConfigValue(CONFIG_APP_SECRET, "钉钉考勤AppSecret", bo.getAppSecret().trim());
        }
        if (bo.getUserIds() != null) {
            saveConfigValue(CONFIG_USER_IDS, "钉钉考勤用户ID", bo.getUserIds().trim());
        }
    }

    private LambdaQueryWrapper<DingTalkAttendanceRecord> buildQueryWrapper(DingTalkAttendanceQuery query) {
        DingTalkAttendanceQuery q = query == null ? new DingTalkAttendanceQuery() : query;
        return new LambdaQueryWrapper<DingTalkAttendanceRecord>()
            .like(StringUtils.isNotBlank(q.getDingUserId()), DingTalkAttendanceRecord::getDingUserId, q.getDingUserId())
            .like(StringUtils.isNotBlank(q.getUserName()), DingTalkAttendanceRecord::getUserName, q.getUserName())
            .eq(StringUtils.isNotBlank(q.getCheckType()), DingTalkAttendanceRecord::getCheckType, q.getCheckType())
            .eq(StringUtils.isNotBlank(q.getTimeResult()), DingTalkAttendanceRecord::getTimeResult, q.getTimeResult())
            .ge(q.getBeginTime() != null, DingTalkAttendanceRecord::getCheckTime, q.getBeginTime())
            .le(q.getEndTime() != null, DingTalkAttendanceRecord::getCheckTime, q.getEndTime());
    }

    private LambdaQueryWrapper<FinancingDailyDetail> buildDetailQueryWrapper(DingTalkAttendanceQuery query) {
        DingTalkAttendanceQuery q = query == null ? new DingTalkAttendanceQuery() : query;
        return new LambdaQueryWrapper<FinancingDailyDetail>()
            .like(StringUtils.isNotBlank(q.getDingUserId()), FinancingDailyDetail::getDingUserId, q.getDingUserId())
            .like(StringUtils.isNotBlank(q.getUserName()), FinancingDailyDetail::getEmployeeName, q.getUserName())
            .ge(q.getBeginTime() != null, FinancingDailyDetail::getAttendanceDate, q.getBeginTime())
            .le(q.getEndTime() != null, FinancingDailyDetail::getAttendanceDate, q.getEndTime());
    }

    private DingTalkAttendanceQuery defaultQuery(DingTalkAttendanceQuery query) {
        DingTalkAttendanceQuery actual = query == null ? new DingTalkAttendanceQuery() : query;
        if (actual.getEndTime() == null) {
            actual.setEndTime(new Date());
        }
        if (actual.getBeginTime() == null) {
            actual.setBeginTime(addDays(actual.getEndTime(), -6));
        }
        return actual;
    }

    private DingTalkAttendanceRecord toRecord(JSONObject json) {
        DingTalkAttendanceRecord record = new DingTalkAttendanceRecord();
        record.setDingRecordId(firstJsonString(json, "id", "recordId"));
        record.setBizId(json.getString("bizId"));
        record.setCorpId(json.getString("corpId"));
        record.setDingUserId(json.getString("userId"));
        record.setUserName(firstJsonString(json, "userName", "name"));
        record.setWorkDate(parseDingDate(json.get("workDate")));
        record.setBaseCheckTime(parseDingDate(json.get("baseCheckTime")));
        record.setUserCheckTime(parseDingDate(json.get("userCheckTime")));
        record.setCheckTime(record.getUserCheckTime() == null ? record.getBaseCheckTime() : record.getUserCheckTime());
        if (record.getWorkDate() == null) {
            record.setWorkDate(record.getCheckTime());
        }
        record.setCheckType(json.getString("checkType"));
        record.setSourceType(json.getString("sourceType"));
        record.setTimeResult(json.getString("timeResult"));
        record.setLocationResult(json.getString("locationResult"));
        record.setLocationMethod(json.getString("locationMethod"));
        record.setGroupId(firstJsonString(json, "groupId"));
        record.setPlanId(firstJsonString(json, "planId"));
        record.setProcInstId(json.getString("procInstId"));
        record.setApproveId(json.getString("approveId"));
        record.setDeviceId(json.getString("deviceId"));
        record.setUserAddress(json.getString("userAddress"));
        record.setUserLongitude(toBigDecimal(json.get("userLongitude")));
        record.setUserLatitude(toBigDecimal(json.get("userLatitude")));
        record.setRawData(json.toJSONString());
        return record;
    }

    private void saveOrUpdateRecord(DingTalkAttendanceRecord record) {
        DingTalkAttendanceRecord existing = null;
        if (StringUtils.isNotBlank(record.getDingRecordId())) {
            existing = baseMapper.selectOne(new LambdaQueryWrapper<DingTalkAttendanceRecord>()
                .eq(DingTalkAttendanceRecord::getDingRecordId, record.getDingRecordId())
                .last("limit 1"));
        }
        if (existing == null && StringUtils.isNotBlank(record.getDingUserId()) && record.getCheckTime() != null
            && StringUtils.isNotBlank(record.getCheckType())) {
            existing = baseMapper.selectOne(new LambdaQueryWrapper<DingTalkAttendanceRecord>()
                .eq(DingTalkAttendanceRecord::getDingUserId, record.getDingUserId())
                .eq(DingTalkAttendanceRecord::getCheckTime, record.getCheckTime())
                .eq(DingTalkAttendanceRecord::getCheckType, record.getCheckType())
                .last("limit 1"));
        }
        if (existing == null) {
            baseMapper.insert(record);
        } else {
            record.setRecordId(existing.getRecordId());
            baseMapper.updateById(record);
        }
    }

    private List<String> parseUserIds(String userIds) {
        if (StringUtils.isBlank(userIds)) {
            return new ArrayList<>();
        }
        String[] items = userIds.split("[,，\\s]+");
        Set<String> unique = new LinkedHashSet<>();
        for (String item : items) {
            if (StringUtils.isNotBlank(item)) {
                unique.add(item.trim());
            }
        }
        return new ArrayList<>(unique);
    }

    private <T> List<List<T>> splitList(List<T> source, int batchSize) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < source.size(); i += batchSize) {
            result.add(source.subList(i, Math.min(source.size(), i + batchSize)));
        }
        return result;
    }

    private void saveConfigValue(String key, String name, String value) {
        SysConfig config = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
            .eq(SysConfig::getConfigKey, key)
            .last("limit 1"));
        if (config == null) {
            config = new SysConfig();
            config.setConfigName(name);
            config.setConfigKey(key);
            config.setConfigValue(value == null ? "" : value);
            config.setConfigType("N");
            config.setRemark("钉钉考勤同步配置");
            sysConfigService.insertConfig(config);
        } else {
            config.setConfigName(name);
            config.setConfigValue(value == null ? "" : value);
            config.setRemark("钉钉考勤同步配置");
            sysConfigService.updateConfig(config);
        }
    }

    private DingTalkAttendancePersonStatsVo personStats(Map<String, DingTalkAttendancePersonStatsVo> statsMap, String dingUserId, String userName) {
        String key = firstNotBlank(dingUserId, userName, "未知");
        DingTalkAttendancePersonStatsVo stats = statsMap.get(key);
        if (stats == null) {
            stats = new DingTalkAttendancePersonStatsVo();
            stats.setDingUserId(dingUserId);
            stats.setUserName(userName);
            statsMap.put(key, stats);
        }
        if (StringUtils.isBlank(stats.getDingUserId())) {
            stats.setDingUserId(dingUserId);
        }
        if (StringUtils.isBlank(stats.getUserName())) {
            stats.setUserName(userName);
        }
        return stats;
    }

    private void addDetailStats(DingTalkAttendancePersonStatsVo stats, FinancingDailyDetail detail) {
        BigDecimal attendanceDays = nvl(detail.getAttendanceDays());
        if (attendanceDays.compareTo(BigDecimal.ZERO) == 0 && isWorkDayByDetail(detail)) {
            attendanceDays = BigDecimal.ONE;
        }
        addWorkDays(stats, attendanceDays);
        if (containsAnyResult(detail, "请假")) {
            addLeaveDays(stats, BigDecimal.ONE);
        }
        addRestDays(stats, nvl(detail.getRestDays()));
        addAbsenteeismDays(stats, nvl(detail.getAbsenteeismDays()));
        addWorkMinutes(stats, nvl(detail.getWorkDuration()));
        addOvertimeHours(stats, parseOvertimeApprovalHours(detail.getOvertimeApprovalStats()));
        addBusinessTripHours(stats, nvl(detail.getBusinessTripDuration()));
        addOutsideHours(stats, nvl(detail.getOutsideDuration()));
        addLateCount(stats, nvl(detail.getLateCount()) + nvl(detail.getSeriousLateCount()));
        addEarlyCount(stats, nvl(detail.getEarlyCount()));
        addMissingCardCount(stats, nvl(detail.getOnMissingCount()) + nvl(detail.getOffMissingCount()));
        if (hasException(detail)) {
            addExceptionCount(stats, 1);
        }
        addRecordCount(stats, 1);
    }

    private void addRawDayStats(DingTalkAttendancePersonStatsVo stats, RawPersonDayStats dayStats) {
        if (dayStats.leave) {
            addLeaveDays(stats, BigDecimal.ONE);
        } else if (dayStats.rest) {
            addRestDays(stats, BigDecimal.ONE);
        } else if (dayStats.absenteeism) {
            addAbsenteeismDays(stats, BigDecimal.ONE);
        } else {
            addWorkDays(stats, BigDecimal.ONE);
        }

        BigDecimal workMinutes = dayStats.workMinutes();
        addWorkMinutes(stats, workMinutes);
        if (workMinutes.compareTo(STANDARD_WORK_MINUTES) > 0) {
            addOvertimeHours(stats, workMinutes.subtract(STANDARD_WORK_MINUTES).divide(MINUTES_PER_HOUR, 2, RoundingMode.HALF_UP));
        }
        addLateCount(stats, dayStats.lateCount);
        addEarlyCount(stats, dayStats.earlyCount);
        addMissingCardCount(stats, dayStats.missingCardCount);
        if (dayStats.exception) {
            addExceptionCount(stats, 1);
        }
        addRecordCount(stats, dayStats.recordCount);
    }

    private boolean isWorkDayByDetail(FinancingDailyDetail detail) {
        if (detail == null) {
            return false;
        }
        if (nvl(detail.getRestDays()).compareTo(BigDecimal.ZERO) > 0 || containsAnyResult(detail, "请假", "休息")) {
            return false;
        }
        return StringUtils.isNotBlank(detail.getOn1Time()) || StringUtils.isNotBlank(detail.getOff1Time())
            || nvl(detail.getWorkDuration()).compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean hasException(FinancingDailyDetail detail) {
        return nvl(detail.getLateCount()) > 0
            || nvl(detail.getSeriousLateCount()) > 0
            || nvl(detail.getEarlyCount()) > 0
            || nvl(detail.getOnMissingCount()) > 0
            || nvl(detail.getOffMissingCount()) > 0
            || nvl(detail.getAbsenteeismDays()).compareTo(BigDecimal.ZERO) > 0
            || containsAnyResult(detail, "迟到", "早退", "缺卡", "旷工");
    }

    private boolean containsAnyResult(FinancingDailyDetail detail, String... keywords) {
        if (detail == null || keywords == null) {
            return false;
        }
        List<String> results = Arrays.asList(
            detail.getOn1Result(), detail.getOff1Result(),
            detail.getOn2Result(), detail.getOff2Result(),
            detail.getOn3Result(), detail.getOff3Result()
        );
        for (String result : results) {
            if (StringUtils.isBlank(result)) {
                continue;
            }
            for (String keyword : keywords) {
                if (StringUtils.isNotBlank(keyword) && result.contains(keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String personDayKey(String dingUserId, Date date) {
        return dingUserId + "#" + DateUtils.dateTime(date);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private void addWorkDays(DingTalkAttendancePersonStatsVo stats, BigDecimal value) {
        stats.setWorkDays(stats.getWorkDays().add(nvl(value)));
    }

    private void addLeaveDays(DingTalkAttendancePersonStatsVo stats, BigDecimal value) {
        stats.setLeaveDays(stats.getLeaveDays().add(nvl(value)));
    }

    private void addRestDays(DingTalkAttendancePersonStatsVo stats, BigDecimal value) {
        stats.setRestDays(stats.getRestDays().add(nvl(value)));
    }

    private void addAbsenteeismDays(DingTalkAttendancePersonStatsVo stats, BigDecimal value) {
        stats.setAbsenteeismDays(stats.getAbsenteeismDays().add(nvl(value)));
    }

    private BigDecimal parseOvertimeApprovalHours(String value) {
        if (StringUtils.isBlank(value)) {
            return BigDecimal.ZERO;
        }
        Matcher matcher = OVERTIME_APPROVAL_NUMBER_PATTERN.matcher(value.replace(",", ""));
        BigDecimal total = BigDecimal.ZERO;
        while (matcher.find()) {
            total = total.add(new BigDecimal(matcher.group()));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private void addWorkMinutes(DingTalkAttendancePersonStatsVo stats, BigDecimal value) {
        stats.setWorkMinutes(stats.getWorkMinutes().add(nvl(value)).setScale(2, RoundingMode.HALF_UP));
    }

    private void addOvertimeHours(DingTalkAttendancePersonStatsVo stats, BigDecimal value) {
        stats.setOvertimeHours(stats.getOvertimeHours().add(nvl(value)).setScale(2, RoundingMode.HALF_UP));
    }

    private void enrichOvertimeFromDetails(List<DingTalkAttendanceRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> detailIds = new LinkedHashSet<>();
        for (DingTalkAttendanceRecord record : records) {
            if (record != null && record.getDailyDetailId() != null) {
                detailIds.add(record.getDailyDetailId());
            }
        }
        if (detailIds.isEmpty()) {
            return;
        }
        Map<Long, FinancingDailyDetail> detailMap = new HashMap<>();
        for (FinancingDailyDetail detail : financingDailyDetailService.listByIds(detailIds)) {
            if (detail != null && detail.getDetailId() != null) {
                detailMap.put(detail.getDetailId(), detail);
            }
        }
        for (DingTalkAttendanceRecord record : records) {
            if (record == null || record.getDailyDetailId() == null) {
                continue;
            }
            FinancingDailyDetail detail = detailMap.get(record.getDailyDetailId());
            if (detail == null) {
                continue;
            }
            record.setOvertimeApprovalStats(detail.getOvertimeApprovalStats());
            record.setOvertimeHours(parseOvertimeApprovalHours(detail.getOvertimeApprovalStats()));
        }
    }

    private void addBusinessTripHours(DingTalkAttendancePersonStatsVo stats, BigDecimal value) {
        stats.setBusinessTripHours(stats.getBusinessTripHours().add(nvl(value)).setScale(2, RoundingMode.HALF_UP));
    }

    private void addOutsideHours(DingTalkAttendancePersonStatsVo stats, BigDecimal value) {
        stats.setOutsideHours(stats.getOutsideHours().add(nvl(value)).setScale(2, RoundingMode.HALF_UP));
    }

    private void addLateCount(DingTalkAttendancePersonStatsVo stats, int value) {
        stats.setLateCount(stats.getLateCount() + value);
    }

    private void addEarlyCount(DingTalkAttendancePersonStatsVo stats, int value) {
        stats.setEarlyCount(stats.getEarlyCount() + value);
    }

    private void addMissingCardCount(DingTalkAttendancePersonStatsVo stats, int value) {
        stats.setMissingCardCount(stats.getMissingCardCount() + value);
    }

    private void addExceptionCount(DingTalkAttendancePersonStatsVo stats, int value) {
        stats.setExceptionCount(stats.getExceptionCount() + value);
    }

    private void addRecordCount(DingTalkAttendancePersonStatsVo stats, int value) {
        stats.setRecordCount(stats.getRecordCount() + value);
    }

    private BigDecimal calculateAverageWorkHours(List<DingTalkAttendanceRecord> records) {
        Map<String, Date[]> workDayMap = new HashMap<>();
        for (DingTalkAttendanceRecord record : records) {
            if (record.getCheckTime() == null || StringUtils.isBlank(record.getDingUserId())) {
                continue;
            }
            Date workDate = record.getWorkDate() == null ? record.getCheckTime() : record.getWorkDate();
            String key = record.getDingUserId() + "#" + DateUtils.dateTime(workDate);
            Date[] pair = workDayMap.computeIfAbsent(key, k -> new Date[2]);
            if ("OnDuty".equals(record.getCheckType())) {
                pair[0] = pair[0] == null || record.getCheckTime().before(pair[0]) ? record.getCheckTime() : pair[0];
            } else if ("OffDuty".equals(record.getCheckType())) {
                pair[1] = pair[1] == null || record.getCheckTime().after(pair[1]) ? record.getCheckTime() : pair[1];
            }
        }

        long totalMillis = 0;
        long dayCount = 0;
        for (Date[] pair : workDayMap.values()) {
            if (pair[0] != null && pair[1] != null && pair[1].after(pair[0])) {
                totalMillis += pair[1].getTime() - pair[0].getTime();
                dayCount++;
            }
        }
        if (dayCount == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(totalMillis)
            .divide(BigDecimal.valueOf(3600000L * dayCount), 2, RoundingMode.HALF_UP);
    }

    private List<String> buildDays(Date beginTime, Date endTime) {
        Date begin = beginTime == null ? addDays(new Date(), -6) : beginTime;
        Date end = endTime == null ? new Date() : endTime;
        List<String> days = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(begin);
        clearTime(calendar);
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(end);
        clearTime(endCalendar);
        while (!calendar.after(endCalendar)) {
            days.add(DateUtils.dateTime(calendar.getTime()));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        return days;
    }

    private Map<String, Long> initDayMap(List<String> days) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (String day : days) {
            map.put(day, 0L);
        }
        return map;
    }

    private void addOne(Map<String, Long> map, String key) {
        map.put(key, map.getOrDefault(key, 0L) + 1);
    }

    private boolean isNormal(DingTalkAttendanceRecord record) {
        boolean timeNormal = StringUtils.isBlank(record.getTimeResult()) || isAcceptedResult(record.getTimeResult());
        boolean locationNormal = StringUtils.isBlank(record.getLocationResult()) || "Normal".equals(record.getLocationResult());
        return timeNormal && locationNormal;
    }

    private boolean isAcceptedResult(String timeResult) {
        return "Normal".equals(timeResult)
            || "Leave".equals(timeResult)
            || "Rest".equals(timeResult)
            || "Field".equals(timeResult)
            || "BusinessTrip".equals(timeResult)
            || "Approval".equals(timeResult);
    }

    private boolean isLate(String timeResult) {
        return "Late".equals(timeResult) || "SeriousLate".equals(timeResult);
    }

    private String resultLabel(DingTalkAttendanceRecord record) {
        if (StringUtils.isNotBlank(record.getTimeResult()) && !"Normal".equals(record.getTimeResult())) {
            return timeResultLabel(record.getTimeResult());
        }
        if (StringUtils.isNotBlank(record.getLocationResult()) && !"Normal".equals(record.getLocationResult())) {
            return "位置异常";
        }
        return "正常";
    }

    private String timeResultLabel(String timeResult) {
        if ("Late".equals(timeResult)) {
            return "迟到";
        }
        if ("SeriousLate".equals(timeResult)) {
            return "严重迟到";
        }
        if ("Early".equals(timeResult)) {
            return "早退";
        }
        if ("Absenteeism".equals(timeResult)) {
            return "旷工";
        }
        if ("NotSigned".equals(timeResult)) {
            return "未打卡";
        }
        if ("Leave".equals(timeResult)) {
            return "请假";
        }
        if ("Rest".equals(timeResult)) {
            return "休息";
        }
        if ("Field".equals(timeResult)) {
            return "外出";
        }
        if ("BusinessTrip".equals(timeResult)) {
            return "出差";
        }
        if ("Approval".equals(timeResult)) {
            return "补卡审批通过";
        }
        return timeResult;
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String firstJsonString(JSONObject json, String... keys) {
        if (json == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = json.get(key);
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private Date parseDingDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        }
        String str = String.valueOf(value);
        if (StringUtils.isBlank(str)) {
            return null;
        }
        if (str.matches("^\\d+$")) {
            return new Date(Long.parseLong(str));
        }
        return DateUtils.parseDate(str);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null || StringUtils.isBlank(String.valueOf(value))) {
            return null;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private Date addDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }

    private void clearTime(Calendar calendar) {
        for (int field : Arrays.asList(Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND)) {
            calendar.set(field, 0);
        }
    }

    private static class RawPersonDayStats {

        private final String dingUserId;
        private final String userName;
        private final Date workDate;
        private Date onDutyTime;
        private Date offDutyTime;
        private boolean leave;
        private boolean rest;
        private boolean absenteeism;
        private boolean exception;
        private int lateCount;
        private int earlyCount;
        private int missingCardCount;
        private int recordCount;

        private RawPersonDayStats(String dingUserId, String userName, Date workDate) {
            this.dingUserId = dingUserId;
            this.userName = userName;
            this.workDate = workDate;
        }

        private void accept(DingTalkAttendanceRecord record) {
            recordCount++;
            Date checkTime = record.getCheckTime();
            if ("OnDuty".equals(record.getCheckType()) && checkTime != null) {
                onDutyTime = onDutyTime == null || checkTime.before(onDutyTime) ? checkTime : onDutyTime;
            } else if ("OffDuty".equals(record.getCheckType()) && checkTime != null) {
                offDutyTime = offDutyTime == null || checkTime.after(offDutyTime) ? checkTime : offDutyTime;
            }

            String result = record.getTimeResult();
            if ("Leave".equals(result)) {
                leave = true;
            } else if ("Rest".equals(result)) {
                rest = true;
            } else if ("Absenteeism".equals(result)) {
                absenteeism = true;
                exception = true;
            } else if ("Late".equals(result) || "SeriousLate".equals(result)) {
                lateCount++;
                exception = true;
            } else if ("Early".equals(result)) {
                earlyCount++;
                exception = true;
            } else if ("NotSigned".equals(result)) {
                missingCardCount++;
                exception = true;
            } else if (StringUtils.isNotBlank(result) && !"Normal".equals(result)
                && !"Field".equals(result) && !"BusinessTrip".equals(result) && !"Approval".equals(result)) {
                exception = true;
            }
            if (StringUtils.isNotBlank(record.getLocationResult()) && !"Normal".equals(record.getLocationResult())) {
                exception = true;
            }
        }

        private BigDecimal workMinutes() {
            if (onDutyTime == null || offDutyTime == null || !offDutyTime.after(onDutyTime)) {
                return BigDecimal.ZERO;
            }
            return BigDecimal.valueOf(offDutyTime.getTime() - onDutyTime.getTime())
                .divide(BigDecimal.valueOf(60000L), 2, RoundingMode.HALF_UP);
        }

    }

}
