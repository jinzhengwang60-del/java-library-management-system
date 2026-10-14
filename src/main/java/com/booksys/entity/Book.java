package com.booksys.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 图书实体类
 */
public class Book {
    private Integer id;
    private String bookNo;
    private String title;
    private String author;
    private String publisher;
    private Timestamp publishDate;
    private String isbn;
    private Integer categoryId;
    private String categoryName;   // 关联查询字段
    private BigDecimal price;
    private Integer stock;
    private String cover;
    private String description;
    private Integer clickCount;
    private Integer status;
    private Timestamp createTime;

    public Book() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getBookNo() { return bookNo; }
    public void setBookNo(String bookNo) { this.bookNo = bookNo; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public Timestamp getPublishDate() { return publishDate; }
    public void setPublishDate(Timestamp publishDate) { this.publishDate = publishDate; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getCover() { return cover; }
    public void setCover(String cover) { this.cover = cover; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getClickCount() { return clickCount; }
    public void setClickCount(Integer clickCount) { this.clickCount = clickCount; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Timestamp getCreateTime() { return createTime; }
    public void setCreateTime(Timestamp createTime) { this.createTime = createTime; }
}
