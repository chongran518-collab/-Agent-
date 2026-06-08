package com.cosmo.aiagent.app;

import com.cosmo.aiagent.advisor.MyLoggerAdvisor;
import com.cosmo.aiagent.chatmemory.FileBasedChatMemory;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class AuditApp {

    private final ChatClient chatClient;

//    private static final String SYSTEM_PROMPT ="你的任务是：分析用户输入，识别其中包含的审计问题，并进行审计问题推理。"+
//            "第一步：阅读和理解用户输入，识别可能存在的审计问题。 输出你的学习结果。"+
//            "第二步：根据第一步的学习结果，建立审计问题推理框架,运用审计问题推理框架对用户输入中的每一个可能的审计问题进行深入推理"+
//            "结构化输出如下：审计问题：问题事实：涉嫌违规：利益相关方及应承担责任：审计整改类型：根据你对从审计问题的推理，确定审计问题整改类型（属于立行立改？或是分阶段整改？或是持续整改？）";

    private static final String SYSTEM_PROMPT = """
            你是一个全能的AI助手，能够解答用户提出的各种问题，包括但不限于审计问题。
            请根据用户的具体问题，提供准确、专业和有用的回答。
            
            如果用户的问题涉及审计内容，请按照以下步骤和格式进行输出：
            
            ## 第一步：识别审计问题
            阅读并理解用户输入，识别其中存在的所有审计问题。
            
            ## 第二步：审计问题推理
            对每个识别出的审计问题，按照以下结构化格式进行分析：
            
            1. **审计问题**：简明扼要描述发现的审计问题
            2. **问题事实**：详细列出审计问题的具体事实
            3. **涉嫌违规**：说明该问题可能违反的法律法规或规定
            4. **整改建议**：提出合理的审计整改建议
            
            对于非审计问题，请直接提供相关的专业知识和解答。
            """;

    public AuditApp(@Qualifier("ollamaChatModel") ChatModel ollamaChatModel) {
//        // 初始化基于文件的对话记忆
//        String fileDir = System.getProperty("user.dir") + "/chat-memory";
//        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        // 初始化基于内存的对话记忆
        ChatMemory chatMemory = new InMemoryChatMemory();
        chatClient = ChatClient.builder(ollamaChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
//                        new MessageChatMemoryAdvisor(chatMemory),
                        new MyLoggerAdvisor()
                )
                .build();
    }

    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 1))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
    
    /**
     * AI 基础对话（支持文件上传）
     * 
     * @param message
     * @param chatId
     * @param filePath
     * @return
     */
    public String doChat(String message, String chatId, String filePath) {
        // 处理文件内容并与消息合并
        String processedMessage = processFileAndMessage(message, filePath);
        return doChat(processedMessage, chatId);
    }

    /**
     * AI 基础对话（支持多轮对话记忆，SSE 流式传输）
     *
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new QuestionAnswerAdvisor(auditAppVectorStore))
                .stream()
                .content();
    }
    
    /**
     * AI 基础对话（支持文件上传，SSE 流式传输）
     * 
     * @param message
     * @param chatId
     * @param filePath
     * @return
     */
    public Flux<String> doChatByStream(String message, String chatId, String filePath) {
        // 处理文件内容并与消息合并
        String processedMessage = processFileAndMessage(message, filePath);
        return doChatByStream(processedMessage, chatId);
    }

   public record AuditReport(String title, String factsAndConclusions) {

    }

    /**
     * AI 报告功能（实战结构化输出）
     * @param message
     * @param chatId
     * @return
     */
    public AuditReport doChatWithReport(String message, String chatId) {
        AuditReport auditReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成结果，标题为{审计问题}的审计报告，内容为审计认定的事实摘要及审计结论列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new QuestionAnswerAdvisor(auditAppVectorStore))
                .call()
                .entity(AuditReport.class);
        log.info("auditReport: {}", auditReport);
        return auditReport;
    }

    /**
     *  rag 功能
     */
    @Resource
    private VectorStore auditAppVectorStore;

    public String doChatWithRag(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // 应用知识库问答
                .advisors(new QuestionAnswerAdvisor(auditAppVectorStore))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    // AI 调用工具能力
    @Resource
    private ToolCallback[] allTools;

    /**
     * AI 报告功能（支持调用工具）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithTools(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .tools(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
    
    /**
     * 处理文件内容并与消息合并
     * 
     * @param message 用户消息
     * @param filePath 文件路径
     * @return 处理后的消息
     */
    private String processFileAndMessage(String message, String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return message;
        }
        
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                log.error("文件不存在: {}", filePath);
                return message + "\n\n注意：上传的文件不存在。";
            }
            
            String fileContent = readFileContent(file);
            
            // 限制文件内容大小，避免消息过长
            int maxContentLength = 10000;
            if (fileContent.length() > maxContentLength) {
                fileContent = fileContent.substring(0, maxContentLength) + "\n\n...（文件内容过长，已截断）";
            }
            
            return message + "\n\n用户上传了文件内容如下：\n" + fileContent;
            
        } catch (Exception e) {
            log.error("处理文件时出错: {}", e.getMessage(), e);
            return message + "\n\n注意：处理上传的文件时出错。";
        }
    }
    
    /**
     * 根据文件类型读取文件内容
     * 
     * @param file 文件对象
     * @return 文件内容
     * @throws IOException
     */
    private String readFileContent(File file) throws IOException {
        String fileName = file.getName().toLowerCase();
        
        if (fileName.endsWith(".csv")) {
            return readCsvFile(file);
        } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            return readExcelFile(file);
        } else {
            // 默认读取为文本文件
            return readTextFile(file);
        }
    }
    
    /**
     * 读取CSV文件内容
     * 
     * @param file CSV文件
     * @return 文件内容
     * @throws IOException
     */
    private String readCsvFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
    
    /**
     * 读取Excel文件内容
     * 
     * @param file Excel文件
     * @return 文件内容
     * @throws IOException
     */
    private String readExcelFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = file.getName().toLowerCase().endsWith(".xlsx") ? 
                                new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {
            
            // 只读取第一个工作表
            Sheet sheet = workbook.getSheetAt(0);
            
            for (Row row : sheet) {
                for (Cell cell : row) {
                    // 根据单元格类型获取内容
                    switch (cell.getCellType()) {
                        case STRING:
                            content.append(cell.getStringCellValue()).append("\t");
                            break;
                        case NUMERIC:
                            if (DateUtil.isCellDateFormatted(cell)) {
                                content.append(cell.getDateCellValue()).append("\t");
                            } else {
                                content.append(cell.getNumericCellValue()).append("\t");
                            }
                            break;
                        case BOOLEAN:
                            content.append(cell.getBooleanCellValue()).append("\t");
                            break;
                        case FORMULA:
                            content.append(cell.getCellFormula()).append("\t");
                            break;
                        default:
                            content.append("\t");
                    }
                }
                content.append("\n");
            }
        }
        
        return content.toString();
    }
    
    /**
     * 读取文本文件内容
     * 
     * @param file 文本文件
     * @return 文件内容
     * @throws IOException
     */
    private String readTextFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    public String doChatWithPrompt(String userMessage, String chatId) {
        List<Message> messageList = new ArrayList<>();
        messageList.add(new UserMessage(userMessage));
        messageList.add(new UserMessage("你好"));

        Prompt prompt=new Prompt(messageList);
        return chatClient
                .prompt(prompt)
                .advisors(new MyLoggerAdvisor())
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse()
                .getResult()
                .getOutput()
                .getText();

    }
    
    /**
     * AI实时分析审计过程
     * 
     * @param auditProcess 审计过程描述
     * @return 分析结果
     */
    public String analyzeAuditProcess(String auditProcess) {
        String prompt = "请对以下审计过程进行实时分析，并提供结构化的分析结果：\n\n" + auditProcess + "\n\n" +
                       "请按照以下结构进行分析：\n" +
                       "1. 审计过程概述\n" +
                       "2. 可能存在的审计风险\n" +
                       "3. 审计程序建议\n" +
                       "4. 整改方向\n" +
                       "\n请确保分析结果简洁明了，重点突出。";
        
        ChatResponse response = chatClient
                .prompt()
                .user(prompt)
                .advisors(new MyLoggerAdvisor())
                .call()
                .chatResponse();
        
        return response.getResult().getOutput().getText();
    }

}
