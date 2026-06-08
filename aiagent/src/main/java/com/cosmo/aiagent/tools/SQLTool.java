package com.cosmo.aiagent.tools;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.annotation.Tool;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import java.sql.*;

@Component
public class SQLTool {
    // Ollama聊天模型实例
    private final ChatModel chatModel;
    
    // 存储最近上传的表格文件路径
    private static String latestTableFilePath = null;
    
    // 存储表格结构和示例数据
    private static Map<String, List<String>> tableColumns = new HashMap<>();
    private static Map<String, List<Map<String, String>>> tableData = new HashMap<>();
    
    // 存储实际在数据库中创建的表名
    private static String actualTableName = null;

    // 构造函数注入ChatModel，指定使用ollamaChatModel
    @Autowired
    public SQLTool(@Qualifier("ollamaChatModel") ChatModel chatModel) {
        this.chatModel = chatModel;
    }
    
    /**
     * 设置最近上传的表格文件路径和实际表名
     * @param filePath 表格文件路径
     * @param tableName 实际在数据库中创建的表名
     */
    public static void setLatestTableFilePath(String filePath, String tableName) {
        latestTableFilePath = filePath;
        actualTableName = tableName;
        try {
            // 读取表格结构和数据
            readTableFromFile(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 设置最近上传的表格文件路径
     * @param filePath 表格文件路径
     * @deprecated 请使用带表名参数的setLatestTableFilePath方法
     */
    @Deprecated
    public static void setLatestTableFilePath(String filePath) {
        setLatestTableFilePath(filePath, null);
    }
    
    /**
     * 从文件中读取表格结构和数据
     * @param filePath 文件路径
     * @throws IOException
     */
    private static void readTableFromFile(String filePath) throws IOException {
        File file = new File(filePath);
        String fileName = file.getName().toLowerCase();
        String tableName = fileName.replaceFirst("\\.[^.]+$", ""); // 移除文件扩展名作为表名
        
        if (fileName.endsWith(".csv")) {
            readCsvTable(file, tableName);
        } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            readExcelTable(file, tableName);
        }
    }
    
    /**
     * 读取CSV文件中的表格数据
     * @param file CSV文件
     * @param tableName 表名
     * @throws IOException
     */
    private static void readCsvTable(File file, String tableName) throws IOException {
        List<String> columns = new ArrayList<>();
        List<Map<String, String>> data = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                
                if (isFirstLine) {
                    // 第一行是列名
                    for (String value : values) {
                        columns.add(value.trim());
                    }
                    isFirstLine = false;
                } else {
                    // 数据行
                    Map<String, String> row = new HashMap<>();
                    for (int i = 0; i < Math.min(values.length, columns.size()); i++) {
                        row.put(columns.get(i), values[i].trim());
                    }
                    data.add(row);
                    
                    // 只读取前10行作为示例
                    if (data.size() >= 10) {
                        break;
                    }
                }
            }
        }
        
        tableColumns.put(tableName, columns);
        tableData.put(tableName, data);
    }
    
    /**
     * 读取Excel文件中的表格数据
     * @param file Excel文件
     * @param tableName 表名
     * @throws IOException
     */
    private static void readExcelTable(File file, String tableName) throws IOException {
        List<String> columns = new ArrayList<>();
        List<Map<String, String>> data = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = file.getName().toLowerCase().endsWith(".xlsx") ? 
                                new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {
            
            // 只读取第一个工作表
            Sheet sheet = workbook.getSheetAt(0);
            boolean isFirstRow = true;
            
            for (Row row : sheet) {
                if (isFirstRow) {
                    // 第一行是列名
                    for (Cell cell : row) {
                        columns.add(getCellValue(cell).trim());
                    }
                    isFirstRow = false;
                } else {
                    // 数据行
                    Map<String, String> rowData = new HashMap<>();
                    for (int i = 0; i < Math.min(row.getLastCellNum(), columns.size()); i++) {
                        Cell cell = row.getCell(i);
                        rowData.put(columns.get(i), getCellValue(cell).trim());
                    }
                    data.add(rowData);
                    
                    // 只读取前10行作为示例
                    if (data.size() >= 10) {
                        break;
                    }
                }
            }
        }
        
        tableColumns.put(tableName, columns);
        tableData.put(tableName, data);
    }
    
    /**
     * 获取Excel单元格的值
     * @param cell Excel单元格
     * @return 单元格值的字符串表示
     */
    private static String getCellValue(Cell cell) {
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
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * 获取表的结构信息（兼容旧代码，实际不再使用）
     * @param tableName 表名
     * @return 表结构信息
     */
    private String getTableStructure(String tableName) throws SQLException {
        return "表结构信息已从上传的表格文件中获取";
    }

    /**
     * 调用Ollama模型生成SQL查询语句
     * @param userQuestion 用户的自然语言问题
     * @param tableName 表名（已废弃，现在使用上传的表格文件）
     * @return 生成的SQL查询语句
     */
    @Tool(name = "generateSql", description = "根据用户的自然语言问题和上传的表格文件生成SQL查询语句")
    public String generateSql(@ToolParam(description = "用户的自然语言问题") String userQuestion,
                              @ToolParam(description = "表名") String tableName) {
        try {
            if (tableColumns.isEmpty() || tableData.isEmpty()) {
                return "请先上传表格文件";
            }
            
            // 使用实际在数据库中创建的表名，如果没有则使用从文件中读取的表名
            String dbTableName = actualTableName;
            if (dbTableName == null || dbTableName.isEmpty()) {
                // 获取第一个表名（因为当前只支持一个上传的表格）
                dbTableName = tableColumns.keySet().iterator().next();
            }
            
            List<String> columns = tableColumns.get(tableColumns.keySet().iterator().next());
            List<Map<String, String>> data = tableData.get(tableData.keySet().iterator().next());
            
            // 构建表结构信息
            StringBuilder tableStructure = new StringBuilder();
            tableStructure.append("表 `").append(dbTableName).append("` 的结构信息：\n");
            tableStructure.append("列名\n");
            
            for (String column : columns) {
                tableStructure.append(column).append("\n");
            }
            
            // 添加示例数据
            tableStructure.append("\n表 `").append(dbTableName).append("` 的数据示例（前10行）：\n");
            
            // 打印列名
            for (String column : columns) {
                tableStructure.append(column);
                if (!column.equals(columns.get(columns.size() - 1))) {
                    tableStructure.append(" | ");
                }
            }
            tableStructure.append("\n");
            
            // 打印数据行
            for (Map<String, String> row : data) {
                for (int i = 0; i < columns.size(); i++) {
                    String column = columns.get(i);
                    String value = row.getOrDefault(column, "");
                    tableStructure.append(value);
                    if (i < columns.size() - 1) {
                        tableStructure.append(" | ");
                    }
                }
                tableStructure.append("\n");
            }

            // 创建PromptTemplate
            PromptTemplate promptTemplate = new PromptTemplate("""
                你是一位专业的SQL查询生成器，请根据用户的问题和提供的表结构，生成对应的SQL查询语句。
                1. 仅生成SQL查询语句，不要包含其他任何解释或说明文字
                2. 使用标准SQL语法，确保查询的正确性
                3. 请严格基于提供的表结构进行查询，不要添加任何额外的表或列
                4. 确保生成的SQL查询语句可以直接在关系型数据库中执行
                5. 如果需要，使用JOIN语句连接相关表
                6. 确保WHERE子句的条件正确，避免语法错误
                7. 对于字符串类型的条件值，请使用单引号括起来
                8. 确保查询的列名与提供的表结构中的列名一致
                
                表结构信息：
                {tableStructure}
                
                用户的问题：
                {userQuestion}
            """);

            // 创建Prompt并设置模板参数
            Prompt prompt = promptTemplate.create(Map.of(
                    "tableStructure", tableStructure.toString(),
                    "userQuestion", userQuestion
            ));

            // 调用ChatModel生成SQL查询语句
            return chatModel.call(prompt).getResult().getOutput().getText();

        } catch (Exception e) {
            return "生成SQL失败: " + e.getMessage();
        }
    }

    /**
     * 执行SQL查询并返回结果
     * @param sql SQL查询语句
     * @return 查询结果的JSON格式
     */
    @Tool(name = "executeSql", description = "执行SQL查询并返回结果")
    public String executeSql(@ToolParam(description = "sql") String sql) {
        try {
            // 清理SQL语句，去除可能的前缀和引号
            sql = sql.trim();
            if (sql.toLowerCase().startsWith("sql")) {
                sql = sql.substring(3).trim();
            }
            if ((sql.startsWith("'")) && sql.endsWith("'")) {
                sql = sql.substring(1, sql.length() - 1).trim();
            }
            if ((sql.startsWith("\"")) && sql.endsWith("\"")) {
                sql = sql.substring(1, sql.length() - 1).trim();
            }
            if ((sql.startsWith("`")) && sql.endsWith("`")) {
                sql = sql.substring(1, sql.length() - 1).trim();
            }
            
            // 连接到MySQL数据库
            String url = "jdbc:mysql://localhost:3306/aiagent?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&useLegacyDatetimeCode=false";
            String username = "root";
            String password = "123456";
            
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement()) {
                
                // 执行SQL语句
                boolean hasResultSet = stmt.execute(sql);
                
                Gson gson = new Gson();
                JsonObject result = new JsonObject();
                
                if (hasResultSet) {
                    // 处理查询结果
                    try (ResultSet rs = stmt.getResultSet()) {
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        
                        // 构建列信息
                        JsonArray columnsArray = new JsonArray();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);
                            JsonObject column = new JsonObject();
                            column.addProperty("name", columnName);
                            column.addProperty("title", columnName);
                            column.addProperty("dataIndex", columnName);
                            column.addProperty("key", columnName);
                            columnsArray.add(column);
                        }
                        
                        // 构建数据信息
                        JsonArray dataArray = new JsonArray();
                        while (rs.next()) {
                            JsonObject row = new JsonObject();
                            for (int i = 1; i <= columnCount; i++) {
                                String columnName = metaData.getColumnName(i);
                                Object value = rs.getObject(i);
                                row.addProperty(columnName, value != null ? value.toString() : "");
                            }
                            dataArray.add(row);
                        }
                        
                        result.add("columns", columnsArray);
                        result.add("data", dataArray);
                        result.addProperty("status", "success");
                    }
                } else {
                    // 处理更新语句
                    int updateCount = stmt.getUpdateCount();
                    result.addProperty("status", "success");
                    result.addProperty("message", "执行成功，影响行数: " + updateCount);
                }
                
                return gson.toJson(result);
                
            } catch (SQLException e) {
                // 处理SQL异常，记录详细错误信息
                e.printStackTrace();
                
                // 构建错误响应
                Gson gson = new Gson();
                JsonObject errorResult = new JsonObject();
                errorResult.addProperty("status", "error");
                errorResult.addProperty("message", "SQL执行失败: " + e.getMessage());
                errorResult.addProperty("sql", sql);
                
                return gson.toJson(errorResult);
            }

        } catch (Exception e) {
            // 处理其他异常
            e.printStackTrace();
            
            Gson gson = new Gson();
            JsonObject errorResult = new JsonObject();
            errorResult.addProperty("status", "error");
            errorResult.addProperty("message", "SQL执行失败: " + e.getMessage());
            
            return gson.toJson(errorResult);
        }
    }
}