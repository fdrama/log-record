package com.simple.log;

import com.simple.log.config.EnableLogRecord;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author fdrama
 */
@SpringBootApplication
@EnableLogRecord(tenant = "LOG-RECORD-TEST")
public class LogRecordTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogRecordTestApplication.class, args);
    }

}
