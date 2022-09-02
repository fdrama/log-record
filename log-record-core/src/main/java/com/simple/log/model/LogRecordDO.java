package com.simple.log.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author fdrama
 */
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class LogRecordDO {

    /**
     * 租户
     */
    private String tenant;
    /**
     * 操作人
     */
    private String operator;

    /**
     * 保存的操作日志的类型，比如：订单类型、商品类型
     */
    private String type;
    /**
     * 日志的子类型，比如订单的C端日志，和订单的B端日志，type都是订单类型，但是子类型不一样
     */
    private String subType;

    /**
     * 日志绑定的业务标识
     */
    private String bizNo;

    /**
     * 日志内容
     */
    private String content;

    /**
     * 记录是否是操作失败的日志
     */
    private Boolean fail;

    /**
     * 日志的创建时间
     */
    private Long createTime;
    /**
     * 日志的额外信息
     *
     * @since 2.0.0 从detail 修改为extra
     */
    private String extra;

    /**
     * 打印日志的代码信息
     * 日志记录的ClassName、
     */
    private String className;

    /**
     * MethodName
     */
    private String methodName;


}