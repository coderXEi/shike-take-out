# AGENTS.md

本文件面向在此仓库中工作的 AI Coding Agent。人类贡献者也可参考。

---

## 1. 项目概述

**食客外卖（shike-take-out）** 是一个基于 Spring Boot 的外卖点餐 / 配送后端服务，采用 **Maven 多模块** 架构。项目同时对外提供两套 API：

| 端 | 路径前缀 | 使用者 | 认证 Header |
|---|---|---|---|
| **管理端（商家后台）** | `/admin/**` | 商家员工（PC 后台） | `token`（JWT） |
| **用户端（微信小程序）** | `/user/**` | 小程序用户 | `authentication`（JWT） |
| **支付回调** | `/notify/**` | 微信支付平台（无需认证） | — |
| **WebSocket** | `/ws/{sid}` | 后台来单/催单提醒 | — |

两端的 Controller 包已物理隔离：`com.shike.controller.admin` / `com.shike.controller.user`。

技术栈：Spring Boot 2.7.3、MyBatis + PageHelper、MySQL、Redis、Spring Cache、Spring Task、WebSocket（JSR-356）、Knife4j / Swagger2、阿里云 OSS SDK、微信小程序 + 微信支付 SDK v3、Apache POI（Excel 导出）、Lombok、JJWT。

---

## 2. 模块结构

```
shike-take-out/                 # 父 POM（版本/依赖统一管理）
├── shike-common/               # 通用层：无业务依赖，可被任意模块引用
│   └── com.shike.{constant, context, enumeration, exception, json,
│                  properties, result, utils}
├── shike-pojo/                 # 数据模型：entity / dto / vo，无逻辑
│   └── com.shike.{entity, dto, vo}
├── shike-server/               # 应用层：启动类 + 全部 Web/业务代码
│   └── com.shike.{anno, aop, aspect, config, controller, handler,
│                  interceptor, mapper, service, task, websocket}
├── shike_take_out.sql          # 建库建表脚本（13 张表）
└── shike_take_out_test_data.sql# 测试数据
```

依赖方向严格为：`shike-server → shike-pojo / shike-common`；`shike-pojo`、`shike-common` 之间互不依赖。

**新增类时的放置规则：**
- 纯数据类（DB 实体、DTO、VO）→ `shike-pojo`
- 工具类、常量、异常、通用配置属性、统一返回体 → `shike-common`
- Controller / Service / Mapper / 配置 / AOP / 定时任务 / WebSocket → `shike-server`

---

## 3. 分层与调用链

标准请求链路（以菜品为例）：

```
Controller(admin/DishController)
  → Service 接口(DishService)
    → ServiceImpl(DishServiceImpl)      ← 业务逻辑、事务、缓存
      → Mapper 接口(DishMapper)         ← MyBatis
        → resources/mapper/DishMapper.xml  ← SQL
```

约定：
- Service 一律 **接口 + `impl` 实现类** 成对存在，接口放 `service/`，实现放 `service/impl/`。
- 简单查询可用注解 SQL，复杂 SQL 写在 `resources/mapper/*.xml`（`mybatis.mapper-locations=classpath:mapper/*.xml`）。
- 实体别名：`type-aliases-package=com.shike.entity`，且开启 `map-underscore-to-camel-case`。
- Controller 只做参数接收、日志、调用 Service、返回 `Result`，**不写业务逻辑**。
- 事务使用 `@Transactional`（启动类已 `@EnableTransactionManagement`）。

---

## 4. 关键基础设施

### 4.1 统一返回体
所有接口返回 `com.shike.result.Result<T>`：`code`（1=成功，0=失败）、`msg`、`data`。
分页统一用 `com.shike.result.PageResult`。**不要发明新的返回结构。**

### 4.2 异常处理
- 业务异常：继承 `com.shike.exception.BaseException`（如 `OrderBusinessException`、`DeletionNotAllowedException`），在 `GlobalExceptionHandler` 中统一转成 `Result.error(msg)`。
- 提示文案集中在 `com.shike.constant.MessageConstant`，不要硬编码中文文案。
- 抛业务异常时沿用既有语义，例如「分类下关联菜品/套餐时不允许删除」。

### 4.3 认证与上下文
- JWT 拦截器：`JwtTokenAdminInterceptor`（`/admin/**`）、`JwtTokenUserInterceptor`（`/user/**`），已排除 `/admin/employee/login`、`/admin/shop/status`、`/user/user`、`/user/shop/status`。
- 需要放行的接口可加 `@NoAuth`（`com.shike.anno.NoAuth`）。
- JWT 校验通过后把当前登录 id 写入 `com.shike.utils.CurrentHolder`（ThreadLocal），业务层通过 `CurrentHolder.get()` 取当前员工/用户 id。**不要自己解析请求头。**
- 密钥与过期时间在 `shike.jwt.*`（管理端 `admin-*`，用户端 `user-*`）。

### 4.4 公共字段自动填充
- 注解 `@AutoFill(OperationType.INSERT / UPDATE)` + 切面 `AutoFillAspect`，自动填充 `create_time / update_time / create_user / update_user`。
- 常量见 `AutoFillConstant`。
- **约定：Mapper 的 insert/update 方法若涉及这些字段，需标注 `@AutoFill`。**

### 4.5 操作日志
- 注解 `@Log` + 切面 `OperateLogAspect`（`com.shike.aop`），记录操作人、方法、参数、返回值、耗时到 `operate_log` 表。

### 4.6 Redis 缓存
- `RedisConfiguration` 中 `RedisTemplate` 的 key 使用 `StringRedisSerializer`（key 可读），value 为 JDK 序列化。
- 菜品缓存 key 统一由 `DishCacheConstant` 生成（前缀 `dish_`），**必须用它拼 key**。
- 缓存更新策略：**主动清理**，不依赖过期。菜品增删改/起停售后调用清理逻辑（见 `DishController#cleanDishCache`，按 `dish_*` 前缀删除），缓存读取后再回填。

### 4.7 定时任务
- `@EnableScheduling` 已开启，任务位于 `com.shike.task`。
- `OrderTask`：超时未支付订单处理（原每分钟，cron 当前已注释）、派送中订单每日 1 点归档为已完成。
- `MyTask`：定时任务编写示例。
- 新增任务参考其写法，cron 与业务含义写清注释。

### 4.8 WebSocket
- `WebSocketServer`（`@ServerEndpoint("/ws/{sid}")`）维护 `sid → Session`，提供 `sendToAllClient(String)`。
- 用途：管理端「来单提醒」「客户催单」。消息为 JSON 字符串，含 `type`、`orderId`、`content`。
- `WebSocketTask` 提供定时群发示例（当前 cron 已注释），`WebSocketConfiguration` 负责注册端点。
- 注意：`WebSocketServer` 依赖静态 `sessionMap`，**多实例部署时无法跨节点广播**，且 Spring 单例注入 `WebSocketServer` 时其静态字段生效。

### 4.9 文件上传 / 导出
- 图片上传：`CommonController#upload` → `AliOssUtil`，配置在 `alioss.*`。
- Excel 导出：`ReportController` 使用 POI，模板 `resources/template/info.xlsx`。

### 4.10 序列化
`JacksonObjectMapper`（注册在 `WebMvcConfiguration#extendMessageConverters`，优先级最高）统一 `LocalDateTime/LocalDate/LocalTime` 格式并忽略未知字段。新增时间字段无需额外注解。

---

## 5. 接口文档

Knife4j 已配置，启动后访问：

- 文档首页：`http://localhost:8080/doc.html`
- 分组：`管理端接口`（扫描 `controller.admin`）、`用户端接口`（扫描 `controller.user`）

新增 Controller 需带 `@Api(tags=...)`，方法带 `@ApiOperation("...")`，与现有风格保持一致。

---

## 6. 数据模型

数据库：`shike_take_out`（MySQL 8，`serverTimezone=Asia/Shanghai`）。共 13 张表：

`employee`、`user`、`category`、`dish`、`dish_flavor`、`setmeal`、`setmeal_dish`、
`shopping_cart`、`address_book`、`orders`、`order_detail`、`operate_log`、`shop`。

核心业务模型：
- **菜品体系**：`category`（分类）→ `dish`（菜品）+ `dish_flavor`（口味）；`setmeal`（套餐）通过 `setmeal_dish` 关联多个菜品。
- **交易体系**：`shopping_cart`（购物车）→ `orders`（订单）+ `order_detail`（明细）。
- **状态流转（`orders.status`）**：`1 待付款 → 2 待接单 → 3 已接单 → 4 派送中 → 5 已完成 → 6 已取消`；`pay_status`：`0 未支付 / 1 已支付 / 2 已退款`。状态常量定义在 `Orders` 实体中（`Orders.TO_BE_CONFIRMED` 等），**禁止魔法数字**。
- **店铺营业状态**：Redis key `SHOP_STATUS`（见 `ShopServiceImpl.KEY`）为读缓存，`shop` 表为持久化存储，`1 营业 / 0 打烊`；写操作先更新 DB 再同步缓存。

SQL 文件仅供参考（已被 `.gitignore` 忽略，见第 8 节），**不要提交库结构变更时只改代码不改脚本**。

---

## 7. 构建与运行

```bash
# 构建（在仓库根目录）
mvn clean package -DskipTests

# 仅编译 server 模块
mvn -pl shike-server -am compile

# 运行
mvn -pl shike-server spring-boot:run
# 或
java -jar shike-server/target/shike-server-1.0-SNAPSHOT.jar
```

服务端口 **8080**，默认激活 `dev` profile。

### 外部依赖（本地需提前就绪）
| 组件 | 默认地址 | 配置项 |
|---|---|---|
| MySQL | `localhost:3306/shike_take_out` | `shike.datasource.*` |
| Redis | `localhost:6379` (db 0) | `shike.redis.*` |
| 阿里云 OSS | — | `shike.alioss.*` |
| 微信小程序 / 支付 | — | `shike.wechat.*` |

初始化数据库：依次执行 `shike_take_out.sql`、`shike_take_out_test_data.sql`。
管理端默认登录账号见测试数据（密码为 MD5 存储，见 `PasswordConstant`）。

---

## 8. ⚠️ 重要约定与陷阱

1. **`application.yml`、`application-dev.yml`、`change.md`、两个 `.sql` 文件都在 `.gitignore` 中**，不入版本库。修改配置时注意它们不会被提交，不要假定 CI/他人拥有相同配置。配置含真实密钥，**严禁把密钥写进源码或提交**。
2. **微信支付当前为 Mock**：`OrderServiceImpl#payment()` 中的真实 `weChatPayUtil.pay(...)` 调用已被注释（标记「勿删除」），改为直接调用 `paySuccess(orderNumber)`，即跳过微信下单、直接置为「已支付 + 待接单」并推送 WebSocket 来单提醒；退款同理被注释，直接置 `pay_status = 2`。**恢复真实支付前不要删除注释块，也不要让 `notify/paySuccess` 重复触发。**
3. **不要提交 `target/`、`.idea/`**（已在 `.gitignore`）。
4. 换行符问题：仓库曾在 Windows(CRLF) 下产生过大规模 diff，**不要做无意义的整文件格式化**，提交前用 `git diff --stat` 确认改动范围。
5. 已知的 Lombok `log` 符号编译告警是历史遗留问题，与业务改动无关。
6. `spring.main.allow-circular-references: true` 已开启，但**不要以此为由新增循环依赖**。

---

## 9. 编码规范

- 语言：Java 8 语法风格、Spring Boot 2.7 / `javax.*`（**不是** `jakarta.*`）。
- 使用 Lombok：`@Data` / `@Builder` / `@Slf4j` / `@NoArgsConstructor` / `@AllArgsConstructor`，与所在包既有类保持一致。
- Controller 方法入口打印 `log.info(...)`，参数与关键分支打日志。
- 常量集中管理（`com.shike.constant`），枚举放 `com.shike.enumeration`。
- 依赖注入：项目主要用字段注入 `@Autowired`（部分新类用构造器注入），**同一文件内保持一致**；新代码推荐构造器注入。
- 命名遵循现有风格：DTO 后缀 `DTO`、VO 后缀 `VO`、分页查询对象 `XxxPageQueryDTO`。

---

## 10. 提交规范

Commit message 采用 Conventional Commits 前缀 + 中文描述，例如：

```
feat:新增ws功能，添加下单提醒与催单提醒
fix: 修复小程序登录 token 失效问题
chore: stop tracking sql files
```

---

## 11. 快速定位（常见改动入口）

| 需求 | 入手位置 |
|---|---|
| 新增管理端接口 | `shike-server/.../controller/admin/XxxController.java` + `service/(impl)/` + `mapper/` + `mapper/XxxMapper.xml` |
| 新增小程序接口 | 同上，包换成 `controller/user`，注意 `JwtTokenUserInterceptor` 与 `CurrentHolder` |
| 订单状态/流程 | `OrderServiceImpl`、`OrderMapper.xml`、`Orders` 常量 |
| 来单/催单提醒 | `OrderServiceImpl#paySuccess`、`WebSocketServer`、`WebSocketTask` |
| 缓存相关 | `DishCacheConstant`、`RedisConfiguration`、`DishController` |
| 报表统计 | `ReportController`、`ReportServiceImpl`、`WorkspaceServiceImpl` |
| 数据统计 SQL | `shike-server/src/main/resources/mapper/OrderMapper.xml` |
```
