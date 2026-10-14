package com.booksys.entity;

import java.sql.Timestamp;

/**
 * 图书分类实体类
 */
public class Category {
    private Integer id;
    private String name;
    private String description;
    private Timestamp createTime;

    public Category() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Timestamp getCreateTime() { return createTime; }
    public void setCreateTime(Timestamp createTime) { this.createTime = createTime; }
}
