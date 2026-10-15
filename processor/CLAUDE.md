# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

- Build: `./gradlew clean assemble`
- Run all tests: `./gradlew test`
- Run a single test class: `./gradlew test --tests "com.hulk.processor.ApplicationTests"`
- Run the app locally: `./gradlew bootRun` (requires Postgres up via `docker-compose up -d postgres`; Elasticsearch and Kafka are referenced in `application.yaml` but not yet provisioned — see TODOs below)
- CI mirrors `build.gradle.kts` via `.github/workflows/gradle.yml` (JDK 21, `./gradlew clean assemble` on push/PR to `master`)

## Architecture

This is a Spring Batch application (Java 21, Spring Boot 3.5) that consumes GitHub repository events from Kafka, persists metrics to Elasticsearch, and runs ML-based predictions on repositories. It is one service in a larger pipeline — a separate loader service presumably publishes `Repository` records onto the Kafka topic this app reads from.

### Two batch jobs, driven by one topic

`BatchConfiguration` wires two independent Spring Batch jobs, both reading from the same Kafka topic (`app.kafka.consumer.topic`) but with different consumer group IDs (`KafkaConfiguration`), so each job gets its own copy of every message:

- **`articleJob`** (`articleStep`, chunk size 100): reads `Articles` from directory

Both steps use `.readerIsTransactionalQueue()` and `.faultTolerant()` since the reader is a Kafka queue, not a resumable datastore.

### Job triggering: scheduled + on-demand

`RepositoryScheduler` fires both jobs on cron schedules (`repositoryMetricsJob` every 30 min, `repositoryMlJob` hourly), each on its own dedicated virtual-thread `ExecutorService` bean (`repositorySchedulerExecutor` / `repositoryMlSchedulerExecutor`) via `@Async`. `RepositoryJobController` (`/api/repositories/jobs`) exposes the same capability over HTTP (start a job by name, list running executions, list job names), backed by `RepositoryJobManager`, which resolves job names to `Job` beans (`afterPropertiesSet`) and delegates to Spring Batch's `JobLauncher`/`JobExplorer`. Any new `Job` bean is automatically picked up here — no registration step beyond defining the bean.

### Writer pattern

New Elasticsearch sinks follow a fixed shape: an `ElasticsearchRepository<Document, UUID>` + an `ItemWriter` that extends either `AbstractItemWriter` (plain `Repository` input, like the metrics writers) or `AbstractCompletableFutureItemWriter` (futures-based input, like the ML writers), passing a `Function<V, D>` mapper and the Elasticsearch repository into the inherited `write(...)`. Document model classes live in `model/`.

### Persistence

- **Postgres**: used only as the Spring Batch job repository (`spring.batch.jdbc.initialize-schema: never` — schema is managed by Liquibase, `changelog/db.changelog-master.yaml`), not for domain data.
- **Elasticsearch**: the actual data store for all domain documents (commits, collaborators, languages, support duration, ML outputs). `ElasticSearchConfiguration` builds the client from the first URI in `spring.elasticsearch.uris` plus basic auth.

### Known gaps (don't assume these are wired up)

- `application.yaml` has `elasticsearch.uris` and `kafka.bootstrap-servers` marked `//todo` — only Postgres is actually provisioned in `docker-compose.yaml` (the `app` service itself is commented out).
