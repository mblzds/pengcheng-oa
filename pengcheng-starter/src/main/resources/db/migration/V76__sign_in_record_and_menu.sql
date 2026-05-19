-- V76: 签到（拍照签到，区别于 V22 之前预留的"扫码签到"）
--
-- 背景：SignInRecord 实体在代码里早已存在（com.pengcheng.hr.attendance.entity.SignInRecord），
--       但表从未通过迁移建出来。这次按"拍照签到"需求建表并补字段：photo_url（必填）+
--       latitude/longitude（GPS 经纬度）+ address（百度逆地理翻译后的中文地址，供后台展示）。
--       同时在「人事管理」一级菜单下新增「签到记录」子菜单。

-- 1. 建表（IF NOT EXISTS 幂等；表名 sign_in_record 与实体 @TableName 一致）
CREATE TABLE IF NOT EXISTS sign_in_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '签到员工 user_id',
    sign_in_time DATETIME NOT NULL COMMENT '签到时刻',
    -- location 历史上是"经纬度字符串"或"位置描述"，新接口里仍按 lat,lng 写入做兜底；
    -- 中文地址走独立列 address，便于后台直接展示而无需重新解析
    location VARCHAR(255) NULL COMMENT '签到原始位置（lat,lng 字符串）',
    address VARCHAR(255) NULL COMMENT '逆地理翻译后的中文地址',
    latitude DECIMAL(10, 7) NULL COMMENT 'GPS 纬度（WGS-84）',
    longitude DECIMAL(10, 7) NULL COMMENT 'GPS 经度（WGS-84）',
    photo_url VARCHAR(500) NULL COMMENT '签到照片访问 URL（拍照签到必填）',
    remark VARCHAR(500) NULL COMMENT '签到备注',
    create_by BIGINT NULL,
    update_by BIGINT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '0-未删除 1-已删除',
    KEY idx_user_time (user_id, sign_in_time),
    KEY idx_sign_in_time (sign_in_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工签到记录表';

-- 2. 兼容历史：如果之前其他迁移已经把表建出来但缺新字段，逐列 ALTER
-- 用动态 SQL + INFORMATION_SCHEMA 判定，避免 MySQL 不支持 IF NOT EXISTS ADD COLUMN
DROP PROCEDURE IF EXISTS pengcheng_v76_add_col;
DELIMITER $$
CREATE PROCEDURE pengcheng_v76_add_col(IN col_name VARCHAR(64), IN col_def TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sign_in_record'
          AND COLUMN_NAME = col_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE sign_in_record ADD COLUMN ', col_name, ' ', col_def);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL pengcheng_v76_add_col('address',   'VARCHAR(255) NULL COMMENT ''逆地理翻译后的中文地址''');
CALL pengcheng_v76_add_col('latitude',  'DECIMAL(10, 7) NULL COMMENT ''GPS 纬度''');
CALL pengcheng_v76_add_col('longitude', 'DECIMAL(10, 7) NULL COMMENT ''GPS 经度''');
CALL pengcheng_v76_add_col('photo_url', 'VARCHAR(500) NULL COMMENT ''签到照片访问 URL''');

DROP PROCEDURE IF EXISTS pengcheng_v76_add_col;

-- 3. 注册「签到记录」菜单（挂在「人事管理」一级目录下，紧邻「考勤设置」）
--    权限码直接复用「考勤打卡」的 realty:attendance:list ——
--    接口侧 @SaCheckPermission 校验此码，菜单侧用同一个码做权限继承。
--    去重判定不能再按 permission（会与考勤打卡碰撞），改按 path/name 判断。
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, create_time, update_time, deleted)
SELECT p.id, '签到记录', 2, '/realty/attendance/sign-in', 'realty/attendance/SignInRecords',
       'realty:attendance:list', 'LocationOutline', 88, 1, 1, 0, NOW(), NOW(), 0
FROM sys_menu p
WHERE p.name = '人事管理' AND p.parent_id = 0 AND p.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu
      WHERE path = '/realty/attendance/sign-in' AND deleted = 0
  );

-- 4. 角色关联：把所有已经挂了「考勤打卡」菜单的角色，自动挂上「签到记录」
--    这样不必维护独立权限码，"能看考勤打卡的就能看签到记录"语义在数据层自动成立。
--    @keep_attendance_menu_id / @signin_menu_id 用变量避免重复子查询
SELECT MIN(id) INTO @attendance_menu_id
FROM sys_menu
WHERE path = '/realty/attendance'
  AND permission = 'realty:attendance:list'
  AND deleted = 0;

SELECT MIN(id) INTO @signin_menu_id
FROM sys_menu
WHERE path = '/realty/attendance/sign-in' AND deleted = 0;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, @signin_menu_id
FROM sys_role_menu rm
WHERE rm.menu_id = @attendance_menu_id
  AND @attendance_menu_id IS NOT NULL
  AND @signin_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm2
      WHERE rm2.role_id = rm.role_id AND rm2.menu_id = @signin_menu_id
  );

-- 5. 兜底：超级管理员 (id=1) + code='admin' 角色，确保至少这两个能看到
--    （如果他们本来就没挂考勤打卡菜单，步骤 4 不会触发，这里补一手）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, @signin_menu_id
FROM (SELECT 1) AS _one
WHERE @signin_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = @signin_menu_id
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, @signin_menu_id
FROM sys_role r
WHERE r.code = 'admin' AND r.deleted = 0
  AND @signin_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = @signin_menu_id
  );
