package com.cosmo.aiagent.app;

import cn.hutool.core.lang.UUID;
import com.cosmo.aiagent.agent.Manus;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuditAppTest {

    @Resource
    private AuditApp auditApp;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，你能帮我做审计问题的法规依据检索嘛？";
        String answer = auditApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第二轮
        message = "执行项目合同支付条款不合规。因项目业主对合同支付条款审核不严、资金监管不到位等原因，个项目施工合同约定的支付条件与招标文件要求不符，其中个合同将工程预付款比例从10%降至5%";
        answer = auditApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第三轮
        message = "刚刚的审计问题叫什么来着？刚跟你说过，帮我回忆一下";
        answer = auditApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void testChatByStreamAndSync() {
        String chatId1 = UUID.randomUUID().toString();
        String chatId2 = UUID.randomUUID().toString();
        String message = "执行项目合同支付条款不合规。因项目业主对合同支付条款审核不严、资金监管不到位等原因，个项目施工合同条款支付条件与招标文件要求不符，其中个合同将工程预付款比例从10%降至5%";
        String answer1 = auditApp.doChat(message, chatId1);
        String answer2 = auditApp.doChatByStream(message, chatId2).collectList().map(list  -> String.join("", list)).block();
        System.out.println(answer1);
        System.out.println(answer2);
        Assertions.assertEquals(answer1, answer2);
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "某审计局在进行2020年度财政预算执行、决算草案和其他财政收支情况审计时经抽查发现，截至2020年12月31日，在财政全额安排资金的项目中，有30个工程建设项目未及时编报竣工财务决算。";
        AuditApp.AuditReport auditReport = auditApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(auditReport);
    }

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "执行项目合同支付条款不合规。因项目业主对合同支付条款审核不严、资金监管不到位等原因，个项目施工合同约定的支付条件与招标文件要求不符，其中个合同将工程预付款比例从10%降至5%，**个合同延迟支付进度款超过合同约定的30个自然日；个项目的材料采购合同未明确价格调整机制，导致超付金额达万元；**个设计合同未按约定支付节点提交成果，造成施工延期1个月，监理单位未按合同约定对付款流程进行审核";
        String answer =  auditApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithTools() {
        // 测试执行工具
        testMessage("运营想要计算一些参加了答题的不同学校、不同难度的用户平均答题量，请你取出相应数据");

//        testMessage("执行项目合同支付条款不合规。因项目业主对合同支付条款审核不严、资金监管不到位等原因，个项目施工合同约定的支付条件与招标文件要求不符，其中个合同将工程预付款比例从10%降至5%，**个合同延迟支付进度款超过合同约定的30个自然日；个项目的材料采购合同未明确价格调整机制，导致超付金额达万元；**个设计合同未按约定支付节点提交成果，造成施工延期1个月，监理单位未按合同约定对付款流程进行审核。根据模板文件：审计工作底稿模板文件.docx,生成一份Word文件");

    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = auditApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }


    @Test
    void doChatWithPrompt() {
        String chatId = UUID.randomUUID().toString();
        String message = "早上好";
        String answer = auditApp.doChatWithPrompt(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Resource

    private Manus manus;
    @Test
    void run(){
        String UserPrompt = "今天是周二，你今天要吃什么？";
        String result = auditApp.doChat(UserPrompt, "");
        System.out.println(result);
    }
}

