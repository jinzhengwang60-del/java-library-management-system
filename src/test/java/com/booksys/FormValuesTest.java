package com.booksys;

import com.booksys.util.FormValues;
import com.booksys.entity.Book;
import org.junit.jupiter.api.Test;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FormValuesTest {
    private Map<String,String> form() {
        return new HashMap<>(Map.of("bookNo", "IT-1", "title", "Title", "author", "Author",
                "price", "10.50", "stock", "2", "categoryId", "1"));
    }
    private Book parse(Map<String,String> values) {
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> "getParameter".equals(method.getName()) ? values.get(args[0]) : null);
        return FormValues.book(request);
    }
    @Test void normalBookFieldsAreParsed() {
        Map<String,String> form = form(); form.put("publishDate", "2024-02-29");
        assertEquals(2, parse(form).getStock());
        assertEquals(java.time.LocalDate.of(2024, 2, 29),
                new java.sql.Timestamp(parse(form).getPublishDate().getTime()).toLocalDateTime().toLocalDate());
    }
    @Test void invalidNumbersDatesAndEmptyRequiredFieldsAreRejected() {
        for (String[] entry : new String[][] {{"stock", "-1"}, {"stock", "1.5"},
                {"stock", "2147483648"}, {"price", "-0.01"}, {"price", "1.999"},
                {"price", "NaN"}, {"publishDate", "2023-02-29"}, {"title", "   "},
                {"categoryId", "-1"}, {"title", "x".repeat(201)}}) {
            Map<String,String> form = form(); form.put(entry[0], entry[1]);
            assertThrows(IllegalArgumentException.class, () -> parse(form), Arrays.toString(entry));
        }
    }
    @Test void OptionalWhitespaceBecomesNull() {
        assertNull(FormValues.text("  ", "Optional", 50, false));
        assertEquals("text", FormValues.text(" text ", "Optional", 50, false));
    }
}
