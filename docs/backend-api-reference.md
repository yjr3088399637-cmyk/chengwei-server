# 城味项目后端接口文档（学习版）

这份文档不是给前端联调用的 Swagger 替代品，而是给你**看源码时建立业务全景**用的。

建议阅读顺序：

1. 先看 [backend-table-relations.md](./backend-table-relations.md)，先把表关系理顺
2. 再按本文档的模块顺序看接口
3. 最后按“Controller -> ServiceImpl -> Mapper/Redis/MySQL”去读源码

---

## 1. 难度分级说明

- `L1 基础 CRUD`
  - 以新增、查询、修改、删除为主
  - 主要训练 Controller、Service、MyBatis-Plus 的基本调用链
- `L2 中等业务`
  - 有分页、条件筛选、关联查询、简单权限判断
  - 适合开始理解项目业务怎么串起来
- `L3 复杂业务`
  - 有状态流转、Redis、事务、角色权限、异步/防重复下单等
  - 是这个项目最能体现技术深度的部分

---

## 2. 认证与权限总览

### 2.1 登录态

- 用户端：`/user/login`
- 店员端：`/clerk/login`
- 管理端：`/admin/login`

### 2.2 拦截器入口

源码入口：

- `src/main/java/com/chengwei/config/MvcConfig.java`
- `src/main/java/com/chengwei/utils/RefreshInterceptor.java`
- `src/main/java/com/chengwei/utils/LoginInterceptor.java`
- `src/main/java/com/chengwei/utils/ClerkLoginInterceptor.java`
- `src/main/java/com/chengwei/utils/AdminLoginInterceptor.java`

### 2.3 角色划分

- 用户：`tb_user`
- 店长 / 店员：`tb_shop_clerk`
  - `role = 1` 店长
  - `role = 2` 店员
- 管理员：`tb_admin`

---

## 3. 用户模块接口

### 模块定位

负责：

- 验证码登录
- 用户资料查询与修改
- 密码设置与修改
- 签到

核心表：

- `tb_user`
- `tb_user_info`
- `tb_sign`

源码入口：

- `src/main/java/com/chengwei/controller/UserController.java`
- `src/main/java/com/chengwei/service/impl/UserServiceImpl.java`
- `src/main/java/com/chengwei/service/impl/UserInfoServiceImpl.java`

### 接口清单

| 方法 | 路径 | 作用 | 主要入参 | 关键表 | 难度 |
|---|---|---|---|---|---|
| POST | `/user/code` | 发送手机验证码 | `phone` | `tb_user` | L1 |
| POST | `/user/login` | 用户登录 | `LoginFormDTO{phone, code, password}` | `tb_user` | L2 |
| PUT | `/user/me` | 修改当前用户基本资料 | `User` | `tb_user` | L1 |
| GET | `/user/password/status` | 查询是否已设置密码 | 无 | `tb_user` | L1 |
| POST | `/user/password/set` | 首次设置密码 | `SetPasswordDTO{password}` | `tb_user` | L1 |
| PUT | `/user/password/change` | 修改密码 | `ChangePasswordDTO{oldPassword,newPassword}` | `tb_user` | L1 |
| POST | `/user/logout` | 退出登录 | Header 中 `authorization` | Redis 登录态 | L1 |
| GET | `/user/me` | 获取当前登录用户简要信息 | 无 | Redis 登录态 | L1 |
| GET | `/user/info/{id}` | 查询指定用户扩展资料 | `id` | `tb_user_info` | L1 |
| GET | `/user/info/me` | 查询自己的扩展资料 | 无 | `tb_user_info` | L1 |
| PUT | `/user/info` | 更新自己的扩展资料 | `UserInfo` | `tb_user_info` | L1 |
| GET | `/user/{id}` | 查询指定用户简要信息 | `id` | `tb_user` | L1 |
| POST | `/user/sign` | 今日签到 | 无 | `tb_sign` + Redis 位图 | L2 |
| POST | `/user/signCount` | 连续签到统计 | 无 | Redis 位图 | L2 |

### 学习建议

先看这 3 个接口最容易建立感觉：

1. `PUT /user/me`
2. `GET /user/info/me`
3. `PUT /user/password/change`

---

## 4. 关注关系模块接口

### 模块定位

负责：

- 关注 / 取关
- 查看是否已关注
- 查看共同关注
- 查询我的关注与粉丝

核心表：

- `tb_follow`
- `tb_user`

源码入口：

- `src/main/java/com/chengwei/controller/FollowController.java`
- `src/main/java/com/chengwei/service/impl/FollowServiceImpl.java`

### 接口清单

| 方法 | 路径 | 作用 | 主要入参 | 关键表 | 难度 |
|---|---|---|---|---|---|
| PUT | `/follow/{id}/{isFollow}` | 关注或取关用户 | `id`, `isFollow` | `tb_follow` | L2 |
| GET | `/follow/or/not/{id}` | 查询是否已关注某用户 | `id` | `tb_follow` | L1 |
| GET | `/follow/common/{id}` | 查询共同关注 | `id` | `tb_follow`, Redis | L2 |
| GET | `/follow/me/follows` | 查询我的关注列表 | 无 | `tb_follow`, `tb_user` | L2 |
| GET | `/follow/me/fans` | 查询我的粉丝列表 | 无 | `tb_follow`, `tb_user` | L2 |

### 学习建议

这个模块很适合学“中间关系表”：

- `tb_follow.user_id`
- `tb_follow.follow_user_id`

---

## 5. 博客与内容模块接口

### 模块定位

负责：

- 发布 / 编辑 / 删除博客
- 点赞博客
- 热门博客列表
- 个人博客列表
- 关注流博客
- 博客评论

核心表：

- `tb_blog`
- `tb_blog_comments`
- `tb_user`
- 可选关联：`tb_shop`

源码入口：

- `src/main/java/com/chengwei/controller/BlogController.java`
- `src/main/java/com/chengwei/service/impl/BlogServiceImpl.java`
- `src/main/java/com/chengwei/controller/BlogCommentsController.java`
- `src/main/java/com/chengwei/service/impl/BlogCommentsServiceImpl.java`

### 5.1 博客接口

| 方法 | 路径 | 作用 | 主要入参 | 关键表 | 难度 |
|---|---|---|---|---|---|
| POST | `/blog` | 发布博客 | `Blog` | `tb_blog` | L2 |
| PUT | `/blog` | 编辑博客 | `Blog` | `tb_blog` | L2 |
| DELETE | `/blog/{id}` | 删除博客 | `id` | `tb_blog` | L2 |
| PUT | `/blog/like/{id}` | 点赞 / 取消点赞博客 | `id` | `tb_blog`, Redis ZSet | L2 |
| GET | `/blog/of/me` | 查询我的博客 | `current` | `tb_blog` | L1 |
| GET | `/blog/hot` | 查询热门博客 | `current` | `tb_blog`, `tb_user`, Redis | L2 |
| GET | `/blog/{id}` | 查询博客详情 | `id` | `tb_blog`, `tb_user` | L2 |
| GET | `/blog/likes/{id}` | 查询点赞用户列表 | `id` | Redis ZSet, `tb_user` | L2 |
| GET | `/blog/of/user` | 查询某用户博客 | `current`, `id` | `tb_blog` | L1 |
| GET | `/blog/of/follow` | 关注流滚动分页 | `lastId`, `offset` | Redis, `tb_blog` | L3 |

### 5.2 博客评论接口

| 方法 | 路径 | 作用 | 主要入参 | 关键表 | 难度 |
|---|---|---|---|---|---|
| POST | `/blog-comments` | 发布评论 / 回复评论 | `BlogComments` | `tb_blog_comments` | L2 |
| DELETE | `/blog-comments/{id}` | 删除评论 | `id` | `tb_blog_comments` | L2 |
| GET | `/blog-comments/of/blog/{id}` | 查询某博客评论列表 | `id`, `current` | `tb_blog_comments`, `tb_user` | L2 |

### 学习建议

看博客模块时重点关注 3 件事：

1. 博客作者信息是怎么补全的
2. 点赞为什么用了 Redis ZSet
3. 关注流为什么是 `lastId + offset` 的滚动分页

---

## 6. 店铺与分类模块接口

### 模块定位

负责：

- 查询店铺详情
- 按分类 / 名称 / 区域 / 地址查店铺
- 查询店铺分类

核心表：

- `tb_shop`
- `tb_shop_type`

源码入口：

- `src/main/java/com/chengwei/controller/ShopController.java`
- `src/main/java/com/chengwei/service/impl/ShopServiceImpl.java`
- `src/main/java/com/chengwei/controller/ShopTypeController.java`
- `src/main/java/com/chengwei/service/impl/ShopTypeServiceImpl.java`

### 6.1 店铺接口

| 方法 | 路径 | 作用 | 主要入参 | 关键表 | 难度 |
|---|---|---|---|---|---|
| GET | `/shop/{id}` | 查询店铺详情 | `id` | `tb_shop`, Redis 缓存 | L2 |
| POST | `/shop` | 旧公共新增入口，现已禁用 | `Shop` | - | L1 |
| PUT | `/shop` | 旧公共修改入口，现已禁用 | `Shop` | - | L1 |
| GET | `/shop/of/type` | 按分类分页查询店铺，可带坐标距离 | `typeId,current,sortBy,x,y` | `tb_shop`, Redis GEO | L3 |
| GET | `/shop/of/name` | 按名称/商圈/地址搜索店铺，可带分类 | `name,typeId,current,sortBy` | `tb_shop` | L2 |

### 6.2 分类接口

| 方法 | 路径 | 作用 | 主要入参 | 关键表 | 难度 |
|---|---|---|---|---|---|
| GET | `/shop-type/list` | 查询全部店铺分类 | 无 | `tb_shop_type`, Redis | L1 |

### 学习建议

店铺模块里最值得精读的是：

1. `GET /shop/{id}`：缓存穿透 / 缓存重建思路
2. `GET /shop/of/type`：Redis GEO 距离查询

---

## 7. 店铺评论模块接口

### 模块定位

负责：

- 用户发布店铺评论
- 删除店铺评论
- 查询店铺评论列表

核心表：

- `tb_shop_comment`
- `tb_shop`
- `tb_user`

源码入口：

- `src/main/java/com/chengwei/controller/ShopCommentController.java`
- `src/main/java/com/chengwei/service/impl/ShopCommentServiceImpl.java`

### 接口清单

| 方法 | 路径 | 作用 | 主要入参 | 关键表 | 难度 |
|---|---|---|---|---|---|
| POST | `/shop-comments` | 发布店铺评论 | `ShopComment` | `tb_shop_comment` | L2 |
| DELETE | `/shop-comments/{id}` | 删除店铺评论 | `id` | `tb_shop_comment` | L2 |
| GET | `/shop-comments/of/shop/{id}` | 查询某店铺评论列表 | `id`, `current` | `tb_shop_comment`, `tb_user` | L2 |

---

## 8. 图片上传模块接口

### 模块定位

负责：

- 上传博客 / 店铺使用的图片
- 删除图片

核心对象：

- OSS 存储
- 本地图片路径（旧逻辑）

源码入口：

- `src/main/java/com/chengwei/controller/UploadController.java`
- `src/main/java/com/chengwei/utils/AliyunOSSOperator.java`

### 接口清单

| 方法 | 路径 | 作用 | 主要入参 | 关键对象 | 难度 |
|---|---|---|---|---|---|
| POST | `/upload/blog` | 上传图片 | `file` | OSS / 文件系统 | L2 |
| GET | `/upload/blog/delete` | 删除图片 | `name` | 文件系统 | L1 |

---

## 9. 优惠券与订单模块接口

### 模块定位

负责：

- 店铺优惠券查询
- 秒杀下单
- 用户订单查询
- 假支付 / 取消 / 用户端使用限制

核心表：

- `tb_voucher`
- `tb_seckill_voucher`
- `tb_voucher_order`

源码入口：

- `src/main/java/com/chengwei/controller/VoucherController.java`
- `src/main/java/com/chengwei/service/impl/VoucherServiceImpl.java`
- `src/main/java/com/chengwei/controller/VoucherOrderController.java`
- `src/main/java/com/chengwei/service/impl/VoucherOrderServiceImpl.java`
- `src/main/java/com/chengwei/service/impl/SeckillVoucherServiceImpl.java`

### 9.1 优惠券接口

| 方法 | 路径 | 作用 | 主要入参 | 关键表 | 难度 |
|---|---|---|---|---|---|
| POST | `/voucher` | 新增普通优惠券 | `Voucher` | `tb_voucher` | L2 |
| POST | `/voucher/seckill` | 新增秒杀券 | `Voucher` | `tb_voucher`, `tb_seckill_voucher` | L2 |
| GET | `/voucher/list/{shopId}` | 查询店铺优惠券列表 | `shopId` | `tb_voucher`, `tb_seckill_voucher` | L1 |

### 9.2 订单接口

| 方法 | 路径 | 作用 | 主要入参 | 关键表 | 难度 |
|---|---|---|---|---|---|
| POST | `/voucher-order/seckill/{id}` | 秒杀下单 | `id` | `tb_voucher_order`, Redis, `tb_seckill_voucher` | L3 |
| GET | `/voucher-order/me` | 查询我的订单 | 无 | `tb_voucher_order`, `tb_voucher`, `tb_shop` | L2 |
| PUT | `/voucher-order/{id}/pay` | 模拟支付订单 | `id` | `tb_voucher_order` | L2 |
| PUT | `/voucher-order/{id}/cancel` | 取消订单 | `id` | `tb_voucher_order`, Redis, `tb_seckill_voucher` | L3 |
| PUT | `/voucher-order/{id}/use` | 用户端使用入口（现已受限） | `id` | `tb_voucher_order` | L2 |

### 学习建议

这个模块建议重点啃：

1. `POST /voucher-order/seckill/{id}`
2. `PUT /voucher-order/{id}/cancel`

这是整套后端里最能体现：

- Redis Lua
- 防重复下单
- 订单状态流转
- 库存回滚

---

## 10. 店员 / 店长模块接口

### 模块定位

负责：

- 店员登录
- 查询当前店铺
- 店长修改店铺资料
- 店长创建员工
- 店员查询本店订单
- 店员核销订单
- 店员修改密码

核心表：

- `tb_shop_clerk`
- `tb_shop`
- `tb_voucher_order`

源码入口：

- `src/main/java/com/chengwei/controller/ClerkController.java`
- `src/main/java/com/chengwei/service/impl/ShopClerkServiceImpl.java`
- `src/main/java/com/chengwei/service/impl/VoucherOrderServiceImpl.java`

### 接口清单

| 方法 | 路径 | 作用 | 主要入参 | 关键表 | 难度 |
|---|---|---|---|---|---|
| POST | `/clerk/login` | 店员 / 店长登录 | `ClerkLoginFormDTO{username,password}` | `tb_shop_clerk` | L1 |
| GET | `/clerk/me` | 查询当前登录店员信息 | 无 | `tb_shop_clerk` | L1 |
| GET | `/clerk/shop` | 查询当前所属店铺信息 | 无 | `tb_shop_clerk`, `tb_shop` | L2 |
| PUT | `/clerk/shop` | 修改当前店铺信息（仅店长） | `ClerkShopUpdateDTO{name,images,area,address,avgPrice,openHours}` | `tb_shop` | L2 |
| GET | `/clerk/staff` | 查询本店员工列表（仅店长） | 无 | `tb_shop_clerk` | L2 |
| POST | `/clerk/staff` | 创建本店员工（仅店长） | `ClerkStaffSaveDTO{username,password,name}` | `tb_shop_clerk` | L2 |
| PUT | `/clerk/password/change` | 店员修改自己的密码 | `ChangePasswordDTO` | `tb_shop_clerk` | L1 |
| GET | `/clerk/orders` | 查询本店订单 | `status,keyword` | `tb_voucher_order`, `tb_voucher`, `tb_shop` | L2 |
| PUT | `/clerk/orders/verify` | 核销订单（校验核销码） | `ClerkVerifyOrderDTO{orderId,verifyCode}` | `tb_voucher_order`, `tb_shop_clerk` | L3 |

### 学习建议

如果你想理解“店长和店员权限差异”，这一组是最好的入口。

重点看：

1. `POST /clerk/staff`
2. `PUT /clerk/shop`
3. `PUT /clerk/orders/verify`

---

## 11. 管理员模块接口

### 模块定位

负责：

- 管理员登录
- 管理端首页统计
- 查询店铺分类
- 新增 / 编辑店铺
- 创建店长
- 查询店长列表

核心表：

- `tb_admin`
- `tb_shop`
- `tb_shop_type`
- `tb_shop_clerk`

源码入口：

- `src/main/java/com/chengwei/controller/AdminController.java`
- `src/main/java/com/chengwei/service/impl/AdminServiceImpl.java`

### 接口清单

| 方法 | 路径 | 作用 | 主要入参 | 关键表 | 难度 |
|---|---|---|---|---|---|
| POST | `/admin/login` | 管理员登录 | `AdminLoginFormDTO{username,password}` | `tb_admin` | L1 |
| GET | `/admin/me` | 查询当前管理员信息 | 无 | `tb_admin` | L1 |
| GET | `/admin/overview` | 管理端首页统计 | 无 | 多表聚合 | L2 |
| GET | `/admin/shop-types` | 查询所有店铺分类 | 无 | `tb_shop_type` | L1 |
| GET | `/admin/shops` | 查询店铺列表 | `keyword` | `tb_shop`, `tb_shop_type` | L2 |
| POST | `/admin/shops` | 新增店铺，并强制创建首个店长 | `AdminShopSaveDTO{name,typeId,images,area,address,x,y,avgPrice,openHours,clerkUsername,clerkPassword,clerkName}` | `tb_shop`, `tb_shop_clerk`, Redis GEO | L3 |
| PUT | `/admin/shops/{id}` | 编辑店铺 | `id + AdminShopSaveDTO` | `tb_shop`, Redis GEO | L2 |
| GET | `/admin/clerks` | 查询店长列表 | `keyword` | `tb_shop_clerk` | L2 |
| POST | `/admin/clerks` | 为某店铺创建店长 | `AdminClerkSaveDTO{shopId,username,password,name}` | `tb_shop_clerk` | L2 |

### 学习建议

管理端里最值得看的不是登录，而是：

1. `POST /admin/shops`
2. `POST /admin/clerks`
3. `GET /admin/overview`

因为这里能看到：

- 平台管理员和门店角色的职责边界
- 建店时为什么要同步首个店长
- 为什么新店要写入 Redis GEO

---

## 12. 建议你按这个顺序读源码

### 第 1 轮：基础 CRUD

建议先看：

1. `GET /shop-type/list`
2. `GET /user/info/me`
3. `PUT /user/me`

### 第 2 轮：中等业务

建议再看：

1. `POST /blog`
2. `GET /blog/hot`
3. `PUT /follow/{id}/{isFollow}`
4. `POST /clerk/staff`

### 第 3 轮：复杂业务

最后重点看：

1. `GET /shop/of/type`
2. `POST /voucher-order/seckill/{id}`
3. `PUT /voucher-order/{id}/cancel`
4. `PUT /clerk/orders/verify`
5. `POST /admin/shops`

---

## 13. 最值得优先精读的 10 个接口

如果你时间有限，先啃这 10 个：

1. `PUT /user/me`
2. `GET /shop/{id}`
3. `GET /shop/of/type`
4. `POST /blog`
5. `GET /blog/hot`
6. `PUT /follow/{id}/{isFollow}`
7. `POST /voucher-order/seckill/{id}`
8. `PUT /voucher-order/{id}/cancel`
9. `PUT /clerk/orders/verify`
10. `POST /admin/shops`

这 10 个几乎已经覆盖了这个项目的大部分关键后端能力。
