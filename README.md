# JavaWeb 图书管理系统

使用 **JSP、Servlet、JDBC 和 MySQL** 的图书管理课程项目。提供读者与管理员两种角色，支持注册登录、图书查询、借阅归还和后台管理。

优化版本保留原项目的分层结构，补全借还书事务、库存并发保护、历史记录保留、密码升级和安全图片上传。仓库只包含源码、必要资源、虚构演示数据和验证代码。

## 功能与结构

- 注册、验证码登录、个人资料、头像上传和记住登录状态。
- 图书分页查询、关键词搜索、分类筛选、详情页与热门排行。
- 借阅、归还、个人借阅记录、库存控制和重复借阅检查。
- 管理员图书与用户管理、全部借阅记录、在线用户列表。

~~~text
src/main/java/com/booksys/
├── entity/        用户、图书、分类、借阅记录及分页对象
├── dao/           数据访问接口与实现：DBUtils + C3P0
├── service/       登录、图书管理和借还书业务
├── servlet/       HTTP 路由
├── filter/        登录、账号状态、表单凭证与编码处理
├── listener/      在线用户记录与连接池关闭
└── util/          数据库配置与事务、密码、图片及表单工具
src/main/webapp/   JSP、JSTL、样式与默认图标
sql/booksys.sql   建表与虚构演示数据
config/           数据库配置示例
src/test/java/    单元测试与 MySQL + Tomcat 集成测试
sql/migrations/   已有数据库的增量升级脚本
~~~

~~~mermaid
flowchart LR
    Browser[浏览器] --> Filter[登录、表单凭证与编码过滤器]
    Filter --> Servlet[Servlet]
    Servlet --> Service[Service 业务层]
    Service --> DAO[DAO / DBUtils]
    DAO --> DB[(MySQL / C3P0)]
    Servlet --> JSP[JSP 页面]
    JSP --> Browser
~~~

## 本地运行

需要 **JDK 11 或以上、Maven 3.6.3 或以上、MySQL 8.0、Tomcat 9**。项目使用 javax.servlet，请部署到 Tomcat 9。

### 1. 初始化新的演示数据库

在仓库根目录打开 MySQL 客户端：

~~~text
mysql -h 127.0.0.1 -u root -p --default-character-set=utf8mb4
~~~

执行：

~~~sql
SOURCE sql/booksys.sql;
~~~

脚本创建 booksys 数据库、4 张 InnoDB 表、4 个演示账号、6 个分类、15 本图书及示例借阅记录。脚本不会删除已有数据库；如果同名表已存在，初始化会报错，请使用空的演示数据库。

为应用创建仅用于这个数据库的账号，将密码占位符换成自己的本地密码：

~~~sql
CREATE USER 'booksys_app'@'127.0.0.1' IDENTIFIED BY 'YOUR_LOCAL_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE ON booksys.* TO 'booksys_app'@'127.0.0.1';
~~~

已有初始发布版本数据库时，不要重复导入初始化脚本。先备份并停止应用，再执行 `SOURCE sql/migrations/001_harden_schema.sql;`，然后部署新 WAR。升级只扩展密码字段、增加版本列和索引，保留业务数据；详细步骤和旧上传图片的处理见 [代码理解与优化说明](docs/optimization.md)。

### 2. 设置私有数据库配置

Windows PowerShell：

~~~powershell
Copy-Item .\config\jdbc.example.properties .\jdbc-local.properties
# 编辑 jdbc-local.properties，填写自己的数据库连接信息。
$env:BOOKSYS_CONFIG = (Resolve-Path .\jdbc-local.properties).Path
~~~

Linux / macOS：

~~~bash
cp config/jdbc.example.properties jdbc-local.properties
# 编辑配置后：
export BOOKSYS_CONFIG="$(pwd)/jdbc-local.properties"
~~~

jdbc-local.properties 已被 .gitignore 排除，不会打包到 WAR。也可以设置 BOOKSYS_DB_URL、BOOKSYS_DB_USER、BOOKSYS_DB_PASSWORD；环境变量优先于文件中的值。配置示例用于本地演示，部署到其他环境时请自行配置连接地址与 TLS 参数。

头像与封面保存在 WAR 外。可在私有文件中设置 `upload.directory=C:/booksys-data/uploads`，或设置 `BOOKSYS_UPLOAD_DIR`；默认使用 Tomcat 的 `catalina.base/booksys-data/uploads`。

“记住我”默认使用每次启动生成的签名密钥，重启后需要重新登录。需要跨重启保留时，在私有文件中设置 `remember.key`，值为至少 32 字节随机数的 Base64 编码，也可用 `BOOKSYS_REMEMBER_KEY`。密钥不要提交到 Git。

### 3. 编译并部署

~~~text
mvn package
~~~

生成 target/booksys.war。复制到 Tomcat 9 的 webapps 目录，从设置好 BOOKSYS_CONFIG 的同一终端启动 Tomcat，让新进程继承配置。

浏览器访问：

~~~text
http://localhost:8080/booksys/
~~~

### 4. 演示账号

账号均为虚构示例，密码统一为 123456。

| 账号 | 角色 |
|---|---|
| admin | 管理员 |
| test | 普通读者 |
| zhangsan | 普通读者 |
| lisi | 普通读者 |

数据使用 example.invalid 邮箱，不包含真实手机号、学号或个人住址。

## 验证

默认 mvn package 会执行 17 项不依赖数据库的单元测试、编译并生成 WAR，**MySQL 集成测试默认跳过**，避免意外连接或改动使用者的数据库。

完整验证需要独立的 MySQL 演示数据库，先导入上面的脚本，并赋予测试账号该数据库的完整权限。测试创建专用测试书籍、虚构读者和故障注入触发器，结束后清理这些记录。不要把测试指向真实业务数据库。

设置好 BOOKSYS_CONFIG 后执行：

~~~text
mvn "-Dbooksys.integration=true" verify
~~~

或者指定私有配置的绝对路径：

~~~text
mvn "-Dbooksys.integration=true" "-Dbooksys.config=/absolute/path/jdbc-local.properties" verify
~~~

测试自动启动只监听本机、使用随机端口的嵌入式 Tomcat。验证码测试直接读取测试服务器内存中的 Session，没有新增绕过验证码的公开接口。

完整验证包含 **17 项单元测试 + 38 项 HTTP/MySQL 集成测试，共 55 项**：覆盖密码与签名、分页边界、图片解码和限制、表单校验、登录注册、权限、借还书并发与回滚，以及旧编辑页面、CSRF、页面转义、停用账号、下架图书、多会话在线状态等回归场景。

已在 **Windows / JDK 11 / MySQL 8.0 / 嵌入式 Tomcat 9.0.123** 上实际运行并通过全部 55 项测试，WAR 打包成功。旧结构迁移到新结构的脚本也已在独立测试库中连续执行两次，并验证所有原字段数据保持一致。详见 [验证记录](docs/validation.md)。

GitHub Actions 配置了 Java 11 / 17 与独立 MySQL 容器；本地本轮结果不代表尚未执行的新提交远端结果。

## 代码理解与主要改进

- 借还书使用统一事务和行锁；编辑图书检查版本，防止旧页面覆盖新库存。
- 图书下架、读者停用保留借阅历史，下架图书仍可归还。
- 新密码使用 PBKDF2-HMAC-SHA256，旧 MD5 在正确登录后自动升级。
- 记住登录状态使用七天有效的签名凭证；密码摘要不进入 Cookie。
- POST 表单校验会话凭证，页面输出转义，退出登录使用 POST。
- 图片实际解码后重新编码，随机命名并保存在 WAR 外，失败保存清理本次新文件。
- 共用分页组件和配置读取，修正页码溢出与多会话在线统计。
- 私有配置、IDE 设置、编译产物、依赖 JAR 和原课程压缩包均不入库。

阅读 [代码理解与优化说明](docs/optimization.md) 可以从一次借书的数据流开始理解源码，并查看具体路径、升级方法和验证边界。

## 参考文档

- [Apache Tomcat 9](https://tomcat.apache.org/tomcat-9.0-doc/)
- [Apache Commons DBUtils](https://commons.apache.org/proper/commons-dbutils/)
- [MySQL 8.0](https://dev.mysql.com/doc/refman/8.0/en/)

