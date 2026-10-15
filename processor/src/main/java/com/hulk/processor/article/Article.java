package com.hulk.processor.article;

import java.time.Instant;
import java.util.List;

public record Article(
    String id,
    String url,
    Instant timePublished,
    String postType,
    String title,
    String lead,
    Statistics statistics,
    List<Hub> hubs,
    List<String> tags,
    List<String> matchedTokens
) {
}
