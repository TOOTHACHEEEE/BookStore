package com.bookstore.backend.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 【作业3 · Chunking 层】PolicyChunkSplitter —— Markdown 政策文档切片器。
 *
 * 采用"章节 + 句子 + 重叠窗口"的两级切分策略：
 *  1. 先按 Markdown 的 "## " 标题切成章节，保留章节标题作为元数据；
 *  2. 章节正文按行、再按中文句号/问号/分号切成句子；
 *  3. 句子依次装入长度不超过 chunkSize 的窗口，窗口写满后保留末尾约 overlap 个字符的句子作为重叠，
 *     避免把"拆封后可以退"和"但是需要满足条件"切到两个互不相关的片段里。
 *
 * 这种切分方式比固定长度硬切更符合政策文本的语义边界，也能让检索结果直接引用到具体章节。
 */
@Component
public class PolicyChunkSplitter {

    private static final Pattern SECTION_HEADING = Pattern.compile("^##\\s+(.+)$");
    private static final Pattern SENTENCE = Pattern.compile("[^。！？!?；;\\n]+[。！？!?；;]?");

    // 把政策文档切成若干块：先按二级标题切成章节，章节内按句子顺序装入不超过 chunkSize 的窗口，
    // 窗口写满后保留末尾若干完整句子（约 overlap 个字）作为重叠，再开始下一个窗口。
    public List<RawChunk> split(String markdown, int chunkSize, int overlap) {
        List<RawChunk> chunks = new ArrayList<>();
        if (markdown == null || markdown.isBlank()) {
            return chunks;
        }

        List<Section> sections = parseSections(markdown);
        int sectionIndex = 0;
        for (Section section : sections) {
            sectionIndex++;
            List<String> sentences = splitSentences(section.body);
            if (sentences.isEmpty()) {
                continue;
            }

            List<String> buffer = new ArrayList<>();
            int bufferLength = 0;
            int chunkIndex = 0;

            for (String sentence : sentences) {
                if (bufferLength + sentence.length() > chunkSize && !buffer.isEmpty()) {
                    chunks.add(buildChunk(sectionIndex, ++chunkIndex, section.title, buffer));
                    buffer = tailSentences(buffer, overlap);
                    bufferLength = buffer.stream().mapToInt(String::length).sum();
                }
                buffer.add(sentence);
                bufferLength += sentence.length();
            }
            if (!buffer.isEmpty()) {
                chunks.add(buildChunk(sectionIndex, ++chunkIndex, section.title, buffer));
            }
        }
        return chunks;
    }

    /** 解析 "## 标题" 章节；首个标题之前的内容（版本、适用范围等）作为"文档说明"章节保留 */
    private List<Section> parseSections(String markdown) {
        List<Section> sections = new ArrayList<>();
        String currentTitle = "文档说明";
        StringBuilder currentBody = new StringBuilder();

        for (String line : markdown.split("\\R")) {
            Matcher heading = SECTION_HEADING.matcher(line.trim());
            if (heading.matches()) {
                if (!currentBody.isEmpty()) {
                    sections.add(new Section(currentTitle, currentBody.toString()));
                    currentBody.setLength(0);
                }
                currentTitle = heading.group(1).trim();
            } else if (!line.startsWith("# ")) {
                currentBody.append(line).append('\n');
            }
        }
        if (!currentBody.isEmpty()) {
            sections.add(new Section(currentTitle, currentBody.toString()));
        }
        return sections;
    }

    /** 逐行切句：行边界本身也是一个语义边界（政策条目通常一行一条） */
    private List<String> splitSentences(String body) {
        List<String> sentences = new ArrayList<>();
        for (String line : body.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith(">")) {
                continue;
            }
            Matcher matcher = SENTENCE.matcher(trimmed);
            while (matcher.find()) {
                String sentence = matcher.group().trim();
                if (!sentence.isEmpty()) {
                    sentences.add(sentence);
                }
            }
        }
        return sentences;
    }

    /** 取当前窗口末尾若干完整句子作为下一窗口的重叠部分 */
    private List<String> tailSentences(List<String> buffer, int overlap) {
        List<String> tail = new ArrayList<>();
        int length = 0;
        for (int i = buffer.size() - 1; i >= 0; i--) {
            String sentence = buffer.get(i);
            if (!tail.isEmpty() && length + sentence.length() > overlap) {
                break;
            }
            tail.add(0, sentence);
            length += sentence.length();
            if (length >= overlap) {
                break;
            }
        }
        return tail;
    }

    private RawChunk buildChunk(int sectionIndex, int chunkIndex, String section, List<String> sentences) {
        String id = String.format("policy-%02d-%02d", sectionIndex, chunkIndex);
        return new RawChunk(id, section, String.join("", sentences));
    }

    /** 切片结果（尚未向量化） */
    public static class RawChunk {
        private final String id;
        private final String section;
        private final String text;

        public RawChunk(String id, String section, String text) {
            this.id = id;
            this.section = section;
            this.text = text;
        }

        public String getId() {
            return id;
        }

        public String getSection() {
            return section;
        }

        public String getText() {
            return text;
        }
    }

    private static class Section {
        private final String title;
        private final String body;

        private Section(String title, String body) {
            this.title = title;
            this.body = body;
        }
    }
}
