# 校园二手闲置交易平台（Campus Secondhand）

> 《Spring Boot 企业级项目开发》期末综合大作业 · 高阶版实现
> 后端 Spring Boot 2.7 + MyBatis-Plus + MySQL 8 + Redis + RabbitMQ + Spring Security，**服务端渲染 Thymeleaf（无前后端分离、无 Vue/React）**

---

## 1. 项目简介

面向校园场景的二手闲置交易平台。学生可以把闲置的教材、数码、生活用品、运动器材、服饰鞋包、代步工具发布到平台，
其他同学按分类/关键词/价格区间检索，通过站内留言沟通、下单交易、线下交付；管理员在后台完成物品审核、违规下架、
分类与用户管理，并通过数据看板掌握平台运营情况。

核心业务闭环：

```
注册/登录 → 发布闲置（多图，待审核） → 管理员审核 → 在售
   → 浏览/搜索/收藏/留言 → 下单（Redis 幂等 + 乐观 CAS 锁定物品）
   → 支付 → 线下交付 → 买家确认收货（完成）
   ↘ 30 分钟未支付 → RabbitMQ 延迟队列自动关闭订单并释放物品
```

## 2. 技术栈

| 层次 | 选型 |
| --- | --- |
| 运行环境 | JDK 17、Spring Boot 2.7.18 |
| 视图层 | Thymeleaf 3 + thymeleaf-extras-springsecurity5（**服务端渲染**）、原生 CSS/JS、ECharts 5（本地文件，断网可用） |
| 持久层 | MyBatis-Plus 3.5.3 + XML 动态 SQL、MySQL 8.0 |
| 安全 | Spring Security（表单登录、BCrypt、三级角色 + 权限码、CSRF、越权校验） |
| 缓存 | Redis（StringRedisTemplate/RedisTemplate，分类、热门物品、物品详情、空值缓存、浏览记录、购物车、幂等令牌、分布式锁） |
| 消息 | RabbitMQ（TTL + 死信队列实现延迟取消，异步邮件，站内消息推送） |
| 定时任务 | Spring Task（订单超时兜底扫描、每日数据统计） |
| 邮件 | Spring Mail + JavaMailSender + Thymeleaf 邮件模板（默认 mock 模式，只落库不外发） |
| 工程化 | 全局异常处理、统一返回体 R、JSR303 校验、AOP 日志、Logback 按天切割 |

## 3. 目录结构

```
campus-secondhand
├── pom.xml
├── sql/01-init.sql                        # 建库建表 + 索引 + 注释 + 演示种子数据（15 张表）
├── assets/demo/                           # 18 张演示图（12 物品封面 + 6 头像），拷到上传目录即可用
├── deploy/                                # Linux 部署物料
│   ├── campus-secondhand.service          # systemd 单元
│   ├── nginx.conf                         # Nginx 反向代理 / 静态资源 / 上传目录
│   ├── start.sh / stop.sh / restart.sh    # 启停脚本
│   └── init-db.sh                         # 服务器上初始化数据库
├── docs/                                  # 交付文档
│   ├── 01-功能清单与评分对照.md
│   ├── 02-数据库设计说明.md
│   ├── 03-部署文档.md
│   ├── 04-答辩要点与演示脚本.md
│   ├── 05-项目开发说明与提示词.md
│   └── screenshots/                       # 8 张验收截图（首页/详情/看板/审核/分类/用户/订单/邮件日志）
├── src/main/java/com/campus/secondhand
│   ├── SecondhandApplication.java
│   ├── common/        R 统一返回、Constants 状态常量、GlobalExceptionHandler、GlobalModelAdvice
│   ├── config/        Security、MyBatis-Plus、Redis、RabbitMQ、WebMvc(静态资源)、Jackson、线程池、文件属性
│   ├── controller/    前台 8 个 + 后台 8 个（admin 子包）+ 状态页
│   ├── dto/ vo/       表单校验对象与视图对象
│   ├── entity/        15 个实体（与表一一对应）
│   ├── mapper/        16 个 Mapper（复杂查询用 XML）
│   ├── mq/            消息体、生产者、3 个消费者
│   ├── security/      LoginUser、UserDetailsService、登录成功处理器
│   ├── service/       14 个业务服务
│   ├── task/          订单超时兜底、每日统计
│   └── utils/         RedisKey 统一管理、文件上传、当前登录人
└── src/main/resources
    ├── application.yml / -dev / -test / -prod、logback-spring.xml
    ├── mapper/*.xml
    ├── static/{css,js,lib,img}            # 自研样式 app.css/admin.css、app.js、echarts.min.js
    └── templates/{fragments,portal,admin,auth,error,mail}   # 32 个页面 + 4 个邮件模板
```

## 4. 快速开始（Windows / IDEA）

### 4.1 准备环境

| 组件 | 版本 | 端口 |
| --- | --- | --- |
| JDK | 17（项目编译与运行都用 17） | - |
| Maven | 3.6+ | - |
| MySQL | 8.0 | 3306 |
| Redis | 5.0+（默认无密码） | 6379 |
| RabbitMQ | 3.8+（默认 guest/guest） | 5672 / 15672 |

### 4.2 初始化数据库

```bash
mysql --host=127.0.0.1 --port=3306 --user=root --password=你的密码 < sql/01-init.sql
```

脚本会创建 `campus_secondhand` 库、15 张表、索引与注释，并写入演示数据（6 个账号、20 件物品、8 笔订单、留言/收藏/审核记录/站内消息/7 天统计/邮件日志）。

### 4.2.1 放置演示图片

种子数据里物品封面与头像引用 `/upload/demo/item-01.jpg`、`avatar-01.jpg` 等路径，需把仓库自带的 `assets/demo/`（18 张）复制到上传目录：

```powershell
New-Item -ItemType Directory -Force -Path D:\campus-upload\demo | Out-Null
Copy-Item assets\demo\* D:\campus-upload\demo\ -Force
```

上传目录由 `application.yml` 的 `file.upload-path` 决定（默认 `D:/campus-upload/`），换目录时两边一起改。

### 4.3 配置数据库密码

默认密码是 `123456`，可用环境变量覆盖（推荐，避免把真实密码写进仓库）：

```powershell
$env:MYSQL_PASSWORD='你的密码'
```

或直接改 `src/main/resources/application-dev.yml` 中的 `password: ${MYSQL_PASSWORD:123456}`。

### 4.4 启动

```powershell
mvn spring-boot:run
# 或先打包再运行
mvn clean package -DskipTests
java -jar target/campus-secondhand.jar
```

浏览器访问 <http://localhost:8080>。

### 4.5 演示账号

| 账号 | 密码 | 角色 | 说明 |
| --- | --- | --- | --- |
| admin | admin123 | 管理员 | 后台全部菜单、审核、看板 |
| seller01 | 123456 | 卖家 | 有多件在售物品与订单 |
| seller02 | 123456 | 卖家 | 有多件在售物品与订单 |
| student01~03 | 123456 | 普通用户 | 用于买家流程演示 |

## 5. 主要页面

| 前台 | 路径 | 后台 | 路径 |
| --- | --- | --- | --- |
| 首页 | `/` | 数据看板 | `/admin/dashboard` |
| 闲置市场（分类/关键词/价格区间/排序/分页） | `/items` | 用户管理 | `/admin/users` |
| 物品详情（图集/留言/收藏/下单） | `/item/{id}` | 物品审核与管理 | `/admin/items`、`/admin/items/audits` |
| 发布/编辑闲置 | `/items/publish`、`/items/{id}/edit` | 分类管理 | `/admin/categories` |
| 我的闲置 | `/my-items` | 订单管理 | `/admin/orders` |
| 购物车 | `/cart` | 留言管理 | `/admin/comments` |
| 我的收藏 | `/user/favorites` | 角色/菜单（只读） | `/admin/roles`、`/admin/menus` |
| 下单/订单详情/我的订单 | `/order/create`、`/order/{id}`、`/orders/mine` | 邮件日志 | `/admin/mail-logs` |
| 个人资料/改密/消息/浏览记录 | `/user/profile`、`/user/password`、`/user/messages`、`/user/history` | 图表数据接口 | `/admin/api/stat/**` |

## 6. 常见问题

1. **启动报 `Access denied for user 'root'`**：数据库密码没配对，见 4.3，用 `MYSQL_PASSWORD` 环境变量覆盖。
2. **启动报 RabbitMQ 连接失败**：确认 5672 端口可连、账号 guest/guest；如果只想跑前台，可先停用消费者（`mq.consumer.enabled=false` 仅影响测试环境配置，dev 请保持开启以演示异步能力）。
3. **邮件发不出去**：默认 `app.mail.mock: true`，只把邮件内容渲染后写入 `tb_mail_log`，不真正外发，答辩演示无需配置真实邮箱；要真发邮件时把 `MAIL_MOCK` 设为 false 并填 `spring.mail.username/password`。
4. **图片 404**：上传目录由 `file.upload-path` 决定（默认 `D:/campus-upload/`），请把仓库 `assets/demo/` 下的 18 张演示图复制到该目录的 `demo/` 子目录（见 4.2.1），或改成自己的路径。
5. **端口冲突**：改 `application.yml` 的 `server.port`。
6. **演示 30 分钟超时太长**：临时用 `SPRING_APPLICATION_JSON='{"mq":{"order":{"delay-ms":60000}}}'` 覆盖为 60 秒；注意 RabbitMQ 队列已按旧 TTL 声明过时需要先在管理台删除 `sc.order.delay.queue`，否则会参数冲突。

## 7. 文档索引

- 功能清单与评分对照：[docs/01-功能清单与评分对照.md](docs/01-功能清单与评分对照.md)
- 数据库设计说明：[docs/02-数据库设计说明.md](docs/02-数据库设计说明.md)
- Linux 部署文档：[docs/03-部署文档.md](docs/03-部署文档.md)
- 答辩要点与演示脚本：[docs/04-答辩要点与演示脚本.md](docs/04-答辩要点与演示脚本.md)
- 开发说明与 AI 提示词约束：[docs/05-项目开发说明与提示词.md](docs/05-项目开发说明与提示词.md)