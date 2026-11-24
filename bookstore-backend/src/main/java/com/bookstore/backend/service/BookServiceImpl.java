package com.bookstore.backend.service;

import com.bookstore.backend.dto.ApiResponse;
import com.bookstore.backend.dto.BookRequest;
import com.bookstore.backend.dto.BookView;
import com.bookstore.backend.entity.Book;
import com.bookstore.backend.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 【Service实现类】书籍服务实现
 * 
 * 实现BookService接口，提供具体的业务逻辑
 */
@Service
public class BookServiceImpl implements BookService {

    private static final Logger log = LoggerFactory.getLogger(BookServiceImpl.class);

    @Autowired
    private BookRepository bookRepository;

    @Override
    public ApiResponse getAllBooks() {
        log.info("Querying all books");
        List<Book> books = bookRepository.findAll();
        List<BookView> views = new ArrayList<>();
        for (Book b : books) {
            views.add(toView(b));
        }
        return ApiResponse.success(views);
    }

    @Override
    public ApiResponse getBookById(Long id) {
        log.info("Querying book by id: {}", id);
        return bookRepository.findById(id)
                .map(b -> ApiResponse.success(toView(b)))
                .orElseGet(() -> new ApiResponse(404, "书籍不存在", null));
    }

    @Override
    public ApiResponse addBook(BookRequest request) {
        log.info("Adding new book: {}", request.getTitle());
        
        if (request.getTitle() == null || request.getTitle().isEmpty()) {
            return ApiResponse.error("书名不能为空");
        }
        
        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPrice(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO);
        book.setIsbn(request.getIsbn());
        book.setCover(request.getCover());
        book.setDescription(request.getDescription());
        book.setPublisher(request.getPublisher());
        book.setPublishDate(request.getPublishDate());
        book.setPages(request.getPages());
        book.setStock(request.getStock() != null ? request.getStock() : 100);
        
        Book saved = bookRepository.save(book);
        return ApiResponse.success(toView(saved));
    }

    @Override
    public ApiResponse updateBook(Long id, BookRequest request) {
        log.info("Updating book id: {}", id);
        
        Optional<Book> bookOpt = bookRepository.findById(id);
        if (bookOpt.isEmpty()) {
            return new ApiResponse(404, "书籍不存在", null);
        }
        
        Book book = bookOpt.get();
        if (request.getTitle() != null) {
            book.setTitle(request.getTitle());
        }
        if (request.getAuthor() != null) {
            book.setAuthor(request.getAuthor());
        }
        if (request.getPrice() != null) {
            book.setPrice(request.getPrice());
        }
        if (request.getIsbn() != null) {
            book.setIsbn(request.getIsbn());
        }
        if (request.getCover() != null) {
            book.setCover(request.getCover());
        }
        if (request.getDescription() != null) {
            book.setDescription(request.getDescription());
        }
        if (request.getPublisher() != null) {
            book.setPublisher(request.getPublisher());
        }
        if (request.getPublishDate() != null) {
            book.setPublishDate(request.getPublishDate());
        }
        if (request.getPages() != null) {
            book.setPages(request.getPages());
        }
        if (request.getStock() != null) {
            book.setStock(request.getStock());
        }
        
        Book saved = bookRepository.save(book);
        return ApiResponse.success(toView(saved));
    }

    @Override
    public ApiResponse deleteBook(Long id) {
        log.info("Deleting book id: {}", id);
        
        Optional<Book> bookOpt = bookRepository.findById(id);
        if (bookOpt.isEmpty()) {
            return new ApiResponse(404, "书籍不存在", null);
        }
        
        bookRepository.deleteById(id);
        return ApiResponse.success("删除成功");
    }

    /**
     * Entity转DTO
     */
    private BookView toView(Book book) {
        BookView v = new BookView();
        v.setId(book.getId());
        v.setTitle(book.getTitle());
        v.setAuthor(book.getAuthor());
        BigDecimal price = book.getPrice() == null ? BigDecimal.ZERO : book.getPrice();
        v.setPrice("$" + price.setScale(2, RoundingMode.HALF_UP).toPlainString());
        v.setIsbn(book.getIsbn());
        v.setCover(book.getCover());
        v.setDescription(book.getDescription());
        v.setPublisher(book.getPublisher());
        v.setPublishDate(book.getPublishDate());
        v.setPages(book.getPages());
        v.setStock(book.getStock());
        return v;
    }
}