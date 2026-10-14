package com.booksys.entity;

import java.util.List;

/**
 * 分页工具类（封装分页参数和数据）
 */
public class PageBean<T> {
    private int pageNum;        // 当前页
    private int pageSize;       // 每页大小
    private int totalCount;     // 总记录数
    private int totalPages;     // 总页数
    private List<T> list;       // 当前页数据

    public PageBean() {}

    public PageBean(int pageNum, int pageSize, int totalCount) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
        this.totalPages = (totalCount + pageSize - 1) / pageSize;
    }

    public int getPageNum() { return pageNum; }
    public void setPageNum(int pageNum) { this.pageNum = pageNum; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
        this.totalPages = (totalCount + pageSize - 1) / pageSize;
    }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public List<T> getList() { return list; }
    public void setList(List<T> list) { this.list = list; }
}
