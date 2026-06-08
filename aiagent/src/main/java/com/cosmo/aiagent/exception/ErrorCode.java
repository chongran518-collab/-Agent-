package com.cosmo.aiagent.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(200, "ok"),
    FAIL(500, "失败"),

    // 系统级别错误码
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN_ERROR(401, "未登录"),
    NO_AUTH_ERROR(-7, "无权限"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    SYSTEM_ERROR(500, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败"),
    ERROR(-1, "操作异常"),
    ERROR_PASSWORD(-8,"用户帐号或者密码错误!"),
    DISABLE_ACCOUNT(-12,"该账号已被管理员禁止登录!"),
    // 服务层面
    ERROR_EXCEPTION_MOBILE_CODE(10003,"验证码不正确或已过期，请重新输入"),
    ERROR_USER_NOT_EXIST(10009, "用户不存在"),
    PARAMS_ILLEGAL(10018,"参数不合法!!"),
    CATEGORY_IS_EXIST(10019,"该分类名称已存在!"),
    CATEGORY_IS_TOP(10020,"该分类已经在顶端!!"),
    TAG_IS_EXIST(10021,"该标签名已存在!"),
    CRAWLING_ARTICLE_FAILED(10022,"抓取文章失败!"),
    ARTICLE_NOT_EXIST(10023,"文章不存在!");
    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}