package com.example.technodomkz;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
//@ConfigurationProperties(prefix = "parser.options")
public class WebDriverProperties {
    @Value("${parser.page.timeout:20000}")
    private long timeout;
    @Value("${parser.retry.count:3}")
    private int retryCount;

    public long getTimeout() {
        return timeout;
    }

    public int getRetryCount() {
        return retryCount;
    }
}
