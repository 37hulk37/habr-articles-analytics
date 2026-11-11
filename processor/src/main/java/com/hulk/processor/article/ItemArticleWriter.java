package com.hulk.processor.article;

import com.hulk.processor.model.ArticleDocument;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ItemArticleWriter implements ItemWriter<ArticleDocument> {

    private final ArticleRepository articleRepository;

    @Override
    public void write(@NonNull Chunk<? extends ArticleDocument> chunk) {
        articleRepository.saveAll(chunk.getItems());
    }

}
