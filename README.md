# JavaWeb 图书管理系统

使用 **JSP、Servlet、JDBC 和 MySQL** 的图书管理课程项目。提供读者与管理员两种角色，支持注册登录、图书查询、借阅归还和后台管理。

发布版本保留原项目的分层结构，整理了构建与配置方式，并修正借还书的事务一致性、归还权限和注册页面地址接口问题。仓库只包含源码、必要资源、虚构演示数据和验证代码。

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
├── filter/        登录检查与编码处理
├── listener/      在线用户记录与连接池关闭
└── util/          数据库配置、验证码等工具
src/main/webapp/   JSP、JSTL、样式与默认图标
sql/booksys.sql   建表与虚构演示数据
config/           数据库配置示例
src/test/java/    MySQL + Tomcat 集成测试
~~~

~~~mermaid
flowchart LR
    Browser[浏览器] --> Filter[登录与编码过滤器]
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

默认 mvn package 会编译并生成 WAR，**MySQL 集成测试默认跳过**，避免意外连接或改动使用者的数据库。

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

17 项测试覆盖登录、注册、地址接口、权限、图书查询与搜索、正常借还书、重复借阅、无库存、越权归还、最后一本书的并发借阅，以及借阅或归还失败时的事务回滚。

已在 **Windows / JDK 11 / MySQL 8.0 / 嵌入式 Tomcat 9.0.123** 上实际运行并通过全部 17 项测试，详见 [验证记录](docs/validation.md)。GitHub Actions 会使用独立 MySQL 容器复跑测试，其他环境的结果以具体运行记录为准。

## 发布版本的调整

- 数据库密码改为外部私有文件或环境变量配置。
- 借阅与归还在同一事务中更新记录和库存，对竞争同一本书的请求使用行锁。
- 普通读者只能归还自己的借阅记录，管理员可以管理全部记录。
- 注册页面的地址选择接口允许未登录访问。
- 应用停止时关闭数据库连接池。
- 不提交 IDE 设置、编译产物、依赖 JAR、原始提交压缩包和课程报告。

身份验证仍沿用课程实现的 MD5 摘要与记住登录状态机制。本次未将它改造为生产级认证系统，也未对上传文件和全站 CSRF 防护进行专项验证。

## 参考文档

- [Apache Tomcat 9](https://tomcat.apache.org/tomcat-9.0-doc/)
- [Apache Commons DBUtils](https://commons.apache.org/proper/commons-dbutils/)
- [MySQL 8.0](https://dev.mysql.com/doc/refman/8.0/en/)

