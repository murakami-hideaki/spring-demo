package com.example.demo.framework.service;

import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.demo.framework.log.WebLogger;

@Service
public class AsyncService {
    private static final WebLogger log = WebLogger.getLogger(AsyncService.class);

    @Async
    public void helloAsync() {
        String msg = "子スレッド： traceId=" + MDC.get("traceId");
        log.info(msg);
    }
}