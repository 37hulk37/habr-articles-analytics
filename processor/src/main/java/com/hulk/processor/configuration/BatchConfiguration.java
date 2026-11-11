package com.hulk.processor.configuration;

import com.hulk.processor.article.Article;
import com.hulk.processor.article.ArticleRepository;
import com.hulk.processor.article.ItemArticleProcessor;
import com.hulk.processor.model.ArticleDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.json.JacksonJsonObjectReader;
import org.springframework.batch.infrastructure.item.json.builder.JsonItemReaderBuilder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@EnableConfigurationProperties(ArticleProperties.class)
@RequiredArgsConstructor
public class BatchConfiguration implements InitializingBean {

    private final ArticleProperties properties;

    //todo: create path initializer
    @Override
    public void afterPropertiesSet() throws IOException {
        Files.createDirectories(Path.of(
            properties.directory().outputDirectory())
        );
    }

    @Bean
    @StepScope
    public ItemReader<Article> articleItemReader(
        @Value("#{stepExecutionContext['fileName']}") String fileName
    ) throws MalformedURLException {
        return new JsonItemReaderBuilder<Article>()
            .jsonObjectReader(new JacksonJsonObjectReader<>(Article.class))
            .resource(new UrlResource(fileName))
            .name("articleJsonItemReader")
            .build();
    }

    @Bean
    public Step articleWorkerStep(
        JobRepository jobRepository,
        ItemReader<Article> articleItemReader,
        ItemArticleProcessor itemArticleProcessor,
        ItemWriter<ArticleDocument> articleItemWriter,
        ArticleProperties properties
    ) {
        return new StepBuilder("articleWorkerStep", jobRepository)
            .<Article, ArticleDocument>chunk(100)
            .reader(articleItemReader)
            .processor(itemArticleProcessor)
            .writer(articleItemWriter)
            .faultTolerant()
            .retry(Exception.class)
            .retryLimit(properties.batch().retryLimit())
            .build();
    }

    @Bean
    public Step articleStep(
        JobRepository jobRepository,
        Step articleWorkerStep,
        ArticleProperties properties,
        TaskExecutor articlePartitionTaskExecutor
    ) throws IOException {
        var resources = new PathMatchingResourcePatternResolver().getResources(properties.directory().sourcePattern());

        var partitioner = new MultiResourcePartitioner();
        partitioner.setResources(resources);

        return new StepBuilder("articleStep", jobRepository)
            .partitioner("articleWorkerStep", partitioner)
            .step(articleWorkerStep)
            .taskExecutor(articlePartitionTaskExecutor)
            .build();
    }

    @Bean
    public Job articleJob(Step articleStep, JobRepository jobRepository) {
        return new JobBuilder("articleJob", jobRepository)
            .start(articleStep)
            .build();
    }

    @Bean
    public TaskExecutor articlePartitionTaskExecutor(ArticleProperties properties) {
        var executor = new SimpleAsyncTaskExecutor("article-partition-");
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(properties.batch().partitionConcurrencyLimit());

        return executor;
    }

}
