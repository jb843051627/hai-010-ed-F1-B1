-- ed 智慧校园教务管理系统 -- schema (hai-010)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：hai_010

CREATE TABLE IF NOT EXISTS t_ed_adjust_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '调课缓考签核单号',
  node_no int DEFAULT NULL COMMENT '当前所在道 0..2（学生提/院系核/教务处批）',
  sign_mode int DEFAULT NULL COMMENT '同口并印办法 0任一人 1两名点齐',
  need_count int DEFAULT NULL COMMENT '本道应落印数',
  sign_count int DEFAULT NULL COMMENT '本道已落印数',
  status int DEFAULT NULL COMMENT '报批情形 0在核 1已核讫 2已打回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='调课缓考签核单';

CREATE TABLE IF NOT EXISTS t_ed_archive_card (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '学籍档案卡号',
  node_no int DEFAULT NULL COMMENT '本卡版本次序',
  site_id int DEFAULT NULL COMMENT '所属底档',
  site_no varchar(64) DEFAULT NULL COMMENT '所属底档代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本卡年修读学分',
  fine_amt decimal(12,2) DEFAULT NULL COMMENT '本年度新增学分',
  content varchar(255) DEFAULT NULL COMMENT '校验串（随版本次序走）',
  grade_level int DEFAULT NULL COMMENT '学分折到的学业关注层级',
  status int DEFAULT NULL COMMENT '进展 0待核对 1已核对 2已压死',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学籍档案卡';

CREATE TABLE IF NOT EXISTS t_ed_course_task (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '选课催办事项号',
  due_at datetime DEFAULT NULL COMMENT '选课截止那一天最后一刻（定后不许改）',
  amount decimal(12,2) DEFAULT NULL COMMENT '单条提前催办的自然日数（唯一可调的口径）',
  content varchar(255) DEFAULT NULL COMMENT '催办事由与受派辅导员记要',
  status int DEFAULT NULL COMMENT '条目情形 0待派 1已派 2已撤单 3已挂起',
  student_id bigint DEFAULT NULL COMMENT '关联学生底档 t_ed_student.id（浮出前查在籍）',
  assignee varchar(64) DEFAULT NULL COMMENT '受派辅导员工号 t_ed_counselor.counselor_no',
  dispatched_at datetime DEFAULT NULL COMMENT '真派出的时刻（写一次，不许覆盖）',
  window_code varchar(16) DEFAULT NULL COMMENT '派出所在档 DAY白天档 PEAK高峰档',
  round_id varchar(40) DEFAULT NULL COMMENT '派出那一轮的轮次号（档位-时刻）',
  stop_reason varchar(255) DEFAULT NULL COMMENT '挂起或撤单的原由（留痕）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='选课催办事项条';

CREATE TABLE IF NOT EXISTS t_ed_counselor (
  id bigint NOT NULL COMMENT '主键',
  counselor_no varchar(64) DEFAULT NULL COMMENT '辅导员工号',
  counselor_name varchar(64) DEFAULT NULL COMMENT '辅导员姓名',
  group_no varchar(64) DEFAULT NULL COMMENT '所属辅导组',
  status int DEFAULT NULL COMMENT '在岗情形 0在岗 1整组调走',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='辅导员名册';

CREATE TABLE IF NOT EXISTS t_ed_grade_rule (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '成绩评定判定线代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '判定线名',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '优秀档界值（含）',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '良好档界值（含）',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '及格档界值（含）',
  eff_start datetime DEFAULT NULL COMMENT '启用之日',
  eff_end datetime DEFAULT NULL COMMENT '交棒之日(不含)',
  priority int DEFAULT NULL COMMENT '判定线顺位(数值大的先说话)',
  status int DEFAULT NULL COMMENT '判定线情形 0现行 1已让位',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成绩评定判定线';

CREATE TABLE IF NOT EXISTS t_ed_remake_flow (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '补考处置单号',
  stage int DEFAULT NULL COMMENT '当前处置段 0..3（申请/审核/安排/归档封卷）',
  status int DEFAULT NULL COMMENT '单子落定 0未起 1在办 2已封存',
  content varchar(255) DEFAULT NULL COMMENT '单上记事',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次过口动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='补考处置单';

CREATE TABLE IF NOT EXISTS t_ed_score_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '成绩上传批次号',
  row_no int DEFAULT NULL COMMENT '原表行次',
  item_code varchar(64) DEFAULT NULL COMMENT '被校验出的数据项代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行卷面分',
  status int DEFAULT NULL COMMENT '行落地情形 0待核 1已入库 2退回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成绩导入核对行';

CREATE TABLE IF NOT EXISTS t_ed_student (
  id bigint NOT NULL COMMENT '主键',
  site_no varchar(64) DEFAULT NULL COMMENT '学生底档代号',
  site_name varchar(128) DEFAULT NULL COMMENT '底档名称',
  site_type varchar(32) DEFAULT NULL COMMENT '底档类别(本科/专科/研究生)',
  road_name varchar(128) DEFAULT NULL COMMENT '所属行政区划(省—市—区—街道)',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '单档年修读学分一档上限',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '二档上限',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '三档上限',
  status int DEFAULT NULL COMMENT '底档情形 0在籍 1已注销',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生底档';

-- 初始档案数据（id=1 启用 / id=2 停用）
-- 初始档案数据：id=0 在籍、id=1 已注销
-- 年修读学分上限 30/60/120：等于上限算高一档。
INSERT IGNORE INTO t_ed_student (id, site_no, site_name, site_type, road_name, th1_max, th2_max, th3_max, status, del_flag, create_by, create_time)
VALUES (0, 'ST00', '临江市东湖街本科培养组（在籍）', '本科', '临江省—临江市—东湖区—东湖街道', 30.00, 60.00, 120.00, 0, 0, 'seed', NOW()),
       (1, 'ST01', '临江市城南研究生培养组（已注销）', '研究生', '临江省—临江市—城南区—望湖街道', 30.00, 60.00, 120.00, 1, 0, 'seed', NOW());

-- 初始辅导员名册：id=0..2 在岗、id=3 整组调走（派单只在在岗里派）
INSERT IGNORE INTO t_ed_counselor (id, counselor_no, counselor_name, group_no, status, del_flag, create_by, create_time)
VALUES (0, 'CS00', '陈启', 'G1', 0, 0, 'seed', NOW()),
       (1, 'CS01', '林岚', 'G1', 0, 0, 'seed', NOW()),
       (2, 'CS02', '赵蔚', 'G2', 0, 0, 'seed', NOW()),
       (3, 'CS03', '周航', 'G3', 1, 0, 'seed', NOW());

