package com.example.technodomkz;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApplicationProperties {
    @Value("${parser.page.timeout}")
    private long timeout;
    @Value("${parser.retry.count}")
    private int retryCount;

    public long getTimeout() {
        return timeout;
    }

    public int getRetryCount() {
        return retryCount;
    }
}
