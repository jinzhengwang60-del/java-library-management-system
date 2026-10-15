-- Upgrade the initial release without deleting users, books or borrowing records.
USE booksys;
ALTER TABLE t_user MODIFY COLUMN password VARCHAR(255) NOT NULL COMMENT 'Password hash';
SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='t_book' AND column_name='version'),
    'SELECT 1', 'ALTER TABLE t_book ADD COLUMN version INT NOT NULL DEFAULT 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='t_book' AND index_name='idx_book_status_click'),
    'SELECT 1', 'ALTER TABLE t_book ADD INDEX idx_book_status_click(status,click_count)');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='t_borrow' AND index_name='idx_borrow_active'),
    'SELECT 1', 'ALTER TABLE t_borrow ADD INDEX idx_borrow_active(user_id,book_id,status)');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='t_borrow' AND index_name='idx_borrow_user_date'),
    'SELECT 1', 'ALTER TABLE t_borrow ADD INDEX idx_borrow_user_date(user_id,borrow_date)');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
