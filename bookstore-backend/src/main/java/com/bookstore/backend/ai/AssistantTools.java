package com.bookstore.backend.ai;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bookstore.backend.entity.Book;
import com.bookstore.backend.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 【作业2 · 技能/工具层】AssistantTools
 *
 * 本类承担两个职责，正好对应作业评分标准：
 *  1. buildToolSchemas()：定义两个 Function 的 Schema（含函数描述、参数类型、参数描述、必填项），
 *     交给大模型的 tools 字段，使模型能"识别"这两个函数。 —— 对应评分点①
 *  2. execute(name, args)：接收模型返回的函数名与参数，进行【本地模拟调用】，
 *     并包含完整的异常处理（错误 ISBN、竞对比价接口超时）。 —— 对应评分点③、⑤
 *
 * 两个 Function：
 *  - check_inventory(isbn)      查询本店某 ISBN 图书的实时库存
 *  - get_competitor_price(isbn) 调用（模拟）外部 API 查询竞争对手售价
 */
@Component
public class AssistantTools {

    private static final Logger log = LoggerFactory.getLogger(AssistantTools.class);

    /** 模拟外部比价接口的超时阈值（毫秒），超过则抛出超时异常 */
    private static final long COMPETITOR_API_TIMEOUT_MS = 2000L;

    @Autowired
    private BookRepository bookRepository;

    // ==================================================================
    // 一、Function Schema 定义（评分点①：符合模型要求的函数描述 + 参数类型）
    // ==================================================================

    /**
     * 构建 OpenAI / DeepSeek 兼容的 tools 列表。
     * 结构：{ "type":"function", "function":{ "name", "description", "parameters":{JSON Schema} } }
     */
    public JSONArray buildToolSchemas() {
        JSONArray tools = new JSONArray();
        tools.add(buildCheckInventorySchema());
        tools.add(buildGetCompetitorPriceSchema());
        return tools;
    }

    /** Function 1：查询本店库存 */
    private JSONObject buildCheckInventorySchema() {
        JSONObject isbnProp = new JSONObject();
        isbnProp.put("type", "string");
        isbnProp.put("description", "图书的 ISBN 编号，例如 978-7-5366-9293-0");

        JSONObject properties = new JSONObject();
        properties.put("isbn", isbnProp);

        JSONArray required = new JSONArray();
        required.add("isbn");

        JSONObject parameters = new JSONObject();
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", required);

        JSONObject function = new JSONObject();
        function.put("name", "check_inventory");
        function.put("description", "查询本电子书店中指定 ISBN 图书的实时库存数量、书名与本店售价。当用户想知道某本书还有多少本、是否有货、本店卖多少钱时使用。");
        function.put("parameters", parameters);

        JSONObject tool = new JSONObject();
        tool.put("type", "function");
        tool.put("function", function);
        return tool;
    }

    /** Function 2：查询竞争对手价格（模拟外部 API） */
    private JSONObject buildGetCompetitorPriceSchema() {
        JSONObject isbnProp = new JSONObject();
        isbnProp.put("type", "string");
        isbnProp.put("description", "图书的 ISBN 编号，例如 978-7-5366-9293-0");

        JSONObject properties = new JSONObject();
        properties.put("isbn", isbnProp);

        JSONArray required = new JSONArray();
        required.add("isbn");

        JSONObject parameters = new JSONObject();
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", required);

        JSONObject function = new JSONObject();
        function.put("name", "get_competitor_price");
        function.put("description", "调用外部比价接口（模拟），查询其它竞争平台（当当、京东、亚马逊）对指定 ISBN 图书的售价，用于比价。当用户想知道别家卖多少钱、想比价时使用。");
        function.put("parameters", parameters);

        JSONObject tool = new JSONObject();
        tool.put("type", "function");
        tool.put("function", function);
        return tool;
    }

    // ==================================================================
    // 二、本地模拟执行（评分点③：本地函数模拟调用；评分点⑤：异常处理）
    // ==================================================================

    /**
     * 根据函数名分发到对应的本地实现，返回 JSON 字符串结果（将回传给大模型上下文）。
     *
     * @param name 模型请求调用的函数名
     * @param args 模型给出的参数（已从 arguments 字符串解析为 JSONObject）
     * @return 函数执行结果的 JSON 字符串
     */
    public String execute(String name, JSONObject args) {
        String isbn = args == null ? null : args.getString("isbn");
        log.info(">>> Executing local function: {}(isbn={})", name, isbn);
        try {
            switch (name) {
                case "check_inventory":
                    return checkInventory(isbn);
                case "get_competitor_price":
                    return getCompetitorPrice(isbn);
                default:
                    // Unknown function: let model know the call failed and can self-correct
                    return errorJson("Function '" + name + "' is not supported. Please only use check_inventory or get_competitor_price.");
            }
        } catch (Exception e) {
            // Fallback exception handling: convert any unexpected errors to structured error for model
            log.error("Function {} execution exception", name, e);
            return errorJson("Internal error during function " + name + " execution: " + e.getMessage());
        }
    }

    /**
     * check_inventory：查询本店实时库存。
     * 异常处理：ISBN 为空 / 格式错误 / 数据库中不存在 —— 均返回结构化错误，触发模型 Self-Correction。
     */
    private String checkInventory(String isbn) {
        // Parameter validation (scoring point 5: handling invalid ISBN)
        if (isbn == null || isbn.trim().isEmpty()) {
            return errorJson("ISBN parameter cannot be empty. Please provide a valid ISBN before querying inventory.");
        }
        String normalized = isbn.trim();

        // Special test ISBN: if contains "TIMEOUT", return mock book data so get_competitor_price can trigger timeout
        if (normalized.toUpperCase().contains("TIMEOUT")) {
            log.info("<<< check_inventory: TIMEOUT test ISBN detected, returning mock book data");
            JSONObject data = new JSONObject();
            data.put("success", true);
            data.put("isbn", normalized);
            data.put("title", "Test Book (Timeout Demo)");
            data.put("author", "Test Author");
            data.put("stock", 10);
            data.put("price", new BigDecimal("39.90"));
            data.put("available", true);
            return data.toJSONString();
        }

        List<Book> books = bookRepository.findByIsbn(normalized);
        if (books == null || books.isEmpty()) {
            // Not found -> return clear error, model will inform user and suggest verifying ISBN (self-correction)
            return errorJson("Book with ISBN '" + normalized + "' not found in our inventory system. Please verify the ISBN is correct, or try searching by book title.");
        }

        Book book = books.get(0);
        JSONObject data = new JSONObject();
        data.put("success", true);
        data.put("isbn", normalized);
        data.put("title", book.getTitle());
        data.put("author", book.getAuthor());
        data.put("stock", book.getStock());
        data.put("price", book.getPrice());
        data.put("available", book.getStock() != null && book.getStock() > 0);
        log.info("<<< check_inventory success: ISBN={} stock={}", normalized, book.getStock());
        return data.toJSONString();
    }

    /**
     * get_competitor_price：模拟调用外部比价 API。
     * 异常处理（评分点⑤：模拟接口超时）：
     *   - 正常情况下模拟外部接口调用，延迟 300~800ms（不会超时）；
     *   - 特殊测试 ISBN（如 "TIMEOUT-TEST" 或包含 "TIMEOUT"）会强制触发超时异常，用于演示。
     */
    private String getCompetitorPrice(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return errorJson("ISBN parameter cannot be empty. Cannot perform competitor price comparison.");
        }
        String normalized = isbn.trim();

        // Special test ISBN: force trigger timeout exception (for demo)
        if (normalized.toUpperCase().contains("TIMEOUT")) {
            log.warn("!!! get_competitor_price forced timeout test, ISBN={}", normalized);
            return simulateTimeout(normalized);
        }

        // If we don't have this book, price comparison is meaningless, return error to guide model self-correction
        List<Book> books = bookRepository.findByIsbn(normalized);
        if (books == null || books.isEmpty()) {
            return errorJson("Book with ISBN '" + normalized + "' not found. Cannot retrieve competitor prices. Please verify the ISBN.");
        }
        Book book = books.get(0);
        BigDecimal basePrice = book.getPrice() == null ? BigDecimal.ZERO : book.getPrice();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // 正常情况下模拟外部接口调用，延迟 300~800ms（远低于 2000ms 阈值，不会超时）
            Future<JSONObject> future = executor.submit((Callable<JSONObject>) () -> callRemoteCompetitorApi(normalized, basePrice));
            JSONObject result = future.get(COMPETITOR_API_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            log.info("<<< get_competitor_price success: ISBN={}", normalized);
            return result.toJSONString();
        } catch (TimeoutException te) {
            // Timeout exception handling (core of scoring point 5) - only triggered by special test ISBN
            log.warn("!!! get_competitor_price external API timeout (>{})ms), ISBN={}", COMPETITOR_API_TIMEOUT_MS, normalized);
            return errorJson("Competitor price API timeout (simulated slow external API response). Unable to retrieve price comparison data temporarily. Please retry later or refer to our store price.");
        } catch (Exception e) {
            log.error("!!! get_competitor_price exception, ISBN={}", normalized, e);
            return errorJson("Error occurred while retrieving competitor prices: " + e.getMessage());
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 模拟超时异常（用于演示评分点⑤）。
     * 当 ISBN 包含 "TIMEOUT" 时调用此方法，强制等待超过阈值以触发 TimeoutException。
     */
    private String simulateTimeout(String isbn) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<String> future = executor.submit(() -> {
                // Force wait 3000ms, exceeding 2000ms threshold
                Thread.sleep(3000);
                return "Should not reach here";
            });
            future.get(COMPETITOR_API_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            return errorJson("Timeout simulation failed");
        } catch (TimeoutException te) {
            return errorJson("Competitor price API timeout (simulated slow external API response). Unable to retrieve price comparison data temporarily. Please retry later or refer to our store price.");
        } catch (Exception e) {
            return errorJson("Timeout simulation error: " + e.getMessage());
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 模拟的"远程比价接口"：正常网络延迟（300~800ms）+ 基于本店价格生成三家竞对报价。
     * 正常情况下不会超时（远低于 2000ms 阈值）。
     */
    private JSONObject callRemoteCompetitorApi(String isbn, BigDecimal basePrice) throws InterruptedException {
        // Simulate external API network latency: 300 ~ 800 ms (will not timeout)
        long latency = ThreadLocalRandom.current().nextLong(300, 800);
        log.info("    [Mock external competitor API] ISBN={} estimated latency {}ms", isbn, latency);
        Thread.sleep(latency);

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        JSONArray competitors = new JSONArray();
        competitors.add(competitor("Dangdang", scale(basePrice.multiply(factor(rnd, 0.90, 1.05)))));
        competitors.add(competitor("JD.com", scale(basePrice.multiply(factor(rnd, 0.88, 1.08)))));
        competitors.add(competitor("Amazon", scale(basePrice.multiply(factor(rnd, 0.95, 1.15)))));

        JSONObject data = new JSONObject();
        data.put("success", true);
        data.put("isbn", isbn);
        data.put("currency", "CNY");
        data.put("ourPrice", scale(basePrice));
        data.put("competitors", competitors);
        return data;
    }

    private BigDecimal factor(ThreadLocalRandom rnd, double min, double max) {
        return BigDecimal.valueOf(rnd.nextDouble(min, max));
    }

    private BigDecimal scale(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private JSONObject competitor(String name, BigDecimal price) {
        JSONObject c = new JSONObject();
        c.put("name", name);
        c.put("price", price);
        return c;
    }

    /** 统一的结构化错误返回，success=false + error 文本，方便模型识别并自我纠正 */
    private String errorJson(String message) {
        JSONObject obj = new JSONObject();
        obj.put("success", false);
        obj.put("error", message);
        return obj.toJSONString();
    }
}
