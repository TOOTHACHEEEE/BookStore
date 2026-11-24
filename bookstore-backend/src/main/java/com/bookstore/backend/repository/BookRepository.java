package com.bookstore.backend.repository;

import com.bookstore.backend.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 【数据访问层】Repository接口
 *
 * 【接口与实现分离】
 * 这是一个接口，不是实现类！
 * Spring Data JPA会在运行时自动创建实现类
 *
 * 【好处】
 * 1. 不用写SQL，方法名自动生成SQL
 * 2. 易于测试，可以用Mock替代
 * 3. 更换数据库时，只需更换JPA实现，不影响业务代码
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    // 【自动SQL生成】继承JpaRepository后自动获得以下方法：
    // - findAll()       → SELECT * FROM book
    // - findById(id)    → SELECT * FROM book WHERE id = ?
    // - save(entity)    → INSERT INTO book ... 或 UPDATE book ...
    // - deleteById(id)  → DELETE FROM book WHERE id = ?
    // - count()         → SELECT COUNT(*) FROM book

    // 【自定义查询方法】
    // Spring Data JPA会根据方法名自动生成SQL
    // 例如：findByTitle(String title) → SELECT * FROM book WHERE title = ?
    // 例如：findByAuthorContaining(String author) → SELECT * FROM book WHERE author LIKE '%author%'

    // 【作业2 新增】根据 ISBN 查询书籍，供智能助手 check_inventory 函数使用
    // Spring Data JPA 自动生成：SELECT * FROM book WHERE isbn = ?
    List<Book> findByIsbn(String isbn);
}