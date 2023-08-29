package com.ruoyi.system.service.impl;

import com.alibaba.fastjson.JSONObject;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.DingTalkAttendanceRecord;
import com.ruoyi.system.domain.FinancingDailyDetail;
import com.ruoyi.system.domain.bo.FinancingDailyDetailQuery;
import com.ruoyi.system.domain.vo.AttendanceExcelMergeResultVo;
import com.ruoyi.system.mapper.DingTalkAttendanceRecordMapper;
import com.ruoyi.system.mapper.FinancingDailyDetailMapper;
import com.ruoyi.system.service.IFinancingDailyDetailService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 考勤每日统计明细服务实现
 *
 * @author codex
 */
@RequiredArgsConstructor
@Service
public class FinancingDailyDetailServiceImpl extends ServiceImpl<FinancingDailyDetailMapper, FinancingDailyDetail>
    implements IFinancingDailyDetailService {

    private static final String DAILY_SHEET_NAME = "每日统计";
    private static final int TITLE_ROW_INDEX = 0;
    private static final int GENERATED_TIME_ROW_INDEX = 1;
    private static final int HEADER_ROW_INDEX = 2;
    private static final int DATA_START_ROW_INDEX = 4;
    private static final Pattern REPORT_RANGE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})\\s*至\\s*(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern DATE_LABEL_PATTERN = Pattern.compile("(\\d{2})-(\\d{2})-(\\d{2})");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(?:\\.\\d+)?");
    private static final Pattern CLOCK_TIME_PATTERN = Pattern.compile("(\\d{1,2}:\\d{2})(?::\\d{2})?");
    private static final Pattern SHIFT_TIME_PATTERN = Pattern.compile("(\\d{1,2}:\\d{2})\\s*-\\s*(\\d{1,2}:\\d{2})");
    private static final String EXCEL_SOURCE_TYPE = "Excel导入";
    private static final Charset GB18030 = Charset.forName("GB18030");
    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String XLS_CONTENT_TYPE = "application/vnd.ms-excel";

    private final FinancingDailyDetailMapper baseMapper;
    private final DingTalkAttendanceRecordMapper attendanceRecordMapper;

    @Override
    public TableDataInfo<FinancingDailyDetail> selectPageList(FinancingDailyDetailQuery query, PageQuery pageQuery) {
        LambdaQueryWrapper<FinancingDailyDetail> lqw = buildQueryWrapper(query)
            .orderByDesc(FinancingDailyDetail::getAttendanceDate)
            .orderByAsc(FinancingDailyDetail::getEmployeeName);
        Page<FinancingDailyDetail> page = baseMapper.selectPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public FinancingDailyDetail selectById(Long detailId) {
        return baseMapper.selectById(detailId);
    }

    @Override
    public String importData(MultipartFile file, boolean updateSupport) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请选择要导入的Excel文件");
        }
        String sourceFile = file.getOriginalFilename();
        DataFormatter formatter = new DataFormatter(Locale.CHINA);

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Sheet sheet = workbook.getSheet(DAILY_SHEET_NAME);
            if (sheet == null) {
                sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            }
            if (sheet == null) {
                throw new ServiceException("Excel文件中未找到工作表");
            }
            validateHeader(sheet, formatter, evaluator);

            String title = cellString(sheet.getRow(TITLE_ROW_INDEX), 0, formatter, evaluator);
            String generatedTime = cleanGeneratedTime(cellString(sheet.getRow(GENERATED_TIME_ROW_INDEX), 0, formatter, evaluator));
            Date[] reportRange = parseReportRange(title);
            String importBatchNo = DateUtils.dateTimeNow() + IdUtil.fastSimpleUUID().substring(0, 8);

            int insertCount = 0;
            int updateCount = 0;
            int skipCount = 0;
            int attendanceRecordCount = 0;
            for (int i = DATA_START_ROW_INDEX; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (isBlankDataRow(row, formatter, evaluator)) {
                    continue;
                }
                FinancingDailyDetail detail = toDetail(row, formatter, evaluator);
                detail.setSourceFile(sourceFile);
                detail.setImportBatchNo(importBatchNo);
                detail.setReportGeneratedTime(generatedTime);
                detail.setReportStartDate(reportRange[0]);
                detail.setReportEndDate(reportRange[1]);

                FinancingDailyDetail exists = selectExisting(detail);
                if (exists != null) {
                    if (updateSupport) {
                        detail.setDetailId(exists.getDetailId());
                        baseMapper.updateById(detail);
                        attendanceRecordCount += syncAttendanceRecords(detail);
                        updateCount++;
                    } else {
                        attendanceRecordCount += syncAttendanceRecords(exists);
                        skipCount++;
                    }
                } else {
                    baseMapper.insert(detail);
                    attendanceRecordCount += syncAttendanceRecords(detail);
                    insertCount++;
                }
            }
            return String.format("导入完成：新增 %d 条，更新 %d 条，跳过 %d 条，关联钉钉考勤记录 %d 条。",
                insertCount, updateCount, skipCount, attendanceRecordCount);
        }
    }

    @Override
    public AttendanceExcelMergeResultVo mergeDailyStatistics(MultipartFile[] files) throws IOException {
        if (files == null || files.length < 2) {
            throw new ServiceException("请至少选择两个需要合并的Excel文件");
        }

        DataFormatter formatter = new DataFormatter(Locale.CHINA);
        List<Workbook> workbooks = new ArrayList<>();
        List<MergeRowSnapshot> rows = new ArrayList<>();
        Workbook targetWorkbook = null;
        Sheet targetSheet = null;
        int targetColumnCount = 0;
        Date beginDate = null;
        Date endDate = null;
        int originalIndex = 0;
        int validFileCount = 0;

        try {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                validFileCount++;
                Workbook workbook = WorkbookFactory.create(file.getInputStream());
                workbooks.add(workbook);
                Sheet sheet = workbook.getSheet(DAILY_SHEET_NAME);
                if (sheet == null) {
                    sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
                }
                if (sheet == null) {
                    throw new ServiceException("Excel文件中未找到工作表");
                }
                FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                validateHeader(sheet, formatter, evaluator);
                if (targetWorkbook == null) {
                    targetWorkbook = workbook;
                    targetSheet = sheet;
                    targetColumnCount = sheet.getRow(HEADER_ROW_INDEX).getLastCellNum();
                } else if (!targetWorkbook.getClass().equals(workbook.getClass())) {
                    throw new ServiceException("请上传同一格式的Excel文件，建议统一使用 .xlsx 文件");
                }
                Date[] reportRange = parseReportRange(cellString(sheet.getRow(TITLE_ROW_INDEX), 0, formatter, evaluator));
                beginDate = minDate(beginDate, reportRange[0]);
                endDate = maxDate(endDate, reportRange[1]);

                for (int rowIndex = DATA_START_ROW_INDEX; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (isBlankDataRow(row, formatter, evaluator)) {
                        continue;
                    }
                    rows.add(snapshotRow(workbook, row, targetColumnCount, formatter, evaluator, originalIndex++));
                }
            }

            if (validFileCount < 2) {
                throw new ServiceException("请至少选择两个需要合并的Excel文件");
            }
            if (targetWorkbook == null || targetSheet == null || rows.isEmpty()) {
                throw new ServiceException("未读取到可合并的每日统计数据");
            }

            rows.sort(this::compareMergeRows);
            clearDataRows(targetSheet);
            updateMergeTitle(targetSheet, beginDate, endDate, rows.size(), validFileCount);
            Map<String, CellStyle> styleCache = new HashMap<>();
            int outputRowIndex = DATA_START_ROW_INDEX;
            for (MergeRowSnapshot row : rows) {
                writeSnapshotRow(targetWorkbook, targetSheet, outputRowIndex++, row, styleCache);
            }

            String extension = isXlsx(targetWorkbook) ? ".xlsx" : ".xls";
            String fileName = buildMergeFileName(beginDate, endDate, extension);
            String contentType = isXlsx(targetWorkbook) ? XLSX_CONTENT_TYPE : XLS_CONTENT_TYPE;
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                targetWorkbook.write(outputStream);
                return new AttendanceExcelMergeResultVo(fileName, contentType, outputStream.toByteArray());
            }
        } finally {
            for (Workbook workbook : workbooks) {
                workbook.close();
            }
        }
    }

    private LambdaQueryWrapper<FinancingDailyDetail> buildQueryWrapper(FinancingDailyDetailQuery query) {
        FinancingDailyDetailQuery q = query == null ? new FinancingDailyDetailQuery() : query;
        LambdaQueryWrapper<FinancingDailyDetail> lqw = new LambdaQueryWrapper<FinancingDailyDetail>()
            .like(StringUtils.isNotBlank(q.getEmployeeName()), FinancingDailyDetail::getEmployeeName, q.getEmployeeName())
            .like(StringUtils.isNotBlank(q.getDingUserId()), FinancingDailyDetail::getDingUserId, q.getDingUserId())
            .like(StringUtils.isNotBlank(q.getDeptName()), FinancingDailyDetail::getDeptName, q.getDeptName())
            .like(StringUtils.isNotBlank(q.getShiftName()), FinancingDailyDetail::getShiftName, q.getShiftName())
            .ge(q.getBeginDate() != null, FinancingDailyDetail::getAttendanceDate, q.getBeginDate())
            .le(q.getEndDate() != null, FinancingDailyDetail::getAttendanceDate, q.getEndDate());
        if (StringUtils.isNotBlank(q.getResultKeyword())) {
            lqw.and(wrapper -> wrapper
                .like(FinancingDailyDetail::getOn1Result, q.getResultKeyword())
                .or().like(FinancingDailyDetail::getOff1Result, q.getResultKeyword())
                .or().like(FinancingDailyDetail::getOn2Result, q.getResultKeyword())
                .or().like(FinancingDailyDetail::getOff2Result, q.getResultKeyword())
                .or().like(FinancingDailyDetail::getOn3Result, q.getResultKeyword())
                .or().like(FinancingDailyDetail::getOff3Result, q.getResultKeyword())
            );
        }
        return lqw;
    }

    private void validateHeader(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
        Row header = sheet.getRow(HEADER_ROW_INDEX);
        String nameHeader = cellString(header, 0, formatter, evaluator);
        String userIdHeader = cellString(header, 5, formatter, evaluator);
        String workDateHeader = cellString(header, 7, formatter, evaluator);
        if (!"姓名".equals(nameHeader) || !"UserId".equalsIgnoreCase(userIdHeader) || !"workDate".equalsIgnoreCase(workDateHeader)) {
            throw new ServiceException("Excel表头不匹配，请上传钉钉每日统计文件");
        }
    }

    private FinancingDailyDetail toDetail(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        FinancingDailyDetail detail = new FinancingDailyDetail();
        detail.setEmployeeName(cellString(row, 0, formatter, evaluator));
        detail.setAttendanceGroup(cellString(row, 1, formatter, evaluator));
        detail.setDeptName(cellString(row, 2, formatter, evaluator));
        detail.setEmployeeNo(cellString(row, 3, formatter, evaluator));
        detail.setPositionName(cellString(row, 4, formatter, evaluator));
        detail.setDingUserId(cellString(row, 5, formatter, evaluator));
        detail.setAttendanceDateLabel(cellString(row, 6, formatter, evaluator));
        detail.setWorkDateMillis(cellLong(row, 7, formatter, evaluator));
        detail.setAttendanceDate(parseAttendanceDate(detail.getWorkDateMillis(), detail.getAttendanceDateLabel()));
        detail.setShiftName(cellString(row, 8, formatter, evaluator));
        detail.setOn1Time(cellString(row, 9, formatter, evaluator));
        detail.setOn1Result(cellString(row, 10, formatter, evaluator));
        detail.setOff1Time(cellString(row, 11, formatter, evaluator));
        detail.setOff1Result(cellString(row, 12, formatter, evaluator));
        detail.setOn2Time(cellString(row, 13, formatter, evaluator));
        detail.setOn2Result(cellString(row, 14, formatter, evaluator));
        detail.setOff2Time(cellString(row, 15, formatter, evaluator));
        detail.setOff2Result(cellString(row, 16, formatter, evaluator));
        detail.setOn3Time(cellString(row, 17, formatter, evaluator));
        detail.setOn3Result(cellString(row, 18, formatter, evaluator));
        detail.setOff3Time(cellString(row, 19, formatter, evaluator));
        detail.setOff3Result(cellString(row, 20, formatter, evaluator));
        detail.setRelatedApproval(cellString(row, 21, formatter, evaluator));
        detail.setAttendanceDays(cellBigDecimal(row, 22, formatter, evaluator));
        detail.setRestDays(cellBigDecimal(row, 23, formatter, evaluator));
        detail.setWorkDuration(cellBigDecimal(row, 24, formatter, evaluator));
        detail.setLateCount(cellInteger(row, 25, formatter, evaluator));
        detail.setLateDuration(cellBigDecimal(row, 26, formatter, evaluator));
        detail.setSeriousLateCount(cellInteger(row, 27, formatter, evaluator));
        detail.setSeriousLateDuration(cellBigDecimal(row, 28, formatter, evaluator));
        detail.setAbsenteeLateCount(cellInteger(row, 29, formatter, evaluator));
        detail.setEarlyCount(cellInteger(row, 30, formatter, evaluator));
        detail.setEarlyDuration(cellBigDecimal(row, 31, formatter, evaluator));
        detail.setOnMissingCount(cellInteger(row, 32, formatter, evaluator));
        detail.setOffMissingCount(cellInteger(row, 33, formatter, evaluator));
        detail.setAbsenteeismDays(cellBigDecimal(row, 34, formatter, evaluator));
        detail.setBusinessTripDuration(cellBigDecimal(row, 35, formatter, evaluator));
        detail.setOutsideDuration(cellBigDecimal(row, 36, formatter, evaluator));
        detail.setOvertimeApprovalStats(cellString(row, 37, formatter, evaluator));
        detail.setOvertimeDurationRule(cellBigDecimal(row, 38, formatter, evaluator));
        return detail;
    }

    private FinancingDailyDetail selectExisting(FinancingDailyDetail detail) {
        if (StringUtils.isBlank(detail.getDingUserId()) || detail.getWorkDateMillis() == null) {
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<FinancingDailyDetail>()
            .eq(FinancingDailyDetail::getDingUserId, detail.getDingUserId())
            .eq(FinancingDailyDetail::getWorkDateMillis, detail.getWorkDateMillis())
            .last("limit 1"));
    }

    private Date[] parseReportRange(String title) {
        Date[] range = new Date[2];
        if (StringUtils.isBlank(title)) {
            return range;
        }
        Matcher matcher = REPORT_RANGE_PATTERN.matcher(title);
        if (matcher.find()) {
            range[0] = DateUtils.parseDate(matcher.group(1));
            range[1] = DateUtils.parseDate(matcher.group(2));
        }
        return range;
    }

    private MergeRowSnapshot snapshotRow(Workbook workbook, Row row, int columnCount, DataFormatter formatter, FormulaEvaluator evaluator, int originalIndex) {
        MergeRowSnapshot snapshot = new MergeRowSnapshot();
        snapshot.sourceWorkbook = workbook;
        snapshot.name = cellString(row, 0, formatter, evaluator);
        snapshot.workDateSort = cellSortNumber(row, 7, formatter, evaluator);
        snapshot.originalIndex = originalIndex;
        snapshot.height = row.getHeight();
        snapshot.zeroHeight = row.getZeroHeight();
        snapshot.rowStyle = row.getRowStyle();
        snapshot.cells = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            snapshot.cells.add(snapshotCell(workbook, row.getCell(columnIndex)));
        }
        return snapshot;
    }

    private CellSnapshot snapshotCell(Workbook workbook, Cell cell) {
        CellSnapshot snapshot = new CellSnapshot();
        snapshot.sourceWorkbook = workbook;
        if (cell == null) {
            snapshot.cellType = CellType.BLANK;
            return snapshot;
        }
        snapshot.cellType = cell.getCellType();
        snapshot.cellStyle = cell.getCellStyle();
        switch (snapshot.cellType) {
            case STRING:
                snapshot.stringValue = cell.getStringCellValue();
                break;
            case NUMERIC:
                snapshot.numericValue = cell.getNumericCellValue();
                break;
            case BOOLEAN:
                snapshot.booleanValue = cell.getBooleanCellValue();
                break;
            case ERROR:
                snapshot.errorValue = cell.getErrorCellValue();
                break;
            case FORMULA:
                snapshot.formula = cell.getCellFormula();
                break;
            default:
                break;
        }
        return snapshot;
    }

    private BigDecimal cellSortNumber(Row row, int index, DataFormatter formatter, FormulaEvaluator evaluator) {
        Cell cell = row == null ? null : row.getCell(index);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        String value = cellString(row, index, formatter, evaluator);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(value.replace(",", ""));
        if (!matcher.find()) {
            return null;
        }
        try {
            return new BigDecimal(matcher.group());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int compareMergeRows(MergeRowSnapshot left, MergeRowSnapshot right) {
        int nameCompare = compareByGb18030(left.name, right.name);
        if (nameCompare != 0) {
            return nameCompare;
        }
        int literalNameCompare = firstNotBlank(left.name).compareTo(firstNotBlank(right.name));
        if (literalNameCompare != 0) {
            return literalNameCompare;
        }
        int workDateCompare = compareNullableNumber(left.workDateSort, right.workDateSort);
        if (workDateCompare != 0) {
            return workDateCompare;
        }
        return Integer.compare(left.originalIndex, right.originalIndex);
    }

    private int compareByGb18030(String left, String right) {
        byte[] leftBytes = firstNotBlank(left).getBytes(GB18030);
        byte[] rightBytes = firstNotBlank(right).getBytes(GB18030);
        int length = Math.min(leftBytes.length, rightBytes.length);
        for (int i = 0; i < length; i++) {
            int compare = Integer.compare(Byte.toUnsignedInt(leftBytes[i]), Byte.toUnsignedInt(rightBytes[i]));
            if (compare != 0) {
                return compare;
            }
        }
        return Integer.compare(leftBytes.length, rightBytes.length);
    }

    private int compareNullableNumber(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private void clearDataRows(Sheet sheet) {
        for (int rowIndex = sheet.getLastRowNum(); rowIndex >= DATA_START_ROW_INDEX; rowIndex--) {
            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                sheet.removeRow(row);
            }
        }
    }

    private void updateMergeTitle(Sheet sheet, Date beginDate, Date endDate, int rowCount, int fileCount) {
        Row titleRow = sheet.getRow(TITLE_ROW_INDEX);
        if (titleRow == null) {
            titleRow = sheet.createRow(TITLE_ROW_INDEX);
        }
        Row generatedRow = sheet.getRow(GENERATED_TIME_ROW_INDEX);
        if (generatedRow == null) {
            generatedRow = sheet.createRow(GENERATED_TIME_ROW_INDEX);
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        if (beginDate != null && endDate != null) {
            titleRow.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
                .setCellValue("每日统计 统计日期：" + dateFormat.format(beginDate) + " 至 " + dateFormat.format(endDate));
        }
        generatedRow.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
            .setCellValue("报表生成时间：" + DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, new Date())
                + "；合并文件数：" + fileCount + "；合并数据：" + rowCount + "条");
    }

    private void writeSnapshotRow(Workbook targetWorkbook, Sheet targetSheet, int rowIndex, MergeRowSnapshot snapshot, Map<String, CellStyle> styleCache) {
        Row targetRow = targetSheet.createRow(rowIndex);
        targetRow.setHeight(snapshot.height);
        targetRow.setZeroHeight(snapshot.zeroHeight);
        if (snapshot.rowStyle != null) {
            targetRow.setRowStyle(resolveStyle(targetWorkbook, snapshot.sourceWorkbook, snapshot.rowStyle, styleCache));
        }
        for (int columnIndex = 0; columnIndex < snapshot.cells.size(); columnIndex++) {
            CellSnapshot cellSnapshot = snapshot.cells.get(columnIndex);
            Cell targetCell = targetRow.createCell(columnIndex);
            writeSnapshotCell(targetWorkbook, targetCell, cellSnapshot, styleCache);
        }
    }

    private void writeSnapshotCell(Workbook targetWorkbook, Cell targetCell, CellSnapshot snapshot, Map<String, CellStyle> styleCache) {
        if (snapshot.cellStyle != null) {
            targetCell.setCellStyle(resolveStyle(targetWorkbook, snapshot.sourceWorkbook, snapshot.cellStyle, styleCache));
        }
        switch (snapshot.cellType) {
            case STRING:
                targetCell.setCellValue(snapshot.stringValue);
                break;
            case NUMERIC:
                targetCell.setCellValue(snapshot.numericValue == null ? 0D : snapshot.numericValue);
                break;
            case BOOLEAN:
                targetCell.setCellValue(Boolean.TRUE.equals(snapshot.booleanValue));
                break;
            case ERROR:
                targetCell.setCellErrorValue(snapshot.errorValue == null ? 0 : snapshot.errorValue);
                break;
            case FORMULA:
                if (StringUtils.isNotBlank(snapshot.formula)) {
                    targetCell.setCellFormula(snapshot.formula);
                }
                break;
            default:
                targetCell.setBlank();
                break;
        }
    }

    private CellStyle resolveStyle(Workbook targetWorkbook, Workbook sourceWorkbook, CellStyle sourceStyle, Map<String, CellStyle> styleCache) {
        if (sourceStyle == null) {
            return null;
        }
        if (targetWorkbook == sourceWorkbook) {
            return sourceStyle;
        }
        String cacheKey = System.identityHashCode(sourceWorkbook) + ":" + sourceStyle.getIndex();
        CellStyle cached = styleCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        CellStyle targetStyle = targetWorkbook.createCellStyle();
        targetStyle.cloneStyleFrom(sourceStyle);
        styleCache.put(cacheKey, targetStyle);
        return targetStyle;
    }

    private Date minDate(Date left, Date right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.before(right) ? left : right;
    }

    private Date maxDate(Date left, Date right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.after(right) ? left : right;
    }

    private String buildMergeFileName(Date beginDate, Date endDate, String extension) {
        SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyyMMdd");
        if (beginDate != null && endDate != null) {
            return "融资团队_每日统计_" + fileDateFormat.format(beginDate) + "-" + fileDateFormat.format(endDate) + "_按姓名排序" + extension;
        }
        return "融资团队_每日统计_合并_按姓名排序" + extension;
    }

    private boolean isXlsx(Workbook workbook) {
        return workbook instanceof XSSFWorkbook;
    }

    private String firstNotBlank(String value) {
        return value == null ? "" : value.trim();
    }

    private Date parseAttendanceDate(Long workDateMillis, String dateLabel) {
        if (workDateMillis != null && workDateMillis > 0) {
            return new Date(workDateMillis);
        }
        if (StringUtils.isBlank(dateLabel)) {
            return null;
        }
        Matcher matcher = DATE_LABEL_PATTERN.matcher(dateLabel);
        if (matcher.find()) {
            return DateUtils.parseDate("20" + matcher.group(1) + "-" + matcher.group(2) + "-" + matcher.group(3));
        }
        return null;
    }

    private boolean isBlankDataRow(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null) {
            return true;
        }
        return StringUtils.isBlank(cellString(row, 0, formatter, evaluator))
            && StringUtils.isBlank(cellString(row, 5, formatter, evaluator))
            && StringUtils.isBlank(cellString(row, 6, formatter, evaluator))
            && StringUtils.isBlank(cellString(row, 7, formatter, evaluator));
    }

    private String cleanGeneratedTime(String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        return value.replace("报表生成时间：", "").replace("报表生成时间:", "").trim();
    }

    private String cellString(Row row, int index, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(index);
        if (cell == null) {
            return "";
        }
        String value = formatter.formatCellValue(cell, evaluator);
        return value == null ? "" : value.trim();
    }

    private Long cellLong(Row row, int index, DataFormatter formatter, FormulaEvaluator evaluator) {
        Cell cell = row == null ? null : row.getCell(index);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue()).longValue();
        }
        String value = cellString(row, index, formatter, evaluator);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(",", "")).longValue();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal cellBigDecimal(Row row, int index, DataFormatter formatter, FormulaEvaluator evaluator) {
        String value = cellString(row, index, formatter, evaluator);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(value.replace(",", ""));
        if (!matcher.find()) {
            return null;
        }
        try {
            return new BigDecimal(matcher.group());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer cellInteger(Row row, int index, DataFormatter formatter, FormulaEvaluator evaluator) {
        BigDecimal value = cellBigDecimal(row, index, formatter, evaluator);
        return value == null ? null : value.intValue();
    }

    private int syncAttendanceRecords(FinancingDailyDetail detail) {
        if (detail == null || StringUtils.isBlank(detail.getDingUserId()) || detail.getAttendanceDate() == null) {
            return 0;
        }
        int count = 0;
        count += saveAttendanceRecord(detail, 1, "OnDuty", detail.getOn1Time(), detail.getOn1Result());
        count += saveAttendanceRecord(detail, 1, "OffDuty", detail.getOff1Time(), detail.getOff1Result());
        count += saveAttendanceRecord(detail, 2, "OnDuty", detail.getOn2Time(), detail.getOn2Result());
        count += saveAttendanceRecord(detail, 2, "OffDuty", detail.getOff2Time(), detail.getOff2Result());
        count += saveAttendanceRecord(detail, 3, "OnDuty", detail.getOn3Time(), detail.getOn3Result());
        count += saveAttendanceRecord(detail, 3, "OffDuty", detail.getOff3Time(), detail.getOff3Result());
        return count;
    }

    private int saveAttendanceRecord(FinancingDailyDetail detail, int clockIndex, String checkType, String clockTime, String result) {
        if (StringUtils.isBlank(clockTime) && isBlankOrRest(result)) {
            return 0;
        }
        Date userCheckTime = parseClockTime(detail.getAttendanceDate(), clockTime);
        Date baseCheckTime = userCheckTime == null ? parseClockTime(detail.getAttendanceDate(), scheduledClockTime(detail, checkType)) : userCheckTime;
        DingTalkAttendanceRecord record = new DingTalkAttendanceRecord();
        record.setDingRecordId(buildExcelRecordId(detail, clockIndex, checkType));
        record.setDingUserId(detail.getDingUserId());
        record.setUserName(detail.getEmployeeName());
        record.setWorkDate(detail.getAttendanceDate());
        record.setCheckTime(userCheckTime == null ? baseCheckTime : userCheckTime);
        record.setBaseCheckTime(baseCheckTime);
        record.setUserCheckTime(userCheckTime);
        record.setCheckType(checkType);
        record.setSourceType(EXCEL_SOURCE_TYPE);
        record.setTimeResult(toTimeResult(result));
        record.setLocationResult("Normal");
        record.setDailyDetailId(detail.getDetailId());
        record.setSourceFile(detail.getSourceFile());
        record.setImportBatchNo(detail.getImportBatchNo());
        record.setRawData(buildRawData(detail, clockIndex, checkType, clockTime, result));
        upsertAttendanceRecord(record);
        return 1;
    }

    private void upsertAttendanceRecord(DingTalkAttendanceRecord record) {
        DingTalkAttendanceRecord existing = attendanceRecordMapper.selectOne(new LambdaQueryWrapper<DingTalkAttendanceRecord>()
            .eq(DingTalkAttendanceRecord::getDingRecordId, record.getDingRecordId())
            .last("limit 1"));
        if (existing == null) {
            attendanceRecordMapper.insert(record);
        } else {
            record.setRecordId(existing.getRecordId());
            attendanceRecordMapper.updateById(record);
        }
    }

    private String buildExcelRecordId(FinancingDailyDetail detail, int clockIndex, String checkType) {
        Long workDateMillis = detail.getWorkDateMillis();
        if (workDateMillis == null && detail.getAttendanceDate() != null) {
            workDateMillis = detail.getAttendanceDate().getTime();
        }
        return "EXCEL:" + md5Hex(detail.getDingUserId() + ":" + workDateMillis + ":" + clockIndex + ":" + checkType);
    }

    private String md5Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : bytes) {
                String hex = Integer.toHexString(item & 0xff);
                if (hex.length() == 1) {
                    builder.append('0');
                }
                builder.append(hex);
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new ServiceException("生成Excel考勤记录ID失败");
        }
    }

    private String buildRawData(FinancingDailyDetail detail, int clockIndex, String checkType, String clockTime, String result) {
        JSONObject json = new JSONObject();
        json.put("source", EXCEL_SOURCE_TYPE);
        json.put("dailyDetailId", detail.getDetailId());
        json.put("sourceFile", detail.getSourceFile());
        json.put("importBatchNo", detail.getImportBatchNo());
        json.put("clockIndex", clockIndex);
        json.put("checkType", checkType);
        json.put("clockTime", clockTime);
        json.put("result", result);
        json.put("attendanceDateLabel", detail.getAttendanceDateLabel());
        json.put("shiftName", detail.getShiftName());
        return json.toJSONString();
    }

    private Date parseClockTime(Date attendanceDate, String clockTime) {
        if (attendanceDate == null || StringUtils.isBlank(clockTime)) {
            return null;
        }
        Matcher matcher = CLOCK_TIME_PATTERN.matcher(clockTime);
        if (!matcher.find()) {
            return null;
        }
        return DateUtils.parseDate(DateUtils.dateTime(attendanceDate) + " " + normalizeTime(matcher.group(1)) + ":00");
    }

    private String scheduledClockTime(FinancingDailyDetail detail, String checkType) {
        if (detail == null || StringUtils.isBlank(detail.getShiftName()) || detail.getShiftName().contains("休息")) {
            return "";
        }
        Matcher matcher = SHIFT_TIME_PATTERN.matcher(detail.getShiftName());
        if (matcher.find()) {
            return "OnDuty".equals(checkType) ? matcher.group(1) : matcher.group(2);
        }
        return "OnDuty".equals(checkType) ? "09:00" : "18:00";
    }

    private String normalizeTime(String time) {
        String[] parts = time.split(":");
        String hour = parts[0].length() == 1 ? "0" + parts[0] : parts[0];
        return hour + ":" + parts[1];
    }

    private boolean isBlankOrRest(String result) {
        return StringUtils.isBlank(result) || result.contains("休息");
    }

    private String toTimeResult(String result) {
        if (StringUtils.isBlank(result)) {
            return "Normal";
        }
        if (result.contains("严重迟到")) {
            return "SeriousLate";
        }
        if (result.contains("迟到")) {
            return "Late";
        }
        if (result.contains("早退")) {
            return "Early";
        }
        if (result.contains("旷工")) {
            return "Absenteeism";
        }
        if (result.contains("缺卡") || result.contains("未打卡")) {
            return "NotSigned";
        }
        if (result.contains("请假")) {
            return "Leave";
        }
        if (result.contains("外出")) {
            return "Field";
        }
        if (result.contains("出差")) {
            return "BusinessTrip";
        }
        if (result.contains("补卡")) {
            return "Approval";
        }
        if (result.contains("正常")) {
            return "Normal";
        }
        return result;
    }

    private static class MergeRowSnapshot {
        private Workbook sourceWorkbook;
        private String name;
        private BigDecimal workDateSort;
        private int originalIndex;
        private short height;
        private boolean zeroHeight;
        private CellStyle rowStyle;
        private List<CellSnapshot> cells;
    }

    private static class CellSnapshot {
        private Workbook sourceWorkbook;
        private CellType cellType;
        private String stringValue;
        private Double numericValue;
        private Boolean booleanValue;
        private Byte errorValue;
        private String formula;
        private CellStyle cellStyle;
    }

}
