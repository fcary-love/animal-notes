package com.pettimeline.ai.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class DocumentChunker {

    private static final int MAX_CHARS = 500;
    private static final int OVERLAP = 50;
    private static final Pattern SENTENCE_END = Pattern.compile("[。！？\\n](?=\\S)");

    public static List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        String[] segments = SENTENCE_END.split(text);
        StringBuilder buf = new StringBuilder();
        int idx = 0;

        for (String seg : segments) {
            String s = seg.trim();
            if (s.isEmpty()) continue;
            if (buf.length() + s.length() > MAX_CHARS && buf.length() > 0) {
                chunks.add(buf.toString().trim());
                if (buf.length() > OVERLAP) {
                    buf = new StringBuilder(buf.substring(buf.length() - OVERLAP));
                } else {
                    buf = new StringBuilder();
                }
            }
            buf.append(s).append("。");
            idx++;
        }
        if (buf.length() > 0) {
            chunks.add(buf.toString().trim());
        }
        return chunks;
    }
}
