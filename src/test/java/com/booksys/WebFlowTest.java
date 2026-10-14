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
    private static final String FIXTURE = "BOOKSYS_INTEGRATION_FIXTURE";

    @BeforeAll
    static void start() throws Exception {
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
        sql.update("DELETE FROM t_borrow WHERE book_id=?", bookId);
        sql.update("UPDATE t_book SET stock=2 WHERE id=?", bookId);
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
                sql.update("DELETE FROM t_book WHERE id=?", bookId);
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
        assertThrows(IllegalStateException.class, () -> new BorrowServiceImpl().borrowBook(-1, bookId));
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

        HttpResponse<String> post(String path, Map<String, String> fields) throws Exception {
            String body = fields.entrySet().stream()
                    .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue())).collect(Collectors.joining("&"));
            return client.send(HttpRequest.newBuilder(URI.create(base + path))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).timeout(Duration.ofSeconds(15)).build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
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

