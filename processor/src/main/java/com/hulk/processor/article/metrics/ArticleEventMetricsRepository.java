package com.hulk.processor.article.metrics;

import com.hulk.processor.model.ArticleEventMetricsDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.UUID;

public interface ArticleEventMetricsRepository extends ElasticsearchRepository<ArticleEventMetricsDocument, UUID> {
}
