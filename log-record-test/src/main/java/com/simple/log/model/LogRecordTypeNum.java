package com.simple.log.model;

/**
 * @author fdrama
 * date 2022年09月02日 13:38
 */
public enum LogRecordTypeNum {
    /**
     * 订单类型
     */
    ORDER(Instance.ORDER, "订单");;


    public static class Instance {
        public static final String ORDER = "ORDER";
    }

    private final String code;

    private final String desc;

    LogRecordTypeNum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public String getCode() {
        return code;
    }
}
