package com.cosmo.aiagent.model.dto;

import lombok.Data;

@Data
public class AuditGenerateRequest {
    private String projectName;
    private String projectProfile;
    private String auditPerson;
    private String date;
    private String auditProcess;
}
