-- =============================================================
-- 校园二手闲置交易平台  campus_secondhand
-- MySQL 8.0 / utf8mb4 / InnoDB
-- 说明：本脚本可重复执行（先 DROP 再 CREATE），包含建表、索引、
--       字段注释与演示种子数据；每张表测试数据均 ≥5 条。
-- 测试账号（BCrypt 加密存储）：
--   admin    / admin123   超级管理员
--   seller01 / 123456     认证卖家
--   seller02 / 123456     认证卖家
--   student01~03 / 123456 普通用户
-- =============================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 建库并切换（可重复执行）
CREATE DATABASE IF NOT EXISTS campus_secondhand
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;
USE campus_secondhand;

DROP TABLE IF EXISTS tb_mail_log;
DROP TABLE IF EXISTS tb_daily_stat;
DROP TABLE IF EXISTS tb_message;
DROP TABLE IF EXISTS tb_audit_record;
DROP TABLE IF EXISTS tb_orders;
DROP TABLE IF EXISTS tb_favorite;
DROP TABLE IF EXISTS tb_comment;
DROP TABLE IF EXISTS tb_item_image;
DROP TABLE IF EXISTS tb_item;
DROP TABLE IF EXISTS tb_category;
DROP TABLE IF EXISTS tb_role_menu;
DROP TABLE IF EXISTS tb_menu;
DROP TABLE IF EXISTS tb_user_role;
DROP TABLE IF EXISTS tb_role;
DROP TABLE IF EXISTS tb_user;

-- -------------------------------------------------------------
-- 1. 用户表
-- -------------------------------------------------------------
CREATE TABLE tb_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username      VARCHAR(30)  NOT NULL COMMENT '登录名',
  password      VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
  nickname      VARCHAR(30)  NOT NULL COMMENT '昵称',
  avatar        VARCHAR(255) DEFAULT NULL COMMENT '头像相对路径',
  phone         VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  email         VARCHAR(60)  NOT NULL COMMENT '邮箱（接收通知）',
  student_no    VARCHAR(30)  DEFAULT NULL COMMENT '学号',
  campus        VARCHAR(30)  DEFAULT NULL COMMENT '校区',
  credit_score  INT          NOT NULL DEFAULT 100 COMMENT '信用分',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
  deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  KEY idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- -------------------------------------------------------------
-- 2. 角色表
-- -------------------------------------------------------------
CREATE TABLE tb_role (
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  code        VARCHAR(30) NOT NULL COMMENT '角色编码 ROLE_XXX',
  name        VARCHAR(30) NOT NULL COMMENT '角色名称',
  remark      VARCHAR(100) DEFAULT NULL COMMENT '说明',
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- -------------------------------------------------------------
-- 3. 用户-角色关联表
-- -------------------------------------------------------------
CREATE TABLE tb_user_role (
  id      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_role (user_id, role_id),
  KEY idx_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- -------------------------------------------------------------
-- 4. 菜单/权限表
-- -------------------------------------------------------------
CREATE TABLE tb_menu (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父级ID，0为顶级',
  name        VARCHAR(30)  NOT NULL COMMENT '菜单名称',
  path        VARCHAR(100) DEFAULT NULL COMMENT '访问路径',
  perms       VARCHAR(60)  DEFAULT NULL COMMENT '权限标识 item:audit',
  icon        VARCHAR(30)  DEFAULT NULL COMMENT '图标名',
  type        TINYINT      NOT NULL DEFAULT 1 COMMENT '0目录 1菜单 2按钮',
  sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
  visible     TINYINT      NOT NULL DEFAULT 1 COMMENT '1显示 0隐藏',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_parent (parent_id, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

-- -------------------------------------------------------------
-- 5. 角色-菜单关联表
-- -------------------------------------------------------------
CREATE TABLE tb_role_menu (
  id      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  menu_id BIGINT NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_menu (role_id, menu_id),
  KEY idx_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- -------------------------------------------------------------
-- 6. 物品分类表
-- -------------------------------------------------------------
CREATE TABLE tb_category (
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  parent_id   BIGINT      NOT NULL DEFAULT 0 COMMENT '父级ID，0为一级分类',
  name        VARCHAR(30) NOT NULL COMMENT '分类名称',
  icon        VARCHAR(30) DEFAULT NULL COMMENT '图标名',
  sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
  status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_parent_sort (parent_id, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品分类表';

-- -------------------------------------------------------------
-- 7. 闲置物品表
-- -------------------------------------------------------------
CREATE TABLE tb_item (
  id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '物品ID',
  seller_id       BIGINT        NOT NULL COMMENT '发布者ID',
  category_id     BIGINT        NOT NULL COMMENT '分类ID',
  title           VARCHAR(60)   NOT NULL COMMENT '标题',
  description     TEXT          COMMENT '详细描述',
  price           DECIMAL(10,2) NOT NULL COMMENT '售价',
  original_price  DECIMAL(10,2) DEFAULT NULL COMMENT '原价（划线价）',
  condition_level TINYINT       NOT NULL DEFAULT 3 COMMENT '成色 1全新 2几乎全新 3轻微使用 4明显使用',
  cover_img       VARCHAR(255)  DEFAULT NULL COMMENT '封面图（第一张图）',
  campus          VARCHAR(30)   DEFAULT NULL COMMENT '交易校区',
  status          TINYINT       NOT NULL DEFAULT 0 COMMENT '0待审核 1在售 2已下架 3已售出 4驳回 5交易中',
  audit_remark    VARCHAR(200)  DEFAULT NULL COMMENT '审核驳回理由',
  view_count      INT           NOT NULL DEFAULT 0 COMMENT '浏览量',
  favorite_count  INT           NOT NULL DEFAULT 0 COMMENT '收藏数',
  deleted         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_status_cat_price (status, category_id, price),
  KEY idx_seller (seller_id, status),
  KEY idx_create (create_time),
  KEY idx_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='闲置物品表';

-- -------------------------------------------------------------
-- 8. 物品图片表（多图）
-- -------------------------------------------------------------
CREATE TABLE tb_item_image (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  item_id     BIGINT       NOT NULL COMMENT '物品ID',
  url         VARCHAR(255) NOT NULL COMMENT '图片相对路径',
  sort        INT          NOT NULL DEFAULT 0 COMMENT '排序，0为封面',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_item_sort (item_id, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品图片表';

-- -------------------------------------------------------------
-- 9. 留言表（支持两级：留言 + 回复）
-- -------------------------------------------------------------
CREATE TABLE tb_comment (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  item_id     BIGINT       NOT NULL COMMENT '物品ID',
  user_id     BIGINT       NOT NULL COMMENT '留言用户ID',
  parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父留言ID，0为一级留言',
  content     VARCHAR(500) NOT NULL COMMENT '留言内容',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_item_parent (item_id, parent_id),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品留言表';

-- -------------------------------------------------------------
-- 10. 收藏表
-- -------------------------------------------------------------
CREATE TABLE tb_favorite (
  id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT   NOT NULL COMMENT '用户ID',
  item_id     BIGINT   NOT NULL COMMENT '物品ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_item (user_id, item_id),
  KEY idx_item (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- -------------------------------------------------------------
-- 11. 订单表
-- 注意：不建 (item_id, buyer_id) 唯一索引，否则取消后无法再次下单
-- -------------------------------------------------------------
CREATE TABLE tb_orders (
  id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_no      VARCHAR(32)   NOT NULL COMMENT '订单号',
  item_id       BIGINT        NOT NULL COMMENT '物品ID',
  buyer_id      BIGINT        NOT NULL COMMENT '买家ID',
  seller_id     BIGINT        NOT NULL COMMENT '卖家ID',
  amount        DECIMAL(10,2) NOT NULL COMMENT '成交金额（快照）',
  status        TINYINT       NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付待交付 2已完成 3已取消(含超时关闭)',
  pay_time      DATETIME      DEFAULT NULL COMMENT '支付时间',
  finish_time   DATETIME      DEFAULT NULL COMMENT '完成时间',
  cancel_reason VARCHAR(100)  DEFAULT NULL COMMENT '取消原因',
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_buyer_status (buyer_id, status),
  KEY idx_seller_status (seller_id, status),
  KEY idx_item (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- -------------------------------------------------------------
-- 12. 物品审核记录表
-- -------------------------------------------------------------
CREATE TABLE tb_audit_record (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  item_id     BIGINT       NOT NULL COMMENT '物品ID',
  admin_id    BIGINT       NOT NULL COMMENT '审核人ID',
  result      TINYINT      NOT NULL COMMENT '1通过 2驳回 3违规下架',
  remark      VARCHAR(200) DEFAULT NULL COMMENT '审核意见',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
  PRIMARY KEY (id),
  KEY idx_item (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品审核记录表';

-- -------------------------------------------------------------
-- 13. 站内消息表（MQ 异步推送落地）
-- -------------------------------------------------------------
CREATE TABLE tb_message (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT       NOT NULL COMMENT '接收人ID',
  title       VARCHAR(60)  NOT NULL COMMENT '标题',
  content     VARCHAR(500) NOT NULL COMMENT '内容',
  biz_type    VARCHAR(30)  DEFAULT NULL COMMENT '业务类型 ORDER/ITEM/SYSTEM',
  biz_id      BIGINT       DEFAULT NULL COMMENT '业务ID',
  is_read     TINYINT      NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表';

-- -------------------------------------------------------------
-- 14. 日统计表（定时任务产出）
-- -------------------------------------------------------------
CREATE TABLE tb_daily_stat (
  id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  stat_date       DATE          NOT NULL COMMENT '统计日期',
  new_user_count  INT           NOT NULL DEFAULT 0 COMMENT '新增用户数',
  new_item_count  INT           NOT NULL DEFAULT 0 COMMENT '新增物品数',
  order_count     INT           NOT NULL DEFAULT 0 COMMENT '订单数',
  trade_amount    DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '成交金额',
  cancel_count    INT           NOT NULL DEFAULT 0 COMMENT '取消订单数',
  create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台日统计表';

-- -------------------------------------------------------------
-- 15. 邮件发送日志表
-- -------------------------------------------------------------
CREATE TABLE tb_mail_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  to_email    VARCHAR(60)  NOT NULL COMMENT '收件人',
  subject     VARCHAR(100) NOT NULL COMMENT '主题',
  biz_type    VARCHAR(30)  DEFAULT NULL COMMENT 'COMMENT/ORDER/AUDIT',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1成功 0失败',
  error_msg   VARCHAR(300) DEFAULT NULL COMMENT '失败原因',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (id),
  KEY idx_create (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件发送日志表';

-- -------------------------------------------------------------
-- 16. 支付流水表（在线支付 + 担保交易资金托管）
-- -------------------------------------------------------------
CREATE TABLE tb_payment (
  id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  pay_no      VARCHAR(32)   NOT NULL COMMENT '平台支付流水号',
  order_id    BIGINT        NOT NULL COMMENT '订单主键',
  order_no    VARCHAR(32)   NOT NULL COMMENT '订单号快照',
  item_id     BIGINT        NOT NULL COMMENT '物品主键',
  item_title  VARCHAR(100)  NOT NULL COMMENT '物品标题快照',
  user_id     BIGINT        NOT NULL COMMENT '付款人',
  amount      DECIMAL(10,2) NOT NULL COMMENT '支付金额',
  channel     VARCHAR(20)   NOT NULL COMMENT '支付渠道 ALIPAY/WECHAT/CAMPUS_CARD（演示环境为模拟网关）',
  status      TINYINT       NOT NULL DEFAULT 0 COMMENT '0待支付 1支付成功(托管中) 2支付失败 3已关闭 4已结算给卖家',
  trade_no    VARCHAR(64)   DEFAULT NULL COMMENT '模拟网关交易号',
  notify_time DATETIME      DEFAULT NULL COMMENT '网关回调时间',
  settle_time DATETIME      DEFAULT NULL COMMENT '托管款结算给卖家的时间（买家确认收货或系统自动完成）',
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_pay_no (pay_no),
  KEY idx_order (order_id),
  UNIQUE KEY uk_order (order_id),
  KEY idx_status (status),
  KEY idx_user_status (user_id, status),
  KEY idx_create (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';

-- =============================================================
-- 种子数据
-- =============================================================

-- 角色
INSERT INTO tb_role (id, code, name, remark) VALUES
(1, 'ROLE_USER',   '普通用户',   '浏览、收藏、留言、下单'),
(2, 'ROLE_SELLER', '认证卖家',   '在普通用户基础上可发布物品'),
(3, 'ROLE_ADMIN',  '超级管理员', '平台全部管理权限');

-- 用户（password：admin=admin123，其余=123456，BCrypt cost=10）
INSERT INTO tb_user (id, username, password, nickname, avatar, phone, email, student_no, campus, credit_score, status) VALUES
(1, 'admin',     '$2a$10$2P5EYxIbdx4BJdd/m7m0K.uv2fq25WOPXZc9Y3tHbTTRY7GTIV.4m', '平台管理员', '/upload/demo/avatar-01.jpg', '13800000001', 'admin@campus.edu.cn',   NULL,          '主校区', 100, 1),
(2, 'seller01',  '$2a$10$PKOL8u3DRV9Xc9Op3Is94uq6RWbvUpvxXfmRenKmt/VlorW1AQ4HC', '学长二手铺', '/upload/demo/avatar-02.jpg', '13800000002', 'seller01@campus.edu.cn', '2021001',    '东校区', 100, 1),
(3, 'seller02',  '$2a$10$PKOL8u3DRV9Xc9Op3Is94uq6RWbvUpvxXfmRenKmt/VlorW1AQ4HC', '数码小站',   '/upload/demo/avatar-03.jpg', '13800000003', 'seller02@campus.edu.cn', '2021002',    '西校区', 100, 1),
(4, 'student01', '$2a$10$PKOL8u3DRV9Xc9Op3Is94uq6RWbvUpvxXfmRenKmt/VlorW1AQ4HC', '小林同学',   '/upload/demo/avatar-04.jpg', '13800000004', 'student01@campus.edu.cn', '2023001',    '东校区', 100, 1),
(5, 'student02', '$2a$10$PKOL8u3DRV9Xc9Op3Is94uq6RWbvUpvxXfmRenKmt/VlorW1AQ4HC', '阿泽',       '/upload/demo/avatar-05.jpg', '13800000005', 'student02@campus.edu.cn', '2023002',    '南校区', 98,  1),
(6, 'student03', '$2a$10$PKOL8u3DRV9Xc9Op3Is94uq6RWbvUpvxXfmRenKmt/VlorW1AQ4HC', '小周',       '/upload/demo/avatar-06.jpg', '13800000006', 'student03@campus.edu.cn', '2023003',    '北校区', 100, 1);

-- 用户角色
INSERT INTO tb_user_role (user_id, role_id) VALUES
(1, 3),
(2, 1), (2, 2),
(3, 1), (3, 2),
(4, 1),
(5, 1),
(6, 1);

-- 菜单
INSERT INTO tb_menu (id, parent_id, name, path, perms, icon, type, sort, visible) VALUES
(1,  0,  '物品管理', NULL,                    NULL,           'IconFolder',  0, 1, 1),
(11, 1,  '物品审核', '/admin/items',           'item:audit',   'IconSuccess', 1, 1, 1),
(12, 1,  '分类管理', '/admin/categories',     'category:edit','IconFilter',  1, 2, 1),
(13, 1,  '我的物品', '/my-items',             'item:publish', 'IconText',    1, 3, 1),
(14, 1,  '发布物品', '/items/publish',        'item:publish', 'IconPlus',    1, 4, 1),
(2,  0,  '订单管理', NULL,                    NULL,           'IconDocument',0, 2, 1),
(21, 2,  '全部订单', '/admin/orders',         'order:manage', 'IconSort',    1, 1, 1),
(23, 2,  '支付流水', '/admin/payments',       'payment:list', 'IconSuccess', 1, 2, 1),
(22, 2,  '我的订单', '/orders/mine',          'order:mine',   'IconTime',    1, 3, 1),
(3,  0,  '用户管理', '/admin/users',          'user:list',    'IconUser',    1, 3, 1),
(4,  0,  '留言管理', '/admin/comments',       'comment:manage','IconMessageCircle', 1, 4, 1),
(5,  0,  '数据看板', '/admin/dashboard',      'stat:view',    'IconPicture', 1, 5, 1),
(6,  0,  '系统管理', NULL,                    NULL,           'IconSetting', 0, 6, 1),
(61, 6,  '角色管理', '/admin/roles',          'role:manage',  'IconUser',    1, 1, 1),
(62, 6,  '菜单管理', '/admin/menus',          'menu:manage',  'IconSetting', 1, 2, 1);

-- 角色菜单（管理员全部；卖家：物品管理/发布/我的物品 + 订单；用户：我的订单）
INSERT INTO tb_role_menu (role_id, menu_id) VALUES
(3,1),(3,11),(3,12),(3,13),(3,14),(3,2),(3,21),(3,22),(3,23),(3,3),(3,4),(3,5),(3,6),(3,61),(3,62),
(2,1),(2,13),(2,14),(2,2),(2,22),
(1,2),(1,22);

-- 分类
INSERT INTO tb_category (id, parent_id, name, icon, sort, status) VALUES
(1, 0, '教材书籍', '📚', 1, 1),
(2, 0, '数码电子', '💻', 2, 1),
(3, 0, '生活用品', '🏠', 3, 1),
(4, 0, '运动器材', '🏀', 4, 1),
(5, 0, '服饰鞋包', '👕', 5, 1),
(6, 0, '代步工具', '🛴', 6, 1);

-- 物品（状态分布：在售 8 / 待审核 4 / 已下架 2 / 已售出 4 / 交易中 2 / 驳回 1）
INSERT INTO tb_item (id, seller_id, category_id, title, description, price, original_price, condition_level, cover_img, campus, status, audit_remark, view_count, favorite_count, create_time) VALUES
(1,  2, 1, '《高等数学》上下册 第七版 同济大学', '大一用过一学期，无笔记无划线，书角轻微磨损，配套习题册一起送。', 25.00, 79.00, 3, '/upload/demo/item-07.jpg', '东校区', 3, NULL, 320, 12, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(2,  3, 2, 'Kindle Paperwhite 4 8G 黑色', '去年买的，一直贴膜带壳，屏幕无划痕，电池健康，送原装保护套。', 380.00, 998.00, 2, '/upload/demo/item-02.jpg', '西校区', 3, NULL, 512, 28, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(3,  2, 3, '宿舍小台灯 护眼三档调光', '毕业清仓，USB 供电，三档亮度，灯光偏暖，适合书桌。', 18.00, 49.00, 3, '/upload/demo/item-09.jpg', '东校区', 3, NULL, 96, 4, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(4,  3, 4, '李宁羽毛球拍 双拍套装', '打了三四次，拍线完好，送 3 个球和拍套。', 65.00, 199.00, 2, '/upload/demo/item-04.jpg', '西校区', 5, NULL, 143, 9, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(5,  2, 1, '《计算机网络》第 7 版 谢希仁', '考前复习用过，重点章节有少量铅笔标注，可擦除。', 20.00, 45.00, 3, '/upload/demo/item-01.jpg', '东校区', 1, NULL, 210, 6, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(6,  3, 2, '罗技 M330 无线静音鼠标', '宿舍用了一年，功能正常，静音效果好，接收器齐全。', 45.00, 129.00, 3, '/upload/demo/item-08.jpg', '西校区', 1, NULL, 178, 7, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(7,  2, 5, '优衣库 男士连帽卫衣 L 码 灰色', '买大了没怎么穿，洗过一次，无起球无污渍。', 55.00, 199.00, 2, '/upload/demo/item-05.jpg', '东校区', 3, NULL, 88, 3, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(8,  3, 2, '小米移动电源 3 10000mAh', '备用电源，循环次数少，接口正常，送数据线。', 39.00, 79.00, 3, '/upload/demo/item-02.jpg', '西校区', 5, NULL, 132, 5, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(9,  2, 1, '考研英语一 真题解析 2010-2024', '只做了前 5 套，后面全新，答案册完整。', 30.00, 68.00, 3, '/upload/demo/item-01.jpg', '东校区', 1, NULL, 401, 21, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(10, 3, 3, '宿舍折叠衣架 落地式', '承重好，可折叠收纳，搬宿舍神器，九成新。', 22.00, 59.00, 2, '/upload/demo/item-03.jpg', '西校区', 1, NULL, 67, 2, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(11, 2, 4, '瑜伽垫 加厚防滑 183x61cm', '买来练了两周，已清洁，送收纳绑带。', 28.00, 89.00, 3, '/upload/demo/item-04.jpg', '东校区', 1, NULL, 74, 3, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(12, 3, 6, '捷安特 山地车 24 速', '骑了一年半，链条刚上油，刹车灵敏，毕业出。', 480.00, 1680.00, 3, '/upload/demo/item-06.jpg', '西校区', 1, NULL, 623, 34, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(13, 2, 5, '帆布鞋 男女同款 40 码 白色', '穿过两次，鞋底干净，尺码偏大建议 41。', 35.00, 129.00, 2, '/upload/demo/item-11.jpg', '东校区', 1, NULL, 52, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(14, 3, 3, '迷你电煮锅 1.5L', '宿舍神器，可煮面煮粥，带自动断电，功能正常。', 42.00, 119.00, 3, '/upload/demo/item-09.jpg', '西校区', 1, NULL, 118, 4, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(15, 2, 1, '《Java 核心技术 卷 I》第 11 版', '课程参考书，翻阅痕迹少，无破损。', 45.00, 149.00, 2, '/upload/demo/item-07.jpg', '东校区', 0, NULL, 12, 0, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(16, 3, 2, 'iPad 第 9 代 64G WiFi 深空灰', '网课用，屏幕完好，电池 92%，含保护壳，无划痕。', 1350.00, 2499.00, 2, '/upload/demo/item-08.jpg', '西校区', 0, NULL, 8, 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(17, 2, 4, '迪卡侬 哑铃 5kg 一对', '自用半年，包胶完好，无锈，自提。', 75.00, 159.00, 3, '/upload/demo/item-10.jpg', '东校区', 0, NULL, 5, 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(18, 3, 5, '羽绒服 女款 M 码 米白', '只穿过一季，干洗过，充绒均匀，无钻绒。', 160.00, 599.00, 2, '/upload/demo/item-05.jpg', '西校区', 0, NULL, 3, 0, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(19, 2, 3, '收纳箱 3 个装 可折叠', '搬宿舍用剩的，无破损，可叠放。', 15.00, 45.00, 2, '/upload/demo/item-03.jpg', '东校区', 2, NULL, 41, 1, DATE_SUB(NOW(), INTERVAL 15 DAY)),
(20, 3, 6, '滑板 双翘 7 层枫木', '练了两次就吃灰，砂纸完整，轮子顺滑。', 90.00, 260.00, 3, '/upload/demo/item-12.jpg', '西校区', 4, '图片模糊，请补充清晰实拍图后重新提交', 39, 2, DATE_SUB(NOW(), INTERVAL 40 DAY));

-- 物品图片（封面 + 细节图，图片编号与物品分类保持一致：教材 01/07、数码 02/08、生活 03/09、运动 04/10、服饰 05/11、代步 06/12）
INSERT INTO tb_item_image (item_id, url, sort) VALUES
(1,'/upload/demo/item-07.jpg',0),(1,'/upload/demo/item-01.jpg',1),
(2,'/upload/demo/item-02.jpg',0),(2,'/upload/demo/item-08.jpg',1),
(3,'/upload/demo/item-09.jpg',0),(3,'/upload/demo/item-03.jpg',1),
(4,'/upload/demo/item-04.jpg',0),(4,'/upload/demo/item-10.jpg',1),
(5,'/upload/demo/item-01.jpg',0),(5,'/upload/demo/item-07.jpg',1),
(6,'/upload/demo/item-08.jpg',0),(6,'/upload/demo/item-02.jpg',1),
(7,'/upload/demo/item-05.jpg',0),(7,'/upload/demo/item-11.jpg',1),
(8,'/upload/demo/item-02.jpg',0),(8,'/upload/demo/item-08.jpg',1),
(9,'/upload/demo/item-01.jpg',0),(9,'/upload/demo/item-07.jpg',1),
(10,'/upload/demo/item-03.jpg',0),(10,'/upload/demo/item-09.jpg',1),
(11,'/upload/demo/item-04.jpg',0),(11,'/upload/demo/item-10.jpg',1),
(12,'/upload/demo/item-06.jpg',0),(12,'/upload/demo/item-12.jpg',1),
(13,'/upload/demo/item-11.jpg',0),(13,'/upload/demo/item-05.jpg',1),
(14,'/upload/demo/item-09.jpg',0),(14,'/upload/demo/item-03.jpg',1),
(15,'/upload/demo/item-07.jpg',0),(15,'/upload/demo/item-01.jpg',1),
(16,'/upload/demo/item-08.jpg',0),(16,'/upload/demo/item-02.jpg',1),
(17,'/upload/demo/item-10.jpg',0),(17,'/upload/demo/item-04.jpg',1),
(18,'/upload/demo/item-05.jpg',0),(18,'/upload/demo/item-11.jpg',1),
(19,'/upload/demo/item-03.jpg',0),(19,'/upload/demo/item-09.jpg',1),
(20,'/upload/demo/item-12.jpg',0),(20,'/upload/demo/item-06.jpg',1);

-- 留言（含 3 组「留言-回复」）
INSERT INTO tb_comment (id, item_id, user_id, parent_id, content, create_time) VALUES
(1, 5, 4, 0, '这本书还有吗？可以周末在东校区图书馆门口交易吗？', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2, 5, 2, 1, '在的，周六下午两点图书馆门口可以，我带上书。', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(3, 9, 5, 0, '真题里面有答案解析吗？想确认一下。', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(4, 9, 2, 3, '有的，解析单独一本，完整无缺页。', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(5, 12, 6, 0, '自行车能便宜点吗？400 可以的话我今天就来提。', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(6, 12, 3, 5, '可以，400 成交，你到西门车棚找我。', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(7, 6, 4, 0, '鼠标的接收器还在吗？', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(8, 6, 3, 7, '在的，接收器放在电池仓里，一起给你。', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(9, 11, 5, 0, '瑜伽垫厚度多少？', DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(10, 14, 6, 0, '电煮锅会不会跳闸？宿舍限电。', DATE_SUB(NOW(), INTERVAL 10 HOUR));

-- 收藏
INSERT INTO tb_favorite (user_id, item_id, create_time) VALUES
(4, 2, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(4, 5, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(5, 9, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(5, 12, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(6, 6, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(6, 11, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 订单（覆盖 待支付/已支付/已完成/已取消）
INSERT INTO tb_orders (id, order_no, item_id, buyer_id, seller_id, amount, status, pay_time, finish_time, cancel_reason, create_time) VALUES
(1, 'SC202610280930120001', 1, 4, 2, 25.00, 2, DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY), NULL, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(2, 'SC202610291410350002', 2, 5, 3, 380.00, 2, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY), NULL, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(3, 'SC202611021015440003', 3, 6, 2, 18.00, 1, DATE_SUB(NOW(), INTERVAL 9 DAY), NULL, NULL, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(4, 'SC202611031920060004', 4, 4, 3, 65.00, 0, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 MINUTE)),
(5, 'SC202611041105270005', 5, 5, 2, 20.00, 3, NULL, NULL, '买家取消', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(6, 'SC202611051630410006', 6, 6, 3, 45.00, 3, NULL, NULL, '超时未支付', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(7, 'SC202611060845330007', 7, 4, 2, 55.00, 2, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), NULL, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(8, 'SC202611070932180008', 8, 5, 3, 39.00, 0, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 8 MINUTE));

-- 支付流水（从既有订单派生：已支付/已完成的订单都有对应流水，保证对账数据完整）
INSERT INTO tb_payment (pay_no, order_id, order_no, item_id, item_title, user_id, amount, channel, status, trade_no, notify_time, settle_time, create_time)
SELECT CONCAT('PAY', DATE_FORMAT(o.create_time, '%Y%m%d%H%i%s'), LPAD(o.id, 4, '0')),
       o.id, o.order_no, o.item_id, i.title, o.buyer_id, o.amount,
       ELT((o.id % 3) + 1, 'ALIPAY', 'WECHAT', 'CAMPUS_CARD'),
       CASE WHEN o.status = 2 THEN 4 WHEN o.status = 1 THEN 1 WHEN o.status = 0 THEN 0 ELSE 3 END,
       CASE WHEN o.status IN (1, 2) THEN CONCAT('MOCK', o.id, DATE_FORMAT(o.create_time, '%Y%m%d%H%i%s')) ELSE NULL END,
       CASE WHEN o.status IN (1, 2) THEN o.pay_time ELSE NULL END,
       CASE WHEN o.status = 2 THEN o.finish_time ELSE NULL END,
       o.create_time
FROM tb_orders o
JOIN tb_item i ON i.id = o.item_id;

-- 审核记录
INSERT INTO tb_audit_record (item_id, admin_id, result, remark, create_time) VALUES
(1, 1, 1, '信息完整，图片清晰', DATE_SUB(NOW(), INTERVAL 12 DAY)),
(2, 1, 1, '价格合理，通过', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(3, 1, 1, '通过', DATE_SUB(NOW(), INTERVAL 9 DAY)),
(19, 1, 3, '疑似重复发布且描述与实际不符，下架处理', DATE_SUB(NOW(), INTERVAL 14 DAY)),
(20, 1, 2, '图片模糊，请补充清晰实拍图后重新提交', DATE_SUB(NOW(), INTERVAL 38 DAY));

-- 站内消息
INSERT INTO tb_message (user_id, title, content, biz_type, biz_id, is_read, create_time) VALUES
(2, '物品已售出', '你的物品《高等数学》上下册 第七版 同济大学 已被买家拍下。', 'ORDER', 1, 1, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(3, '物品已售出', '你的物品 Kindle Paperwhite 4 8G 黑色 已被买家拍下。', 'ORDER', 2, 1, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(2, '收到新订单', '买家已下单《宿舍小台灯 护眼三档调光》，请及时沟通交易。', 'ORDER', 3, 0, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(3, '收到新留言', '有用户给你的物品《罗技 M330 无线静音鼠标》留言。', 'ITEM', 6, 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, '物品审核通过', '你发布的《考研英语一 真题解析 2010-2024》已通过审核，正在展示中。', 'ITEM', 9, 1, DATE_SUB(NOW(), INTERVAL 5 DAY));

-- 日统计（近 7 天）
INSERT INTO tb_daily_stat (stat_date, new_user_count, new_item_count, order_count, trade_amount, cancel_count) VALUES
(DATE_SUB(CURDATE(), INTERVAL 6 DAY), 2, 5, 3, 268.00, 0),
(DATE_SUB(CURDATE(), INTERVAL 5 DAY), 1, 6, 4, 512.00, 1),
(DATE_SUB(CURDATE(), INTERVAL 4 DAY), 3, 4, 2, 143.00, 0),
(DATE_SUB(CURDATE(), INTERVAL 3 DAY), 2, 7, 5, 896.00, 1),
(DATE_SUB(CURDATE(), INTERVAL 2 DAY), 4, 6, 3, 377.00, 0),
(DATE_SUB(CURDATE(), INTERVAL 1 DAY), 3, 5, 6, 1024.00, 2),
(CURDATE(),                          1, 3, 2, 210.00, 0);

-- 邮件日志
INSERT INTO tb_mail_log (to_email, subject, biz_type, status, error_msg, create_time) VALUES
('seller02@campus.edu.cn', '【校园二手】你的物品收到新留言', 'COMMENT', 1, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
('seller01@campus.edu.cn', '【校园二手】有买家下单了你的物品', 'ORDER',   1, NULL, DATE_SUB(NOW(), INTERVAL 9 DAY)),
('seller01@campus.edu.cn', '【校园二手】物品审核通过通知',     'AUDIT',   1, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
('seller02@campus.edu.cn', '【校园二手】物品审核驳回通知',     'AUDIT',   0, 'Mail server connection timeout', DATE_SUB(NOW(), INTERVAL 38 DAY)),
('admin@campus.edu.cn',    '【校园二手】平台日报',             'SYSTEM',  1, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY));

SET FOREIGN_KEY_CHECKS = 1;

-- 校验：各表数据量
-- SELECT 'tb_user', COUNT(*) FROM tb_user UNION ALL
-- SELECT 'tb_category', COUNT(*) FROM tb_category UNION ALL
-- SELECT 'tb_item', COUNT(*) FROM tb_item UNION ALL
-- SELECT 'tb_orders', COUNT(*) FROM tb_orders UNION ALL
-- SELECT 'tb_comment', COUNT(*) FROM tb_comment UNION ALL
-- SELECT 'tb_menu', COUNT(*) FROM tb_menu;