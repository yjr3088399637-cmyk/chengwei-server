# 城味项目后端源码怎么读

这份不是“完整课程目录”，而是给你一个**最省力的阅读方案**。

你的目标不是把所有文件名背下来，而是先回答这 3 个问题：

1. 请求是怎么进后端的？
2. 数据最后落到哪些表里？
3. 复杂业务为什么会那样设计？

所以最好的办法不是“按目录树看”，而是**按业务链看**。

---

## 开始前先看：utils 目录怎么分类

很多人第一次看这个项目会先被 `utils` 吓到，因为文件名不少，而且看起来都像“工具类”。

其实这里面并不是一堆零散工具，基本可以分成 6 组。

你只要先把这 6 组分清，后面看源码会直观很多。

### 1. 登录态与当前用户上下文

这些类解决的是：

- 当前请求是谁发的
- 为什么业务代码里能直接拿当前用户 / 店员 / 管理员

对应文件：

- `UserHolder.java`
- `ClerkHolder.java`
- `AdminHolder.java`

你可以把它们理解成：

- **当前线程里的“登录用户暂存区”**

后面你在 Service 里看到：

- `UserHolder.getUser()`
- `ClerkHolder.getClerk()`
- `AdminHolder.getAdmin()`

就知道是在从这里取当前登录对象。

### 2. 登录拦截与权限入口

这些类解决的是：

- 哪些请求要登录
- 哪些请求是用户端
- 哪些请求是店员端
- 哪些请求是管理端

对应文件：

- `RefreshInterceptor.java`
- `LoginInterceptor.java`
- `ClerkLoginInterceptor.java`
- `AdminLoginInterceptor.java`

你可以这样记：

- `RefreshInterceptor`：每次请求先刷新登录态
- `LoginInterceptor`：管普通用户接口
- `ClerkLoginInterceptor`：管店员 / 店长接口
- `AdminLoginInterceptor`：管管理员接口

如果你后面看某个接口总搞不清“为什么它需要登录”，就回来看这一组。

### 3. Redis 基础设施

这些类解决的是：

- Redis 的 key 怎么统一命名
- 缓存对象怎么封装
- 全局唯一 ID 怎么生成

对应文件：

- `RedisConstants.java`
- `RedisData.java`
- `RedisWorker.java`

可以这样理解：

- `RedisConstants`：Redis key 的字典
- `RedisData`：带过期时间的缓存包装对象
- `RedisWorker`：基于 Redis 生成全局 ID

后面看：

- 店铺缓存
- 订单号生成
- 登录 token
- 博客点赞

这些逻辑时，都会反复遇到这一组。

### 4. 缓存与分布式锁

这些类解决的是：

- 缓存查询怎么统一写
- 缓存击穿怎么处理
- 简单分布式锁怎么实现

对应文件：

- `CacheClient.java`
- `ILock.java`
- `SimpleRedisLock.java`

你可以这样记：

- `CacheClient`：项目里的缓存工具核心
- `ILock`：锁接口
- `SimpleRedisLock`：Redis 实现的简单锁

如果你后面看店铺详情、缓存重建、库存相关逻辑，这一组很重要。

### 5. 通用校验与常量

这些类解决的是：

- 正则校验
- 项目里的固定常量

对应文件：

- `RegexPatterns.java`
- `RegexUtils.java`
- `SystemConstants.java`

可以这样记：

- `RegexPatterns`：正则表达式常量
- `RegexUtils`：手机号之类的校验工具
- `SystemConstants`：分页大小、默认头像、图片路径等系统常量

这组不难，但会频繁出现。

### 6. 业务辅助工具

这些类不属于上面几组，但也是项目运行需要的支撑工具：

- `PasswordEncoder.java`
- `AliyunOSSOperator.java`

用途：

- `PasswordEncoder`：密码加密 / 校验
- `AliyunOSSOperator`：阿里云 OSS 上传图片

### 你现在只要先记住这张简化图

可以把 `utils` 记成：

- **Holder**：当前登录的人是谁
- **Interceptor**：谁能进哪个接口
- **Redis**：缓存、ID、Key
- **Lock / Cache**：缓存工具和锁
- **Regex / Constants**：校验和常量
- **Password / OSS**：密码和上传

如果你只先记住这 6 组，已经足够了。

---

## 先说最重要的结论

如果你现在没头绪，**不要一上来啃 60 多个接口**。

最好的顺序只有这 4 步：

1. 先搞懂“登录态和权限”
2. 再看最简单的增删改查
3. 然后看店长 / 店员 / 管理员这条权限链
4. 最后再看秒杀和订单

也就是说：

- 先看“系统怎么运转”
- 再看“普通业务怎么写”
- 最后看“难的业务为什么这么写”

---

## 第一部分：先搞懂登录态和权限

这一部分你只看 5 个文件就够了。

### 1. `MvcConfig.java`

文件：

- `src/main/java/com/chengwei/config/MvcConfig.java`

你要解决的问题：

- 哪些接口不需要登录？
- 哪些接口是用户登录后才能访问？
- 哪些接口是店员端？
- 哪些接口是管理端？

为什么先看它：

因为这是整个后端的“门禁总表”。

只要你先看懂这里，后面再看接口时就不会老问自己：

- 这个接口是谁在调？
- 为什么这个接口能进？
- 为什么另一个接口会被拦？

你看这个文件时，只盯一件事：

- `addInterceptors()` 里每条拦截器拦了谁，放过了谁

### 2. `RefreshInterceptor.java`

文件：

- `src/main/java/com/chengwei/utils/RefreshInterceptor.java`

你要解决的问题：

- 用户每次请求时，登录信息是怎么刷新的？
- Redis 里的 token 为什么不会很快过期？

为什么第二个看它：

因为这个文件解释了“登录态为什么能一直续命”。

### 3. `LoginInterceptor.java`

文件：

- `src/main/java/com/chengwei/utils/LoginInterceptor.java`

你要解决的问题：

- 用户端接口为什么能判断“已登录/未登录”？

它很简单，但它能让你理解：

- 用户端权限是怎么拦的

### 4. `ClerkLoginInterceptor.java` 和 `AdminLoginInterceptor.java`

文件：

- `src/main/java/com/chengwei/utils/ClerkLoginInterceptor.java`
- `src/main/java/com/chengwei/utils/AdminLoginInterceptor.java`

你要解决的问题：

- 为什么店员端、管理端能和用户端分开？

这两个文件的作用其实就是一句话：

- **不同角色走不同登录态，不混用**

### 5. `UserHolder.java` / `ClerkHolder.java` / `AdminHolder.java`

文件：

- `src/main/java/com/chengwei/utils/UserHolder.java`
- `src/main/java/com/chengwei/utils/ClerkHolder.java`
- `src/main/java/com/chengwei/utils/AdminHolder.java`

你要解决的问题：

- 为什么业务代码里可以直接拿“当前登录用户 / 当前店员 / 当前管理员”？

看完这里，你就会明白：

- 登录信息并不是每次都手动传进 Service
- 而是被放进了当前线程上下文

### 这一部分看完后，你应该能回答

1. 用户、店员、管理员三种登录态怎么分开的？
2. 为什么 Controller / Service 里可以直接拿当前登录对象？
3. 为什么有的接口不用登录，有的必须登录？

如果这 3 个问题你都能回答，后面看业务会轻松很多。

---

## 第二部分：先看最简单的业务

这里我不让你看很多接口，只看 4 个最典型的。

---

### 1. 查询分类：`GET /shop-type/list`

入口文件：

- `ShopTypeController.java`
- `ShopTypeServiceImpl.java`

你要看什么：

- 一个最简单的查询接口是怎么写的
- `Controller -> Service -> 返回 Result` 是怎么走的

为什么先看它：

- 简单
- 没复杂权限
- 没复杂参数
- 很适合建立基本感觉

你看完这个接口后，要明白：

- 一个接口最基本的后端链路长什么样

---

### 2. 查当前用户：`GET /user/me`

入口文件：

- `UserController.java`

这个接口看起来很简单，但它特别重要。

你要看什么：

- 为什么这个接口不用查数据库也能拿到当前用户简要信息

看完你要明白：

- 登录态缓存和 `UserHolder` 是怎么配合的

---

### 3. 修改用户资料：`PUT /user/me`

入口文件：

- `UserController.java`
- `UserServiceImpl.java`

你要看什么：

- 用户修改昵称、头像时，数据库怎么更新
- 为什么更新后还要同步登录态

这个接口很值得精读，因为它是典型的“更新自己信息”的写法。

你要重点找：

- 更新数据库的代码
- 更新 Redis 登录态的代码

---

### 4. 查店铺详情：`GET /shop/{id}`

入口文件：

- `ShopController.java`
- `ShopServiceImpl.java`
- `CacheClient.java`

这个接口是你后端里第一条“技术感比较明显”的链路。

你要看什么：

- 为什么查店铺不用每次都查数据库
- Redis 缓存为什么会出现在这里

你要重点理解：

- 缓存的作用是什么
- 这个项目是怎么查缓存、查数据库、回填缓存的

如果这里看懂了，后面很多 Redis 使用方式都更容易接受。

---

## 第三部分：开始看真正的业务模块

这一部分最推荐你先看 3 条业务线。

---

### 业务线 1：博客

推荐顺序：

1. `POST /blog`
2. `GET /blog/hot`
3. `GET /blog/{id}`
4. `PUT /blog/like/{id}`

入口文件：

- `BlogController.java`
- `BlogServiceImpl.java`

你为什么应该先看博客：

- 它比订单简单
- 但比纯 CRUD 更像真实业务

你要重点看：

- 博客怎么关联用户
- 博客怎么关联店铺
- 热榜列表里为什么要补作者昵称和头像
- 点赞为什么用了 Redis

看完博客模块后，你会开始理解：

- “接口不只是 CRUD，还会做数据补全和状态计算”

---

### 业务线 2：关注关系

推荐顺序：

1. `PUT /follow/{id}/{isFollow}`
2. `GET /follow/or/not/{id}`
3. `GET /follow/common/{id}`

入口文件：

- `FollowController.java`
- `FollowServiceImpl.java`

你要重点看：

- `tb_follow` 这张表是怎么表示用户之间关系的
- “共同关注”为什么适合和 Redis 配合

这条线能帮助你理解：

- 一张中间表怎么表示多对多关系

---

### 业务线 3：博客评论

推荐顺序：

1. `POST /blog-comments`
2. `GET /blog-comments/of/blog/{id}`

入口文件：

- `BlogCommentsController.java`
- `BlogCommentsServiceImpl.java`

你要重点看：

- 一级评论和回复评论怎么区分
- `parentId` 和 `answerId` 各自是什么意思

---

## 第四部分：看你项目最有特色的权限链

这是你这个项目最值得讲的地方：

- 管理员
- 店长
- 店员
- 用户

不是一锅粥，而是 4 套角色。

这一部分建议分成两条线看。

---

### 线 1：店员 / 店长

推荐顺序：

1. `POST /clerk/login`
2. `GET /clerk/me`
3. `GET /clerk/shop`
4. `PUT /clerk/shop`
5. `GET /clerk/staff`
6. `POST /clerk/staff`

入口文件：

- `ClerkController.java`
- `ShopClerkServiceImpl.java`

你要重点看：

- 店长和店员是怎么区分的
- 为什么只有店长能修改店铺
- 为什么只有店长能创建员工

你要特别注意：

- 这里不是光前端隐藏按钮
- 后端也做了权限校验

这是项目完整度很高的体现。

---

### 线 2：管理员

推荐顺序：

1. `POST /admin/login`
2. `GET /admin/overview`
3. `POST /admin/shops`
4. `POST /admin/clerks`

入口文件：

- `AdminController.java`
- `AdminServiceImpl.java`

这里最值得你精读的其实只有一个接口：

- `POST /admin/shops`

因为它串起来的是：

- 创建店铺
- 必须指定首个店长
- 保存店铺数据
- 同步 GEO 坐标

这条链最像真实平台业务。

你看这个接口时，重点不是记代码细节，而是回答：

- 为什么管理员建店时必须指定首个店长？
- 为什么新店还要同步坐标？

---

## 第五部分：最后再看最难的订单和秒杀

这一步一定要最后看。

如果前面没铺垫，直接看这里真的会很痛苦。

---

### 先看订单，再看秒杀

推荐顺序：

1. `GET /voucher-order/me`
2. `PUT /voucher-order/{id}/pay`
3. `PUT /voucher-order/{id}/cancel`
4. `PUT /clerk/orders/verify`
5. `POST /voucher-order/seckill/{id}`

入口文件：

- `VoucherOrderController.java`
- `VoucherOrderServiceImpl.java`

### 为什么这样排

因为你应该先理解：

- 订单是什么
- 订单有哪些状态
- 订单怎么变已支付
- 订单为什么能取消
- 订单怎么核销

等这些都懂了，再去看：

- 秒杀为什么难
- 为什么要 Lua
- 为什么要 Stream

就不会懵。

### 这里最关键的问题

你看订单模块时，不要只看代码，强迫自己回答：

1. 状态有哪些？
2. 每个状态能往哪里转？
3. 谁能改这个状态？
4. Redis 在这里到底帮了什么？

如果这 4 个问题答出来，订单模块基本就通了。

---

## 如果你时间有限，只看这 8 个接口

我给你一个真正高价值的最小集合。

这 8 个看懂，整个项目后端就已经通了大半：

1. `GET /user/me`
2. `PUT /user/me`
3. `GET /shop/{id}`
4. `POST /blog`
5. `PUT /follow/{id}/{isFollow}`
6. `PUT /clerk/shop`
7. `POST /admin/shops`
8. `POST /voucher-order/seckill/{id}`

为什么是这 8 个：

- 有登录态
- 有 CRUD
- 有缓存
- 有社交关系
- 有店长权限
- 有平台建店
- 有秒杀交易

这 8 个刚好覆盖你项目最核心的后端能力。

---

## 你每看一个接口，只问自己这 5 个问题

以后你看任何接口，不要再被文件列表带着跑。

只问自己：

1. 这个接口是谁在用？
2. 它改了哪张表？
3. 有没有权限判断？
4. 有没有 Redis / 缓存参与？
5. 前端点一下这个按钮，页面会发生什么变化？

只要这 5 个问题能回答出来，这个接口就是真的看懂了。

---

## 最后给你的直接建议

如果你现在马上开始，我建议你就按这个顺序：

1. `MvcConfig.java`
2. `GET /user/me`
3. `PUT /user/me`
4. `GET /shop/{id}`
5. `POST /blog`

不要再摊开整份接口文档看了。

你先把这 5 个点真的吃透，后面效率会高很多。
