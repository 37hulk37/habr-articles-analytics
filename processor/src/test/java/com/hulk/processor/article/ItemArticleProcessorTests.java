package com.hulk.processor.article;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemArticleProcessorTests {

    @Test
    void mapsArticle() {
        var article = new Article("248135", "https://habr.com/ru/articles/248135/",
            Instant.parse("2015-01-20T22:10:30Z"), "article", "Высокие значения DPI в ОС Windows",
            "lead", null, null, List.of("windows", "dpi"), List.of("DPI"));
        var processor = new ItemArticleProcessor(mock(ArticleRepository.class));

        var document = processor.process(article);

        assertNotNull(document.getId());
        assertEquals(article.id(), document.getArticleId());
        assertEquals(article.url(), document.getUrl());
        assertEquals(article.timePublished(), document.getPublishedAt());
        assertEquals(article.title(), document.getTitle());
        assertEquals(article.tags(), document.getTags());
        assertEquals(article.matchedTokens(), document.getMatchedTokens());
    }

}
