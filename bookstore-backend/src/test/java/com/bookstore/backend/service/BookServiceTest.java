package com.bookstore.backend.service;

import com.bookstore.backend.dto.ApiResponse;
import com.bookstore.backend.entity.Book;
import com.bookstore.backend.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book testBook;

    @BeforeEach
    void setUp() {
        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("三体");
        testBook.setAuthor("刘慈欣");
        testBook.setPrice(new BigDecimal("68.00"));
        testBook.setCover("https://example.com/cover.jpg");
    }

    @Test
    void getAllBooks_ShouldReturnAllBooks() {
        List<Book> books = new ArrayList<>();
        books.add(testBook);

        when(bookRepository.findAll()).thenReturn(books);

        ApiResponse response = bookService.getAllBooks();

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
    }

    @Test
    void getBookById_ShouldReturnBookWhenExists() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        ApiResponse response = bookService.getBookById(1L);

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
    }

    @Test
    void getBookById_ShouldReturnNotFoundWhenNotExists() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        ApiResponse response = bookService.getBookById(999L);

        assertEquals(404, response.getCode());
        assertEquals("书籍不存在", response.getMessage());
    }
}