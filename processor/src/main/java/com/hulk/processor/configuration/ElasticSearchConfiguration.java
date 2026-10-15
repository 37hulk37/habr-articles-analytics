package com.hulk.processor.configuration;

import com.hulk.processor.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

import java.util.Objects;

@Configuration
@RequiredArgsConstructor
public class ElasticSearchConfiguration extends ElasticsearchConfiguration {

    private final ElasticsearchProperties properties;

    @NonNull
    @Override
    public ClientConfiguration clientConfiguration() {
        var uri = properties.getUris().stream()
            .findFirst()
            .orElseThrow(() -> new ApplicationException("uri is empty"));

        return ClientConfiguration.builder()
            .connectedTo(uri)
            .withBasicAuth(
                Objects.requireNonNull(properties.getUsername()),
                Objects.requireNonNull(properties.getPassword())
            )
            .build();
    }
}
