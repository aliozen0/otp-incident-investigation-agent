# 14 — Implementation Plan

## Geliştirme döngüsü

Her faz:

1. spec,
2. failing test,
3. minimum implementation,
4. refactor,
5. documentation update

sırasını izler.

## M0 — Bootstrap

- Java 21 Maven project
- Spring Boot
- Dockerfile/Compose
- PostgreSQL + pgvector
- Flyway
- Actuator
- CI
- formatting

**Kabul:** `mvn verify`, compose up ve health UP.

## M1 — Domain foundation

- Investigation aggregate
- IncidentDraft aggregate
- value objects/enums
- invariants
- repository ports
- unit tests

**Kabul:** Domain Spring/LangChain4j olmadan test edilir.

## M2 — Fixture tools

- Fixture loader
- getOtpMetrics
- getErrorDistribution
- getQueueHealth
- getProviderHealth
- getRecentChanges
- tool envelope
- timeout simulation

**Kabul:** Her tool component testli ve fixture toplamları tutarlı.

## M3 — Persistence/audit

- Flyway schema
- repositories
- audit
- idempotency
- Testcontainers

**Kabul:** Restart sonrası GET, duplicate approval tek kayıt.

## M4 — RAG

- Knowledge fixture Markdown'ları
- ingestion/chunking
- embedding adapter
- pgvector search
- metadata filter
- retrieval tests

**Kabul:** Ana query için `INC-2026-041` top-5.

## M5 — Agent orchestration

- LangChain4j config
- AI Service
- @Tool adapters
- tool budget
- evidence collector
- structured output
- stub model
- prompt versioning

**Kabul:** Ana fixture expected tool'ları kullanır ve max 8 çağrı.

## M6 — Validation/governance

- evidence validator
- numeric claim validator
- forbidden action validator
- correlation warning
- PII scan
- prompt injection signal
- repair-once

**Kabul:** Güvenlik acceptance testleri geçer.

## M7 — REST/approval

- investigation POST/GET
- preview
- approve/reject
- problem details
- idempotency
- audit

**Kabul:** Ana Gherkin akışları uçtan uca geçer.

## M8 — Demo readiness

- Quickstart
- Swagger examples
- seed
- sample curl
- architecture diagram
- demo script
- clean logs
- failure demo
- optional minimal UI

**Kabul:** Temiz bilgisayarda tek komut, 5–7 dakikalık demo.

## Backlog önceliği

### P0

Bootstrap, domain, tools, RAG, structured result, validation, approval, ATDD, Docker.

### P1

Micrometer, tracing, live model profile, semantic eval.

### P2

UI, streaming, client examples, Grafana.

## Scope kuralı

Ana fixture, approval, RAG citation, Docker ve README tamamlanmadan Kafka, Redis, Kubernetes veya multi-agent eklenmez.

## M12.3 — Intent-aware OTP operational chat

1. Bounded-chat scope, API, ADR, acceptance ve ATDD'yi spec-first güncelle.
2. Failing testlerle intent/interaction/response ve evidence-bound visualization domain/application
   kontratlarını ekle.
3. Tool-free LangChain4j router/chat adapter'ları, purpose-specific stub'lar ve ayrı bounded semantic
   context oluştur.
4. `/api/v1/chat/messages` ile AUTO/CHAT/CLARIFICATION/INVESTIGATION orchestration'ını mevcut
   investigation pipeline'ına bağla.
5. Visualization validation, drop+warning, Flyway/JDBC canonical snapshot ve GET round trip ekle.
6. Frontend discriminated turns, store migration, adaptive composer/bubbles ve typed güvenli chart
   renderer ekle.
7. Offline suite, frontend test/build, WSL Compose stub ve onaylı sentetik live smoke doğrula.

**Kabul:** AC-041–AC-052; tool budget/duplicate/approval/idempotency regressionları korunur. Yeni
servis, multi-agent runtime, arbitrary visualization config veya kalıcı genel chat transcript yoktur.
