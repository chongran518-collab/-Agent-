package com.cosmo.aiagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class AuditTokenTextSplitter{
    public List<Document> splitDocuments(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter();
        return splitter.apply(documents);
    }

    public List<Document> splitCustomized(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter(200, 100, 10, 5000, true);
        return splitter.apply(documents);
    }

    public List<Document> splitByTripleStar(List<Document> documents) {
        Pattern separator = Pattern.compile("(?m)^\\s*([*]{3,}|[-]{3,}|[_]{3,})\\s*$");

        return documents.stream()
                .flatMap(doc -> {
                    // 按分隔符切成若干段
                    String[] parts = separator.split(doc.getText());
                    return Arrays.stream(parts)
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            // 每段仍保持与原 Document 相同的元数据
                            .map(text -> new Document(text, doc.getMetadata()));
                })
                .collect(Collectors.toList());
    }

}
