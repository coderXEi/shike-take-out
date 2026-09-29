USE shike_take_out;

-- ----------------------------
-- 分类表测试数据
-- ----------------------------
INSERT INTO `category` (`type`, `name`, `sort`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES
(1, '川菜', 1, 1, '2026-09-01 10:00:00', '2026-09-01 10:00:00', 1, 1),
(1, '粤菜', 2, 1, '2026-09-01 10:00:00', '2026-09-01 10:00:00', 1, 1),
(1, '主食', 3, 1, '2026-09-01 10:00:00', '2026-09-01 10:00:00', 1, 1),
(2, '商务套餐', 1, 1, '2026-09-01 10:00:00', '2026-09-01 10:00:00', 1, 1),
(2, '家庭套餐', 2, 1, '2026-09-01 10:00:00', '2026-09-01 10:00:00', 1, 1);

-- ----------------------------
-- 菜品表测试数据
-- ----------------------------
INSERT INTO `dish` (`name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES
('麻婆豆腐', 1, 28.00, 'https://xei.oss-cn-hangzhou.aliyuncs.com/mapo_tofu.jpg', '麻辣鲜香，下饭神器', 1, '2026-09-02 10:00:00', '2026-09-02 10:00:00', 1, 1),
('宫保鸡丁', 1, 32.00, 'https://xei.oss-cn-hangzhou.aliyuncs.com/gongbao_chicken.jpg', '经典川菜，花生鸡丁', 1, '2026-09-02 10:00:00', '2026-09-02 10:00:00', 1, 1),
('水煮鱼', 1, 58.00, 'https://xei.oss-cn-hangzhou.aliyuncs.com/boiled_fish.jpg', '麻辣水煮，鲜嫩鱼片', 1, '2026-09-02 10:00:00', '2026-09-02 10:00:00', 1, 1),
('白切鸡', 2, 48.00, 'https://xei.oss-cn-hangzhou.aliyuncs.com/white_chicken.jpg', '原汁原味，蘸料提鲜', 1, '2026-09-02 10:00:00', '2026-09-02 10:00:00', 1, 1),
('扬州炒饭', 3, 18.00, 'https://xei.oss-cn-hangzhou.aliyuncs.com/yangzhou_rice.jpg', '粒粒分明，配料丰富', 1, '2026-09-02 10:00:00', '2026-09-02 10:00:00', 1, 1),
('蛋炒饭', 3, 15.00, 'https://xei.oss-cn-hangzhou.aliyuncs.com/egg_rice.jpg', '简单美味', 0, '2026-09-02 10:00:00', '2026-09-02 10:00:00', 1, 1);

-- ----------------------------
-- 菜品口味表测试数据
-- ----------------------------
INSERT INTO `dish_flavor` (`dish_id`, `name`, `value`) VALUES
(1, '辣度', '["微辣","中辣","特辣"]'),
(1, '份量', '["小份","大份"]'),
(2, '辣度', '["微辣","中辣","特辣"]'),
(2, '加料', '["加花生","不加花生"]'),
(3, '辣度', '["微辣","中辣","特辣"]'),
(4, '蘸料', '["姜葱酱","沙姜酱","酱油"]'),
(5, '份量', '["小份","大份"]');

-- ----------------------------
-- 套餐表测试数据
-- ----------------------------
INSERT INTO `setmeal` (`category_id`, `name`, `price`, `status`, `description`, `image`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES
(4, '商务午餐A套餐', 38.00, 1, '含一份主食+一份菜品', 'https://xei.oss-cn-hangzhou.aliyuncs.com/setmeal_a.jpg', '2026-09-03 10:00:00', '2026-09-03 10:00:00', 1, 1),
(4, '商务午餐B套餐', 45.00, 1, '含一份主食+两份菜品', 'https://xei.oss-cn-hangzhou.aliyuncs.com/setmeal_b.jpg', '2026-09-03 10:00:00', '2026-09-03 10:00:00', 1, 1),
(5, '家庭欢乐套餐', 128.00, 1, '四菜一汤，适合3-4人', 'https://xei.oss-cn-hangzhou.aliyuncs.com/setmeal_family.jpg', '2026-09-03 10:00:00', '2026-09-03 10:00:00', 1, 1);

-- ----------------------------
-- 套餐菜品关系表测试数据
-- ----------------------------
INSERT INTO `setmeal_dish` (`setmeal_id`, `dish_id`, `name`, `price`, `copies`) VALUES
(1, 5, '扬州炒饭', 18.00, 1),
(1, 1, '麻婆豆腐', 28.00, 1),
(2, 6, '蛋炒饭', 15.00, 1),
(2, 2, '宫保鸡丁', 32.00, 1),
(2, 4, '白切鸡', 48.00, 1),
(3, 1, '麻婆豆腐', 28.00, 1),
(3, 3, '水煮鱼', 58.00, 1),
(3, 4, '白切鸡', 48.00, 1),
(3, 5, '扬州炒饭', 18.00, 1);

-- ----------------------------
-- 用户表测试数据
-- ----------------------------
INSERT INTO `user` (`openid`, `name`, `phone`, `sex`, `id_number`, `avatar`, `create_time`) VALUES
('o1234567890abcdef', '张三', '13800001111', '1', '110101199003071234', 'https://xei.oss-cn-hangzhou.aliyuncs.com/avatar_zhangsan.jpg', '2026-09-05 12:00:00'),
('o0987654321fedcba', '李四', '13800002222', '0', '110101199505201234', 'https://xei.oss-cn-hangzhou.aliyuncs.com/avatar_lisi.jpg', '2026-09-06 14:00:00'),
('oabcdef1234567890', '王五', '13800003333', '1', '110101198812121234', 'https://xei.oss-cn-hangzhou.aliyuncs.com/avatar_wangwu.jpg', '2026-09-07 16:00:00');

-- ----------------------------
-- 地址簿表测试数据
-- ----------------------------
INSERT INTO `address_book` (`user_id`, `consignee`, `phone`, `sex`, `province_code`, `province_name`, `city_code`, `city_name`, `district_code`, `district_name`, `detail`, `label`, `is_default`) VALUES
(1, '张三', '13800001111', '1', '110000', '北京市', '110100', '北京市', '110105', '朝阳区', '建国路88号SOHO现代城A座501室', '公司', 1),
(1, '张三', '13800001111', '1', '110000', '北京市', '110100', '北京市', '110106', '丰台区', '南三环西路16号草桥欣园3号楼2单元801', '家', 0),
(2, '李四', '13800002222', '0', '310000', '上海市', '310100', '上海市', '310104', '徐汇区', '漕溪北路88号圣爱广场12楼', '公司', 1),
(3, '王五', '13800003333', '1', '440000', '广东省', '440100', '广州市', '440106', '天河区', '天河北路233号中信广场35楼', '公司', 1);

-- ----------------------------
-- 购物车表测试数据
-- ----------------------------
INSERT INTO `shopping_cart` (`name`, `user_id`, `dish_id`, `setmeal_id`, `dish_flavor`, `number`, `amount`, `image`, `create_time`) VALUES
('麻婆豆腐', 1, 1, NULL, '中辣', 2, 56.00, 'https://xei.oss-cn-hangzhou.aliyuncs.com/mapo_tofu.jpg', '2026-09-10 11:30:00'),
('扬州炒饭', 1, 5, NULL, '大份', 1, 18.00, 'https://xei.oss-cn-hangzhou.aliyuncs.com/yangzhou_rice.jpg', '2026-09-10 11:30:00'),
('商务午餐A套餐', 1, NULL, 1, NULL, 1, 38.00, 'https://xei.oss-cn-hangzhou.aliyuncs.com/setmeal_a.jpg', '2026-09-10 11:30:00'),
('宫保鸡丁', 2, 2, NULL, '加花生', 1, 32.00, 'https://xei.oss-cn-hangzhou.aliyuncs.com/gongbao_chicken.jpg', '2026-09-10 12:00:00');

-- ----------------------------
-- 订单表测试数据
-- ----------------------------
INSERT INTO `orders` (`number`, `status`, `user_id`, `address_book_id`, `order_time`, `checkout_time`, `pay_method`, `pay_status`, `amount`, `remark`, `user_name`, `phone`, `address`, `consignee`, `cancel_reason`, `rejection_reason`, `cancel_time`, `estimated_delivery_time`, `delivery_status`, `delivery_time`, `pack_amount`, `tableware_number`, `tableware_status`) VALUES
('20260910113000001', 5, 1, 1, '2026-09-10 11:30:00', '2026-09-10 11:31:00', 1, 1, 94.00, '不要辣，少放盐', '张三', '13800001111', '北京市朝阳区建国路88号SOHO现代城A座501室', '张三', NULL, NULL, NULL, '2026-09-10 12:00:00', 1, '2026-09-10 12:05:00', 2, 2, 1),
('20260910120000002', 5, 2, 3, '2026-09-10 12:00:00', '2026-09-10 12:01:00', 1, 1, 32.00, '多放花生', '李四', '13800002222', '上海市徐汇区漕溪北路88号圣爱广场12楼', '李四', NULL, NULL, NULL, '2026-09-10 12:30:00', 1, '2026-09-10 12:35:00', 1, 1, 1),
('20260910150000003', 6, 1, 2, '2026-09-10 15:00:00', NULL, 1, 0, 38.00, NULL, '张三', '13800001111', '北京市丰台区南三环西路16号草桥欣园3号楼2单元801', '张三', '不想吃了', NULL, '2026-09-10 15:10:00', NULL, 0, NULL, 0, 0, 0),
('20260911090000004', 4, 3, 4, '2026-09-11 09:00:00', '2026-09-11 09:01:00', 1, 1, 128.00, '多人用餐，多给餐具', '王五', '13800003333', '广州市天河区天河北路233号中信广场35楼', '王五', NULL, NULL, NULL, '2026-09-11 09:40:00', 1, NULL, 3, 4, 1);

-- ----------------------------
-- 订单明细表测试数据
-- ----------------------------
INSERT INTO `order_detail` (`name`, `order_id`, `dish_id`, `setmeal_id`, `dish_flavor`, `number`, `amount`, `image`) VALUES
('麻婆豆腐', 1, 1, NULL, '中辣', 2, 56.00, 'https://xei.oss-cn-hangzhou.aliyuncs.com/mapo_tofu.jpg'),
('扬州炒饭', 1, 5, NULL, '大份', 1, 18.00, 'https://xei.oss-cn-hangzhou.aliyuncs.com/yangzhou_rice.jpg'),
('商务午餐A套餐', 1, NULL, 1, NULL, 1, 38.00, 'https://xei.oss-cn-hangzhou.aliyuncs.com/setmeal_a.jpg'),
('宫保鸡丁', 2, 2, NULL, '加花生', 1, 32.00, 'https://xei.oss-cn-hangzhou.aliyuncs.com/gongbao_chicken.jpg'),
('商务午餐A套餐', 3, NULL, 1, NULL, 1, 38.00, 'https://xei.oss-cn-hangzhou.aliyuncs.com/setmeal_a.jpg'),
('麻婆豆腐', 4, 1, NULL, '微辣', 1, 28.00, 'https://xei.oss-cn-hangzhou.aliyuncs.com/mapo_tofu.jpg'),
('水煮鱼', 4, 3, NULL, '中辣', 1, 58.00, 'https://xei.oss-cn-hangzhou.aliyuncs.com/boiled_fish.jpg'),
('白切鸡', 4, 4, NULL, '姜葱酱', 1, 48.00, 'https://xei.oss-cn-hangzhou.aliyuncs.com/white_chicken.jpg'),
('扬州炒饭', 4, 5, NULL, '小份', 1, 18.00, 'https://xei.oss-cn-hangzhou.aliyuncs.com/yangzhou_rice.jpg');

-- ----------------------------
-- 操作日志表测试数据
-- ----------------------------
INSERT INTO `operate_log` (`operate_emp_id`, `operate_time`, `class_name`, `method_name`, `method_params`, `return_value`, `cost_time`) VALUES
(1, '2026-09-10 11:25:00', 'com.shike.controller.admin.EmployeeController', 'save', '[EmployeeDTO(name=赵六, username=zhao6, phone=13900001234, sex=1)]', 'null', 35),
(1, '2026-09-10 11:26:00', 'com.shike.controller.admin.CategoryController', 'updateCategory', '[CategoryDTO(id=1, name=川菜精选, sort=1, type=1)]', 'null', 12),
(1, '2026-09-10 11:27:00', 'com.shike.controller.admin.DishController', 'save', '[DishDTO(name=鱼香肉丝, categoryId=1, price=26.00)]', 'null', 48),
(1, '2026-09-10 11:28:00', 'com.shike.controller.admin.SetmealController', 'save', '[SetmealDTO(name=秋季特惠套餐, categoryId=5, price=99.00)]', 'null', 55),
(1, '2026-09-10 11:29:00', 'com.shike.controller.admin.EmployeeController', 'startOrStop', '[1, 1]', 'null', 8);
