---
name: backend-builder
description: 根据项目架构蓝图（project-architect产出）实现完整可运行的 Spring Boot + MySQL + MyBatis-Plus 后端项目。包括项目骨架、Flyway migration、安全认证、Mapper层、Service层、Controller层、全局异常处理、按需文件上传/Redis/MinIO，以及每完成一个纵向功能切片即写入验证过的前端交接包。当用户说"帮我实现后端""搭建后端项目""写XX模块的后端代码"时使用。
argument-hint: "[架构蓝图文档路径或直接粘贴内容]"
model: opus
effort: high
---

## 目标

拿到 `project-architect` 产出的架构蓝图，生成**完整可运行**的后端项目。默认按蓝图中的"纵向功能切片队列"逐个交付：每实现完一个切片，必须写入一份**验证过的前端交接包**（与蓝图对照，标注差异点），供前端立即接入。

本技能固定面向 **Spring Boot + Maven + MySQL + MyBatis-Plus + Flyway** 项目。Redis、MinIO、WebSocket、定时任务、消息队列等只在蓝图明确需要时加入。

支持两种节奏：
- **协作模式**：交接包落盘后停止，等待前端回执再继续下一个切片
- **Solo 模式**：交接包落盘后可以提示切换到 frontend-builder；前端回执落盘且 `docs/mvp-progress.md` 放行后，再继续下一个后端切片

**模式检测**：读取 `docs/mvp-progress.md` 的"开发模式"字段。若为 Solo，交接包/回执落盘后自动提示切换到另一个 builder，不等待用户手动确认；若为协作，停止等待对端反馈。

## 核心原则

1. **按蓝图施工，但有修正权**：蓝图是设计稿，实现中发现问题要主动修正，并记录到 API 文档中
2. **一个功能切片完整打通**：不按实体批量铺文件，而是围绕一个用户可见功能，从表结构/mapper → service → controller → curl 验证完整走通
3. **代码风格统一**：命名、异常处理、响应包装、日志格式全项目保持一致性
4. **写完就测，测完就交接**：每完成一个切片，用 curl 或浏览器直接调一次，确认返回 JSON 后产出交接包
5. **所有交付物落盘**：交接包、curl 样例、蓝图修正、进度状态必须写入 `docs/`
6. **数据库变化必须有 migration**：不直接口头建表，不让 schema 演进失控
7. **等待放行再继续**：协作模式等前端反馈；Solo 模式等前端回执和进度文件放行

---

## 前置：读取并理解蓝图

开始编码前，先解析架构蓝图中以下章节：

- **§3 数据模型**：有哪些实体、字段、关联关系
- **§2 功能设计中的纵向功能切片队列**：本轮应该实现哪个切片、依赖哪些前置切片
- **§4 API 契约**：全部端点列表、请求/响应格式、错误码
- **§5 后端架构**：技术栈选型、分层设计、包结构
- `docs/mvp-progress.md`：当前切片状态、是否放行下一个、以及**开发模式**（Solo/协作）
- `docs/openapi.yaml`：如果存在，以它校验接口字段和类型

如果蓝图有缺失或模糊的地方，只在会影响实体、字段、权限或 API 语义时向用户确认。非关键工程细节按本技能约定补齐，并在实际 API 文档的变更记录中说明。

### 前置 1.5：确定本轮切片

开始编码前，先从蓝图的"纵向功能切片队列"中选择本轮切片：

- 如果用户指定了切片名称或功能名，只实现该切片
- 如果用户没有指定，读取 `docs/mvp-progress.md`，选择第一个未完成且依赖已满足的切片
- 如果没有切片队列，根据用户故事临时拆出一个最小可交付切片，并写入交接包

本轮切片必须在输出开头说明：切片名称、覆盖故事、后端端点、前端预期接入点、验收标准、交接包保存路径。

本轮持久化路径固定为：

```text
docs/slices/NN-[slice-name]/
├── backend-handoff.md
├── api-examples.md
└── blueprint-changes.md
```

### 前置 2：环境校验（必须执行，不可跳过）

在写任何代码之前，先执行以下检查：

1. **JDK 版本检查**：`java -version`。如果 JDK 版本 ≥ 17，Spring Boot 2.x 的 Maven 插件会报 `Unsupported class file major version`。解决：用 `JAVA_HOME` 指向 JDK 11，或将 `pom.xml` 的 `<java.version>` 设为 11。
2. **依赖版本兼容预判**：
   - `jjwt 0.9.x` 在 Java 11+ 需要额外添加 `javax.xml.bind:jaxb-api:2.3.1`，否则抛 `NoClassDefFoundError: DatatypeConverter`
   - `minio 8.x` 需要 `okhttp 4.x`，而 Spring Boot 2.x 自带 `okhttp 3.x`。如果用到 MinIO，必须在 pom 中显式添加 `okhttp:4.10.0` 并排除 minio 传递的旧版 okhttp
   - Lombok 版本需 ≥ 1.18.30 才支持 Java 21。用 `<lombok.version>` 覆盖父 POM 的旧版本
3. 将检查结果记录到本轮 `backend-handoff.md` 或项目 README 中，后续步骤直接引用。

---

## 执行流程

### 第 1 步：项目骨架

**1.1 创建项目目录结构**

按蓝图 §5 的包结构创建完整目录树。包名、类名全部从蓝图中的实体名推导，不使用固定模板名。

**1.2 配置依赖**

根据固定技术栈创建 Maven 的 `pom.xml`。依赖按用途分组并加注释：

```xml
<!-- === 核心框架 === -->
<!-- === 数据库 & ORM === -->
<!-- === 数据库版本管理 === -->
<!-- === 安全 & 认证 === -->
<!-- === 缓存 === -->
<!-- === 文件存储 === -->
<!-- === 工具库 === -->
<!-- === 测试 === -->
```

根据前置 2 的兼容性检查结果，确保添加：
- MySQL 驱动、MyBatis-Plus、Flyway
- Java 11+：`jaxb-api:2.3.1`
- MinIO：`okhttp:4.10.0` + 排除 minio 内的旧 okhttp
- Java 21：`<lombok.version>1.18.32</lombok.version>`

**1.3 配置文件**

`application.yml` 按以下结构组织，不暴露密钥到代码中：

```yaml
server:
  port: [端口号]

spring:
  datasource:        # 数据库连接
  flyway:            # migration 目录和开关
  redis:             # Redis（如果用到）
  servlet:
    multipart:       # 文件上传限制

mybatis-plus:        # MyBatis-Plus 配置
  mapper-locations: classpath:mapper/*.xml

app:
  jwt:               # JWT 密钥和过期时间
  upload:            # 文件上传路径
  # 按需加 AI/OSS 等第三方配置
```

**1.4 启动类**

创建 `XxxApplication.java`，加 `@MapperScan` 注解指定 mapper 包路径。

**1.5 Flyway migration 基线**

- 创建 `src/main/resources/db/migration/`
- 第一个切片通常从 `V1__[slice].sql` 开始
- 每个切片新增/修改表结构都必须新增 migration 文件，一个切片一个 migration 文件
- migration 文件中写清表、索引、唯一约束、默认值和必要注释
- 不要在运行时自动 `ddl-auto:update` 代替 migration

---

### 第 2 步：安全认证层（最先做）

认证层必须最先实现，因为它是所有其他接口的前置依赖。

**2.1 JWT 工具类**

```java
// 三个核心方法：生成 token、解析 token、校验是否过期
// 密钥从配置文件读取，不硬编码
```

**2.2 登录/注册接口**

按蓝图 §4 的认证相关端点实现。重点是密码必须 bcrypt 加密存储，登录成功后返回的 token 格式与蓝图一致。

**2.3 Security 过滤链**

- 登录/注册接口放行
- 静态资源放行
- 其余全部拦截，校验 token
- 401 返回统一响应格式（不是 Tomcat 默认 HTML）

**2.4 当前用户接口**

`GET /api/auth/me`：从 token 中解析用户 ID → 查库 → 返回 VO（不返回密码字段）。

---

### 第 3 步：本轮功能切片（建立模式）

选择本轮功能切片涉及的最小实体和接口，从底层到上层完整实现一条链路，**建立编码模式**。如果一个切片包含多个实体，只实现该切片验收所需的最小集合。

**3.1 entity**

在写 entity 前，先写本轮 Flyway migration。entity 必须与 migration 字段一致。

- 每个实体一个类，放在 `model/entity/` 下
- 用 MyBatis-Plus 注解映射表名和字段（`@TableName`、`@TableId`、`@TableField`）
- 时间字段用 `LocalDateTime`，自动填充用 `@TableField(fill = ...)`
- 逻辑删除用 `@TableLogic`（业务数据实体需要）

**3.2 Mapper 接口**

- 每个实体一个 Mapper 接口，放在 `mapper/` 下
- 继承 MyBatis-Plus 的 `BaseMapper<T>`，获得基础 CRUD
- 复杂查询（多表联查、聚合、条件组合）声明自定义方法
- 对应的 XML 放在 `resources/mapper/` 下，文件名与 Mapper 接口名一致

**MyBatis-Plus 正确用法（必读）**：

```java
// ✅ 正确：BaseMapper 提供的方法
mapper.insert(entity);           // 新增
mapper.updateById(entity);       // 按ID更新
mapper.selectById(id);           // 按ID查询
mapper.selectList(wrapper);      // 条件查询
mapper.selectPage(page, wrapper);// 分页查询
mapper.deleteById(id);           // 按ID删除

// ❌ 错误：这些方法在 BaseMapper 中不存在
// mapper.insertOrUpdate(...)    // MyBatis-Plus 没有这个方法
// mapper.save(...)              // JPA 的方法名，MyBatis-Plus 用 insert
// mapper.findById(...)          // JPA 的方法名，MyBatis-Plus 用 selectById

// 需要 insertOrUpdate 语义时，显式判断：
if (entity.getId() != null && mapper.selectById(entity.getId()) != null) {
    mapper.updateById(entity);
} else {
    mapper.insert(entity);
}
```

> 这个项目踩过的坑：误写了 `insertOrUpdate` 导致编译失败。MyBatis-Plus 的 BaseMapper 只提供 insert 和 updateById，不存在 JPA 风格的 save/insertOrUpdate 方法。

**3.3 DTO 和 VO**

- **DTO（入参）**：前端请求体用。只包含前端传入的字段，加校验注解（`@NotNull`、`@NotBlank`、`@Size` 等）
- **VO（返参）**：返回给前端用。只包含前端需要的字段，不与 entity 一一对应
- DTO 和 VO 放在 `model/dto/` 和 `model/vo/` 下

**3.4 Service 层**

- 接口放在 `service/` 包下
- 实现放在 `service/impl/` 包下
- 业务逻辑写在这里：校验 → 调 mapper → 组装数据 → 返回
- 遇到错误抛业务异常（自定义异常类，带错误码），不返回 null 或 -1

**3.5 Controller 层**

- 只做三件事：校验入参 → 调 service → 包装统一响应
- 统一返回 `ApiResponse<T>`，controller 层不抛异常（有全局异常处理器兜底）
- 用 `@Validated` 触发 DTO 的校验注解

**3.6 验证**

写完这个模块的所有接口后，用测试和 curl 逐一调通。确认：
- 正常返回的数据结构符合蓝图 §4 的契约
- 空参数时的校验提示清晰
- 返回的 JSON 中没有 `password`、`isDeleted` 等内部字段
- 至少一个关键 Service 方法有单测
- 对外 API 用 curl 冒烟，保留至少 1 个成功样例和 1 个失败/校验样例

---

### 第 4 步：本轮切片内的剩余接口

重复第 3 步的模式，只补齐本轮切片所需接口。不要提前实现后续切片的完整 CRUD。通常顺序为：

```
用户认证 → 核心资源1的CRUD → 核心资源2的CRUD → 关联操作 → 统计/聚合接口
```

每完成一个接口就用 curl 验证，不要攒到最后。

---

### 第 4.5 步：本轮测试门槛

每个切片交接前至少完成：

- `mvn test` 可运行；如果失败，交接包必须记录失败原因和剩余风险
- 关键 Service 方法必须有单测，覆盖核心业务规则
- 对外 API 至少保留 1 个成功 curl 样例和 1 个失败/校验 curl 样例
- 认证相关切片必须验证无 Token / 错误 Token / 正确 Token 三种情况
- 文件上传切片必须验证大小、格式和返回 URL

---

### 第 5 步：全局异常处理

**5.1 自定义异常类**

```java
// BusinessException(code, message)
// code 对应蓝图 §4.4 的错误码枚举
```

**5.2 全局异常处理器**

`@RestControllerAdvice` 统一拦截：
- `BusinessException` → 按异常里的 code 和 message 返回
- `MethodArgumentNotValidException` → 返回参数校验失败的具体字段
- `Exception` → 兜底，返回 500，日志记录完整堆栈
- 所有响应都包装为统一 `ApiResponse` 格式

---

### 第 6 步：跨域和文件上传

**6.1 CORS 配置**

实现 `WebMvcConfigurer`，配置允许的前端源、方法和头。生产环境应从配置文件读取源地址而非硬编码 `*`。

**6.2 文件上传**

- 限制单文件大小和总请求大小（在 `application.yml` 和 controller 层各做一次校验）
- 只放行白名单内的文件扩展名
- 存储路径从配置文件读取，目录不存在时自动创建
- 返回文件访问 URL，不返回服务器本地路径

---

### 第 7 步：输出本轮前端交接包

**本轮切片接口实现完成后**，必须写入 `docs/slices/NN-[slice-name]/backend-handoff.md`，并把 curl 返回写入 `api-examples.md`。格式如下：

```markdown
## 前端交接包：[切片名称]

### 本轮状态
- 后端状态：已完成 / 部分完成 / 阻塞
- 覆盖故事：
- 依赖切片：
- 前端可以开始接入：是 / 否

### 变更记录
| 端点 | 蓝图设计 | 实际实现 | 变更理由 |
|------|---------|---------|---------|
| POST /api/xxx | 字段a 为 String | 字段a 为 Integer | [理由] |

> 如果和蓝图完全一致，写"与蓝图无差异"

### 本轮接口速查
[只列本轮切片新增或变更的端点，标注请求方法、路径、是否需要 Token]

### 数据库变更
- migration 文件：
- 新增/修改表：
- 关键索引/唯一约束：

### 请求/响应示例（实际跑过的）
[链接到 api-examples.md，包含至少 1 个成功样例和 1 个失败/校验样例]

### 测试结果
- `mvn test`：
- 关键单测：
- curl 冒烟：

### 前端接入建议
- 需要新增/修改的 api 文件：
- 需要新增/修改的 composable：
- 需要接入的页面/组件：
- loading/empty/error 状态建议：

### 验收标准
- [ ] [从蓝图切片复制或按实现修正]
- [ ] 前端接入后如发现字段/状态码不匹配，应反馈给后端修正

### 蓝图修正建议
- 是否需要更新 `docs/architecture.md`：
- 是否需要更新 `docs/openapi.yaml`：
- 详细修正写入：`blueprint-changes.md`

### 下一步
协作模式：等待前端完成本切片接入并反馈后，再继续下一个切片。
Solo 模式：切换到 frontend-builder 接入本切片，前端回执落盘后再继续。
```

这份交接包是前端实现本切片的直接依据。写完后更新 `docs/mvp-progress.md`：本切片后端状态改为 `backend_done`，联调状态保持 `pending` 或 `in_progress`。

如果实现中修正了蓝图/API 契约，必须同时更新：
- `docs/slices/NN-[slice-name]/blueprint-changes.md`
- `docs/openapi.yaml`（字段、路径、状态码变化时）
- `docs/architecture.md`（实体、流程、切片变化时）

---

## 代码质量标准

### 命名规范
- entity 类名 = 实体名（单数），表名 = 下划线复数
- Mapper 接口 = 实体名 + Mapper
- Service 接口 = 实体名 + Service
- Controller = 实体名 + Controller
- DTO = 操作名 + DTO（如 `CreateUserDTO`、`UpdateUserDTO`）
- VO = 实体名 + VO（如 `UserVO`）

### 每个模块的检查清单

写完一组接口后，逐项确认：

- [ ] 接口路径与蓝图一致（有差异的已记录到 API 文档）
- [ ] 响应结构为 `{ code, message, data }` 统一格式
- [ ] entity 的 password 等敏感字段不会序列化到响应中
- [ ] 入参有校验注解，异常返回中文提示
- [ ] 空列表返回 `[]` 而不是 null
- [ ] 用 curl 调通过一次
- [ ] 本轮 migration 已创建并与 entity 对齐
- [ ] `mvn test` 已执行，结果写入交接包
- [ ] 交接包已写入 `docs/slices/NN-[slice-name]/backend-handoff.md`
- [ ] `docs/mvp-progress.md` 已更新
- [ ] service 层不出现 `HttpServletRequest` / `HttpServletResponse`
- [ ] controller 层不写业务逻辑
