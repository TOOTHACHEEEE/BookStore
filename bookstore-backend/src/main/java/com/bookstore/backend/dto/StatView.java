package com.bookstore.backend.dto;

import java.math.BigDecimal;

/**
 * 统计结果视图对象
 */
public class StatView {
    
    // 热销榜项
    public static class HotBook {
        private Long bookId;
        private String title;
        private String author;
        private Integer totalQuantity;  // 总销量
        private BigDecimal totalAmount; // 总金额
        
        public HotBook() {}
        
        public Long getBookId() { return bookId; }
        public void setBookId(Long bookId) { this.bookId = bookId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public Integer getTotalQuantity() { return totalQuantity; }
        public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    }
    
    // 消费榜项
    public static class TopUser {
        private String username;
        private Integer totalBooks;     // 购书总本数
        private BigDecimal totalAmount; // 购书总金额
        
        public TopUser() {}
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public Integer getTotalBooks() { return totalBooks; }
        public void setTotalBooks(Integer totalBooks) { this.totalBooks = totalBooks; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    }
    
    // 个人统计项
    public static class PersonalStat {
        private Long bookId;
        private String title;
        private Integer quantity;       // 该书购买数量
        private BigDecimal subtotal;    // 该书购买金额
        
        public PersonalStat() {}
        
        public Long getBookId() { return bookId; }
        public void setBookId(Long bookId) { this.bookId = bookId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    }
}