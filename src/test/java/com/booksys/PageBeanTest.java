package com.booksys;

import com.booksys.entity.PageBean;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PageBeanTest {
    @Test void emptyResultStillHasAValidFirstPage() {
        PageBean<Object> page = new PageBean<>(999, 8, 0);
        assertEquals(1, page.getPageNum());
        assertEquals(1, page.getTotalPages());
        assertEquals(0, page.getOffset());
        assertTrue(page.getList().isEmpty());
        page.setList(null);
        assertTrue(page.getList().isEmpty());
    }
    @Test void outOfRangeInputIsClampedBeforeCalculatingOffset() {
        PageBean<Object> last = new PageBean<>(Integer.MAX_VALUE, 8, 17);
        assertEquals(3, last.getPageNum());
        assertEquals(16, last.getOffset());
        assertEquals(1, new PageBean<>(-100, 8, 17).getPageNum());
        last.setTotalCount(1);
        assertEquals(1, last.getPageNum());
    }
    @Test void largeResultCountsDoNotOverflow() {
        PageBean<Object> page = new PageBean<>(Integer.MAX_VALUE, 100, Integer.MAX_VALUE);
        assertEquals(21_474_837, page.getTotalPages());
        assertEquals(2_147_483_600, page.getOffset());
        page.setPageSize(1);
        page.setPageNum(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE - 1, page.getOffset());
    }
    @Test void invalidLimitsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PageBean<>(1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new PageBean<>(1, 101, 1));
        assertThrows(IllegalArgumentException.class, () -> new PageBean<>(1, 8, -1));
    }
}
