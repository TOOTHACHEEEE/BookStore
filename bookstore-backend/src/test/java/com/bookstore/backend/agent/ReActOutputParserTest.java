package com.bookstore.backend.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【作业3 · ReAct 解析测试】
 * 覆盖标准格式、Markdown 加粗标题、代码块包裹 JSON、纯字符串参数、缺少参数等场景。
 */
class ReActOutputParserTest {

    private final ReActOutputParser parser = new ReActOutputParser();

    @Test
    void shouldParseStandardThoughtActionAndInput() {
        String output = "Thought: 用户想找科幻书，我先检索书库。\n"
                + "Action: search_book_catalog\n"
                + "Action Input: {\"genre\":\"科幻\",\"limit\":3}";

        ReActOutputParser.Result result = parser.parse(output);

        assertFalse(result.hasFinalAnswer());
        assertTrue(result.hasAction());
        assertEquals("search_book_catalog", result.getAction());
        assertEquals("用户想找科幻书，我先检索书库。", result.getThought());
        assertEquals("科幻", result.getArguments().getString("genre"));
        assertEquals(3, result.getArguments().getIntValue("limit"));
        assertNull(result.getInputError());
    }

    @Test
    void shouldTolerateBoldLabelsAndFencedJson() {
        String output = "**Thought:** 需要查政策。\n"
                + "**Action:** query_store_policy\n"
                + "**Action Input:**\n"
                + "```json\n"
                + "{\"query\":\"拆了塑封还能退吗\"}\n"
                + "```";

        ReActOutputParser.Result result = parser.parse(output);

        assertEquals("query_store_policy", result.getAction());
        assertEquals("需要查政策。", result.getThought());
        assertEquals("拆了塑封还能退吗", result.getArguments().getString("query"));
    }

    @Test
    void shouldParseFinalAnswerAndFinalThought() {
        String output = "Thought: 书目和政策条款都齐了。\n"
                + "Final Answer: 推荐《三体》。\n\n拆封后能否退货取决于商品是否完好，详见第三章。";

        ReActOutputParser.Result result = parser.parse(output);

        assertTrue(result.hasFinalAnswer());
        assertEquals("书目和政策条款都齐了。", result.getThought());
        assertTrue(result.getFinalAnswer().startsWith("推荐《三体》。"));
        assertTrue(result.getFinalAnswer().contains("第三章"));
    }

    @Test
    void shouldWrapPlainStringAsQuery() {
        String output = "Thought: 直接搜关键词。\n"
                + "Action: search_book_catalog\n"
                + "Action Input: 微服务";

        ReActOutputParser.Result result = parser.parse(output);

        assertEquals("search_book_catalog", result.getAction());
        assertEquals("微服务", result.getArguments().getString("query"));
    }

    @Test
    void shouldIgnoreTextAfterJsonObject() {
        String output = "Thought: 查一下。\n"
                + "Action: search_book_catalog\n"
                + "Action Input: {\"author\":\"刘\",\"title_length\":2} 这本书应该是三体";

        ReActOutputParser.Result result = parser.parse(output);

        assertEquals("刘", result.getArguments().getString("author"));
        assertEquals(2, result.getArguments().getIntValue("title_length"));
    }

    @Test
    void shouldReportMissingActionInput() {
        String output = "Thought: 查一下。\nAction: search_book_catalog";

        ReActOutputParser.Result result = parser.parse(output);

        assertTrue(result.hasAction());
        assertNotNull(result.getInputError());
    }

    @Test
    void shouldReportInvalidFormat() {
        String output = "我觉得应该推荐三体，但是我不太确定。";

        ReActOutputParser.Result result = parser.parse(output);

        assertFalse(result.hasAction());
        assertFalse(result.hasFinalAnswer());
        assertNotNull(result.getInputError());
    }
}
