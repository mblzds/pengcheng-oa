/*
  MasterLife 基础库初始化入口

  说明：
  - MySQL 官方镜像会按文件名顺序执行 /docker-entrypoint-initdb.d 下的脚本
  - 现有 `pengcheng-system.sql` 是完整基础库 Dump，但文件名排序较靠后，导致其它 V* 脚本先执行并失败，从而中断初始化
  - 本脚本通过 `SOURCE` 先导入基础库，再执行后续增量脚本
*/

SET NAMES utf8mb4;

SOURCE /docker-entrypoint-initdb.d/pengcheng-system.sql;

