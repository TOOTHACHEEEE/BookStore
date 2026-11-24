package com.bookstore.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * 【实体层】Book实体类
 *
 * 【数据抽象】
 * Entity是数据库表在Java代码中的"对象映射"
 * 一张book表 → 一个Book.java类
 * 表的一行记录 → Book类的一个实例
 *
 * 【JPA注解说明】
 * @Entity 标记为JPA实体类，Spring启动时会扫描
 * @Table 指定对应的数据库表名
 * @Id 标记为主键字段
 * @GeneratedValue 主键生成策略：IDENTITY表示自增
 * @Column 标记数据库列的映射关系
 */
@Entity
@Table(name = "book")
public class Book {

    // 【主键】@Id标记这是主键
    // 【主键自增】@GeneratedValue(strategy = GenerationType.IDENTITY)
    // MySQL中通常使用AUTO_INCREMENT，对应IDENTITY策略
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 【列映射】@Column注解指定列名和约束
    // nullable = false 表示该列不能为空
    // length = 200 表示字符串最大长度200
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "author", length = 100)
    private String author;

    // 【精确数值】BigDecimal用于存储金额，避免浮点数精度丢失
    // precision = 10 表示总位数10位，scale = 2 表示小数点后2位
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "isbn", length = 50)
    private String isbn;

    @Column(name = "cover", length = 500)
    private String cover;

    // 【大文本】columnDefinition = "TEXT" 表示存储大文本内容
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "publisher", length = 100)
    private String publisher;

    @Column(name = "publish_date", length = 20)
    private String publishDate;

    @Column(name = "pages")
    private Integer pages;

    // 【默认值】stock = 100 表示库存默认100
    @Column(name = "stock", nullable = false)
    private Integer stock = 100;

    public Book() {
    }

    // 【Getter/Setter】JPA需要通过getter/setter访问私有字段
    // 也用于JavaBean规范

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(String publishDate) {
        this.publishDate = publishDate;
    }

    public Integer getPages() {
        return pages;
    }

    public void setPages(Integer pages) {
        this.pages = pages;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }
}