package com.cosmo.aiagent.tools;

import cn.hutool.core.lang.UUID;
import cn.hutool.extra.spring.SpringUtil;
import com.cosmo.aiagent.app.AuditApp;
import com.cosmo.aiagent.model.dto.AuditGenerateRequest;
import com.cosmo.aiagent.utils.WordGeneration;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.HashMap;
import java.util.List;


/**
 * 文档生成工具
 */

@Component
public class FileGenerationTool {

    private static HashMap<String, String> content = new HashMap<>();


    @Tool(description = "Generate a Word file", returnDirect = false)
    public String generateFile(@ToolParam(description = "模板文件名")String fileName,
                               @ToolParam(description = "审计认定的事实摘要及审计结论")String factsAndConclusions) {
        String chatId = UUID.randomUUID().toString();
        AuditApp  auditApp = SpringUtil.getBean(AuditApp.class);
        AuditApp.AuditReport auditReport = auditApp.doChatWithReport(factsAndConclusions, chatId);
        content.put("auditConclusion", auditReport.factsAndConclusions());
        String result = new WordGeneration().generateWORD(fileName, content);
        return result;
    }

    public void initFile(AuditGenerateRequest request) {
        content.put("projectName", request.getProjectName());
        content.put("projectProfile", request.getProjectProfile());
        content.put("auditPerson", request.getAuditPerson());
        content.put("date", request.getDate());
        content.put("auditProcess", request.getAuditProcess());
    }
}
