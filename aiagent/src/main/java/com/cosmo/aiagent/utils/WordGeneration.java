package com.cosmo.aiagent.utils;

import org.apache.poi.xwpf.usermodel.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class WordGeneration {
    public String generateWORD(String fileName, HashMap<String, String> data) {
        // 1. 加载Word模板
        String templatePath = "src/main/resources/document/template/" + fileName;
        File templateFile = new File(templatePath);
        
        // 2. 保存新的Word文档
        File outputFile = new File("output_poi.docx");
        
        try (FileInputStream fis = new FileInputStream(templateFile);
             XWPFDocument document = new XWPFDocument(fis);
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            
            // 3. 替换占位符
            replacePlaceholders(document, data);
            
            // 4. 保存文档
            document.write(fos);
            
        } catch (IOException e) {
            throw new RuntimeException("生成Word文件失败: " + e.getMessage(), e);
        }
        
        return outputFile.getAbsolutePath();
    }


    public static void replacePlaceholders(XWPFDocument document, HashMap<String, String> data) {
        // 替换段落中的占位符
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            List<XWPFRun> runs = paragraph.getRuns();
            if (runs != null && !runs.isEmpty()) {
                // 合并所有run的文本
                StringBuilder sb = new StringBuilder();
                for (XWPFRun run : runs) {
                    String runText = run.getText(0);
                    if (runText != null) {
                        sb.append(runText);
                    }
                }
                String text = sb.toString();
                
                // 检查是否有需要替换的占位符
                boolean replaced = false;
                for (String key : data.keySet()) {
                    String placeholder = "${" + key + "}";
                    if (text.contains(placeholder)) {
                        text = text.replace(placeholder, data.get(key));
                        replaced = true;
                    }
                }
                
                // 如果有替换，只需要重新设置一次
                if (replaced) {
                    // 清空原有run并重新设置
                    for (int i = runs.size() - 1; i >= 0; i--) {
                        paragraph.removeRun(i);
                    }
                    XWPFRun newRun = paragraph.createRun();
                    newRun.setText(text);
                }
            }
        }

        // 替换表格中的占位符
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        List<XWPFRun> runs = paragraph.getRuns();
                        if (runs != null && !runs.isEmpty()) {
                            // 合并所有run的文本
                            StringBuilder sb = new StringBuilder();
                            for (XWPFRun run : runs) {
                                String runText = run.getText(0);
                                if (runText != null) {
                                    sb.append(runText);
                                }
                            }
                            String text = sb.toString();
                            
                            // 检查是否有需要替换的占位符
                            boolean replaced = false;
                            for (String key : data.keySet()) {
                                String placeholder = "${" + key + "}";
                                if (text.contains(placeholder)) {
                                    text = text.replace(placeholder, data.get(key));
                                    replaced = true;
                                }
                            }
                            
                            // 如果有替换，只需要重新设置一次
                            if (replaced) {
                                // 清空原有run并重新设置
                                for (int i = runs.size() - 1; i >= 0; i--) {
                                    paragraph.removeRun(i);
                                }
                                XWPFRun newRun = paragraph.createRun();
                                newRun.setText(text);
                            }
                        }
                    }
                }
            }
        }
    }
}
