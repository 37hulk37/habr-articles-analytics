# Processor

Spring Boot сервис для загрузки статей Хабра из JSON в Elasticsearch.
Стек: Java 25, Spring Boot 4.1.1, Spring Batch, Spring Data Elasticsearch,
PostgreSQL, Liquibase, Lombok.

## Структура

- `article/` — входная модель Article, ItemArticleProcessor, ItemArticleWriter и ArticleRepository.
- `model/` — Elasticsearch-документ ArticleDocument с аннотациями @Document, @Id, @Field.
- `configuration/` — настройка Batch и параметры входных файлов.
- `job/` — HTTP API запуска job, JobManager и ответ JobInfo.
- `data/` — входные JSON-массивы статей.
- `src/main/resources/changelog/` — Liquibase-схема служебных таблиц Batch.

## Обработка

```text
articleJob → articleStep, chunk=100
MultiResourceItemReader → JsonItemReader<Article>
                       → ItemArticleProcessor
                       → ItemArticleWriter → ArticleRepository.saveAll()
```

Reader последовательно читает файлы по `app.directory.source-pattern`.
Текущий путь `file:processor/data/*.json` рассчитан на рабочую директорию в корне репозитория.
Оба reader — step-scoped beans; возвращаемые типы должны включать ItemStream.

Processor переносит поля Article в ArticleDocument и создаёт UUID документа.
Исходный ID статьи хранится отдельно в articleId. Writer сохраняет полученный chunk.
Индекс — `habr-articles`; PostgreSQL используется только для метаданных Batch.
Повторная загрузка создаёт новые документы, поэтому уникальные статьи в графиках
считаются по articleId.

Автозапуск job отключён. `POST /api/jobs` с `{"jobName":"articleJob"}` запускает обработку;
`GET /api/jobs/names` возвращает имена jobs. Контроллер возвращает JobInfo,
а не служебный граф объектов JobExecution.

## Инфраструктура и Kibana

`docker-compose.yaml` содержит PostgreSQL, Elasticsearch и Kibana.
Настройки подключений — в `src/main/resources/application.yaml`.
Схемой Batch управляет Liquibase, автоматическая JDBC-инициализация отключена.

Kibana читает один индекс `habr-articles`: Lens показывает временной ряд,
Discover — статьи, Vega рассчитывает производные показатели.
Экспорт дашборда со всеми панелями и связанными объектами:
`kibana/dashboard-with-metrics.ndjson`. Он содержит настройки, но не статьи Elasticsearch.
Импорт: Stack Management → Saved Objects → Import в Kibana 9.4.7.
После изменения панелей обновлять этот экспорт через API Kibana.
Даты и тематический фильтр Vega заданы внутри запросов, независимо от общего
переключателя времени дашборда.
