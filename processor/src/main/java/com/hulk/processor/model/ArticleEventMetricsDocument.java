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
@Document(indexName = "habr-event-metrics")
public class ArticleEventMetricsDocument {

    @Id
    @Field(type = FieldType.Keyword)
    private UUID id;

    @Field(type = FieldType.Keyword)
    private String analysisId;

    @Field(type = FieldType.Keyword)
    private String event;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate eventDate;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate windowFrom;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate windowTo;

    @Field(type = FieldType.Keyword)
    private String timezone;

    @Field(type = FieldType.Keyword)
    private String methodologyVersion;

    @Field(type = FieldType.Integer)
    private int baselineDays;

    @Field(type = FieldType.Long)
    private long uniqueArticles;

    @Field(type = FieldType.Double)
    private double baselinePerDay;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate peakDate;

    @Field(type = FieldType.Long)
    private long peakCount;

    @Field(type = FieldType.Integer)
    private Integer daysToPeak;

    @Field(type = FieldType.Double)
    private Double peakHeight;

    @Field(type = FieldType.Integer)
    private Integer halfLifeDays;

    @Field(type = FieldType.Keyword)
    private String peakStatus;

    @Field(type = FieldType.Keyword)
    private String peakHeightStatus;

    @Field(type = FieldType.Keyword)
    private String halfLifeStatus;
}
