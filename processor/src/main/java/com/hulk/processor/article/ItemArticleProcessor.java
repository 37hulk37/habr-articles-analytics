package com.hulk.processor.article;

import com.hulk.processor.model.ArticleDocument;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ItemArticleProcessor implements ItemProcessor<Article, ArticleDocument> {

    private final ArticleRepository articleRepository;

    @Override
    public @NonNull ArticleDocument process(@NonNull Article item) {
        var document = new ArticleDocument();

        document.setId(UUID.randomUUID());
        document.setArticleId(item.id());
        document.setUrl(item.url());
        document.setPublishedAt(item.timePublished());
        document.setTitle(item.title());
        document.setTags(item.tags());
        document.setMatchedTokens(item.matchedTokens());

        return document;
    }

}
