package com.simple.log.function.record;


import com.simple.log.model.LogRecordDO;

import lombok.extern.slf4j.Slf4j;

/**
 * @author muzhantong
 * create on 2020/4/29 4:34 下午
 */
@Slf4j
public class DefaultLogRecordServiceImpl implements ILogRecordService {

    @Override
    public void record(LogRecordDO logRecord) {
        log.info("【logRecord】log={}", logRecord);
    }
}
