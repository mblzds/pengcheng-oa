-- 节假日 / 调休补班日历，给考勤月报算"应当出勤的工作日"用。
-- 默认规则：周一~五 = 工作日，周六/日 = 休息日；本表两类条目改写默认：
--   type=1 法定节假日：本来是工作日的某天改成休息（如劳动节 5/1 周五）
--   type=2 调休补班：本来是周末的某天改成工作（如国庆调休的 9/28 周日补班）
--
-- seed 仅落 2026 年固定日期的法定节日；调休补班 + 春节/端午/中秋等阴历节日由
-- 管理员每年自助维护，避免代码里写死随时调整的 State Council 公布日期。

CREATE TABLE IF NOT EXISTS `holiday_calendar` (
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `holiday_date` DATE        NOT NULL COMMENT '日期',
    `type`        TINYINT     NOT NULL COMMENT '1 法定节假日（休）/ 2 调休补班（上班）',
    `label`       VARCHAR(64) NOT NULL COMMENT '中文显示名，如 元旦 / 国庆调休补班',
    `note`        VARCHAR(255) NULL,
    `create_time` DATETIME    NULL,
    `update_time` DATETIME    NULL,
    `create_by`   BIGINT      NULL,
    `update_by`   BIGINT      NULL,
    `deleted`     TINYINT     NOT NULL DEFAULT 0,
    UNIQUE KEY `uk_date` (`holiday_date`, `deleted`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '节假日 / 调休补班日历';

-- 2026 已知固定日期的法定节假日（不含阴历 + 调休补班，那些每年公布后由运营追加）
INSERT INTO `holiday_calendar` (holiday_date, type, label, create_time, update_time, deleted)
SELECT '2026-01-01', 1, '元旦',     NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM holiday_calendar WHERE holiday_date = '2026-01-01' AND deleted = 0);

INSERT INTO `holiday_calendar` (holiday_date, type, label, create_time, update_time, deleted)
SELECT '2026-04-04', 1, '清明节',   NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM holiday_calendar WHERE holiday_date = '2026-04-04' AND deleted = 0);
INSERT INTO `holiday_calendar` (holiday_date, type, label, create_time, update_time, deleted)
SELECT '2026-04-05', 1, '清明节',   NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM holiday_calendar WHERE holiday_date = '2026-04-05' AND deleted = 0);
INSERT INTO `holiday_calendar` (holiday_date, type, label, create_time, update_time, deleted)
SELECT '2026-04-06', 1, '清明节',   NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM holiday_calendar WHERE holiday_date = '2026-04-06' AND deleted = 0);

INSERT INTO `holiday_calendar` (holiday_date, type, label, create_time, update_time, deleted)
SELECT '2026-05-01', 1, '劳动节',   NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM holiday_calendar WHERE holiday_date = '2026-05-01' AND deleted = 0);
INSERT INTO `holiday_calendar` (holiday_date, type, label, create_time, update_time, deleted)
SELECT '2026-05-02', 1, '劳动节',   NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM holiday_calendar WHERE holiday_date = '2026-05-02' AND deleted = 0);
INSERT INTO `holiday_calendar` (holiday_date, type, label, create_time, update_time, deleted)
SELECT '2026-05-03', 1, '劳动节',   NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM holiday_calendar WHERE holiday_date = '2026-05-03' AND deleted = 0);

INSERT INTO `holiday_calendar` (holiday_date, type, label, create_time, update_time, deleted)
SELECT '2026-10-01', 1, '国庆节',   NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM holiday_calendar WHERE holiday_date = '2026-10-01' AND deleted = 0);
INSERT INTO `holiday_calendar` (holiday_date, type, label, create_time, update_time, deleted)
SELECT '2026-10-02', 1, '国庆节',   NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM holiday_calendar WHERE holiday_date = '2026-10-02' AND deleted = 0);
INSERT INTO `holiday_calendar` (holiday_date, type, label, create_time, update_time, deleted)
SELECT '2026-10-03', 1, '国庆节',   NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM holiday_calendar WHERE holiday_date = '2026-10-03' AND deleted = 0);
INSERT INTO `holiday_calendar` (holiday_date, type, label, create_time, update_time, deleted)
SELECT '2026-10-04', 1, '国庆节',   NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM holiday_calendar WHERE holiday_date = '2026-10-04' AND deleted = 0);
INSERT INTO `holiday_calendar` (holiday_date, type, label, create_time, update_time, deleted)
SELECT '2026-10-05', 1, '国庆节',   NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM holiday_calendar WHERE holiday_date = '2026-10-05' AND deleted = 0);
INSERT INTO `holiday_calendar` (holiday_date, type, label, create_time, update_time, deleted)
SELECT '2026-10-06', 1, '国庆节',   NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM holiday_calendar WHERE holiday_date = '2026-10-06' AND deleted = 0);
INSERT INTO `holiday_calendar` (holiday_date, type, label, create_time, update_time, deleted)
SELECT '2026-10-07', 1, '国庆节',   NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM holiday_calendar WHERE holiday_date = '2026-10-07' AND deleted = 0);
