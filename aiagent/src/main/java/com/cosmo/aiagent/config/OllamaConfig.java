package com.cosmo.aiagent.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ollama本地模型配置类
 * 利用Spring AI Ollama starter的自动配置功能
 */
@Configuration
public class OllamaConfig {

    // 使用@Qualifier明确指定注入ollamaChatModel
    @Bean("localDeepSeekChatModel")
    public ChatModel localDeepSeekChatModel(@Autowired @Qualifier("ollamaChatModel") ChatModel ollamaChatModel) {
        // 直接返回Spring自动配置的Ollama ChatModel实例
        return ollamaChatModel;
    }
}
