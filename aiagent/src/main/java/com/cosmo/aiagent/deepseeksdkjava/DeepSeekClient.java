package com.cosmo.aiagent.deepseeksdkjava;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * DeepSeek客户端，用于调用本地部署的DeepSeek服务
 */
public class DeepSeekClient {
    // 本地DeepSeek服务的默认地址
    private static final String DEFAULT_BASE_URL = "http://localhost:8000";
    private final String baseUrl;
    private final HttpClient httpClient;

    // 无参构造函数，使用默认地址
    public DeepSeekClient() {
        this(DEFAULT_BASE_URL);
    }

    // 带参构造函数，可指定服务地址
    public DeepSeekClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 调用DeepSeek的聊天接口
     * @param prompt 用户输入的提示词
     * @return 模型返回的结果
     * @throws IOException 网络IO异常
     * @throws InterruptedException 线程中断异常
     */
    public String chat(String prompt) throws IOException, InterruptedException {
        // 构建请求体
        String requestBody = String.format("""
                {
                    "prompt": "%s",
                    "max_tokens": 512,
                    "temperature": 0.7
                }""", escapeJson(prompt));

        // 创建HTTP请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        // 发送请求并获取响应
        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        return response.body();
    }

    /**
     * 转义JSON中的特殊字符
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}