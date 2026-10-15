package com.hulk.processor.article;

public record Statistics(
    Integer commentsCount,
    Integer favoritesCount,
    Integer readingCount,
    Integer score,
    Integer votesCount,
    Integer votesCountPlus,
    Integer votesCountMinus,
    Integer reach,
    Integer readers
) {
}
