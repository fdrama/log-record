package com.simple.log.function.record;


import com.simple.log.model.LogRecordDO;


/**
 * @author fdrama
 */
public interface ILogRecordService {
    /**
     * 保存log
     *
     * @param logRecord 日志实体
     */
    void record(LogRecordDO logRecord);
}
