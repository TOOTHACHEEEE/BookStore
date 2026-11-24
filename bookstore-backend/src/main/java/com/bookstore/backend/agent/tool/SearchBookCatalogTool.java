package com.bookstore.backend.agent.tool;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bookstore.backend.entity.Book;
import com.bookstore.backend.rag.LocalTextEmbedding;
import com.bookstore.backend.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 【作业3 · Tool 1】search_book_catalog —— 模拟查书。
 *
 * 面向"模糊需求"设计，支持多种条件的组合筛选：
 *  - keyword       自由关键词，匹配书名 / 作者 / 简介 / 出版社；
 *  - author        作者姓名或姓氏（"刘" 可以命中 "刘慈欣"）；
 *  - title         书名片段；
 *  - title_length  书名精确字数（"书名是两个字" -> 2）；
 *  - genre         图书分类（科幻 / 计算机 / 文学 / 古典 / 科普 / 历史 / 儿童）。
 *
 * 执行流程：硬条件过滤 -> 相关性打分 -> 按分数排序 -> 返回 Top-N 候选书目与匹配理由。
 * 返回结果包含 matchReason，方便模型向用户解释"为什么推荐这本"。
 */
@Component
public class SearchBookCatalogTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(SearchBookCatalogTool.class);

    /** 分类 -> 判定关键词。命中任意一个即认为属于该分类 */
    private static final Map<String, List<String>> GENRE_KEYWORDS = new LinkedHashMap<>();

    /** 模型可能传入的同义分类 */
    private static final Map<String, String> GENRE_ALIASES = new HashMap<>();

    static {
        GENRE_KEYWORDS.put("科幻", List.of("科幻", "宇宙", "星际", "外星", "未来", "三体", "流浪地球", "行星"));
        GENRE_KEYWORDS.put("计算机", List.of("java", "spring", "编程", "计算机", "微服务", "架构", "软件", "算法",
                "设计模式", "分布式", "代码", "开发"));
        GENRE_KEYWORDS.put("文学", List.of("小说", "文学", "散文", "短篇", "长篇", "章回体", "故事"));
        GENRE_KEYWORDS.put("古典", List.of("古典", "名著", "章回体", "红楼梦", "诗词"));
        GENRE_KEYWORDS.put("科普", List.of("科普", "科学", "探索", "自然"));
        GENRE_KEYWORDS.put("历史", List.of("历史", "朝代", "史记"));
        GENRE_KEYWORDS.put("儿童", List.of("儿童", "童书", "绘本", "少年"));

        GENRE_ALIASES.put("科幻", "科幻");
        GENRE_ALIASES.put("sci-fi", "科幻");
        GENRE_ALIASES.put("science fiction", "科幻");
        GENRE_ALIASES.put("计算机", "计算机");
        GENRE_ALIASES.put("技术", "计算机");
        GENRE_ALIASES.put("编程", "计算机");
        GENRE_ALIASES.put("it", "计算机");
        GENRE_ALIASES.put("软件", "计算机");
        GENRE_ALIASES.put("文学", "文学");
        GENRE_ALIASES.put("小说", "文学");
        GENRE_ALIASES.put("古典", "古典");
        GENRE_ALIASES.put("名著", "古典");
        GENRE_ALIASES.put("科普", "科普");
        GENRE_ALIASES.put("科学", "科普");
        GENRE_ALIASES.put("历史", "历史");
        GENRE_ALIASES.put("儿童", "儿童");
        GENRE_ALIASES.put("童书", "儿童");
    }

    private final BookRepository bookRepository;
    private final LocalTextEmbedding embedding;

    public SearchBookCatalogTool(BookRepository bookRepository, LocalTextEmbedding embedding) {
        this.bookRepository = bookRepository;
        this.embedding = embedding;
    }

    @Override
    public String name() {
        return "search_book_catalog";
    }

    @Override
    public String description() {
        return "检索本店书库（模拟查书）。支持关键词、作者（可只传姓氏，例如“刘”）、书名片段、"
                + "书名精确字数（例如“书名是两个字”传 2）、图书分类的组合筛选，返回按相关度排序的候选图书，"
                + "包含书名、作者、价格、库存、分类与匹配理由。用户提出任何找书、荐书、模糊描述找书的需求时，"
                + "必须先调用本工具，不要凭记忆编造书目。";
    }

    @Override
    public JSONObject parameterSchema() {
        JSONObject properties = new JSONObject(new LinkedHashMap<>());
        properties.put("keyword", property("string",
                "自由关键词，例如“科幻”“微服务”“童年回忆”，会匹配书名、作者、简介与出版社"));
        properties.put("author", property("string",
                "作者姓名或姓氏，例如“刘慈欣”“刘”"));
        properties.put("title", property("string",
                "书名中包含的字词，例如“三体”"));
        properties.put("title_length", property("integer",
                "书名精确字数（不含标点），例如“三体”为 2、“流浪地球”为 4"));
        properties.put("genre", property("string",
                "图书分类，可选：科幻、计算机、文学、古典、科普、历史、儿童"));
        properties.put("limit", property("integer",
                "返回条数，默认 5，最大 10"));

        JSONObject schema = new JSONObject(new LinkedHashMap<>());
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", new JSONArray());
        return schema;
    }

    @Override
    public String execute(JSONObject args) {
        JSONObject safeArgs = args == null ? new JSONObject() : args;

        String keyword = firstNonBlank(safeArgs.getString("keyword"), safeArgs.getString("query"));
        String author = trimToNull(safeArgs.getString("author"));
        String title = trimToNull(safeArgs.getString("title"));
        Integer titleLength = safeArgs.getInteger("title_length");
        String requestedGenre = trimToNull(safeArgs.getString("genre"));
        int limit = safeArgs.getIntValue("limit", 5);

        if (titleLength != null && titleLength <= 0) {
            titleLength = null;
        }
        if (limit <= 0 || limit > 10) {
            limit = 5;
        }

        // 未知分类不硬过滤，转为关键词，避免模型用了同义词就查不到书
        String genre = null;
        if (requestedGenre != null) {
            genre = resolveGenre(requestedGenre);
            if (genre == null) {
                keyword = keyword == null ? requestedGenre : keyword + " " + requestedGenre;
            }
        }

        boolean noCriteria = keyword == null && author == null && title == null
                && titleLength == null && genre == null;

        log.info("[Tool:search_book_catalog] keyword={}, author={}, title={}, titleLength={}, genre={}, limit={}",
                keyword, author, title, titleLength, genre, limit);

        List<Book> books = bookRepository.findAll();
        List<ScoredBook> candidates = new ArrayList<>();

        for (Book book : books) {
            if (!matchesHardFilters(book, author, title, titleLength, genre)) {
                continue;
            }
            candidates.add(score(book, keyword, author, title, titleLength, genre, noCriteria));
        }

        // 只要有任意筛选条件，就剔除“零命中、仅因有库存得 0.5 分”的书，
        // 避免 Observation 里混入大量无关书目干扰模型判断。
        if (!noCriteria) {
            candidates.removeIf(scored -> scored.score <= 0.5);
        }

        candidates.sort(Comparator
                .comparingDouble(ScoredBook::getScore).reversed()
                .thenComparing(scored -> scored.book.getTitle()));

        List<ScoredBook> limited = candidates.size() > limit ? candidates.subList(0, limit) : candidates;

        JSONArray bookArray = new JSONArray();
        for (ScoredBook scored : limited) {
            bookArray.add(toJson(scored));
        }

        JSONObject result = new JSONObject(new LinkedHashMap<>());
        result.put("success", true);
        result.put("query", describeQuery(keyword, author, title, titleLength, genre));
        result.put("total", candidates.size());
        result.put("returned", limited.size());
        result.put("books", bookArray);
        result.put("hint", candidates.isEmpty()
                ? "没有书籍满足当前筛选条件。可以尝试放宽 title_length、去掉 genre，或换一个 keyword 重新检索；"
                  + "如果仍然没有结果，请如实告知用户本店暂无相关书目，不要编造。"
                : "请基于以上真实书目进行推荐，向用户说明书名、作者、价格、库存与匹配理由。");

        log.info("[Tool:search_book_catalog] matched {} book(s), returned {}", candidates.size(), limited.size());
        return result.toJSONString();
    }

    // ------------------------------------------------------------------
    // 筛选与打分
    // ------------------------------------------------------------------

    private boolean matchesHardFilters(Book book, String author, String title,
                                       Integer titleLength, String genre) {
        if (titleLength != null && titleLength(book.getTitle()) != titleLength) {
            return false;
        }
        if (title != null && !containsIgnoreCase(book.getTitle(), title)) {
            return false;
        }
        if (author != null && !authorMatches(book.getAuthor(), author)) {
            return false;
        }
        if (genre != null && !inferGenres(book).contains(genre)) {
            return false;
        }
        return true;
    }

    private ScoredBook score(Book book, String keyword, String author, String title,
                             Integer titleLength, String genre, boolean noCriteria) {
        double score = 0.0;
        List<String> reasons = new ArrayList<>();

        if (noCriteria) {
            reasons.add("未提供筛选条件，返回全部书目供浏览");
        }
        if (title != null) {
            boolean exact = book.getTitle().equalsIgnoreCase(title);
            score += exact ? 6.0 : 4.0;
            reasons.add(exact ? "书名完全匹配" : "书名包含「" + title + "」");
        }
        if (author != null) {
            score += 4.0;
            reasons.add("作者匹配「" + book.getAuthor() + "」");
        }
        if (titleLength != null) {
            score += 3.0;
            reasons.add("书名为 " + titleLength + " 个字");
        }
        if (genre != null) {
            score += 3.0;
            reasons.add("分类匹配「" + genre + "」");
        }
        if (keyword != null) {
            KeywordMatch match = keywordMatch(keyword, book);
            if (match.score > 0) {
                score += Math.min(8.0, match.score);
                reasons.add("关键词命中「" + String.join("、", match.hits) + "」");
            }
        }
        if (book.getStock() != null && book.getStock() > 0) {
            score += 0.5;
            reasons.add("有库存");
        }
        return new ScoredBook(book, score, reasons);
    }

    /** 关键词在书名 / 作者 / 简介 / 出版社中的加权命中情况 */
    private KeywordMatch keywordMatch(String keyword, Book book) {
        Map<String, Double> tokens = embedding.tokenize(keyword);
        String title = lower(book.getTitle());
        String author = lower(book.getAuthor());
        String description = lower(book.getDescription());
        String publisher = lower(book.getPublisher());
        String whole = lower(keyword).trim();
        boolean singleCharQuery = whole.codePointCount(0, whole.length()) <= 1;

        double score = 0.0;
        Set<String> hits = new LinkedHashSet<>();
        boolean strongHit = false;

        // 完整关键词直接命中书名 / 作者，视为强命中
        if (!whole.isEmpty() && (title.contains(whole) || author.contains(whole))) {
            strongHit = true;
            hits.add(whole);
        }

        for (String token : tokens.keySet()) {
            boolean hit = false;
            if (title.contains(token)) {
                score += 2.0;
                hit = true;
            }
            if (author.contains(token)) {
                score += 1.5;
                hit = true;
            }
            if (description.contains(token)) {
                score += 0.8;
                hit = true;
            }
            if (publisher.contains(token)) {
                score += 0.4;
                hit = true;
            }
            // 只有 bigram / 英文词 / 单字查询才算强命中；零散单字命中容易误召回
            boolean strongToken = token.length() >= 2 || token.matches("[a-z0-9]+") || singleCharQuery;
            if (hit && strongToken) {
                strongHit = true;
                if (!token.equals(whole)) {
                    hits.add(token);
                }
            }
        }

        // 无强命中时整段归零，让无关书目在过滤阶段被剔除
        if (!strongHit) {
            return new KeywordMatch(0.0, new LinkedHashSet<>());
        }
        if (hits.isEmpty()) {
            hits.add(whole.isEmpty() ? keyword : whole);
        }
        return new KeywordMatch(score, hits);
    }

    /** 推断图书分类：一本图书可以同时属于多个分类 */
    private List<String> inferGenres(Book book) {
        String text = lower(book.getTitle() + " " + book.getAuthor() + " "
                + book.getDescription() + " " + book.getPublisher());
        List<String> genres = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : GENRE_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (text.contains(keyword)) {
                    genres.add(entry.getKey());
                    break;
                }
            }
        }
        return genres;
    }

    private String resolveGenre(String raw) {
        String value = raw.trim().toLowerCase(Locale.ROOT);
        String alias = GENRE_ALIASES.get(value);
        if (alias != null) {
            return alias;
        }
        for (String canonical : GENRE_KEYWORDS.keySet()) {
            if (raw.contains(canonical)) {
                return canonical;
            }
        }
        return null;
    }

    private boolean authorMatches(String bookAuthor, String input) {
        if (bookAuthor == null) {
            return false;
        }
        String normalizedBookAuthor = normalize(bookAuthor);
        String normalizedInput = normalize(input);
        return !normalizedInput.isEmpty() && normalizedBookAuthor.contains(normalizedInput);
    }

    /** 书名精确字数：只统计汉字、字母、数字，忽略标点与间隔号 */
    private int titleLength(String title) {
        if (title == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < title.length(); ) {
            int codePoint = title.codePointAt(i);
            if (Character.isLetterOrDigit(codePoint)) {
                count++;
            }
            i += Character.charCount(codePoint);
        }
        return count;
    }

    private JSONObject toJson(ScoredBook scored) {
        Book book = scored.book;
        JSONObject item = new JSONObject(new LinkedHashMap<>());
        item.put("id", book.getId());
        item.put("title", book.getTitle());
        item.put("author", book.getAuthor());
        item.put("price", book.getPrice());
        item.put("stock", book.getStock());
        item.put("isbn", book.getIsbn());
        item.put("publisher", book.getPublisher());
        item.put("publishDate", book.getPublishDate());
        item.put("genres", inferGenres(book));
        item.put("matchScore", Math.round(scored.score * 100.0) / 100.0);
        item.put("matchReason", String.join("；", scored.reasons));
        item.put("summary", abbreviate(book.getDescription(), 160));
        return item;
    }

    private String describeQuery(String keyword, String author, String title,
                                 Integer titleLength, String genre) {
        JSONObject query = new JSONObject(new LinkedHashMap<>());
        if (keyword != null) query.put("keyword", keyword);
        if (author != null) query.put("author", author);
        if (title != null) query.put("title", title);
        if (titleLength != null) query.put("title_length", titleLength);
        if (genre != null) query.put("genre", genre);
        return query.toJSONString();
    }

    private JSONObject property(String type, String description) {
        JSONObject property = new JSONObject(new LinkedHashMap<>());
        property.put("type", type);
        property.put("description", description);
        return property;
    }

    private String firstNonBlank(String first, String second) {
        String value = trimToNull(first);
        return value != null ? value : trimToNull(second);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("[\\s·.。]", "").toLowerCase(Locale.ROOT);
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private boolean containsIgnoreCase(String text, String part) {
        return text != null && part != null && text.toLowerCase(Locale.ROOT).contains(part.toLowerCase(Locale.ROOT));
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "…";
    }

    private static class KeywordMatch {
        private final double score;
        private final Set<String> hits;

        private KeywordMatch(double score, Set<String> hits) {
            this.score = score;
            this.hits = hits;
        }
    }

    private static class ScoredBook {
        private final Book book;
        private final double score;
        private final List<String> reasons;

        private ScoredBook(Book book, double score, List<String> reasons) {
            this.book = book;
            this.score = score;
            this.reasons = reasons;
        }

        private double getScore() {
            return score;
        }
    }
}
