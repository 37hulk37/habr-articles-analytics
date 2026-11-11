package com.hulk.processor.article.activity;

import com.hulk.processor.model.ActivityArticleDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ActivityArticleRepository extends ElasticsearchRepository<ActivityArticleDocument, String> {
}
