package com.hulk.processor.configuration;

import com.hulk.processor.article.Article;
import com.hulk.processor.model.ArticleDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.file.MultiResourceItemReader;
import org.springframework.batch.infrastructure.item.file.builder.MultiResourceItemReaderBuilder;
import org.springframework.batch.infrastructure.item.json.JacksonJsonObjectReader;
import org.springframework.batch.infrastructure.item.json.JsonItemReader;
import org.springframework.batch.infrastructure.item.json.builder.JsonItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
@EnableConfigurationProperties(ArticleProperties.class)
@RequiredArgsConstructor
public class BatchConfiguration {

    @Bean
    @StepScope
    public JsonItemReader<Article> articleJsonItemReader() {
        return new JsonItemReaderBuilder<Article>()
            .name("articleJsonItemReader")
            .jsonObjectReader(new JacksonJsonObjectReader<>(Article.class))
            .build();
    }

    @Bean
    @StepScope
    public MultiResourceItemReader<Article> articleItemReader(
        @Value("${app.directory.source-pattern}") Resource[] resources,
        JsonItemReader<Article> articleJsonItemReader
    ) {
        return new MultiResourceItemReaderBuilder<Article>()
            .name("articleMultiResourceItemReader")
            .resources(resources)
            .delegate(articleJsonItemReader)
            .setStrict(true)
            .build();
    }

    @Bean
    public Step articleStep(
        JobRepository jobRepository,
        ItemStreamReader<Article> articleItemReader,
        ItemProcessor<Article, ArticleDocument> articleProcessor,
        ItemWriter<ArticleDocument> articleItemWriter,
        ArticleProperties properties
    ) {
        return new StepBuilder("articleStep", jobRepository)
            .<Article, ArticleDocument>chunk(100)
            .reader(articleItemReader)
            .processor(articleProcessor)
            .writer(articleItemWriter)
            .faultTolerant()
            .retry(Exception.class)
            .retryLimit(properties.batch().retryLimit())
            .build();
    }

    @Bean
    public Job articleJob(Step articleStep, JobRepository jobRepository) {
        return new JobBuilder("articleJob", jobRepository)
            .start(articleStep)
            .build();
    }

}
