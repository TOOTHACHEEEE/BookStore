package com.bookstore.backend.service;

import com.bookstore.backend.dto.ApiResponse;
import com.bookstore.backend.dto.BookRequest;

/**
 * 【Service接口】书籍服务接口
 * 
 * 【接口与实现分离】
 * 此接口定义书籍服务的方法签名，具体实现由BookServiceImpl提供
 * 
 * 【设计好处】
 * 1. Controller依赖接口，不依赖具体实现
 * 2. 可以轻松替换实现（如添加缓存、切换数据源等）
 * 3. 便于单元测试，可以Mock接口
 */
public interface BookService {
    
    /**
     * 获取所有书籍列表
     */
    ApiResponse getAllBooks();
    
    /**
     * 根据ID获取书籍详情
     */
    ApiResponse getBookById(Long id);
    
    /**
     * 添加新书籍（管理员功能）
     */
    ApiResponse addBook(BookRequest request);
    
    /**
     * 更新书籍信息（管理员功能）
     */
    ApiResponse updateBook(Long id, BookRequest request);
    
    /**
     * 删除书籍（管理员功能）
     */
    ApiResponse deleteBook(Long id);
}