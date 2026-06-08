package com.cosmo.aiagent.controller;

import com.cosmo.aiagent.agent.Manus;
import com.cosmo.aiagent.app.AuditApp;
import com.cosmo.aiagent.model.dto.AuditGenerateRequest;
import com.cosmo.aiagent.model.dto.SqlGenerateRequest;
import com.cosmo.aiagent.tools.FileGenerationTool;
import com.cosmo.aiagent.tools.SQLTool;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.*;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import java.nio.file.Files;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.google.gson.Gson;

@RestController
@RequestMapping("/ai")
public class AiController {

    private static final String UPLOAD_DIR = "D:\\javaproject\\aiagent\\aiagent\\src\\main\\resources\\document\\template";


    @Resource
    private AuditApp auditApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel ollamaChatModel;
    
    @Resource
    private SQLTool sqlTool;

    @GetMapping("/audit_app/chat/sync")
    public String doChatWithLoveAppSync(String message, String chatId) {
        return auditApp.doChat(message, chatId);
    }

    @GetMapping(value = "/audit_app/chat/sse")
    public Flux<ServerSentEvent<String>> doChatWithAuditAppSSE(String message, String chatId) {
        return auditApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }
    
    /**
     * 带文件的AI聊天（同步）
     * 
     * @param message 用户消息
     * @param chatId 聊天ID
     * @param filePath 文件路径
     * @return 聊天响应
     */
    @GetMapping("/audit_app/chat/sync/file")
    public String doChatWithAuditAppSyncWithFile(String message, String chatId, String filePath) {
        return auditApp.doChat(message, chatId, filePath);
    }
    
    /**
     * 带文件的AI聊天（SSE流式传输）
     * 
     * @param message 用户消息
     * @param chatId 聊天ID
     * @param filePath 文件路径
     * @return 流式响应
     */
    @GetMapping(value = "/audit_app/chat/sse/file")
    public Flux<ServerSentEvent<String>> doChatWithAuditAppSSEWithFile(String message, String chatId, String filePath) {
        return auditApp.doChatByStream(message, chatId, filePath)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }


    @GetMapping(value = "/audit_app/chat/ssetools")
    public String doChatWithTools(String message, String chatId) {
        return auditApp.doChatWithTools(message, chatId);
    }


    /**
     * 流式调用 Manus 超级智能体
     *
     * @param message
     * @param filePath
     * @return
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message, String filePath) {
        // 如果有文件路径，将其添加到消息中
        if (filePath != null && !filePath.isEmpty()) {
            message += "\n\n[文件路径]: " + filePath;
        }
        Manus manus = new Manus(allTools, ollamaChatModel);
        return manus.runStream(message);
    }

    @PostMapping(value = "/audit_app/generateword", produces = "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    public ResponseEntity<byte[]> generateFile(@RequestBody AuditGenerateRequest auditGenerateRequest) {
        try {
            // 初始化文件生成工具
            FileGenerationTool fileGenerationTool = new FileGenerationTool();
            fileGenerationTool.initFile(auditGenerateRequest);
            
            // 直接调用FileGenerationTool生成文件，避免通过Manus类的run方法
            String templateFileName = "审计工作底稿模板文件.docx";
            String filePath = fileGenerationTool.generateFile(templateFileName, auditGenerateRequest.getAuditProcess());
            
            // 读取文件内容
            File file = new File(filePath);
            byte[] fileContent = Files.readAllBytes(file.toPath());
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(ContentDisposition.builder("attachment").filename("审计工作底稿.docx").build());
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
            headers.setContentLength(fileContent.length);
            
            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/audit_app/generateword/file")
    //将前端上传的文件保存在固定路径下
    public String storeFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return "文件为空，上传失败";
        }

        // 确保目录存在
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 构建文件保存路径
        String filePath = UPLOAD_DIR +"\\"+file.getOriginalFilename();
        File dest = new File(filePath);


        try {
            // 保存文件
            file.transferTo(dest);
            
            // 如果是表格文件，通知SQLTool
            String fileName = file.getOriginalFilename().toLowerCase();
            if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls") || fileName.endsWith(".csv")) {
                SQLTool.setLatestTableFilePath(filePath);
            }
            
            return filePath;
        } catch (IOException e) {
            e.printStackTrace();
            return "文件保存失败: " + e.getMessage();
        }

    }
    
    /**
     * 上传表格文件并导入到数据库
     * 
     * @param file 表格文件
     * @return 导入结果
     */
    @PostMapping(value = "/audit_app/upload_table")
    public String uploadTable(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return "文件为空，上传失败";
        }
        
        try {
            // 保存上传的文件
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String filePath = UPLOAD_DIR +"\\"+file.getOriginalFilename();
            File dest = new File(filePath);
            file.transferTo(dest);
            
            // 使用UUID生成唯一表名
            String tableName = "upload_table_" + UUID.randomUUID().toString().replace("-", "");
            
            // 解析Excel文件
            InputStream inputStream = file.getInputStream();
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);
            
            // 获取表头
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return "表格没有表头，导入失败";
            }
            
            List<String> columnNames = new ArrayList<>();
            for (Cell cell : headerRow) {
                String columnName = getCellValueAsString(cell);
                // 清理列名，使其符合SQL命名规范
                columnName = columnName.replaceAll("\\s+|\\W+", "_")
                                      .replaceAll("^_+|_+$", "")
                                      .toLowerCase();
                columnNames.add(columnName);
            }
            
            // 创建数据库连接
            String url = "jdbc:mysql://localhost:3306/aiagent?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai";
            String username = "root";
            String password = "123456";
            
            try (Connection connection = DriverManager.getConnection(url, username, password)) {
                // 创建表
                StringBuilder createTableSql = new StringBuilder("CREATE TABLE `" + tableName + "` (");
                for (int i = 0; i < columnNames.size(); i++) {
                    String columnName = columnNames.get(i);
                    createTableSql.append("`").append(columnName).append("` VARCHAR(255)");
                    if (i < columnNames.size() - 1) {
                        createTableSql.append(", ");
                    }
                }
                createTableSql.append(")");
                
                try (PreparedStatement stmt = connection.prepareStatement(createTableSql.toString())) {
                    stmt.executeUpdate();
                }
                
                // 插入数据
                StringBuilder insertSql = new StringBuilder("INSERT INTO `" + tableName + "` VALUES (");
                for (int i = 0; i < columnNames.size(); i++) {
                    insertSql.append("?");
                    if (i < columnNames.size() - 1) {
                        insertSql.append(", ");
                    }
                }
                insertSql.append(")");
                
                try (PreparedStatement stmt = connection.prepareStatement(insertSql.toString())) {
                    // 从第二行开始插入数据
                    for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                        Row dataRow = sheet.getRow(rowIndex);
                        if (dataRow == null) continue;
                        
                        for (int colIndex = 0; colIndex < columnNames.size(); colIndex++) {
                            Cell cell = dataRow.getCell(colIndex);
                            String cellValue = getCellValueAsString(cell);
                            stmt.setString(colIndex + 1, cellValue);
                        }
                        
                        stmt.addBatch();
                        
                        // 每100行提交一次
                        if (rowIndex % 100 == 0) {
                            stmt.executeBatch();
                            connection.commit();
                        }
                    }
                    
                    // 提交剩余数据
                    stmt.executeBatch();
                    connection.commit();
                }
            }
            
            // 调用SQLTool设置最新的表格文件路径和实际表名
            SQLTool.setLatestTableFilePath(filePath, tableName);
            
            workbook.close();
            inputStream.close();
            
            return tableName;
        } catch (Exception e) {
            e.printStackTrace();
            return "表格导入失败: " + e.getMessage();
        }
    }
    
    /**
     * 执行SQL查询
     * 
     * @param requestBody 包含SQL查询语句的请求体
     * @return 查询结果
     */
    @PostMapping(value = "/audit_app/execute_sql")
    public Object executeSql(@RequestBody Map<String, String> requestBody) {
        try {
            String sql = requestBody.get("sql");
            if (sql == null || sql.isEmpty()) {
                return Map.of("error", "SQL语句不能为空");
            }
            String jsonResult = sqlTool.executeSql(sql);
            // 解析JSON字符串为对象并返回
            Gson gson = new Gson();
            return gson.fromJson(jsonResult, Object.class);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "SQL执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 将自然语言问题转换为SQL查询语句
     * 
     * @param request 包含问题和表名的请求
     * @return 生成的SQL查询语句
     */
    @PostMapping(value = "/audit_app/generate_sql")
    public String generateSql(@RequestBody SqlGenerateRequest request) {
        try {
            return sqlTool.generateSql(request.getQuestion(), request.getTableName());
        } catch (Exception e) {
            e.printStackTrace();
            return "生成SQL失败: " + e.getMessage();
        }
    }
    
    /**
     * 实时分析审计过程
     * 
     * @param auditProcess 审计过程描述
     * @return 分析结果
     */
    @PostMapping(value = "/audit_app/analyze")
    public String analyzeAuditProcess(@RequestBody String auditProcess) {
        try {
            return auditApp.analyzeAuditProcess(auditProcess);
        } catch (Exception e) {
            e.printStackTrace();
            return "分析失败: " + e.getMessage();
        }
    }
    
    /**
     * 获取单元格值作为字符串
     * 
     * @param cell 单元格
     * @return 字符串值
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // 处理数字，避免科学计数法
                    cell.setCellType(CellType.STRING);
                    return cell.getStringCellValue();
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }
            default:
                return "";
        }
    }
}

