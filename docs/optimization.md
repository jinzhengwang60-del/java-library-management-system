# 代码理解与优化说明

本轮修改针对独立发布副本，保留 JSP / Servlet / Service / DAO 结构，运行环境仍为 Java 11+、Tomcat 9、MySQL 8.0。原课程项目不在修改范围内。

## 先理解它是怎么工作的

可以把这个系统理解为一个图书馆前台：

- JSP 是浏览器看到的窗口，负责显示图书、表单和提示。
- Servlet 负责接收请求，识别用户想查询、借阅、归还还是修改资料。
- Service 负责执行规则，例如不能重复借同一本未归还的书。
- DAO 负责用带参数的 SQL 读写数据库；C3P0 管理连接的借出与归还。
- Filter 先检查登录状态和表单凭证，Listener 维护在线用户与应用停止时的清理工作。

主要代码位于 `src/main/java/com/booksys/`：

| 入口或模块 | 路径 | 作用 |
|---|---|---|
| 登录、注册 | `servlet/LoginServlet.java`、`servlet/RegisterServlet.java` | 验证码、密码校验、新账号创建 |
| 登录与请求保护 | `filter/AuthFilter.java`、`filter/CsrfFilter.java` | 登录检查、账号状态刷新、阻止伪造表单请求 |
| 图书管理 | `servlet/BookServlet.java`、`service/impl/BookServiceImpl.java`、`dao/impl/BookDaoImpl.java` | 搜索、分页、新增、编辑、下架 |
| 借阅归还 | `servlet/BorrowServlet.java`、`service/impl/BorrowServiceImpl.java` | 校验归还权限，事务更新借阅记录和库存 |
| 用户管理 | `servlet/UserServlet.java`、`service/impl/UserServiceImpl.java` | 个人资料、管理员停用账号 |
| 数据库基础 | `util/AppConfig.java`、`util/DbUtil.java` | 外部配置、连接池、统一事务执行 |
| 文件上传 | `util/ImageStore.java`、`servlet/UploadServlet.java` | 验证图片、重新编码、外部保存、按固定规则读取 |
| 页面 | `src/main/webapp/WEB-INF/views/` | 图书、借阅和用户页面，共享导航与分页 |

数据库有四张主要表：`t_user` 保存账号，`t_category` 保存分类，`t_book` 保存图书和可借库存，`t_borrow` 连接用户与图书并保存借还时间、状态。`stock` 表示当前可借数量，不是含借出部分的总馆藏量。

一次借书的流程是：

1. 浏览器提交图书 ID 和当前会话的 `_csrf` 表单凭证。
2. 过滤器检查登录、账号状态和表单凭证；Servlet 从登录会话取得用户 ID。
3. Service 开启事务，读取并锁住用户状态和对应图书。
4. 确认图书在架、有库存、该用户没有未归还的同书借阅。
5. 在同一连接上插入借阅记录、扣减库存并增加图书版本号，最后提交。
6. 任一步骤抛出异常就回滚，页面跳转后显示处理结果。

归还时先校验记录归属，再锁住借阅记录，在同一事务内标记归还、增加库存和版本号。重复归还不会重复增加库存。

## 这轮具体改了什么

### 1. 避免旧编辑页面覆盖新库存

原先管理员打开编辑页面时看到库存 2；如果这时读者借走一本，管理员再保存旧页面，就可能把数据库中的 1 改回 2。

现在 `t_book` 增加 `version`。编辑表单带上读取时的版本，保存时使用 `WHERE id=? AND version=?`。借书、还书、编辑和下架都会增加版本。旧版本保存返回 409，要求刷新，库存不会被覆盖。

关键路径：`entity/Book.java`、`dao/impl/BookDaoImpl.java`、`service/impl/BorrowServiceImpl.java`、`servlet/BookServlet.java`、`WEB-INF/views/book_edit.jsp`。

### 2. 保留借阅历史

原先删除图书或用户会触发数据库级联删除借阅记录。现在图书操作改为下架，用户操作改为停用，记录和待归还图书继续保留。下架图书不能继续借出，但已借图书仍可归还；停用读者可由管理员处理其归还。

每次访问受保护路由时重新读取账号状态，已经登录的停用账号也会失效。管理员账号不能通过当前停用接口被停用。当前页面提供下架、停用入口；没有新增恢复上架和启用账号的管理界面。

关键路径：`dao/impl/BookDaoImpl.java`、`dao/impl/UserDaoImpl.java`、`filter/AuthFilter.java`、`servlet/UserServlet.java`。

### 3. 改善密码与“记住我”

新密码使用 Java 自带 PBKDF2-HMAC-SHA256，600,000 次迭代、独立随机盐。旧 MD5 摘要仍可校验，只有正确登录后才更新为新格式，错误密码不会触发升级；并发升级通过条件更新避免相互覆盖。

“记住我” Cookie 只包含用户 ID、有效期和 HMAC 签名，有效期七天。密码摘要不再放入 Cookie；修改密码会使旧凭证失效。Cookie 使用 HttpOnly、SameSite=Lax，在 HTTPS 请求下增加 Secure。显式登录和持久登录恢复时更新 Session ID。

签名密钥默认每次应用启动随机生成，因此重启后原有“记住我”会失效。如需跨重启保留，可在私有配置中设置至少 32 字节随机数的 Base64 编码 `remember.key`，或使用 `BOOKSYS_REMEMBER_KEY`。不同实例共享登录凭证时必须使用同一密钥；不要把密钥提交到仓库。

关键路径：`util/PasswordUtil.java`、`util/RememberToken.java`、`filter/RememberMeHelper.java`、`service/impl/UserServiceImpl.java`。

### 4. 规范表单、页面输出和图片上传

- 写操作检查会话中的 `_csrf`，也支持 `X-CSRF-Token` 请求头。所有现有 POST 表单都加入凭证，退出登录改为 POST。
- 用户输入显示到 HTML 时转义；地区选择使用经过转义的 `data-selected`，避免把资料拼进 JavaScript 字符串。
- 图书金额、数量、日期和字段长度在服务器校验，数据库重复键转为可读提示。服务器内部异常不再显示堆栈。
- 上传不信任文件扩展名或 Content-Type；实际解码 PNG/JPEG/GIF 后重新编码为随机名称的 PNG。
- 头像最多 2 MiB，封面最多 5 MiB；限制图片尺寸与像素数，超过 1024 的长边会缩小。GIF 只保留第一帧。
- 图片保存在 WAR 外，通过只接受固定随机 PNG 名称的路由读取；上传的 JSP 内容不能作为程序执行。
- 上传过大返回 413，内容不合法返回 400。保存冲突或失败时清理本次新上传文件。

关键路径：`filter/CsrfFilter.java`、`util/FormValues.java`、`util/ImageStore.java`、`servlet/UploadServlet.java`、各 JSP。

### 5. 减少重复代码和边界错误

- `DbUtil.transaction(...)` 统一连接、提交、回滚和资源释放，借书、还书使用同一套事务规则。
- `PageBean` 统一检查页码、每页数量和总数，限制越界页码并避免偏移量整数溢出。
- 四个列表使用 `_pagination.jsp`，最多显示附近五个页码；关键词通过 URL 参数编码，`A&B` 不会变成额外参数。
- 在线列表按会话记录、按账号去重，同一账号关闭一个会话后，另一个会话仍显示在线；资料刷新不会丢掉在线状态。
- 增加图书状态与点击量、借阅状态查询、个人借阅时间排序的索引。

这些改变改善正确性和维护成本。本轮没有做吞吐量或大数据基准测试，不据此声称系统已经提速多少；`LIKE '%关键词%'` 搜索仍是原来的实现。

## 使用旧数据库前的升级步骤

空数据库直接导入 `sql/booksys.sql`。

已经使用初始发布版本的数据库，应先备份数据库和已有上传目录，在停止应用后执行：

~~~sql
SOURCE sql/migrations/001_harden_schema.sql;
~~~

该脚本只扩展密码字段、增加图书版本列和索引，不删除或重建业务表，可以重复执行。验证使用初始发布提交的旧结构，在独立测试库中连续执行两次，四张表的所有原字段值及行数均一致。

应用仍使用 `BOOKSYS_CONFIG`、`BOOKSYS_DB_URL` 等已有配置。图片目录可以指定为：

~~~properties
# Windows paths in .properties should use forward slashes.
upload.directory=C:/booksys-data/uploads
~~~

或者设置 `BOOKSYS_UPLOAD_DIR`；默认是 Tomcat 的 `catalina.base/booksys-data/uploads`。目录应允许应用账号读写，不要放在 webapps 下。

如果旧部署曾保存过实际上传图片，旧上传目录的文件不会自动导入新图片存储。应在切换前备份，并通过新上传入口重新保存所需图片、更新引用；本轮迁移测试覆盖数据库，不包含旧媒体批量迁移。仓库的演示数据使用默认图片，不依赖旧媒体。

## 实际验证与后续边界

本机 Windows / JDK 11 / MySQL 8.0 / Tomcat 9.0.123 上完成 **17 项单元测试和 38 项真实 HTTP / MySQL 集成测试，55 项全部通过**。数据库迁移单独完成两次重跑与逐行数据校验，WAR 打包成功。详情见 [validation.md](validation.md)。

继续采用课程项目的技术栈，没有替换框架或增加应用依赖。当前没有新增密码找回、登录限速、操作审计、旧媒体导入或文件存储清理任务；没有执行生产压力测试和完整浏览器视觉验收。在线列表保存在当前 JVM 内存中，多实例部署时需要另行设计共享状态。
