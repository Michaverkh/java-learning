# Учебный план по Java Backend на 24 недели

План рассчитан на опытного frontend-разработчика со стеком React + TypeScript, базой в алгоритмах и структурах данных и
небольшим опытом Node.js.

Цель — не «выучить всю Java», а через полгода уметь:

- читать и ревьюить backend-код своего проекта;
- исправлять небольшие дефекты;
- добавлять REST-методы, бизнес-логику и миграции;
- писать unit- и integration-тесты;
- разбираться в транзакциях, конкурентном доступе и production-логах;
- самостоятельно сделать небольшой production-like сервис.

## Перед началом

Сначала необходимо изучить backend-репозиторий своей команды:

- версия Java;
- Spring Boot 2/3/4;
- Maven и его версия;
- JPA/Hibernate, Spring Data JDBC, jOOQ или чистый JDBC;
- PostgreSQL/Oracle;
- Kafka/RabbitMQ;
- способ авторизации;
- библиотеки тестирования;
- формат архитектуры и модулей.

Учиться следует на тех же версиях и инструментах. Для отдельного учебного проекта используются Java 21, Spring Boot,
**Maven**, PostgreSQL, Flyway, JUnit 5, Mockito и Testcontainers.

**Выбор build tool для всего учебного плана — Maven.** Учебный проект, примеры, команды и практические задания не
дублируются для Gradle. Если рабочий backend- репозиторий команды использует Gradle, его особенности следует изучать
отдельно в контексте этого репозитория, но одновременно поддерживать две версии учебного проекта не нужно.

Актуальный Spring Boot 4 требует минимум Java 17, но это не означает, что нужно переводить на него существующий проект —
совместимость важнее
новизны: [текущие требования Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html).

## Сквозной проект

В течение полугода необходимо разработать
[`Wallet Service`](projects/wallet-service/technical-requirements.md) — упрощённый сервис кошельков:

- клиенты и счета;
- пополнение, списание и перевод;
- история операций;
- идемпотентность запросов;
- запрет ухода баланса в минус;
- конкурентные переводы;
- JWT-авторизация;
- аудит;
- события об операциях;
- метрики и трассировка.

Начинать следует как с модульного монолита. Микросервисы на этом этапе будут мешать обучению.

Рекомендуемый недельный ритм при нагрузке 7–10 часов:

- 2–3 часа — материалы;
- 4–5 часов — код;
- 1–2 часа — чтение реального backend-кода команды.

---

## Месяц 1. Java для опытного TypeScript-разработчика

### Неделя 1. Экосистема и модель выполнения

Темы:

- [JDK, JVM, bytecode, classpath](week-01/jdk-jvm-bytecode-classpath.md);
- [компиляция и запуск](week-01/compilation-and-running.md);
- [структура Java-проекта](week-01/java-project-structure.md);
- [пакеты и visibility](week-01/packages-and-visibility.md);
- [Maven lifecycle](week-01/gradle-maven-lifecycle.md);
- Maven dependency scopes;

Практика:

- создать консольный проект;
- собрать JAR из командной строки;
- поставить breakpoint и исследовать stack trace;
- открыть backend-проект компании и найти entry point, зависимости и тесты.

Материалы:

- [Официальный Java Learning Path](https://dev.java/learn/)
- [Maven: Getting Started](https://maven.apache.org/guides/getting-started/)
- [Maven: Introduction to the Dependency Mechanism](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html)
- Книга *Spring Start Here*: глава 1
- Видео: [Java Full Course — Bro Code](https://www.youtube.com/watch?v=xTtL8E4LzTQ) — выборочно, пропуская знакомые
  основы.

### Неделя 2. Система типов и объектная модель

Темы:

- примитивы и reference types;
- [объектная модель и ООП в Java: классы, объекты, конструкторы, инкапсуляция, наследование и полиморфизм](week-02/object-oriented-programming.md);
- интерфейсы, `abstract`, `final`, `sealed`;
- `record`, `enum`;
- перегрузка и переопределение;
- композиция против наследования;
- `null`, `Optional`;
- разница между Java generics и TypeScript generics;
- type erasure.
- как работает память Хип, стэк, стринг пул где хранится и как достается Как связано с оперативной памятью
- IntelliJ debugger.

Особое внимание:

- `==` против `equals`;
- контракт `equals/hashCode`;
- mutable/immutable objects;
- инвариантность generics;
- PECS: producer `extends`, consumer `super`.

Практика:

- смоделировать `Money`, `Account`, `Transaction`;
- использовать `BigDecimal`, а не `double`;
- сделать value objects immutable;
- написать корректные `equals/hashCode`.

Материалы:

- [Objects, classes and interfaces](https://dev.java/learn/oop/)
- [Records](https://dev.java/learn/records/)
- [Generics](https://dev.java/learn/introducing-generics/)
- *Effective Java*, главы 2–5: objects, общие методы, классы,
  generics — [содержание книги](https://www.pearson.com/en-us/subject-catalog/p/effective-java/P200000000138).

### Неделя 3. Collections, lambdas и Stream API

Темы:

- `List`, `Set`, `Map`, `Queue`;
- `ArrayList`, `HashMap`, `HashSet`;
- mutable и immutable collections;
- `Comparable` и `Comparator`;
- functional interfaces;
- method references;
- Stream API: `map`, `filter`, `flatMap`, `reduce`, collectors;
- когда обычный цикл понятнее stream.

Сопоставление с TypeScript:

- Java Stream похож на цепочку методов массива, но ленивый и одноразовый;
- `Optional` — не полный аналог `T | undefined`;
- Java generics проверяются иначе из-за type erasure.

Практика:

- сгруппировать операции по счёту;
- посчитать обороты и комиссии;
- найти подозрительные операции;
- реализовать один алгоритм через loop и stream, сравнить читаемость.

Материалы:

- [Collections Framework](https://dev.java/learn/api/collections-framework/)
- [Stream API](https://dev.java/learn/api/streams/)
- *Effective Java*, главы 7–8.

### Неделя 4. Исключения, ресурсы и базовые idioms

Темы:

- checked и unchecked exceptions;
- exception hierarchy;
- `try-with-resources`;
- создание domain exceptions;
- stack traces;
- `java.time`;
- аннотации;
- базовая reflection;
- defensive copies.

Практика:

- реализовать импорт операций из CSV;
- обработать ошибочные строки;
- не терять исходную причину исключения;
- написать тесты на негативные сценарии.

Материалы:

- [Java Exceptions](https://dev.java/learn/exceptions/)
- *Effective Java*, главы 9–10.
- Для самопроверки: объяснить, почему `catch (Exception)` почти всегда слишком широк.

**Результат месяца:** уверенное чтение обычного Java-кода и способность написать небольшой модуль без Spring.

---

## Месяц 2. Spring Boot и REST API

### Неделя 5. IoC, DI и Spring Context

Темы:

- inversion of control;
- beans и application context;
- constructor injection;
- `@Component`, `@Service`, `@Repository`;
- `@Configuration`, `@Bean`;
- component scanning;
- bean lifecycle и scopes;
- почему Spring использует proxies/reflection.

Практика:

- сделать доменную логику кошелька сначала без Spring;
- затем собрать зависимости через Spring;
- не использовать field injection.

Материалы:

- *Spring Start Here*, главы 2–5
- [Содержание Spring Start Here](https://livebook.manning.com/book/spring-start-here/contents/)
- [Spring Framework Core](https://docs.spring.io/spring-framework/reference/core.html)

### Неделя 6. Spring Boot и конфигурация

Темы:

- `@SpringBootApplication`;
- auto-configuration;
- starters;
- `application.yml`;
- profiles;
- `@ConfigurationProperties`;
- externalized configuration;
- логирование;
- структура пакетов.

Практика:

- создать сервис через [Spring Initializr](https://start.spring.io/);
- настроить `local` и `test` profiles;
- вынести конфигурацию лимитов переводов в typed properties.

Материалы:

- *Spring Start Here*, глава 7
- [Building an Application with Spring Boot](https://spring.io/guides/gs/spring-boot/)
- [Spring Boot reference](https://docs.spring.io/spring-boot/reference/)

### Неделя 7. Spring MVC и REST

Темы:

- HTTP semantics;
- controller/service/repository;
- DTO против entity;
- Jackson;
- request parameters, path variables, body;
- HTTP status codes;
- validation;
- pagination;
- error response;
- `@ControllerAdvice`;
- idempotency basics.

Практика:

- CRUD для счетов;
- endpoint перевода;
- Bean Validation;
- единый формат ошибок;
- OpenAPI-описание.

Материалы:

- [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
- [Полный Spring REST tutorial](https://spring.io/guides/tutorials/rest/)
- *Spring Start Here*, главы 8 и 10
-

Видео/курс: [Spring Academy — Building a REST API](https://spring.academy/courses/building-a-rest-api-with-spring-boot) —
20 уроков и практические лабораторные.

### Неделя 8. Дизайн API

Темы:

- ресурсы и HTTP verbs;
- PUT против PATCH;
- идемпотентность;
- API versioning;
- validation boundary;
- optimistic concurrency через version/ETag;
- backward compatibility;
- frontend–backend contract.

Практика:

- определить OpenAPI contract до реализации;
- сгенерировать TypeScript-клиент;
- обработать `409 Conflict`, `400`, `404`, `422`;
- реализовать `Idempotency-Key` для перевода.

**Результат месяца:** работающий in-memory REST API и первый integration test.

---

## Месяц 3. SQL, PostgreSQL и persistence

### Неделя 9. Реляционная модель и SQL

Темы:

- таблицы, ключи, constraints;
- normal forms на практическом уровне;
- joins;
- aggregates;
- window functions;
- `NULL`;
- DDL/DML;
- связь между моделью API и моделью хранения.

Практика:

- спроектировать таблицы `account`, `transfer`, `ledger_entry`;
- добавить PK, FK, unique и check constraints;
- написать запрос истории и агрегирования оборотов.

Материалы:

- [PostgreSQL Tutorial: главы 2–3](https://www.postgresql.org/docs/current/tutorial.html)
- Интерактивная практика: [SQLBolt](https://sqlbolt.com/)

### Неделя 10. JDBC, connection pool и migrations

Темы:

- JDBC;
- `DataSource`;
- connection pool;
- prepared statements;
- SQL injection;
- Flyway/Liquibase;
- schema migrations;
- rollback миграций;
- управление конфигурацией БД.

Практика:

- поднять PostgreSQL через Docker Compose;
- подключить HikariCP;
- создать схему исключительно миграциями;
- выполнить несколько запросов через `JdbcTemplate`.

Материалы:

- [Spring Data Access](https://docs.spring.io/spring-framework/reference/data-access.html)
- [Flyway documentation](https://documentation.red-gate.com/flyway)
- *Spring Start Here*, глава 12.

### Неделя 11. JPA/Hibernate или технология проекта

Если команда использует JPA, изучить:

- entity lifecycle;
- persistence context;
- dirty checking;
- lazy/eager loading;
- associations;
- cascade;
- N+1;
- JPQL;
- projections;
- pagination;
- optimistic locking;
- почему entity не стоит возвращать из controller.

Практика:

- добавить persistence в Wallet Service;
- получить N+1 и устранить его;
- включить SQL logging;
- добавить `@Version`.

Материалы:

- [Spring Data JPA reference](https://docs.spring.io/spring-data/jpa/reference/)
- [Repository query keywords](https://docs.spring.io/spring-data/jpa/reference/repositories/query-keywords-reference.html)
- *Spring Start Here*, глава 14.

Если проект использует jOOQ или Spring Data JDBC, следует заменить JPA этой технологией. Не нужно учить ORM «на всякий
случай».

### Неделя 12. Индексы и планы выполнения

Темы:

- B-tree;
- составные и partial indexes;
- selectivity;
- `EXPLAIN ANALYZE`;
- full scan;
- pagination через offset и cursor;
- стоимость индексов при записи.

Практика:

- сгенерировать 100–500 тысяч операций;
- исследовать планы запросов до и после индекса;
- реализовать cursor pagination истории операций.

Материалы:

- [PostgreSQL: Indexes](https://www.postgresql.org/docs/current/indexes.html)
- Книга *Designing Data-Intensive Applications*, глава 1 и разделы о storage/indexing по мере
  необходимости — [второе издание](https://www.oreilly.com/library/view/designing-data-intensive-applications/9781098119058/).

**Результат месяца:** API работает с PostgreSQL, схема мигрируется автоматически, ключевые запросы проанализированы.

---

## Месяц 4. Транзакции, concurrency и тестирование

### Неделя 13. ACID и транзакции

Темы:

- atomicity, consistency, isolation, durability;
- lost update, dirty/non-repeatable/phantom reads;
- isolation levels;
- database locks;
- optimistic/pessimistic locking;
- transaction boundaries;
- rollback;
- propagation;
- ограничения `@Transactional`.

Важно понимать, что Spring реализует declarative transactions через AOP proxy. Поэтому self-invocation и запуск нового
потока могут нарушить ожидаемую семантику: [как работает
`@Transactional`](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-decl-explained.html).

Практика:

- выполнить два конкурентных списания;
- воспроизвести lost update;
- исправить через optimistic или pessimistic locking;
- проверить целостность ledger.

Материалы:

- [Spring Transaction Management](https://docs.spring.io/spring-framework/reference/data-access/transaction.html)
- *Spring Start Here*, глава 13
- *Designing Data-Intensive Applications*, первое издание, глава 7.

### Неделя 14. Java concurrency

Темы:

- threads и thread pools;
- race condition;
- atomicity и visibility;
- Java Memory Model;
- `synchronized`, locks, atomics;
- concurrent collections;
- `ExecutorService`;
- `CompletableFuture`;
- interruption и cancellation;
- thread safety Spring singleton beans;
- virtual threads — обзор, не глубокая оптимизация.

Virtual threads стали финальной возможностью в Java 21 и полезны прежде всего при большом числе блокирующих I/O-задач, а
не для CPU-bound вычислений: [официальное введение](https://dev.java/learn/new-features/virtual-threads/).

Материалы:

- *Java Concurrency in Practice*, главы 2–8 — [содержание](https://jcip.net/contents.html)
- *Effective Java*, глава 11.

### Неделя 15. Unit-тестирование

Темы:

- JUnit 5;
- AssertJ;
- parameterized tests;
- test doubles;
- Mockito;
- state-based против interaction-based testing;
- тестирование domain logic без Spring;
- test pyramid;
- deterministic tests.

Практика:

- покрыть правила перевода unit-тестами;
- использовать fake clock;
- проверить rounding и `BigDecimal`;
- mutation-test хотя бы одного критичного класса — опционально.

Материалы:

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html)
- *Spring Start Here*, глава 15.

### Неделя 16. Integration и API tests

Темы:

- `@SpringBootTest`;
- MVC test slices;
- repository tests;
- Testcontainers;
- testing against real PostgreSQL;
- contract tests;
- что не стоит мокать;
- повторяемость тестовых данных.

Практика:

- integration tests с PostgreSQL Testcontainer;
- API test перевода;
- concurrent integration test;
- тест миграций;
- тест уникальности idempotency key.

Материалы:

- [Spring Boot Testing](https://docs.spring.io/spring-boot/reference/testing/)
- [Spring Boot + Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)
- [Testcontainers examples](https://java.testcontainers.org/examples/)
- *Spring Start Here*, глава 16.

**Результат месяца:** корректность сервиса доказана тестами, включая конкурентный доступ и настоящую БД.

---

## Месяц 5. Security, интеграции и надёжность

### Неделя 17. Spring Security

Темы:

- authentication против authorization;
- security filter chain;
- session/cookie против bearer token;
- OAuth 2.0 и OpenID Connect;
- JWT validation;
- roles, scopes и permissions;
- CORS и CSRF;
- method security;
- password storage;
- secrets;
- OWASP Top 10.

Практика:

- настроить сервис как OAuth2 Resource Server;
- валидировать issuer, audience, expiration;
- разделить read/write scopes;
- написать security integration tests.

Материалы:

- [Spring Security OAuth2](https://docs.spring.io/spring-security/reference/servlet/oauth2/)
- [JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [OWASP API Security Top 10](https://owasp.org/API-Security/)

Не следует писать собственный authorization server или JWT-реализацию.

### Неделя 18. Вызовы других сервисов

Темы:

- HTTP client;
- connect/read/request timeouts;
- retries;
- exponential backoff и jitter;
- circuit breaker;
- bulkhead;
- rate limit;
- idempotency;
- correlation ID;
- ошибка удалённого сервиса;
- синхронный и асинхронный контракт.

Практика:

- добавить mock Fraud Check Service;
- настроить timeouts;
- retry только для безопасных случаев;
- не повторять неидемпотентную операцию без ключа.

Материалы:

- [Spring REST Clients](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html)
- *Designing Data-Intensive Applications*, первое издание, глава 8.

### Неделя 19. Messaging и Kafka — на уровне потребителя

Темы:

- topic, partition, offset, consumer group;
- ordering;
- at-most-once / at-least-once;
- duplicate delivery;
- retry topic и dead-letter queue;
- идемпотентный consumer;
- schema evolution;
- transactional outbox;
- почему «exactly once» не решает все бизнес-проблемы.

Практика:

- после перевода публиковать `TransferCompleted`;
- писать событие в outbox в одной DB-транзакции;
- реализовать идемпотентного consumer;
- воспроизвести повторную доставку.

Материалы:

- [Apache Kafka documentation](https://kafka.apache.org/documentation/)
- [Spring for Apache Kafka](https://docs.spring.io/spring-kafka/reference/)
- *Designing Data-Intensive Applications*, первое издание, глава 11.

Если Kafka отсутствует в проекте, эту неделю лучше отдать его фактической интеграционной технологии.

### Неделя 20. Архитектура приложения

Темы:

- layered architecture;
- package-by-feature;
- dependency direction;
- domain/service/infrastructure boundaries;
- hexagonal architecture — без догматизма;
- modular monolith;
- DTO mapping;
- SOLID на уровне практических решений;
- ADR;
- code review backend-кода.

Практика:

- разделить Wallet Service на `accounts`, `transfers`, `ledger`;
- запретить случайные зависимости между модулями;
- написать ADR о выборе блокировок и idempotency;
- провести себе review по checklist команды.

**Результат месяца:** защищённый сервис с надёжными внешними интеграциями и событиями.

---

## Месяц 6. Production и работа с реальным проектом

### Неделя 21. Логи, метрики и tracing

Темы:

- structured logging;
- log levels;
- correlation/trace ID;
- Micrometer;
- Spring Boot Actuator;
- health/readiness/liveness;
- metrics и percentiles;
- distributed tracing;
- OpenTelemetry;
- защита персональных и финансовых данных в логах.

Spring рассматривает observability как сочетание logs, metrics и traces, а для метрик и tracing использует
Micrometer: [Spring Boot Observability](https://docs.spring.io/spring-boot/reference/actuator/observability.html).

Практика:

- добавить Actuator;
- метрики количества и latency переводов;
- trace внешнего HTTP-вызова;
- structured logs без номера карты, токена и полного payload.

### Неделя 22. JVM в production

Темы:

- heap и stack;
- garbage collection на концептуальном уровне;
- memory leak в managed runtime;
- thread dump;
- heap dump;
- CPU profiling;
- Java Flight Recorder;
- common JVM/container problems;
- graceful shutdown;
- не настраивать GC без измерений.

Практика:

- создать искусственный memory leak;
- снять heap dump;
- найти заблокированные threads;
- посмотреть профиль запроса в JFR.

Материалы:

- [Java troubleshooting tools](https://dev.java/learn/jvm/tools/)
- *Java Concurrency in Practice*, главы 10–12 — выборочно.

### Неделя 23. Docker, CI/CD и эксплуатация

Темы:

- Dockerfile;
- container layers;
- environment variables и secrets;
- application config;
- CI stages;
- migrations during deployment;
- rollback;
- readiness;
- backward-compatible database changes;
- feature flags;
- basic Kubernetes concepts — только если он используется в компании.

Практика:

- собрать OCI image;
- запустить сервис и PostgreSQL через Compose;
- настроить pipeline: compile → unit → integration → package;
- проверить graceful shutdown и readiness.

Материалы:

- [Spring Boot container images](https://docs.spring.io/spring-boot/reference/packaging/container-images/)
- [Docker Java guide](https://docs.docker.com/guides/java/)

### Неделя 24. Вход в реальный backend

Финальная практика:

1. Найти в рабочем backlog небольшой backend bug или техническую задачу.
2. Проследить полный flow: controller → service → transaction → repository → integration.
3. Воспроизвести проблему тестом.
4. Сделать исправление.
5. Добавить unit/integration tests.
6. Проверить SQL, логи и метрики.
7. Отправить PR backend-разработчику на review.
8. Записать все замечания как личный checklist.

Финальный Wallet Service должен содержать:

- REST API и OpenAPI;
- PostgreSQL и Flyway;
- transaction boundaries;
- защиту от конкурентного списания;
- idempotency;
- JWT security;
- unit и integration tests;
- Testcontainers;
- outbox/event processing;
- Actuator, metrics и structured logs;
- Dockerfile и CI;
- README с архитектурными решениями.

---

## Что сознательно не включено

На первые полгода можно не тратить время на:

- JSP, JSF, Swing и JavaFX;
- глубокое изучение servlet API;
- WebFlux/Reactor, если проект его не использует;
- собственные annotation processors;
- JVM bytecode engineering;
- глубокий GC tuning;
- несколько ORM и build systems одновременно;
- Kubernetes administration;
- десятки паттернов GoF;
- преждевременное разделение проекта на микросервисы;
- сертификаты Oracle/Spring.

## Критерий готовности к backend-задачам

Можно брать небольшие production-задачи, если без подсказки получается:

- объяснить жизненный цикл одного HTTP-запроса в приложении;
- определить границу транзакции;
- отличить DTO от entity;
- предсказать SQL, который выполнит ORM;
- найти N+1;
- объяснить `equals/hashCode`;
- написать unit test и integration test с PostgreSQL;
- корректно обработать два конкурентных запроса;
- настроить JWT Resource Server;
- найти ошибку по stack trace, логам и trace ID;
- внести миграцию без несовместимого изменения схемы;
- собрать и запустить сервис из командной строки.

Самая эффективная стратегия — уже с 6–8-й недели брать небольшие backend-задачи совместно с Java-разработчиком. Полгода
самостоятельного обучения дадут базу, но именно review реального enterprise-кода быстрее всего сформирует правильные
Spring- и Java-идиомы.
