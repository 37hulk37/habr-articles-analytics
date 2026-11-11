package com.hulk.processor.article;

import com.hulk.processor.model.ArticleDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.UUID;

public interface ArticleRepository extends ElasticsearchRepository<ArticleDocument, UUID> {
}
