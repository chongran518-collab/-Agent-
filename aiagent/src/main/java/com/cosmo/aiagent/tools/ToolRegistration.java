package com.cosmo.aiagent.tools;

import com.cosmo.aiagent.app.AuditApp;
import jakarta.annotation.Resource;
import lombok.Value;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 集中的工具注册类
 */
@Configuration
public class ToolRegistration {

//    @Value("${search-api.api-key}")
//    private String searchApiKey;


    @Bean
    public ToolCallback[] allTools() {
//          SqlTool sqlTool = new SqlTool();
          FileGenerationTool fileOperationTool = new FileGenerationTool();
//        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);
//        WebScrapingTool webScrapingTool = new WebScrapingTool();
//        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
//        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();
//        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
//        TerminateTool terminateTool = new TerminateTool();
        return ToolCallbacks.from(
//                 sqlTool,
                fileOperationTool
//                webSearchTool,
//                webScrapingTool,
//                resourceDownloadTool,
//                terminalOperationTool,
//                pdfGenerationTool,
//                terminateTool
        );
    }
}
