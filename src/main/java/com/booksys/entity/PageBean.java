package com.booksys.entity;

import java.util.Collections;
import java.util.List;

public class PageBean<T> {
    private int pageNum = 1;
    private int pageSize = 10;
    private int totalCount;
    private List<T> list = Collections.emptyList();

    public PageBean() {}
    public PageBean(int pageNum, int pageSize, int totalCount) {
        setPageSize(pageSize);
        setTotalCount(totalCount);
        setPageNum(pageNum);
    }

    public int getPageNum() { return pageNum; }
    public void setPageNum(int value) { pageNum = Math.max(1, Math.min(value, getTotalPages())); }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int value) {
        if (value < 1 || value > 100) throw new IllegalArgumentException("Page size must be between 1 and 100");
        pageSize = value;
        setPageNum(pageNum);
    }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int value) {
        if (value < 0) throw new IllegalArgumentException("Total count cannot be negative");
        totalCount = value;
        setPageNum(pageNum);
    }
    public int getTotalPages() { return Math.max(1, (int) (((long) totalCount + pageSize - 1) / pageSize)); }
    public int getOffset() { return (pageNum - 1) * pageSize; }
    public List<T> getList() { return list; }
    public void setList(List<T> value) { list = value == null ? Collections.emptyList() : value; }
}
