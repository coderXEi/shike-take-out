USE shike_take_out;

-- ----------------------------
-- 1. 员工表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `employee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) NOT NULL COMMENT '姓名',
  `username` varchar(32) NOT NULL COMMENT '用户名',
  `password` varchar(64) NOT NULL COMMENT '密码',
  `phone` varchar(11) NOT NULL COMMENT '手机号',
  `sex` varchar(2) NOT NULL COMMENT '性别',
  `id_number` varchar(18) NOT NULL COMMENT '身份证号',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态 0:禁用 1:启用',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint NOT NULL COMMENT '创建人',
  `update_user` bigint NOT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工表';

-- ----------------------------
-- 2. 分类表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` int NOT NULL COMMENT '类型 1:菜品分类 2:套餐分类',
  `name` varchar(32) NOT NULL COMMENT '分类名称',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` int DEFAULT 1 COMMENT '状态 0:禁用 1:启用',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint NOT NULL COMMENT '创建人',
  `update_user` bigint NOT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';

-- ----------------------------
-- 3. 菜品表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `dish` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) NOT NULL COMMENT '菜品名称',
  `category_id` bigint NOT NULL COMMENT '菜品分类id',
  `price` decimal(10,2) NOT NULL COMMENT '菜品价格',
  `image` varchar(255) DEFAULT NULL COMMENT '图片',
  `description` varchar(255) DEFAULT NULL COMMENT '描述信息',
  `status` int DEFAULT 1 COMMENT '0 停售 1 起售',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint NOT NULL COMMENT '创建人',
  `update_user` bigint NOT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品表';

-- ----------------------------
-- 4. 菜品口味表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `dish_flavor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dish_id` bigint NOT NULL COMMENT '菜品id',
  `name` varchar(32) NOT NULL COMMENT '口味名称',
  `value` varchar(255) DEFAULT NULL COMMENT '口味数据list',
  PRIMARY KEY (`id`),
  KEY `idx_dish_id` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品口味表';

-- ----------------------------
-- 5. 套餐表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `setmeal` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id` bigint NOT NULL COMMENT '分类id',
  `name` varchar(32) NOT NULL COMMENT '套餐名称',
  `price` decimal(10,2) NOT NULL COMMENT '套餐价格',
  `status` int DEFAULT 1 COMMENT '状态 0:停用 1:启用',
  `description` varchar(255) DEFAULT NULL COMMENT '描述信息',
  `image` varchar(255) DEFAULT NULL COMMENT '图片',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint NOT NULL COMMENT '创建人',
  `update_user` bigint NOT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套餐表';

-- ----------------------------
-- 6. 套餐菜品关系表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `setmeal_dish` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `setmeal_id` bigint NOT NULL COMMENT '套餐id',
  `dish_id` bigint NOT NULL COMMENT '菜品id',
  `name` varchar(32) NOT NULL COMMENT '菜品名称（冗余字段）',
  `price` decimal(10,2) NOT NULL COMMENT '菜品原价',
  `copies` int NOT NULL COMMENT '份数',
  PRIMARY KEY (`id`),
  KEY `idx_setmeal_id` (`setmeal_id`),
  KEY `idx_dish_id` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套餐菜品关系表';

-- ----------------------------
-- 7. 用户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `openid` varchar(64) DEFAULT NULL COMMENT '微信用户唯一标识',
  `name` varchar(64) DEFAULT NULL COMMENT '姓名',
  `phone` varchar(11) DEFAULT NULL COMMENT '手机号',
  `sex` varchar(2) DEFAULT NULL COMMENT '性别 0:女 1:男',
  `id_number` varchar(18) DEFAULT NULL COMMENT '身份证号',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `create_time` datetime NOT NULL COMMENT '注册时间',
  PRIMARY KEY (`id`),
  KEY `idx_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- 8. 地址簿表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `address_book` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `consignee` varchar(32) NOT NULL COMMENT '收货人',
  `phone` varchar(11) NOT NULL COMMENT '手机号',
  `sex` varchar(2) NOT NULL COMMENT '性别 0:女 1:男',
  `province_code` varchar(12) DEFAULT NULL COMMENT '省级区划编号',
  `province_name` varchar(32) DEFAULT NULL COMMENT '省级名称',
  `city_code` varchar(12) DEFAULT NULL COMMENT '市级区划编号',
  `city_name` varchar(32) DEFAULT NULL COMMENT '市级名称',
  `district_code` varchar(12) DEFAULT NULL COMMENT '区级区划编号',
  `district_name` varchar(32) DEFAULT NULL COMMENT '区级名称',
  `detail` varchar(255) DEFAULT NULL COMMENT '详细地址',
  `label` varchar(32) DEFAULT NULL COMMENT '标签',
  `is_default` int DEFAULT 0 COMMENT '是否默认 0:否 1:是',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地址簿表';

-- ----------------------------
-- 9. 购物车表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `shopping_cart` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) DEFAULT NULL COMMENT '名称',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `dish_id` bigint DEFAULT NULL COMMENT '菜品id',
  `setmeal_id` bigint DEFAULT NULL COMMENT '套餐id',
  `dish_flavor` varchar(255) DEFAULT NULL COMMENT '口味',
  `number` int NOT NULL DEFAULT 1 COMMENT '数量',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `image` varchar(255) DEFAULT NULL COMMENT '图片',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';

-- ----------------------------
-- 10. 订单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `number` varchar(50) NOT NULL COMMENT '订单号',
  `status` int NOT NULL DEFAULT 1 COMMENT '订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消',
  `user_id` bigint NOT NULL COMMENT '下单用户id',
  `address_book_id` bigint NOT NULL COMMENT '地址id',
  `order_time` datetime NOT NULL COMMENT '下单时间',
  `checkout_time` datetime DEFAULT NULL COMMENT '结账时间',
  `pay_method` int DEFAULT NULL COMMENT '支付方式 1微信 2支付宝',
  `pay_status` int NOT NULL DEFAULT 0 COMMENT '支付状态 0未支付 1已支付 2退款',
  `amount` decimal(10,2) NOT NULL COMMENT '实收金额',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `user_name` varchar(32) DEFAULT NULL COMMENT '用户名',
  `phone` varchar(11) DEFAULT NULL COMMENT '手机号',
  `address` varchar(255) DEFAULT NULL COMMENT '地址',
  `consignee` varchar(32) DEFAULT NULL COMMENT '收货人',
  `cancel_reason` varchar(255) DEFAULT NULL COMMENT '订单取消原因',
  `rejection_reason` varchar(255) DEFAULT NULL COMMENT '订单拒绝原因',
  `cancel_time` datetime DEFAULT NULL COMMENT '订单取消时间',
  `estimated_delivery_time` datetime DEFAULT NULL COMMENT '预计送达时间',
  `delivery_status` int DEFAULT NULL COMMENT '配送状态 1立即送出 0选择具体时间',
  `delivery_time` datetime DEFAULT NULL COMMENT '送达时间',
  `pack_amount` int NOT NULL DEFAULT 0 COMMENT '打包费',
  `tableware_number` int NOT NULL DEFAULT 0 COMMENT '餐具数量',
  `tableware_status` int DEFAULT NULL COMMENT '餐具数量状态 1按餐量提供 0选择具体数量',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_number` (`number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- ----------------------------
-- 11. 订单明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `order_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) DEFAULT NULL COMMENT '名称',
  `order_id` bigint NOT NULL COMMENT '订单id',
  `dish_id` bigint DEFAULT NULL COMMENT '菜品id',
  `setmeal_id` bigint DEFAULT NULL COMMENT '套餐id',
  `dish_flavor` varchar(255) DEFAULT NULL COMMENT '口味',
  `number` int NOT NULL DEFAULT 1 COMMENT '数量',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `image` varchar(255) DEFAULT NULL COMMENT '图片',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细表';

-- ----------------------------
-- 12. 操作日志表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `operate_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operate_emp_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operate_time` datetime NOT NULL COMMENT '操作时间',
  `class_name` varchar(255) DEFAULT NULL COMMENT '操作类名',
  `method_name` varchar(255) DEFAULT NULL COMMENT '操作方法名',
  `method_params` varchar(255) DEFAULT NULL COMMENT '操作方法参数',
  `return_value` varchar(255) DEFAULT NULL COMMENT '操作方法返回值',
  `cost_time` bigint DEFAULT NULL COMMENT '操作耗时(ms)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ----------------------------
-- 初始数据：admin 员工（密码为 123456 的 MD5）
-- ----------------------------
INSERT INTO `employee` (`name`, `username`, `password`, `phone`, `sex`, `id_number`, `status`, `create_time`, `update_time`, `create_user`, `update_user`)
VALUES ('管理员', 'admin', 'e10adc3949ba59abbe56e057f20f883e', '13800138000', '1', '110101199001011234', 1, NOW(), NOW(), 1, 1);

-- ----------------------------
-- 13. 店铺表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `shop` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) NOT NULL COMMENT '店铺名称',
  `logo` varchar(255) DEFAULT NULL COMMENT '店铺logo图片',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `address` varchar(255) DEFAULT NULL COMMENT '店铺地址',
  `longitude` decimal(11,7) DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10,7) DEFAULT NULL COMMENT '纬度',
  `description` varchar(255) DEFAULT NULL COMMENT '店铺简介',
  `notice` varchar(255) DEFAULT NULL COMMENT '公告',
  `open_time` time DEFAULT NULL COMMENT '开始营业时间',
  `close_time` time DEFAULT NULL COMMENT '结束营业时间',
  `status` int NOT NULL DEFAULT 1 COMMENT '营业状态 0:打烊 1:营业',
  `min_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '起送金额',
  `delivery_fee` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '配送费',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint NOT NULL COMMENT '创建人',
  `update_user` bigint NOT NULL COMMENT '修改人',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='店铺表';

-- 初始化一条店铺记录
INSERT INTO `shop`
(`name`,`logo`,`phone`,`address`,`longitude`,`latitude`,`description`,`notice`,
 `open_time`,`close_time`,`status`,`min_amount`,`delivery_fee`,
 `create_time`,`update_time`,`create_user`,`update_user`)
VALUES
('食刻外卖','','400-000-0000','北京市朝阳区xxx路1号',116.3971280,39.9165270,
 '专注为顾客提供美味、健康、快捷的餐饮服务','新店开业，全场满30减5元',
 '08:00:00','22:00:00',1,20.00,3.00,
 NOW(),NOW(),1,1);
