package com.hulk.processor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "habr-activity")
public class ActivityArticleDocument {

    @Id
    @Field(type = FieldType.Keyword)
    private UUID id;

    @Field(type = FieldType.Keyword)
    private String analysisId;

    @Field(type = FieldType.Keyword)
    private String event;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate date;

    @Field(type = FieldType.Keyword)
    private String granularity;

    @Field(type = FieldType.Keyword)
    private String dimension;

    @Field(type = FieldType.Keyword)
    private String value;

    @Field(type = FieldType.Long)
    private long articlesCount;

    @Field(type = FieldType.Integer)
    private int relativePeriod;
}
