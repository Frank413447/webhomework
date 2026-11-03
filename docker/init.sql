-- 学生成绩管理系统 - 数据库初始化脚本
CREATE DATABASE IF NOT EXISTS company DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE company;

-- 学生表
CREATE TABLE IF NOT EXISTS `student_web_assignment` (
  `sid` varchar(20) NOT NULL COMMENT '学号',
  `sname` varchar(50) NOT NULL COMMENT '姓名',
  `gender` varchar(2) NOT NULL COMMENT '性别',
  `class` varchar(50) NOT NULL COMMENT '班级',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生表';

-- 成绩表
CREATE TABLE IF NOT EXISTS `score_web_assignment` (
  `sid` varchar(20) NOT NULL COMMENT '学号',
  `chinese` double DEFAULT NULL COMMENT '语文成绩',
  `math` double DEFAULT NULL COMMENT '数学成绩',
  `english` double DEFAULT NULL COMMENT '英语成绩',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sid`),
  CONSTRAINT `score_web_assignment_ibfk_1` FOREIGN KEY (`sid`) REFERENCES `student_web_assignment` (`sid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成绩表';

-- 教师表（登录用）
CREATE TABLE IF NOT EXISTS `teacher_web_assignment` (
  `tid` varchar(20) NOT NULL COMMENT '教师账号',
  `tname` varchar(50) NOT NULL COMMENT '教师姓名',
  `password` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`tid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师表';

-- 聊天历史表
CREATE TABLE IF NOT EXISTS `chat_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `type` varchar(50) NOT NULL COMMENT '业务类型',
  `chat_id` varchar(100) NOT NULL COMMENT '会话ID',
  `role` varchar(20) NOT NULL COMMENT '角色',
  `content` text NOT NULL COMMENT '消息内容',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_type_chatid` (`type`,`chat_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聊天历史记录表';

-- 默认管理员账号（密码: admin123）
INSERT INTO `teacher_web_assignment` (`tid`, `tname`, `password`) VALUES
('admin', '管理员', '$2a$10$Iti3POS1tjEXYxEoSWD5LeHZd5WZv.HRLP6XjjbC25J3FN/EteKxm')
ON DUPLICATE KEY UPDATE `tname` = VALUES(`tname`);
