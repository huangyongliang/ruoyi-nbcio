-- 融资团队每日统计明细导入

CREATE TABLE IF NOT EXISTS `financing_daily_detail` (
  `detail_id` bigint NOT NULL COMMENT '明细ID',
  `source_file` varchar(255) DEFAULT NULL COMMENT '来源文件名',
  `import_batch_no` varchar(64) DEFAULT NULL COMMENT '导入批次号',
  `report_start_date` date DEFAULT NULL COMMENT '报表开始日期',
  `report_end_date` date DEFAULT NULL COMMENT '报表结束日期',
  `report_generated_time` varchar(100) DEFAULT NULL COMMENT '报表生成时间',
  `employee_name` varchar(100) DEFAULT NULL COMMENT '姓名',
  `attendance_group` varchar(100) DEFAULT NULL COMMENT '考勤组',
  `dept_name` varchar(255) DEFAULT NULL COMMENT '部门',
  `employee_no` varchar(64) DEFAULT NULL COMMENT '工号',
  `position_name` varchar(100) DEFAULT NULL COMMENT '职位',
  `ding_user_id` varchar(100) DEFAULT NULL COMMENT '钉钉用户ID',
  `attendance_date_label` varchar(50) DEFAULT NULL COMMENT '日期文本',
  `attendance_date` date DEFAULT NULL COMMENT '考勤日期',
  `work_date_millis` bigint DEFAULT NULL COMMENT 'workDate毫秒值',
  `shift_name` varchar(100) DEFAULT NULL COMMENT '班次',
  `on1_time` varchar(32) DEFAULT NULL COMMENT '上班1打卡时间',
  `on1_result` varchar(64) DEFAULT NULL COMMENT '上班1打卡结果',
  `off1_time` varchar(32) DEFAULT NULL COMMENT '下班1打卡时间',
  `off1_result` varchar(64) DEFAULT NULL COMMENT '下班1打卡结果',
  `on2_time` varchar(32) DEFAULT NULL COMMENT '上班2打卡时间',
  `on2_result` varchar(64) DEFAULT NULL COMMENT '上班2打卡结果',
  `off2_time` varchar(32) DEFAULT NULL COMMENT '下班2打卡时间',
  `off2_result` varchar(64) DEFAULT NULL COMMENT '下班2打卡结果',
  `on3_time` varchar(32) DEFAULT NULL COMMENT '上班3打卡时间',
  `on3_result` varchar(64) DEFAULT NULL COMMENT '上班3打卡结果',
  `off3_time` varchar(32) DEFAULT NULL COMMENT '下班3打卡时间',
  `off3_result` varchar(64) DEFAULT NULL COMMENT '下班3打卡结果',
  `related_approval` text COMMENT '关联的审批单',
  `attendance_days` decimal(10,2) DEFAULT NULL COMMENT '出勤天数',
  `rest_days` decimal(10,2) DEFAULT NULL COMMENT '休息天数',
  `work_duration` decimal(10,2) DEFAULT NULL COMMENT '工作时长(分钟)',
  `late_count` int DEFAULT NULL COMMENT '迟到次数',
  `late_duration` decimal(10,2) DEFAULT NULL COMMENT '迟到时长',
  `serious_late_count` int DEFAULT NULL COMMENT '严重迟到次数',
  `serious_late_duration` decimal(10,2) DEFAULT NULL COMMENT '严重迟到时长',
  `absentee_late_count` int DEFAULT NULL COMMENT '旷工迟到次数',
  `early_count` int DEFAULT NULL COMMENT '早退次数',
  `early_duration` decimal(10,2) DEFAULT NULL COMMENT '早退时长',
  `on_missing_count` int DEFAULT NULL COMMENT '上班缺卡次数',
  `off_missing_count` int DEFAULT NULL COMMENT '下班缺卡次数',
  `absenteeism_days` decimal(10,2) DEFAULT NULL COMMENT '旷工天数',
  `business_trip_duration` decimal(10,2) DEFAULT NULL COMMENT '出差时长',
  `outside_duration` decimal(10,2) DEFAULT NULL COMMENT '外出时长',
  `overtime_approval_stats` varchar(500) DEFAULT NULL COMMENT '加班-审批单统计',
  `overtime_duration_rule` decimal(10,2) DEFAULT NULL COMMENT '加班时长-按加班规则计算',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`detail_id`),
  UNIQUE KEY `uk_financing_daily_detail_user_date` (`ding_user_id`, `work_date_millis`),
  KEY `idx_financing_daily_detail_date` (`attendance_date`),
  KEY `idx_financing_daily_detail_name` (`employee_name`),
  KEY `idx_financing_daily_detail_batch` (`import_batch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='融资团队每日统计明细';

-- 历史明细表只作为钉钉考勤 Excel 导入的数据源，不再提供独立“考勤明细”页面。
SET @financing_detail_menu_id := (
  SELECT `menu_id`
  FROM `sys_menu`
  WHERE `perms` = 'system:financingDailyDetail:list'
     OR `component` = 'system/financingDailyDetail/index'
  LIMIT 1
);

DELETE rm
FROM `sys_role_menu` rm
INNER JOIN `sys_menu` m ON rm.`menu_id` = m.`menu_id`
WHERE m.`menu_id` = @financing_detail_menu_id
   OR m.`parent_id` = @financing_detail_menu_id
   OR m.`perms` LIKE 'system:financingDailyDetail:%';

DELETE FROM `sys_menu`
WHERE `menu_id` = @financing_detail_menu_id
   OR `parent_id` = @financing_detail_menu_id
   OR `perms` LIKE 'system:financingDailyDetail:%';
