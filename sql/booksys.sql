-- ===========================================
-- 图书管理系统 数据库脚本
-- 数据库：booksys
-- ===========================================

-- Initialize an empty demo database. Existing tables cause an error, never a reset.
CREATE DATABASE IF NOT EXISTS booksys DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE booksys;

-- 用户表
CREATE TABLE t_user (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码摘要：新用户 PBKDF2，兼容旧 MD5',
    real_name VARCHAR(50) COMMENT '真实姓名',
    email VARCHAR(100) UNIQUE COMMENT '邮箱',
    phone VARCHAR(20) UNIQUE COMMENT '手机号',
    gender VARCHAR(10) COMMENT '性别',
    province VARCHAR(50) COMMENT '省',
    city VARCHAR(50) COMMENT '市',
    district VARCHAR(50) COMMENT '区/县',
    address VARCHAR(200) COMMENT '详细地址',
    avatar VARCHAR(200) DEFAULT '/images/default-avatar.png' COMMENT '头像路径',
    role TINYINT DEFAULT 0 COMMENT '角色：0-普通用户，1-管理员',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间'
) ENGINE=InnoDB COMMENT='用户表';

-- 图书分类表
CREATE TABLE t_category (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    description VARCHAR(200) COMMENT '分类描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='图书分类表';

-- 图书表
CREATE TABLE t_book (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '图书ID',
    book_no VARCHAR(50) NOT NULL UNIQUE COMMENT '图书编号',
    title VARCHAR(100) NOT NULL COMMENT '书名',
    author VARCHAR(50) NOT NULL COMMENT '作者',
    publisher VARCHAR(100) COMMENT '出版社',
    publish_date DATE COMMENT '出版日期',
    isbn VARCHAR(30) COMMENT 'ISBN',
    category_id INT COMMENT '分类ID',
    price DECIMAL(10,2) COMMENT '价格',
    stock INT DEFAULT 0 COMMENT '库存数量',
    cover VARCHAR(200) DEFAULT '/images/default-book.png' COMMENT '封面路径',
    description TEXT COMMENT '图书简介',
    version INT NOT NULL DEFAULT 0 COMMENT '编辑版本号',
    click_count INT DEFAULT 0 COMMENT '点击次数',
    status TINYINT DEFAULT 1 COMMENT '状态：1-上架，0-下架',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    INDEX idx_book_status_click (status, click_count),
    FOREIGN KEY (category_id) REFERENCES t_category(id) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='图书表';

-- 借阅记录表
CREATE TABLE t_borrow (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '借阅ID',
    user_id INT NOT NULL COMMENT '用户ID',
    book_id INT NOT NULL COMMENT '图书ID',
    borrow_date DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '借阅时间',
    due_date DATETIME COMMENT '应还时间',
    return_date DATETIME COMMENT '实际归还时间',
    status TINYINT DEFAULT 0 COMMENT '状态：0-借阅中，1-已归还',
    INDEX idx_borrow_active (user_id, book_id, status),
    INDEX idx_borrow_user_date (user_id, borrow_date),
    FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES t_book(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='借阅记录表';

-- ===========================================
-- 初始化数据
-- ===========================================

-- 默认账号
-- 管理员: admin / 123456
-- 测试用户: test / 123456
-- 注意：密码已MD5加密 (123456 -> e10adc3949ba59abbe56e057f20f883e)
INSERT INTO t_user (username, password, real_name, email, phone, gender, province, city, district, role) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', '演示管理员', 'admin@example.invalid', NULL, NULL, NULL, NULL, NULL, 1),
('test', 'e10adc3949ba59abbe56e057f20f883e', '演示读者', 'reader@example.invalid', NULL, NULL, NULL, NULL, NULL, 0),
('zhangsan', 'e10adc3949ba59abbe56e057f20f883e', '虚构读者甲', 'reader-a@example.invalid', NULL, NULL, NULL, NULL, NULL, 0),
('lisi', 'e10adc3949ba59abbe56e057f20f883e', '虚构读者乙', 'reader-b@example.invalid', NULL, NULL, NULL, NULL, NULL, 0);

-- 图书分类
INSERT INTO t_category (name, description) VALUES
('计算机', '计算机科学与技术相关图书'),
('文学', '文学小说类图书'),
('科学', '自然科学类图书'),
('经管', '经济管理类图书'),
('教育', '教育考试类图书'),
('历史', '历史人文类图书');

-- 图书数据
INSERT INTO t_book (book_no, title, author, publisher, publish_date, isbn, category_id, price, stock, description, click_count) VALUES
('B001', 'Java核心技术 卷I', 'Cay S. Horstmann', '机械工业出版社', '2022-01-01', '9787111669425', 1, 149.00, 50, 'Java领域最有影响力的经典著作之一，深入讲解Java核心技术。', 158),
('B002', 'Java Web程序设计任务教程', '黑马程序员', '人民邮电出版社', '2023-03-15', '9787115580344', 1, 79.80, 30, '基于任务驱动的Java Web开发实战教程。', 92),
('B003', '深入理解计算机系统', 'Randal E. Bryant', '机械工业出版社', '2016-11-01', '9787111544937', 1, 139.00, 25, '从程序员的角度理解计算机系统的经典教材。', 215),
('B004', '算法导论', 'Thomas H. Cormen', '机械工业出版社', '2013-01-01', '9787111407010', 1, 128.00, 20, '算法领域的标准教材，覆盖了广泛的算法主题。', 187),
('B005', '红楼梦', '曹雪芹', '人民文学出版社', '1996-12-01', '9787020002207', 2, 59.70, 40, '中国古典文学四大名著之一。', 320),
('B006', '活着', '余华', '作家出版社', '2017-06-01', '9787506365437', 2, 36.00, 35, '余华的代表作品，讲述了福贵一生的故事。', 268),
('B007', '百年孤独', '加西亚·马尔克斯', '南海出版公司', '2017-06-01', '9787544253994', 2, 55.00, 28, '魔幻现实主义文学的代表作。', 195),
('B008', '时间简史', '史蒂芬·霍金', '湖南科学技术出版社', '2017-02-01', '9787535732309', 3, 45.00, 22, '探索宇宙起源与时间本质的科普名著。', 142),
('B009', '经济学原理', 'N.格里高利·曼昆', '北京大学出版社', '2020-08-01', '9787301314364', 4, 88.00, 18, '经济学入门的经典教材。', 165),
('B010', '货币金融学', '弗雷德里克·S·米什金', '中国人民大学出版社', '2016-01-01', '9787300223490', 4, 79.00, 15, '货币、银行与金融市场领域的权威教材。', 78),
('B011', '高等数学', '同济大学数学系', '高等教育出版社', '2014-07-01', '9787040396638', 5, 39.10, 60, '国内高校广泛使用的高等数学教材。', 425),
('B012', '线性代数', '同济大学数学系', '高等教育出版社', '2014-06-01', '9787040396614', 5, 29.50, 55, '高校线性代数经典教材。', 287),
('B013', '明朝那些事儿', '当年明月', '北京联合出版公司', '2018-04-01', '9787550218987', 6, 358.00, 12, '通俗易懂的明朝历史读物。', 312),
('B014', '万历十五年', '黄仁宇', '中华书局', '2014-08-01', '9787101025422', 6, 25.00, 20, '从1587年看明王朝兴衰的史学名著。', 156),
('B015', 'Spring实战（第6版）', 'Craig Walls', '人民邮电出版社', '2022-03-01', '9787115582478', 1, 119.00, 35, 'Spring Framework权威指南。', 198);

-- 借阅记录（示例数据）
INSERT INTO t_borrow (user_id, book_id, borrow_date, due_date, return_date, status) VALUES
(2, 1, '2026-05-01 10:00:00', '2026-06-01 10:00:00', '2026-05-28 14:30:00', 1),
(2, 5, '2026-05-15 14:00:00', '2026-06-15 14:00:00', NULL, 0),
(3, 3, '2026-06-01 09:00:00', '2026-07-01 09:00:00', NULL, 0),
(4, 7, '2026-06-10 11:00:00', '2026-07-10 11:00:00', NULL, 0);
