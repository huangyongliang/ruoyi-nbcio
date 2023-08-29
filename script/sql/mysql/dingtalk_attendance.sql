-- 钉钉考勤同步与报表

CREATE TABLE IF NOT EXISTS `dingtalk_attendance_record` (
  `record_id` bigint NOT NULL COMMENT '本地记录ID',
  `ding_record_id` varchar(64) DEFAULT NULL COMMENT '钉钉打卡记录ID',
  `biz_id` varchar(100) DEFAULT NULL COMMENT '钉钉业务ID',
  `corp_id` varchar(64) DEFAULT NULL COMMENT '钉钉企业ID',
  `ding_user_id` varchar(100) NOT NULL COMMENT '钉钉用户ID',
  `user_name` varchar(100) DEFAULT NULL COMMENT '用户名称',
  `work_date` datetime DEFAULT NULL COMMENT '工作日',
  `check_time` datetime DEFAULT NULL COMMENT '实际打卡时间',
  `base_check_time` datetime DEFAULT NULL COMMENT '排班打卡时间',
  `user_check_time` datetime DEFAULT NULL COMMENT '用户打卡时间',
  `check_type` varchar(32) DEFAULT NULL COMMENT '打卡类型 OnDuty/OffDuty',
  `source_type` varchar(32) DEFAULT NULL COMMENT '打卡来源',
  `daily_detail_id` bigint DEFAULT NULL COMMENT '关联的每日统计明细ID',
  `source_file` varchar(255) DEFAULT NULL COMMENT '来源文件名',
  `import_batch_no` varchar(64) DEFAULT NULL COMMENT '导入批次号',
  `time_result` varchar(32) DEFAULT NULL COMMENT '时间结果',
  `location_result` varchar(32) DEFAULT NULL COMMENT '位置结果',
  `location_method` varchar(32) DEFAULT NULL COMMENT '定位方式',
  `group_id` varchar(64) DEFAULT NULL COMMENT '考勤组ID',
  `plan_id` varchar(64) DEFAULT NULL COMMENT '排班计划ID',
  `proc_inst_id` varchar(100) DEFAULT NULL COMMENT '审批实例ID',
  `approve_id` varchar(100) DEFAULT NULL COMMENT '审批单ID',
  `device_id` varchar(100) DEFAULT NULL COMMENT '设备ID',
  `user_address` varchar(500) DEFAULT NULL COMMENT '打卡地址',
  `user_longitude` decimal(18,8) DEFAULT NULL COMMENT '经度',
  `user_latitude` decimal(18,8) DEFAULT NULL COMMENT '纬度',
  `raw_data` longtext COMMENT '钉钉原始返回',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `uk_dingtalk_attendance_record_id` (`ding_record_id`),
  KEY `idx_dingtalk_attendance_user_time` (`ding_user_id`, `check_time`),
  KEY `idx_dingtalk_attendance_work_date` (`work_date`),
  KEY `idx_dingtalk_attendance_result` (`time_result`),
  KEY `idx_dingtalk_attendance_daily_detail` (`daily_detail_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钉钉考勤打卡记录';

SET @has_daily_detail_id := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dingtalk_attendance_record' AND COLUMN_NAME = 'daily_detail_id'
);
SET @ddl := IF(@has_daily_detail_id = 0,
  'ALTER TABLE `dingtalk_attendance_record` ADD COLUMN `daily_detail_id` bigint DEFAULT NULL COMMENT ''关联的每日统计明细ID'' AFTER `source_type`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_source_file := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dingtalk_attendance_record' AND COLUMN_NAME = 'source_file'
);
SET @ddl := IF(@has_source_file = 0,
  'ALTER TABLE `dingtalk_attendance_record` ADD COLUMN `source_file` varchar(255) DEFAULT NULL COMMENT ''来源文件名'' AFTER `daily_detail_id`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_import_batch_no := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dingtalk_attendance_record' AND COLUMN_NAME = 'import_batch_no'
);
SET @ddl := IF(@has_import_batch_no = 0,
  'ALTER TABLE `dingtalk_attendance_record` ADD COLUMN `import_batch_no` varchar(64) DEFAULT NULL COMMENT ''导入批次号'' AFTER `source_file`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_daily_detail_idx := (
  SELECT COUNT(1) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dingtalk_attendance_record' AND INDEX_NAME = 'idx_dingtalk_attendance_daily_detail'
);
SET @ddl := IF(@has_daily_detail_idx = 0,
  'CREATE INDEX `idx_dingtalk_attendance_daily_detail` ON `dingtalk_attendance_record` (`daily_detail_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @attendance_parent_id := (SELECT `menu_id` FROM `sys_menu` WHERE `menu_id` = 1803305917766526200 OR (`menu_name` = '考勤管理' AND `parent_id` = 0) LIMIT 1);
SET @attendance_parent_id := IFNULL(@attendance_parent_id, 1803305917766526200);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT @attendance_parent_id, '考勤管理', 0, 21, 'financing', NULL, '', 1, 0, 'M', '0', '0', '', 'chart', 'system', NOW(), '', NULL, '考勤历史明细目录'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = @attendance_parent_id);

UPDATE `sys_menu`
SET `menu_name` = '考勤管理',
    `parent_id` = 0,
    `order_num` = 21,
    `path` = 'financing',
    `component` = NULL,
    `query_param` = '',
    `is_frame` = 1,
    `is_cache` = 0,
    `menu_type` = 'M',
    `visible` = '0',
    `status` = '0',
    `perms` = '',
    `icon` = 'chart',
    `remark` = '考勤历史明细目录'
WHERE `menu_id` = @attendance_parent_id;

SET @old_financing_detail_menu_id := (
  SELECT `menu_id`
  FROM `sys_menu`
  WHERE `perms` = 'system:financingDailyDetail:list'
     OR `component` = 'system/financingDailyDetail/index'
  LIMIT 1
);

DELETE rm
FROM `sys_role_menu` rm
INNER JOIN `sys_menu` m ON rm.`menu_id` = m.`menu_id`
WHERE m.`menu_id` = @old_financing_detail_menu_id
   OR m.`parent_id` = @old_financing_detail_menu_id
   OR m.`perms` LIKE 'system:financingDailyDetail:%';

DELETE FROM `sys_menu`
WHERE `menu_id` = @old_financing_detail_menu_id
   OR `parent_id` = @old_financing_detail_menu_id
   OR `perms` LIKE 'system:financingDailyDetail:%';

SET @attendance_menu_id := (SELECT `menu_id` FROM `sys_menu` WHERE `perms` = 'system:dingtalkAttendance:list' LIMIT 1);
SET @attendance_menu_id := IFNULL(@attendance_menu_id, 1803305917766526000);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT @attendance_menu_id, '钉钉考勤', @attendance_parent_id, 1, 'dingtalkAttendance', 'system/dingtalkAttendance/index', '', 1, 0, 'C', '0', '0', 'system:dingtalkAttendance:list', 'chart', 'system', NOW(), '', NULL, '钉钉考勤同步与报表'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = @attendance_menu_id);

UPDATE `sys_menu`
SET `menu_name` = '钉钉考勤',
    `parent_id` = @attendance_parent_id,
    `order_num` = 1,
    `path` = 'dingtalkAttendance',
    `component` = 'system/dingtalkAttendance/index',
    `query_param` = '',
    `is_frame` = 1,
    `is_cache` = 0,
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'system:dingtalkAttendance:list',
    `icon` = 'chart',
    `remark` = '钉钉考勤同步与报表'
WHERE `menu_id` = @attendance_menu_id;

SET @sync_button_id := (SELECT `menu_id` FROM `sys_menu` WHERE `perms` = 'system:dingtalkAttendance:sync' LIMIT 1);
SET @sync_button_id := IFNULL(@sync_button_id, 1803305917766526001);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT @sync_button_id, '同步考勤', @attendance_menu_id, 1, '#', NULL, '', 1, 0, 'F', '0', '0', 'system:dingtalkAttendance:sync', '#', 'system', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = @sync_button_id);

UPDATE `sys_menu`
SET `menu_name` = '同步考勤',
    `parent_id` = @attendance_menu_id,
    `order_num` = 1,
    `menu_type` = 'F',
    `perms` = 'system:dingtalkAttendance:sync'
WHERE `menu_id` = @sync_button_id;

SET @config_button_id := (SELECT `menu_id` FROM `sys_menu` WHERE `perms` = 'system:dingtalkAttendance:config' LIMIT 1);
SET @config_button_id := IFNULL(@config_button_id, 1803305917766526002);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT @config_button_id, '钉钉配置', @attendance_menu_id, 2, '#', NULL, '', 1, 0, 'F', '0', '0', 'system:dingtalkAttendance:config', '#', 'system', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = @config_button_id);

UPDATE `sys_menu`
SET `menu_name` = '钉钉配置',
    `parent_id` = @attendance_menu_id,
    `order_num` = 2,
    `menu_type` = 'F',
    `perms` = 'system:dingtalkAttendance:config'
WHERE `menu_id` = @config_button_id;

SET @import_button_id := (SELECT `menu_id` FROM `sys_menu` WHERE `perms` = 'system:dingtalkAttendance:import' LIMIT 1);
SET @import_button_id := IFNULL(@import_button_id, 1803305917766526003);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT @import_button_id, '导入历史Excel', @attendance_menu_id, 3, '#', NULL, '', 1, 0, 'F', '0', '0', 'system:dingtalkAttendance:import', '#', 'system', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = @import_button_id);

UPDATE `sys_menu`
SET `menu_name` = '导入历史Excel',
    `parent_id` = @attendance_menu_id,
    `order_num` = 3,
    `menu_type` = 'F',
    `perms` = 'system:dingtalkAttendance:import'
WHERE `menu_id` = @import_button_id;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id`
FROM `sys_menu`
WHERE `menu_id` = @attendance_parent_id
   OR `perms` IN (
  'system:dingtalkAttendance:list',
  'system:dingtalkAttendance:sync',
  'system:dingtalkAttendance:config',
  'system:dingtalkAttendance:import'
);

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
SELECT '钉钉考勤同步开关', 'dingtalk.attendance.enabled', 'true', 'N', 'system', NOW(), '钉钉考勤同步配置'
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'dingtalk.attendance.enabled');

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
SELECT '钉钉考勤AppKey', 'dingtalk.attendance.appKey', '', 'N', 'system', NOW(), '钉钉考勤同步配置'
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'dingtalk.attendance.appKey');

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
SELECT '钉钉考勤AppSecret', 'dingtalk.attendance.appSecret', '', 'N', 'system', NOW(), '钉钉考勤同步配置'
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'dingtalk.attendance.appSecret');

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
SELECT '钉钉考勤用户ID', 'dingtalk.attendance.userIds', '', 'N', 'system', NOW(), '钉钉考勤同步配置'
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'dingtalk.attendance.userIds');
