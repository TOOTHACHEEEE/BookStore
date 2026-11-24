package com.bookstore.backend.controller;

import com.bookstore.backend.dto.ApiResponse;
import com.bookstore.backend.dto.BookRequest;
import com.bookstore.backend.service.BookService;
import com.bookstore.backend.util.AuthUtil;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class BookController {

    private static final Logger log = LoggerFactory.getLogger(BookController.class);

    @Autowired
    private BookService bookService;

    @Autowired
    private AuthUtil authUtil;

    @GetMapping("/books")
    public ApiResponse getAllBooks() {
        log.info("Controller: getAllBooks");
        return bookService.getAllBooks();
    }

    @GetMapping("/book/{id}")
    public ApiResponse getBookById(@PathVariable Long id) {
        log.info("Controller: getBookById, id={}", id);
        return bookService.getBookById(id);
    }

    @PostMapping("/book")
    public ApiResponse addBook(@RequestBody BookRequest request, HttpSession session) {
        ApiResponse authCheck = authUtil.checkAdmin(session);
        if (authCheck != null) {
            return authCheck;
        }
        log.info("Controller: addBook, title={}", request.getTitle());
        return bookService.addBook(request);
    }

    @PutMapping("/book/{id}")
    public ApiResponse updateBook(@PathVariable Long id, @RequestBody BookRequest request, HttpSession session) {
        ApiResponse authCheck = authUtil.checkAdmin(session);
        if (authCheck != null) {
            return authCheck;
        }
        log.info("Controller: updateBook, id={}", id);
        return bookService.updateBook(id, request);
    }

    @DeleteMapping("/book/{id}")
    public ApiResponse deleteBook(@PathVariable Long id, HttpSession session) {
        ApiResponse authCheck = authUtil.checkAdmin(session);
        if (authCheck != null) {
            return authCheck;
        }
        log.info("Controller: deleteBook, id={}", id);
        return bookService.deleteBook(id);
    }
}