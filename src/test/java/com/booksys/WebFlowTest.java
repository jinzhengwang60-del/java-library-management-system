package com.booksys;

import com.booksys.dao.impl.BorrowDaoImpl;
import com.booksys.service.impl.BorrowServiceImpl;
import com.booksys.util.DbUtil;
import org.apache.catalina.Context;
import org.apache.catalina.Session;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.ByteArrayOutputStream;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Runs against an explicitly selected disposable MySQL database, never by default. */
@EnabledIfSystemProperty(named = "booksys.integration", matches = "true")
class WebFlowTest {
    private static Tomcat tomcat;
    private static Context context;
    private static String base;
    private static QueryRunner sql;
    private static int bookId;
    private static Path uploads;
    private static final String FIXTURE = "BOOKSYS_INTEGRATION_FIXTURE";

    @BeforeAll
    static void start() throws Exception {
        uploads = Files.createTempDirectory(Paths.get("target"), "image-test-").toAbsolutePath();
        System.setProperty("booksys.upload.dir", uploads.toString());
        sql = DbUtil.getQueryRunner();
        sql.update("INSERT INTO t_book(book_no,title,author,stock) VALUES(?,?,?,2)",
                FIXTURE, "Integration Fixture", "Fictional Test Author");
        bookId = number("SELECT id FROM t_book WHERE book_no=?", FIXTURE);
        tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory(Paths.get("target"), "tomcat-test-").toString());
        tomcat.setHostname("127.0.0.1");
        tomcat.setPort(0);
        tomcat.getConnector().setProperty("address", "127.0.0.1");
        context = tomcat.addWebapp("/booksys", Paths.get("src/main/webapp").toAbsolutePath().toString());
        context.setParentClassLoader(WebFlowTest.class.getClassLoader());
        ((org.apache.tomcat.util.scan.StandardJarScanner) context.getJarScanner()).setScanManifest(false);
        StandardRoot resources = new StandardRoot(context);
        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
                Paths.get("target/classes").toAbsolutePath().toString(), "/"));
        context.setResources(resources);
        tomcat.start();
        assertTrue(context.getState().isAvailable(), "Web application must start");
        base = "http://127.0.0.1:" + tomcat.getConnector().getLocalPort() + "/booksys";
    }

    @BeforeEach
    void resetFixture() throws Exception {
        sql.update("DROP TRIGGER IF EXISTS booksys_it_fail_update");
        sql.update("DELETE FROM t_book WHERE book_no LIKE ? AND id<>?", FIXTURE + "%", bookId);
        sql.update("DELETE FROM t_borrow WHERE book_id=?", bookId);
        sql.update("UPDATE t_book SET stock=2,status=1,version=0,title='Integration Fixture' WHERE id=?", bookId);
    }

    @AfterEach
    void removeFailureTrigger() throws Exception {
        sql.update("DROP TRIGGER IF EXISTS booksys_it_fail_update");
    }

    @AfterAll
    static void stop() throws Exception {
        try {
            if (tomcat != null) { tomcat.stop(); tomcat.destroy(); }
        } finally {
            if (sql != null) {
                sql.update("DROP TRIGGER IF EXISTS booksys_it_fail_update");
                sql.update("DELETE FROM t_borrow WHERE book_id=?", bookId);
                sql.update("DELETE FROM t_borrow WHERE book_id IN (SELECT id FROM t_book WHERE book_no LIKE ?)", FIXTURE + "%");
                sql.update("DELETE FROM t_book WHERE book_no LIKE ?", FIXTURE + "%");
                sql.update("DELETE FROM t_borrow WHERE user_id IN (SELECT id FROM t_user WHERE username LIKE 'booksys_it_%')");
                sql.update("DELETE FROM t_user WHERE username LIKE 'booksys_it_%'");
            }
            DbUtil.close();
        }
    }

    @Test void loginPageAndCaptchaRender() throws Exception {
        Browser browser = new Browser();
        assertEquals(200, browser.get("/login.jsp").statusCode());
        HttpResponse<byte[]> image = browser.client.send(HttpRequest.newBuilder(
                URI.create(base + "/verifyCode")).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, image.statusCode());
        assertTrue(image.body().length > 100);
        assertNotNull(browser.captcha());
    }

    @Test void wrongCaptchaIsRejected() throws Exception {
        Browser browser = new Browser();
        HttpResponse<String> response = browser.post("/login",
                Map.of("username", "test", "password", "123456", "verifyCode", "WRONG"));
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("验证码错误"));
        assertEquals(302, browser.get("/book/list").statusCode());
    }

    @Test void wrongPasswordIsRejected() throws Exception {
        Browser browser = new Browser();
        browser.get("/verifyCode");
        HttpResponse<String> response = browser.post("/login",
                Map.of("username", "test", "password", "incorrect-password", "verifyCode", browser.captcha()));
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("用户名或密码错误"));
    }

    @Test void authenticatedBookQueryRenders() throws Exception {
        Browser browser = new Browser();
        browser.login("test", false);
        HttpResponse<String> response = browser.get("/book/list");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Integration Fixture"), "First page should show the newly added fixture book");
        HttpResponse<String> search = browser.get("/book/list?keyword=Java");
        assertEquals(200, search.statusCode());
        assertTrue(search.body().contains("Java核心技术"));
    }

    @Test void anonymousBookQueryRequiresLogin() throws Exception {
        HttpResponse<String> response = new Browser().get("/book/list");
        assertEquals(302, response.statusCode());
        assertTrue(response.headers().firstValue("location").orElse("").endsWith("/login.jsp"));
    }

    @Test void registrationRegionsArePublic() throws Exception {
        HttpResponse<String> response = new Browser().get("/region?level=province");
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("content-type").orElse("").contains("json"));
        assertTrue(response.body().contains("河南省"));
    }

    @Test void borrowAndReturnUpdateRecordAndStockTogether() throws Exception {
        Browser browser = new Browser();
        browser.login("test", false);
        assertEquals(302, browser.post("/borrow/do", Map.of("bookId", Integer.toString(bookId))).statusCode());
        assertEquals(1, stock());
        int id = borrowId();
        assertEquals(0, number("SELECT status FROM t_borrow WHERE id=?", id));
        assertEquals(200, browser.get("/borrow/my").statusCode());
        assertEquals(302, browser.post("/borrow/return", Map.of("id", Integer.toString(id))).statusCode());
        assertEquals(2, stock());
        assertEquals(1, number("SELECT status FROM t_borrow WHERE id=?", id));
        assertEquals(302, browser.post("/borrow/return", Map.of("id", Integer.toString(id))).statusCode());
        assertEquals(2, stock(), "Repeated return must not create an extra copy");
    }

    @Test void anotherReaderCannotReturnSomeoneElsesBook() throws Exception {
        assertEquals("借阅成功", new BorrowServiceImpl().borrowBook(2, bookId));
        int id = borrowId();
        Browser other = new Browser();
        other.login("zhangsan", false);
        assertEquals(403, other.post("/borrow/return", Map.of("id", Integer.toString(id))).statusCode());
        assertEquals(0, number("SELECT status FROM t_borrow WHERE id=?", id));
        assertEquals(1, stock());
    }

    @Test void outOfStockDoesNotCreateBorrowRecord() throws Exception {
        sql.update("UPDATE t_book SET stock=0 WHERE id=?", bookId);
        assertEquals("图书已无库存", new BorrowServiceImpl().borrowBook(2, bookId));
        assertEquals(0, borrowCount());
        assertEquals(0, stock());
    }

    @Test void sameReaderCannotBorrowTwice() throws Exception {
        BorrowServiceImpl service = new BorrowServiceImpl();
        assertEquals("借阅成功", service.borrowBook(2, bookId));
        assertEquals("您已借阅此书且未归还", service.borrowBook(2, bookId));
        assertEquals(1, borrowCount());
        assertEquals(1, stock());
    }

    @Test void concurrentBorrowersCannotBothTakeLastCopy() throws Exception {
        sql.update("UPDATE t_book SET stock=1 WHERE id=?", bookId);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch gate = new CountDownLatch(1);
        try {
            Future<String> first = executor.submit(() -> { gate.await(); return new BorrowServiceImpl().borrowBook(2, bookId); });
            Future<String> second = executor.submit(() -> { gate.await(); return new BorrowServiceImpl().borrowBook(3, bookId); });
            gate.countDown();
            List<String> results = Arrays.asList(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertEquals(1, Collections.frequency(results, "借阅成功"));
            assertEquals(1, Collections.frequency(results, "图书已无库存"));
            assertEquals(1, borrowCount());
            assertEquals(0, stock());
        } finally { executor.shutdownNow(); }
    }

    @Test void borrowFailureRollsBackInsertedRecord() throws Exception {
        failStockUpdates();
        assertThrows(IllegalStateException.class, () -> new BorrowServiceImpl().borrowBook(2, bookId));
        assertEquals(0, borrowCount());
        assertEquals(2, stock());
    }

    @Test void returnFailureRollsBackReturnStatus() throws Exception {
        BorrowServiceImpl service = new BorrowServiceImpl();
        assertEquals("借阅成功", service.borrowBook(2, bookId));
        int id = borrowId();
        failStockUpdates();
        assertThrows(IllegalStateException.class, () -> service.returnBook(id));
        assertEquals(0, number("SELECT status FROM t_borrow WHERE id=?", id));
        assertEquals(1, stock());
    }

    @Test void invalidBorrowerDoesNotConsumeStock() throws Exception {
        assertEquals("账号不存在或已停用", new BorrowServiceImpl().borrowBook(-1, bookId));
        assertEquals(0, borrowCount());
        assertEquals(2, stock());
    }

    @Test void administratorBorrowListRenders() throws Exception {
        Browser browser = new Browser();
        browser.login("admin", false);
        assertEquals(200, browser.get("/borrow/list").statusCode());
        Browser reader = new Browser();
        reader.login("test", false);
        assertEquals(403, reader.get("/borrow/list").statusCode());
    }

    @Test void registeredDemoUserCanLogIn() throws Exception {
        Browser browser = new Browser();
        String name = "booksys_it_reader";
        HttpResponse<String> registered = browser.post("/register",
                Map.of("username", name, "password", "123456", "email", "integration@example.invalid"));
        assertEquals(200, registered.statusCode());
        assertTrue(registered.body().contains("注册成功"));
        browser.login(name, false);
        assertEquals(200, browser.get("/user/profile").statusCode());
    }

    @Test void rememberMeRestoresSession() throws Exception {
        Browser browser = new Browser();
        browser.login("test", true);
        for (HttpCookie cookie : new ArrayList<>(browser.cookies.getCookieStore().getCookies())) {
            if ("JSESSIONID".equals(cookie.getName()))
                browser.cookies.getCookieStore().remove(URI.create(base), cookie);
        }
        assertEquals(200, browser.get("/book/list").statusCode());
    }


    @Test void loginAcceptsTheTokenActuallyRenderedInTheForm() throws Exception {
        Browser browser = new Browser();
        String html = browser.get("/login.jsp").body();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(html);
        assertTrue(matcher.find(), "Login must render an anti-forgery form field");
        browser.get("/verifyCode");
        assertEquals(302, browser.postWithToken("/login", Map.of("username", "test", "password", "123456",
                "verifyCode", browser.captcha(), "_csrf", matcher.group(1)), null).statusCode());
    }

    @Test void missingAndTamperedCsrfTokensCannotChangeStock() throws Exception {
        Browser browser = new Browser(); browser.login("test", false);
        Map<String,String> borrow = Map.of("bookId", Integer.toString(bookId));
        assertEquals(403, browser.postWithToken("/borrow/do", borrow, null).statusCode());
        assertEquals(403, browser.postWithToken("/borrow/do", borrow, "forged").statusCode());
        assertEquals(2, stock()); assertEquals(0, borrowCount());
    }

    @Test void logoutRequiresAPostAndPreservesLoginOnGet() throws Exception {
        Browser browser = new Browser(); browser.login("test", true);
        assertEquals(405, browser.get("/logout").statusCode());
        assertEquals(200, browser.get("/book/list").statusCode());
        assertEquals(302, browser.post("/logout", Map.of()).statusCode());
        assertEquals(302, browser.get("/book/list").statusCode());
    }

    @Test void legacyPasswordIsUpgradedOnlyAfterSuccessfulLogin() throws Exception {
        String name = "booksys_it_legacy";
        int id = createUser(name, 0);
        String before = value("SELECT password FROM t_user WHERE id=?", id);
        assertTrue(before.matches("[0-9a-f]{32}"));
        assertNull(new com.booksys.service.impl.UserServiceImpl().login(name, "wrong"));
        assertEquals(before, value("SELECT password FROM t_user WHERE id=?", id));
        Browser browser = new Browser(); browser.login(name, false);
        assertTrue(value("SELECT password FROM t_user WHERE id=?", id).startsWith("pbkdf2$600000$"));
        browser.login(name, false);
    }

    @Test void rememberCookieIsSignedAndTamperingCannotRestoreLogin() throws Exception {
        Browser browser = new Browser(); browser.login("test", true);
        HttpCookie remember = browser.cookie("rememberMe");
        assertTrue(remember.isHttpOnly());
        assertTrue(remember.getValue().matches("[0-9]+\\.[0-9]+\\.[A-Za-z0-9_-]{43}"));
        remember.setValue("3" + remember.getValue().substring(1));
        browser.clearSession();
        assertEquals(302, browser.get("/book/list").statusCode());
    }

    @Test void passwordChangeInvalidatesAnExistingRememberCookie() throws Exception {
        String name = "booksys_it_password_change";
        int id = createUser(name, 0);
        Browser browser = new Browser(); browser.login(name, true);
        sql.update("UPDATE t_user SET password=? WHERE id=?", com.booksys.util.PasswordUtil.hash("changed-password"), id);
        browser.clearSession();
        assertEquals(302, browser.get("/book/list").statusCode());
    }

    @Test void invalidRegistrationAndDuplicateAccountAreRejected() throws Exception {
        Browser browser = new Browser();
        HttpResponse<String> bad = browser.post("/register", Map.of("username", "<script>", "password", "123456"));
        assertTrue(bad.body().contains("用户名"));
        assertEquals(0, number("SELECT COUNT(*) FROM t_user WHERE username=?", "<script>"));
        HttpResponse<String> duplicate = browser.post("/register", Map.of("username", "test", "password", "123456"));
        assertEquals(200, duplicate.statusCode());
        assertEquals(1, number("SELECT COUNT(*) FROM t_user WHERE username='test'"));
    }

    @Test void profileTextCannotEscapeHtmlOrInlineJavascript() throws Exception {
        int id = createUser("booksys_it_escape", 0);
        String name = "<script>alert('name')</script>";
        String province = "';alert('province');//";
        sql.update("UPDATE t_user SET real_name=?,province=?,address=? WHERE id=?",
                name, province, "\"><img src=x onerror=alert(1)>", id);
        Browser browser = new Browser(); browser.login("booksys_it_escape", false);
        HttpResponse<String> response = browser.get("/user/profile");
        assertEquals(200, response.statusCode());
        assertFalse(response.body().contains("<script>alert('name')</script>"));
        assertFalse(response.body().contains("const currentProvince = '" + province));
        assertFalse(response.body().contains("\"><img src=x onerror=alert(1)>"));
        assertTrue(response.body().contains("&lt;script&gt;"));
        assertTrue(response.body().contains("dataset.selected"));
    }

    @Test void bookTextIsEscapedInListDetailAndEditPages() throws Exception {
        String payload = "<img src=x onerror=alert(1)>";
        sql.update("UPDATE t_book SET title=?,description=? WHERE id=?", payload, payload, bookId);
        Browser admin = new Browser(); admin.login("admin", false);
        for (String route : new String[]{"/book/list", "/book/detail?id=" + bookId, "/book/edit?id=" + bookId}) {
            HttpResponse<String> response = admin.get(route);
            assertEquals(200, response.statusCode(), route);
            assertFalse(response.body().contains(payload), route);
            assertTrue(response.body().contains("&lt;img"), route);
        }
    }

    @Test void hugeAndNegativePageNumbersRenderAValidPage() throws Exception {
        Browser reader = new Browser(); reader.login("test", false);
        for (String page : new String[]{"-1", "2147483647", "99999999999999999999999"}) {
            assertEquals(200, reader.get("/book/list?pageNum=" + page).statusCode());
            assertEquals(200, reader.get("/borrow/my?pageNum=" + page).statusCode());
        }
        Browser admin = new Browser(); admin.login("admin", false);
        assertEquals(200, admin.get("/user/list?pageNum=2147483647").statusCode());
        assertEquals(200, admin.get("/borrow/list?pageNum=2147483647").statusCode());
    }

    @Test void paginationEncodesKeywordsInsteadOfAddingNewParameters() throws Exception {
        for (int i=0; i<9; i++) {
            sql.update("INSERT INTO t_book(book_no,title,author,stock) VALUES(?,?,?,1)",
                    FIXTURE + "_pager_" + i, "A&B Book " + i, "Test Author");
        }
        Browser reader = new Browser(); reader.login("test", false);
        HttpResponse<String> response = reader.get("/book/list?keyword=A%26B");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("keyword=A%26B"));
        assertFalse(response.body().contains("keyword=A&B"));
    }

    @Test void staleBookEditCannotOverwriteStockChangedByBorrowing() throws Exception {
        Browser admin = new Browser(); admin.login("admin", false);
        String edit = admin.get("/book/edit?id=" + bookId).body();
        assertTrue(edit.contains("name=\"version\" value=\"0\""));
        assertEquals("借阅成功", new BorrowServiceImpl().borrowBook(2, bookId));
        Map<String,String> fields = bookForm(); fields.put("title", "Stale overwrite");
        assertEquals(409, admin.post("/book/save", fields).statusCode());
        assertEquals(1, stock());
        assertEquals("Integration Fixture", value("SELECT title FROM t_book WHERE id=?", bookId));
    }

    @Test void validBookEditSucceedsButInvalidStockDoesNotReachTheDatabase() throws Exception {
        Browser admin = new Browser(); admin.login("admin", false);
        Map<String,String> fields = bookForm(); fields.put("stock", "-1");
        assertEquals(400, admin.post("/book/save", fields).statusCode());
        assertEquals(2, stock());
        fields.put("stock", "3"); fields.put("title", "Updated title");
        assertEquals(302, admin.post("/book/save", fields).statusCode());
        assertEquals(3, stock());
        assertEquals(1, number("SELECT version FROM t_book WHERE id=?", bookId));
        Map<String,String> duplicate = bookForm(); duplicate.remove("id"); duplicate.remove("version");
        assertEquals(400, admin.post("/book/save", duplicate).statusCode());
    }

    @Test void takingBookOffShelfPreservesLoansAndAllowsReturn() throws Exception {
        BorrowServiceImpl service = new BorrowServiceImpl();
        assertEquals("借阅成功", service.borrowBook(2, bookId));
        int loan = borrowId();
        Browser admin = new Browser(); admin.login("admin", false);
        assertEquals(302, admin.post("/book/delete", Map.of("id", Integer.toString(bookId))).statusCode());
        assertEquals(0, number("SELECT status FROM t_book WHERE id=?", bookId));
        assertEquals(1, borrowCount());
        assertNotEquals("借阅成功", service.borrowBook(3, bookId));
        assertTrue(service.returnBook(loan));
        assertEquals(2, stock());
        assertEquals(1, number("SELECT status FROM t_borrow WHERE id=?", loan));
    }

    @Test void disablingAccountPreservesHistoryAndRevokesActiveAndRememberedLogin() throws Exception {
        int id = createUser("booksys_it_disabled", 0);
        Browser reader = new Browser(); reader.login("booksys_it_disabled", true);
        assertEquals("借阅成功", new BorrowServiceImpl().borrowBook(id, bookId));
        Browser admin = new Browser(); admin.login("admin", false);
        assertEquals(302, admin.post("/user/delete", Map.of("id", Integer.toString(id))).statusCode());
        assertEquals(0, number("SELECT status FROM t_user WHERE id=?", id));
        assertEquals(1, number("SELECT COUNT(*) FROM t_borrow WHERE user_id=?", id));
        assertEquals(302, reader.get("/book/list").statusCode());
        reader.clearSession();
        assertEquals(302, reader.get("/book/list").statusCode());
        assertEquals("账号不存在或已停用", new BorrowServiceImpl().borrowBook(id, bookId));
        assertNull(new com.booksys.service.impl.UserServiceImpl().login("booksys_it_disabled", "123456"));
    }

    @Test void administratorsCannotDisableAnotherAdministrator() throws Exception {
        int id = createUser("booksys_it_second_admin", 1);
        Browser admin = new Browser(); admin.login("admin", false);
        assertEquals(409, admin.post("/user/delete", Map.of("id", Integer.toString(id))).statusCode());
        assertEquals(1, number("SELECT status FROM t_user WHERE id=?", id));
    }

    @Test void loggingOutOneSessionKeepsTheOtherSessionOnline() throws Exception {
        int id = createUser("booksys_it_two_sessions", 0);
        Browser first = new Browser(); Browser second = new Browser();
        first.login("booksys_it_two_sessions", false); second.login("booksys_it_two_sessions", false);
        assertEquals(1, onlineCount(id));
        assertEquals(200, first.get("/user/profile").statusCode());
        assertEquals(1, onlineCount(id), "Replacing session user data must not drop the online entry");
        assertEquals(302, first.post("/logout", Map.of()).statusCode());
        assertEquals(1, onlineCount(id));
        assertEquals(302, second.post("/logout", Map.of()).statusCode());
        assertEquals(0, onlineCount(id));
    }

    @Test void avatarUploadDecodesAndServesAnImageOutsideTheWebRoot() throws Exception {
        int id = createUser("booksys_it_image", 0);
        Browser browser = new Browser(); browser.login("booksys_it_image", false);
        byte[] image = ImageStoreTest.image(8, 8);
        assertEquals(302, browser.multipart("/user/update", Map.of(), "avatarFile",
                "arbitrary.jsp", "image/png", image, true).statusCode());
        String stored = value("SELECT avatar FROM t_user WHERE id=?", id);
        assertTrue(stored.matches("/upload/[0-9a-f]{32}\\.png"));
        assertTrue(Files.isRegularFile(uploads.resolve(stored.substring("/upload/".length()))));
        assertFalse(Files.exists(Paths.get("src/main/webapp").resolve(stored.substring(1))));
        HttpResponse<String> response = browser.get(stored);
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("content-type").orElse("").contains("image/png"));
        assertEquals("nosniff", response.headers().firstValue("x-content-type-options").orElse(""));
        assertEquals(404, browser.get("/upload/does-not-exist.jsp").statusCode());
    }

    @Test void fakeImageCannotBeSavedEvenWithAPngExtension() throws Exception {
        int id = createUser("booksys_it_fake_image", 0);
        Browser browser = new Browser(); browser.login("booksys_it_fake_image", false);
        String previous = value("SELECT avatar FROM t_user WHERE id=?", id);
        assertEquals(400, browser.multipart("/user/update", Map.of(), "avatarFile", "fake.png", "image/png",
                "<% out.print(\"must-not-execute\"); %>".getBytes(StandardCharsets.UTF_8), true).statusCode());
        assertEquals(previous, value("SELECT avatar FROM t_user WHERE id=?", id));
    }

    @Test void oversizedMultipartReturnsAUsefulClientError() throws Exception {
        Browser browser = new Browser(); browser.login("test", false);
        byte[] large = new byte[2 * 1024 * 1024 + 1];
        assertEquals(413, browser.multipart("/user/update", Map.of(), "avatarFile", "large.png", "image/png",
                large, true).statusCode());
        assertEquals(413, browser.multipart("/user/update", Map.of(), "avatarFile", "large.png", "image/png",
                large, false).statusCode());
    }

    @Test void failedBookSaveDoesNotLeaveAnOrphanedImage() throws Exception {
        Browser admin = new Browser(); admin.login("admin", false);
        Map<String,String> fields = bookForm();
        assertEquals("借阅成功", new BorrowServiceImpl().borrowBook(2, bookId));
        long before;
        try (java.util.stream.Stream<Path> files = Files.list(uploads)) { before = files.count(); }
        assertEquals(409, admin.multipart("/book/save", fields, "coverFile", "book.png", "image/png",
                ImageStoreTest.image(8, 8), true).statusCode());
        try (java.util.stream.Stream<Path> files = Files.list(uploads)) { assertEquals(before, files.count()); }
    }

    private static int createUser(String name, int role) throws Exception {
        sql.update("INSERT INTO t_user(username,password,role,status) VALUES(?,?,?,1)",
                name, com.booksys.util.MD5Util.encrypt("123456"), role);
        return number("SELECT id FROM t_user WHERE username=?", name);
    }
    private static String value(String query, Object... parameters) throws Exception {
        return sql.query(query, new ScalarHandler<String>(), parameters);
    }
    private static int onlineCount(int id) throws Exception {
        List<?> users = (List<?>) context.getServletContext().getAttribute("onlineUsers");
        int count = 0;
        if (users != null) for (Object user : users) {
            if (Integer.valueOf(id).equals(user.getClass().getMethod("getId").invoke(user))) count++;
            assertNull(user.getClass().getMethod("getPassword").invoke(user));
        }
        return count;
    }
    private static Map<String,String> bookForm() {
        return new HashMap<>(Map.of("id", Integer.toString(bookId), "version", "0", "bookNo", FIXTURE,
                "title", "Integration Fixture", "author", "Fictional Test Author", "stock", "2", "price", "0"));
    }

    private static void failStockUpdates() throws Exception {
        sql.update("CREATE TRIGGER booksys_it_fail_update BEFORE UPDATE ON t_book FOR EACH ROW "
                + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected integration test failure'");
    }

    private static int stock() throws Exception { return number("SELECT stock FROM t_book WHERE id=?", bookId); }
    private static int borrowCount() throws Exception { return number("SELECT COUNT(*) FROM t_borrow WHERE book_id=?", bookId); }
    private static int borrowId() throws Exception { return number("SELECT id FROM t_borrow WHERE book_id=?", bookId); }
    private static int number(String query, Object... parameters) throws Exception {
        Number number = sql.query(query, new ScalarHandler<Number>(), parameters);
        return number.intValue();
    }

    private static final class Browser {
        final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(5)).build();

        HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(URI.create(base + path)).GET().timeout(Duration.ofSeconds(15)).build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }

        String csrf() throws Exception {
            Optional<HttpCookie> cookie = cookies.getCookieStore().getCookies().stream()
                    .filter(value -> "JSESSIONID".equals(value.getName())).findFirst();
            if (cookie.isEmpty() || context.getManager().findSession(cookie.get().getValue()) == null) {
                get("/login.jsp");
                cookie = cookies.getCookieStore().getCookies().stream()
                        .filter(value -> "JSESSIONID".equals(value.getName())).findFirst();
            }
            Session session = context.getManager().findSession(cookie.orElseThrow().getValue());
            return (String) session.getSession().getAttribute("csrfToken");
        }

        HttpResponse<String> post(String path, Map<String, String> fields) throws Exception {
            return postWithToken(path, fields, csrf());
        }

        HttpResponse<String> postWithToken(String path, Map<String,String> fields, String token) throws Exception {
            String body = fields.entrySet().stream()
                    .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue())).collect(Collectors.joining("&"));
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + path));
            if (token != null) request.header("X-CSRF-Token", token);
            return client.send(request.header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).timeout(Duration.ofSeconds(15)).build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }

        HttpCookie cookie(String name) {
            return cookies.getCookieStore().getCookies().stream()
                    .filter(cookie -> name.equals(cookie.getName())).findFirst().orElseThrow();
        }
        void clearSession() {
            for (HttpCookie cookie : new ArrayList<>(cookies.getCookieStore().getCookies())) {
                if ("JSESSIONID".equals(cookie.getName())) cookies.getCookieStore().remove(URI.create(base), cookie);
            }
        }
        HttpResponse<String> multipart(String path, Map<String,String> fields, String field, String filename,
                                       String contentType, byte[] bytes, boolean tokenHeader) throws Exception {
            String boundary = "booksys-test-" + UUID.randomUUID();
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            Map<String,String> form = new HashMap<>(fields); form.put("_csrf", csrf());
            for (Map.Entry<String,String> entry : form.entrySet()) {
                body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + entry.getKey()
                        + "\"\r\n\r\n" + entry.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
            }
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + field
                    + "\"; filename=\"" + filename + "\"\r\nContent-Type: " + contentType + "\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            body.write(bytes);
            body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + path));
            if (tokenHeader) request.header("X-CSRF-Token", csrf());
            return client.send(request.header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                    .timeout(Duration.ofSeconds(15)).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }

        void login(String username, boolean remember) throws Exception {
            get("/verifyCode");
            Map<String, String> form = new HashMap<>();
            form.put("username", username); form.put("password", "123456");
            form.put("verifyCode", captcha());
            if (remember) form.put("remember", "on");
            assertEquals(302, post("/login", form).statusCode(), "Demo login should succeed");
        }

        String captcha() throws Exception {
            String id = cookies.getCookieStore().getCookies().stream()
                    .filter(cookie -> "JSESSIONID".equals(cookie.getName())).findFirst().orElseThrow().getValue();
            Session session = context.getManager().findSession(id);
            return (String) session.getSession().getAttribute("verifyCode");
        }

        static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    }
}

