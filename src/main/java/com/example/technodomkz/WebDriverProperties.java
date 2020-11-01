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
    @Value("${parser.chrome.path}")
    private String path;
    @Value("${technodom.api.chunk-size}")
    private Integer chunkSize;
    @Value("${technodom.thread-pool.pool-size}")
    private Integer threadPoolSize;
    @Value("${parser.modal-window.present.timeout-ms}")
    private Integer modalWindowTimeout;

    public long getTimeout() {
        return timeout;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getPath() {
        return path;
    }

    public Integer getChunkSize() {
        return chunkSize;
    }

    public Integer getThreadPoolSize() {
        return threadPoolSize;
    }

    public Integer getModalWindowTimeout() {
        return modalWindowTimeout;
    }
}
