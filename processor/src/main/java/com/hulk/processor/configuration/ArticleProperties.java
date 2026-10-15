package com.hulk.processor.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record ArticleProperties(
    DirectoryProperties directory,
    BatchProperties batch
) {

    public record DirectoryProperties(
        String sourcePattern,
        String outputDirectory
    ){}

    public record BatchProperties(
        int partitionConcurrencyLimit,
        int retryLimit
    ){}

}
