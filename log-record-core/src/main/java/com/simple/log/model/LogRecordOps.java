package com.simple.log.model;

import lombok.Builder;
import lombok.Data;

/**
 * @author muzhantong
 * create on 2020/4/29 3:27 下午
 */
@Data
@Builder
public class LogRecordOps {
    /**
     * 成功日志模板
     */
    private String successLogTemplate;
    /**
     * 失败日志模板
     */
    private String failLogTemplate;
    /**
     * 操作人
     */
    private String operatorId;
    /**
     * 日志类型
     */
    private String type;
    /**
     * 日志类型 - 子类
     */
    private String subType;
    /**
     * 业务编号
     */
    private String bizNo;
    /**
     * 额外字段
     */
    private String extra;
    /**
     * 日志记录条件
     */
    private String condition;

    /**
     * 日志成功失败记录条件
     */
    private String successCondition;
}
