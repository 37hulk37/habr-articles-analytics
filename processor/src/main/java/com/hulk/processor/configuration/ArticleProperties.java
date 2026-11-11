package com.hulk.processor.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record ArticleProperties(
    BatchProperties batch
) {

    public record BatchProperties(
        int retryLimit
    ){}

}
