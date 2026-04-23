# 城味 ChengWei

一个围绕 **本地生活消费 + 内容种草 + 门店履约 + 平台管理** 构建的完整本地生活平台系统

## 项目简介

城味不是简单的团购展示站或订单流转后台，而是将本地生活平台最核心的几条链路——用户消费、内容社区、门店履约、平台治理——有机地串联在一起，形成真正闭环的业务系统。

项目采用前后端分离架构，后端提供 RESTful API，前端通过 Nginx 反向代理访问，Nginx 同时承担静态资源托管与负载均衡。

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 1.8 | 开发语言 |
| Spring Boot | 2.3.12 | 基础框架 |
| MyBatis-Plus | 3.4.3 | ORM 框架 |
| MySQL | 5.7 | 关系型数据库 |
| Redis | - | 缓存 / 登录态 / GEO / 布隆过滤器 / Feed 流 / 幂等控制 |
| Redisson | 3.13.6 | 分布式锁 |
| Sentinel | 2.2.9 | 流量控制 |
| SpringDoc | 1.6.15 | API 文档 (OpenAPI 3) |
| 阿里云 OSS | 3.17.4 | 对象存储 |
| Hutool | 5.7.17 | 工具库 |

### 前端

| 技术 | 说明 |
|------|------|
| Vue 2 | 前端框架 |
| Element UI | UI 组件库 |
| Axios | HTTP 请求 |
| Nginx 1.18 | 静态托管 / 反向代理 / 负载均衡 |

## 系统架构

```
浏览器
  │
  ▼
Nginx (:8080)
  ├── /           → 前端静态资源 (html/chengwei)
  └── /api        → 反向代理 → Spring Boot (:8081)
                              ├── 拦截器体系 (登录态恢复 + 权限判断)
                              ├── Controller 层
                              ├── Service 层
                              │    ├── MySQL (MyBatis-Plus)
                              │    ├── Redis (Lettuce + Redisson)
                              │    └── Lua 脚本 (秒杀 / 释放锁)
                              └── 统一返回 Result
```

## 核心功能

### 四类角色，四条业务链

| 角色 | 核心能力 | 业务链 |
|------|---------|--------|
| **普通用户** | 浏览店铺、购买优惠券、下单支付、到店核销、发布博客、点赞评论、关注流 | 用户消费链 + 内容链 |
| **店员** | 查看本店订单、按状态筛选、核销订单 | 门店履约链 |
| **店长** | 店员全部能力 + 维护门店资料 + 创建员工账号 | 门店管理链 |
| **平台管理员** | 新建店铺、指定店长、平台统计 | 平台治理链 |

### 用户端

- 首页热榜推荐（博客滚动加载）
- 分类浏览 / 搜索商户
- 店铺详情（营业时间、地址、图片、优惠券、评论）
- 优惠券购买 → 下单 → 支付 → 待核销
- 探店博客发布（支持关联商户）
- 博客点赞、评论、关注用户
- 关注流（Feed 流，Redis ZSet 收件箱模式）
- 用户签到
- 个人资料编辑

### 店员端 & 店长端

- 店员登录 → 查看本店订单 → 按状态筛选 → 核销
- 店长登录 → 维护门店资料（名称、地址、营业时间、图片等）
- 店长创建员工账号
- 修改密码

### 管理端

- 管理员登录
- 新建店铺（写入主表 + 同步 Redis GEO + 初始化店长关系 + 增量写入布隆过滤器）
- 店铺列表管理
- 平台统计概览

## 技术亮点

### 1. 三套独立登录体系 + 拦截器权限隔离

系统区分了用户、店员、管理员三套登录链路，各自拥有独立的 token、Redis 存储 key 前缀和拦截器：

- `UserHolder` / `ClerkHolder` / `AdminHolder` 三套线程级上下文容器
- 全局刷新拦截器恢复登录态并续期 token
- 业务方法直接从 Holder 取身份，无需前端传 id

### 2. Redis 深度使用（不止缓存）

| 用途 | 方案 |
|------|------|
| 登录态存储 | 随机 token + Redis Hash，支持主动失效 |
| 店铺缓存 | 布隆过滤器 + 逻辑过期 + 互斥锁重建 |
| 附近店铺 | Redis GEO 按经纬度查询 + 有序分页 |
| 关注流 | ZSet 收件箱，写扩散读变轻 |
| 秒杀 | Lua 脚本原子执行：库存判断 + 一人一单 |
| 幂等防重 | 短期 Redis key 拦截重复请求 |

### 3. 并发、缓存一致性与幂等

- **缓存**：布隆过滤器防穿透 + 逻辑过期保可用 + 互斥锁防击穿
- **幂等**：状态机式幂等（条件更新） + Redis 入口防重，双重保障
- **秒杀**：Lua 脚本前置库存扣减和一人一单校验，减少 DB 竞争

### 4. 工程化能力

- **统一参数校验**：`@Valid` + 校验注解 + 全局异常处理
- **AOP 操作日志**：自定义注解 + 切面，记录操作者、模块、动作、IP、耗时、成功/失败
- **目录职责拆分**：utils 按功能拆为 holder / interceptor / redis / cache / lock / security / storage 等子包
- **API 文档**：集成 SpringDoc OpenAPI，访问 `/swagger-ui.html` 查看

## 数据模型

```
用户主线    tb_user ──1:1── tb_user_info
            tb_follow (关注关系)
            tb_sign   (签到)

内容主线    tb_blog (博客，关联用户 + 店铺)
            tb_blog_comments (评论，支持回复链)

店铺主线    tb_shop_type ──1:N── tb_shop
            tb_shop_comment (用户对店铺的评分评论)

交易主线    tb_voucher ──1:1── tb_seckill_voucher
            tb_voucher_order (订单，关联用户 + 券 + 核销门店 + 核销店员)

权限主线    tb_shop_clerk (店长/店员，角色字段区分)
            tb_admin (平台管理员，独立登录体系)

日志主线    tb_operation_log (操作审计日志)
```

## 快速开始

### 环境准备

- JDK 1.8+
- MySQL 5.7+
- Redis 6.0+
- Maven 3.6+
- Nginx 1.18+（Windows 版本已包含在本仓库中）

### 1. 初始化数据库

创建名为 `chengwei` 的数据库，执行 SQL 初始化脚本：

```sql
source src/main/resources/db/chengwei.sql
```

### 2. 修改配置

编辑 `src/main/resources/application.yaml`，按需修改：

- MySQL 连接地址、用户名、密码
- Redis 连接地址
- 阿里云 OSS 配置（如不使用 OSS 上传可忽略）

### 3. 启动后端

```bash
mvn spring-boot:run
```

后端默认启动在 `8081` 端口。

### 4. 启动前端

将 Nginx 目录下的 `nginx-1.18.0` 文件夹中的 `conf/nginx.conf` 配置确认无误后，启动 Nginx：

```bash
# Windows 下
cd nginx-1.18.0
nginx.exe
```

前端默认通过 Nginx 在 `8080` 端口提供访问，API 请求通过 `/api` 前缀反向代理到后端 `8081` 端口。

### 5. 访问应用

| 入口 | 地址 |
|------|------|
| 用户端首页 | http://localhost:8080 |
| 店员端 | http://localhost:8080/clerk-login.html |
| 管理端 | http://localhost:8080/admin-login.html |
| API 文档 | http://localhost:8081/swagger-ui.html |

## 项目结构

```
chengwei-server/
├── src/main/java/com/chengwei/
│   ├── aspect/              # AOP 切面（操作日志）
│   ├── config/              # 配置类（MVC、MyBatis、Redisson、异常处理）
│   ├── controller/          # 控制器层
│   ├── dto/                 # 数据传输对象
│   ├── entity/              # 实体类
│   ├── mapper/              # MyBatis-Plus Mapper
│   ├── properties/          # 配置属性类
│   ├── service/             # 服务接口 + 实现
│   └── utils/
│       ├── annotation/      # 自定义注解
│       ├── cache/           # 缓存工具
│       ├── common/          # 通用工具
│       ├── holder/          # 上下文容器（User / Clerk / Admin Holder）
│       ├── interceptor/     # 拦截器（登录态恢复 + 权限判断）
│       ├── lock/            # 分布式锁
│       ├── redis/           # Redis 工具（布隆过滤器等）
│       ├── security/        # 安全相关
│       └── storage/         # 对象存储
├── src/main/resources/
│   ├── db/                  # SQL 初始化脚本
│   ├── mapper/              # MyBatis XML
│   ├── application.yaml     # 应用配置
│   ├── seckill.lua          # 秒杀 Lua 脚本
│   └── unlock.lua           # 释放锁 Lua 脚本
└── pom.xml

nginx-1.18.0/
├── conf/nginx.conf          # Nginx 配置（反向代理 + 静态托管）
└── html/chengwei/           # 前端页面
    ├── index.html           # 用户端首页
    ├── login.html           # 用户登录
    ├── shop-detail.html     # 店铺详情
    ├── shop-list.html       # 店铺列表
    ├── blog-detail.html     # 博客详情
    ├── blog-edit.html       # 发布博客
    ├── info.html            # 个人主页
    ├── clerk-login.html     # 店员登录
    ├── clerk-home.html      # 店员首页
    ├── clerk-orders.html    # 订单管理
    ├── clerk-shop.html      # 门店资料
    ├── clerk-staff.html     # 员工管理
    ├── admin-login.html     # 管理员登录
    ├── admin-shops.html     # 店铺管理
    └── css/ / js/ / imgs/   # 静态资源
```

## 开发状态

本项目仍在持续迭代中，当前已完成用户端、店员端、店长端和管理端的核心功能闭环，后续计划开发的方向包括但不限于：

- 用户实时聊天 / 私信模块
- 更完善的营销活动能力
- 操作日志查询页面
- 统一错误码体系

项目暂未配置正式部署流程，线上部署方案将在核心功能稳定后补充。当前仅在本地开发环境运行（Spring Boot + Nginx 本地启动）。
