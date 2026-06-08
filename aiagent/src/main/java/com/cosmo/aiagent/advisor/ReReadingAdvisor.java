package com.cosmo.aiagent.advisor;

import org.springframework.ai.chat.client.advisor.api.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义 Re2 Advisor
 * 可提高大型语言模型的推理能力
 */
public class ReReadingAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    /**
     * 执行请求前，改写 Prompt
     * @param advisedRequest
     * @return
     */
    private AdvisedRequest before(AdvisedRequest advisedRequest) {

        Map<String, Object> advisedUserParams = new HashMap<>(advisedRequest.userParams());
        advisedUserParams.put("re2_input_query", advisedRequest.userText());

        //这个方法由框架提供，用于创建新的AdvisedRequest对象
        return AdvisedRequest.from(advisedRequest)
                .userText("""
			    {re2_input_query}
			    Read the question again: {re2_input_query}
			    """)
                .userParams(advisedUserParams)
                .build();
    }

    /**
     * 环绕调用处理方法，在方法调用前后执行自定义逻辑
     *
     * @param advisedRequest 包含被调用方法信息的请求对象
     * @param chain 调用链对象，用于继续执行后续的环绕通知
     * @return 处理后的响应结果
     */
    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        return chain.nextAroundCall(this.before(advisedRequest));
    }

    /**
     * 环绕流式调用处理方法，在流式方法调用前后执行自定义逻辑
     *
     * @param advisedRequest 包含被调用方法信息的请求对象
     * @param chain 流式调用链对象，用于继续执行后续的环绕通知
     * @return 处理后的响应结果流
     */

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        return chain.nextAroundStream(this.before(advisedRequest));
    }

    /**
     * 获取当前通知的执行顺序
     *
     * @return 通知执行顺序，数值越小优先级越高
     */

    @Override
    public int getOrder() {
        return 0;
    }


    /**
     * 获取当前通知的名称
     *
     * @return 通知类的简单类名
     */
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}