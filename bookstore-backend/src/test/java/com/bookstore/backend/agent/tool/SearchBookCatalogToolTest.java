package com.bookstore.backend.agent.tool;

import com.alibaba.fastjson2.JSONObject;
import com.bookstore.backend.entity.Book;
import com.bookstore.backend.rag.LocalTextEmbedding;
import com.bookstore.backend.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 【作业3】search_book_catalog 工具测试。
 *
 * 覆盖三种关键场景：
 *  1. 模糊条件组合（姓刘 + 书名两个字）能收敛到《三体》；
 *  2. 主题关键词（微服务）只召回真正相关的书，不混入仅“有库存”的书；
 *  3. 书库没有的主题（区块链）返回 0 条，避免模型编造书目。
 */
class SearchBookCatalogToolTest {

    private SearchBookCatalogTool tool;

    @BeforeEach
    void setUp() {
        BookRepository repository = mock(BookRepository.class);
        when(repository.findAll()).thenReturn(sampleBooks());
        tool = new SearchBookCatalogTool(repository, new LocalTextEmbedding(1024));
    }

    @Test
    @DisplayName("姓刘 + 书名两个字 -> 只命中《三体》")
    void findsSantiByFuzzyConstraints() {
        JSONObject args = new JSONObject();
        args.put("author", "刘");
        args.put("title_length", 2);
        args.put("limit", 10);

        JSONObject result = JSONObject.parseObject(tool.execute(args));

        assertTrue(result.getBooleanValue("success"));
        assertEquals(1, result.getIntValue("total"));
        assertEquals("三体", result.getJSONArray("books").getJSONObject(0).getString("title"));
    }

    @Test
    @DisplayName("关键词“微服务”只召回相关图书")
    void filtersOutIrrelevantBooksForKeyword() {
        JSONObject args = new JSONObject();
        args.put("keyword", "微服务");

        JSONObject result = JSONObject.parseObject(tool.execute(args));

        assertEquals(1, result.getIntValue("total"));
        assertEquals("微服务架构设计模式",
                result.getJSONArray("books").getJSONObject(0).getString("title"));
    }

    @Test
    @DisplayName("书库没有的主题返回 0 条，不产生单字误召回")
    void returnsEmptyWhenNoStrongMatch() {
        JSONObject args = new JSONObject();
        args.put("keyword", "区块链 智能合约");

        JSONObject result = JSONObject.parseObject(tool.execute(args));

        assertTrue(result.getBooleanValue("success"));
        assertEquals(0, result.getIntValue("total"));
        assertEquals(0, result.getJSONArray("books").size());
    }

    @Test
    @DisplayName("无条件时返回全部书目供浏览")
    void returnsAllBooksWithoutCriteria() {
        JSONObject args = new JSONObject();
        args.put("limit", 10);

        JSONObject result = JSONObject.parseObject(tool.execute(args));

        assertEquals(3, result.getIntValue("total"));
        assertEquals(3, result.getIntValue("returned"));
    }

    private List<Book> sampleBooks() {
        return List.of(
                book(1L, "三体", "刘慈欣",
                        "《三体》是刘慈欣创作的系列长篇科幻小说，讲述地球人类文明和三体文明的信息交流与兴衰历程。",
                        "重庆出版社", "49.90", 100),
                book(2L, "流浪地球", "刘慈欣",
                        "《流浪地球》是刘慈欣的科幻小说，讲述人类推动地球逃离太阳系的故事。",
                        "长江文艺出版社", "32.00", 80),
                book(3L, "微服务架构设计模式", "Chris Richardson",
                        "系统讲解微服务架构的设计模式：服务拆分、API 网关、分布式事务、事件驱动与可观测性。",
                        "机械工业出版社", "99.00", 60)
        );
    }

    private Book book(Long id, String title, String author, String description,
                      String publisher, String price, int stock) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor(author);
        book.setDescription(description);
        book.setPublisher(publisher);
        book.setPrice(new BigDecimal(price));
        book.setStock(stock);
        return book;
    }
}
