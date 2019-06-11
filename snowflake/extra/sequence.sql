/*
Navicat MySQL Data Transfer

Target Server Type    : MYSQL
Target Server Version : 100214
File Encoding         : 65001

Date: 2019-02-13 16:11:21
*/

SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `sys_sequence`;
CREATE TABLE `sys_sequence` (
  `id` bigint(10) NOT NULL AUTO_INCREMENT,
  `max_id` bigint(10) NOT NULL,
  `step` int(11) NOT NULL,
  `biz_tag` varchar(32) CHARACTER SET utf8mb4 NOT NULL DEFAULT '' COMMENT '业务参数',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uidx_biz_tag` (`biz_tag`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
