package com.cosmo.aiagent.model.dto;

import lombok.Data;

/**
 * SQL生成请求DTO
 */
@Data
public class SqlGenerateRequest {
    /**
     * 自然语言问题
     */
    private String question;
    
    /**
     * 上传的表名
     */
    private String tableName;
}