package com.cosmo.aiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AuditAppVectorStoreConfig {
    @Resource
    private AuditAppDocumentLoader auditAppDocumentLoader;

    @Resource
    private AuditTokenTextSplitter auditTokenTextSplitter;

    @Bean
    VectorStore auditAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
                .build();
        // 加载文档
        List<Document> documents = auditAppDocumentLoader.loadMarkdowns();

        //自主切分
        List<Document> splitDocuments = auditTokenTextSplitter.splitByTripleStar(documents);
        simpleVectorStore.add(documents);
        return simpleVectorStore;
    }


}
