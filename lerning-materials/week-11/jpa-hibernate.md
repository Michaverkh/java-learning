# Неделя 11. JPA и Hibernate

> Этот раздел нужен, если рабочий проект использует JPA. Если он использует jOOQ или Spring Data JDBC, изучай вместо
> него технологию проекта: осваивать ORM «на всякий случай» не нужно.

## Цель недели

Научиться понимать, какой SQL и когда выполняет код с JPA, не допускать лишних запросов и сохранять границы между HTTP
API, доменной логикой и хранением данных. К концу недели `Wallet Service` должен читать и сохранять данные PostgreSQL
через JPA, не возвращая persistence-сущности из контроллеров.

JPA не отменяет знания SQL, транзакций и ограничений базы. Это слой спецификаций Java для отображения объектов на
реляционные таблицы. **Hibernate** — наиболее распространённая реализация JPA; Spring Boot обычно подключает именно его.
Spring Data JPA, в свою очередь, создаёт реализации репозиториев вокруг JPA.

```text
Controller → application service → Spring Data repository → JPA API → Hibernate → JDBC → PostgreSQL
                         ↑
                    transaction boundary
```

| Слой                | Ответственность                                            | Не должен делать                                 |
|---------------------|------------------------------------------------------------|--------------------------------------------------|
| Controller          | HTTP, DTO, проверка входных данных, код ответа             | управлять сущностями и транзакциями              |
| Application service | сценарий, права, граница транзакции, вызов доменной логики | содержать SQL и детали HTTP                      |
| Domain model        | инварианты кошелька и операции с деньгами                  | зависеть от `HttpServletRequest` или контроллера |
| Repository          | загрузить и сохранить агрегат, выполнить запрос            | решать, разрешён ли перевод                      |
| Entity              | отображение объекта на таблицу                             | быть DTO ответа API                              |

JPA особенно удобна для обычной работы с агрегатами и связями. Однако отчётные запросы, пакетные обновления, сложная
аналитика и критически чувствительные к производительности операции иногда понятнее через `JdbcTemplate` или jOOQ. Это
не «поражение ORM», а осознанный выбор инструмента.

## 1. Минимальная настройка

Для Spring Boot добавь стартер JPA и драйвер PostgreSQL. Версии зависимостей не указывай вручную: ими управляет BOM
Spring Boot.

```xml

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
<groupId>org.postgresql</groupId>
<artifactId>postgresql</artifactId>
<scope>runtime</scope>
</dependency>
```

Схема `Wallet Service` создаётся только миграциями Flyway, а не Hibernate. В `application.yml` явно запрети
автоматическое изменение схемы:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
```

`validate` проверит соответствие отображений существующей схеме и завершит старт с ошибкой при расхождении. В учебном
коде допустимы `create-drop` на изолированной временной базе, но он не заменяет миграции. Значения `update` и `create`
для общей или production-базы опасны: изменение схемы становится неявным, не версионируется и может потерять данные.

## 2. Entity: отображение таблицы, а не API-модель

Entity — объект, который Hibernate умеет читать из строки таблицы и записывать обратно. Обычно он имеет идентификатор,
пустой или защищённый конструктор и изменяемые поля: Hibernate создаёт объект и заполняет его постепенно. Поэтому
`record` обычно не подходит для entity.

Пример упрощённого кошелька. Названия столбцов должны соответствовать уже созданной миграциями схеме.

```java

@Entity
@Table(name = "account")
public class AccountEntity {

    @Id
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private CurrencyCode currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Version
    @Column(nullable = false)
    private long version;

    protected AccountEntity() {
        // Требование JPA: Hibernate создаёт объект сам.
    }

    public AccountEntity(UUID id, UUID clientId, CurrencyCode currency) {
        this.id = requireNonNull(id);
        this.clientId = requireNonNull(clientId);
        this.currency = requireNonNull(currency);
        this.balance = BigDecimal.ZERO.setScale(2);
        this.status = AccountStatus.ACTIVE;
    }

    public void withdraw(BigDecimal amount) {
        // Здесь находятся проверки суммы, статуса и достаточности баланса.
        balance = balance.subtract(amount);
    }
}
```

`@Enumerated(EnumType.STRING)` сохраняет понятное имя, например `ACTIVE`, а не порядковый номер enum. Порядок элементов
enum может измениться, поэтому `ORDINAL` для бизнес-данных опасен. Для суммы по-прежнему используется `BigDecimal`, а
столбец — `numeric(19, 2)`.

### Почему entity нельзя возвращать из controller

Entity описывает способ хранения, а API — контракт с клиентом. Если вернуть entity напрямую, HTTP-ответ начинает
зависеть от внутренней структуры таблиц и связей, а сериализация может нечаянно загрузить ленивую связь или уйти в цикл
`Account → Client → accounts`.

Кроме того, у API и хранения разная эволюция: столбец может быть нужен только для аудита, а поле ответа — собираться из
нескольких таблиц. Для API используй DTO и явное преобразование:

```java
public record AccountResponse(
        UUID id,
        String currency,
        BigDecimal balance,
        String status
) {
}

static AccountResponse toResponse(AccountEntity entity) {
    return new AccountResponse(
            entity.getId(),
            entity.getCurrency().name(),
            entity.getBalance(),
            entity.getStatus().name()
    );
}
```

## 3. Persistence context и жизненный цикл

**Persistence context** — область, в которой Hibernate отслеживает entity. В Spring он обычно привязан к транзакции.
Внутри него есть кэш первого уровня: повторная загрузка одной entity по тому же id вернёт тот же Java-объект без второго
SQL-запроса.

```java

@Transactional
public void demo(UUID accountId) {
    AccountEntity first = entityManager.find(AccountEntity.class, accountId);
    AccountEntity second = entityManager.find(AccountEntity.class, accountId);

    assertSame(first, second);
}
```

Жизненный цикл entity:

| Состояние | Что означает                                                    | Типичный переход                               |
|-----------|-----------------------------------------------------------------|------------------------------------------------|
| transient | обычный новый объект; Hibernate его не отслеживает              | `persist()` → managed                          |
| managed   | объект находится в persistence context; изменения отслеживаются | commit/clear/detach → detached                 |
| detached  | объект существует, но контекст его больше не отслеживает        | `merge()` создаёт или возвращает managed-копию |
| removed   | объект помечен к удалению                                       | при flush выполняется `DELETE`                 |

Не передавай detached entity между слоями и запросами как «живой объект». В следующем HTTP-запросе persistence context
уже другой; сохранение устаревшего снимка может перезаписать чужое изменение. Нужный агрегат следует загрузить заново
внутри текущей транзакции, проверить права и применить команду.

`save()` Spring Data JPA не является магической командой «записать прямо сейчас». Для новой entity он обычно вызывает
`persist`, для существующей или detached — `merge`. При `merge` managed-объектом будет возвращённый результат, а не
обязательно исходный аргумент. Внутри транзакции после `find...` и изменения managed entity зачастую достаточно dirty
checking без отдельного `save()`.

## 4. Dirty checking, flush и transaction boundary

Hibernate запоминает снимок managed entity. Перед отправкой SQL сравнивает текущие поля со снимком. Если баланс
изменился, Hibernate генерирует `UPDATE`; это называется **dirty checking**.

```java

@Transactional
public void deposit(UUID accountId, BigDecimal amount) {
    AccountEntity account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));

    account.deposit(amount);
    // save(account) не нужен: объект managed, изменение увидит Hibernate.
}
```

`flush` — синхронизация накопленных изменений persistence context с базой: Hibernate отправляет `INSERT`, `UPDATE`,
`DELETE`, но транзакция ещё может быть отменена. Обычно flush происходит перед commit и иногда перед запросом, которому
должны быть видны сделанные изменения. Commit делает изменения постоянными.

Не полагайся на точный момент SQL, если это не нужно. Но понимай последствия: ограничение базы данных и optimistic lock
могут проявиться на flush/commit, а не на строке изменения поля. Если важно получить ошибку раньше, допустим явный
`entityManager.flush()`, но это не должно становиться привычной заменой хорошего дизайна.

Границу транзакции в сервисе определяют вокруг одной бизнес-операции. Для перевода это: прочитать оба кошелька →
проверить правила → изменить балансы → создать `Transfer`, две `LedgerEntry` и `OutboxEvent` → зафиксировать всё одной
транзакцией. Подробно про изоляцию и блокировки — неделя 13.

## 5. Связи: моделируй минимально

JPA умеет связи `@ManyToOne`, `@OneToMany`, `@OneToOne`, `@ManyToMany`, но удобная аннотация не отменяет цену загрузки и
сложность каскадов. Для финансового сервиса полезно хранить `clientId` в `LedgerEntryEntity`, если сценарию не нужно
обходить объектную связь `account.getClient()`. Так меньше скрытых загрузок и проще контролировать доступ.

Когда связь действительно нужна, владелец связи — сторона с внешним ключом. Например, у записи ledger есть `account_id`,
поэтому она владеет связью:

```java

@Entity
@Table(name = "ledger_entry")
public class LedgerEntryEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;
}
```

| Вид           | Смысл                                          | Важный риск                                          |
|---------------|------------------------------------------------|------------------------------------------------------|
| `@ManyToOne`  | много ledger entries принадлежат одному счёту  | по умолчанию JPA задаёт EAGER; указывай `LAZY` явно  |
| `@OneToMany`  | один счёт имеет много записей                  | большая коллекция может быть загружена целиком       |
| `@OneToOne`   | одна сущность соответствует ровно одной другой | часто лишняя сложность, если достаточно id           |
| `@ManyToMany` | связь через промежуточную таблицу              | если у связи есть данные, моделируй отдельную entity |

Двунаправленная связь нужна только если обход требуется в обоих направлениях. `AccountEntity.ledgerEntries` не нужен для
истории с пагинацией: коллекция не выражает `LIMIT`, фильтр по дате и стабильную сортировку. Для истории следует
выполнить отдельный page-запрос репозитория.

### Cascade и orphan removal

`cascade` определяет, какие операции над родителем будут применены к детям. Это не настройка «по умолчанию», а часть
модели владения.

| Настройка              | Когда оправдана                                               | Почему опасна в Wallet Service                     |
|------------------------|---------------------------------------------------------------|----------------------------------------------------|
| `PERSIST`              | ребёнок создаётся только вместе с владельцем                  | может неявно вставить неожиданную entity           |
| `MERGE`                | жизненный цикл ребёнка полностью принадлежит владельцу        | облегчает потерю контроля над обновлениями         |
| `REMOVE`               | дочерняя запись не имеет самостоятельной ценности             | нельзя каскадно удалить ledger или аудит           |
| `ALL`                  | очень редкий полностью закрытый агрегат                       | объединяет риски всех операций                     |
| `orphanRemoval = true` | удалённый из коллекции ребёнок действительно должен исчезнуть | удаление из Java-коллекции превращается в `DELETE` |

`LedgerEntry`, аудит и outbox — исторические записи. Для них не ставь `CascadeType.REMOVE` или `orphanRemoval`:
требования проекта запрещают менять или удалять ledger entries через приложение.

## 6. LAZY, EAGER и LazyInitializationException

При `LAZY` Hibernate сначала загружает основную entity, а связанную — только при первом обращении. Он подставляет proxy
или специальную коллекцию. Если persistence context уже закрыт, то загрузить данные невозможно и возникает
`LazyInitializationException`.

Ошибка не лечится массовым `EAGER`: это переносит проблему в производительность и порождает лишние запросы. Не следует и
включать [Open Session in View (документация Spring)](https://docs.spring.io/spring-framework/docs/7.0.0/javadoc-api/org/springframework/orm/jpa/support/OpenEntityManagerInViewInterceptor.html)
ради удобной сериализации. При `spring.jpa.open-in-view=true` persistence context живёт до конца HTTP-ответа, поэтому
controller/JSON-сериализатор может неявно ходить в БД; границы доступа к данным становятся неочевидными.

Правильный вопрос: «какие данные нужны этому конкретному сценарию?» Затем вырази это запросом или DTO-проекцией, а
преобразование в DTO выполни в сервисе внутри транзакции.

## 7. N+1: как увидеть и устранить

N+1 возникает, когда один запрос получает N объектов, а последующий обход ленивой связи запускает ещё N запросов.
Например, история получила 50 записей ledger, затем для каждой читает `entry.getAccount().getClientId()`.

```java
List<LedgerEntryEntity> entries = ledgerRepository.findByCreatedAtBetween(from, to);
return entries.

stream()
    .

map(entry ->new

LedgerEntryResponse(
        entry.getId(),entry.

getAccount().

getClientId(),entry.

getAmount()
    ))
            .

toList();
```

В этом примере второе поле `LedgerEntryResponse` — идентификатор клиента. Первый запрос получает entries, затем
Hibernate может выполнить до 50 `select ... from account where id = ?`, если записи относятся к 50 разным ещё не
загруженным счетам. Если несколько записей относятся к одному счёту, persistence context позволяет использовать уже
загруженный объект повторно. Само чтение `account.getId()` обычно не требует загрузки счёта: идентификатор уже известен
Hibernate. Поэтому для демонстрации N+1 здесь читается `clientId`.

В test/local-профиле включи наблюдаемое логирование SQL и параметров. Точная конфигурация зависит от версии Hibernate,
поэтому проверь фактические логи; базовый минимум:

```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
logging:
  level:
    org.hibernate.SQL: debug
```

`show-sql` годится для локального обучения; в обычной работе лучше структурированные логи и метрики. Никогда не включай
подробные параметры SQL без понимания, что в них не попадут секреты и чувствительные данные.

Способы исправления выбирай по сценарию:

1. **DTO-проекция** — лучший путь, когда нужен конкретный плоский ответ.
2. **`join fetch` в JPQL** — загрузить нужную связь одним запросом для ограниченного набора объектов.
3. **`@EntityGraph`** — декларативно задать план загрузки для метода репозитория.
4. **Batch fetching** — компромисс для контролируемой пакетной загрузки; он уменьшает число запросов, но не заменяет
   осмысленный запрос.

Ниже рассматривается связь `LedgerEntryEntity.account` из раздела 5. Примеры методов размещаются в репозитории записей
операций; проверки прав и преобразование entity в DTO остаются в сервисе.

### 7.1. DTO-проекция: сразу получить поля ответа

Если сценарию нужны только данные для чтения, можно попросить JPA создать DTO непосредственно из результата запроса. Не
требуется сначала загружать полные entity, а затем обходить их связи.

Предположим, для внутреннего отчёта нужны идентификатор операции, идентификатор клиента и сумма:

```java
// Отдельный файл com/example/wallet/ledger/LedgerClientItem.java
public record LedgerClientItem(UUID id, UUID clientId, BigDecimal amount) {
}

// Метод репозитория
@Query("""
        select new com.example.wallet.ledger.LedgerClientItem(
            e.id, a.clientId, e.amount
        )
        from LedgerEntryEntity e
        join e.account a
        where e.createdAt >= :from and e.createdAt < :to
        order by e.createdAt desc, e.id desc
        """)
List<LedgerClientItem> findClientItems(
        @Param("from") Instant from,
        @Param("to") Instant to
);
```

`select new` вызывает конструктор DTO для каждой строки результата. В JPQL указывается полное имя класса, а порядок и
типы выбранных полей должны соответствовать конструктору. Здесь DTO содержит только простые значения, поэтому ни
`LedgerEntryEntity`, ни `LedgerEntryEntity` не становятся управляемыми объектами этого результата.

Ожидаемая форма SQL — один `SELECT` с соединением таблиц и только нужными столбцами:

```sql
select e.id, a.client_id, e.amount
from ledger_entry e
         join account a on a.id = e.account_id
where e.created_at >= ?
  and e.created_at < ?
order by e.created_at desc, e.id desc
```

При чтении полей DTO дополнительных запросов нет: в нём отсутствуют ленивые связи. Для большой выборки добавь
постраничное чтение; `List` сам по себе не ограничивает объём результата. Для истории одного кошелька используй более
узкую проекцию `OperationItem` из раздела 8: данные клиента для этого ответа не нужны.

**Выбирай этот способ**, когда нужен определённый набор полей для API или отчёта. Для изменения состояния через методы
entity проекция не подходит: нужную сущность следует загрузить отдельно внутри транзакции.

Подробнее: [DTO-проекции в Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/repositories/projections.html).

### 7.2. `join fetch`: загрузить entity вместе с нужной связью

Обычный `join` позволяет фильтровать или выбирать поля связанной сущности, но сам по себе не требует заполнить связь в
возвращаемой entity. `join fetch` явно требует загрузить её вместе с основным объектом:

```java

@Query("""
        select e
        from LedgerEntryEntity e
        join fetch e.account
        where e.id in :ids
        """)
List<LedgerEntryEntity> findWithAccountByIds(@Param("ids") List<UUID> ids);
```

Для заранее ограниченного набора идентификаторов Hibernate получает записи и их счета одним SQL-запросом с `JOIN`.
Внутри текущего контекста вызов `entry.getAccount().getClientId()` использует уже загруженный счёт. В отличие от
DTO-проекции, загружаются entity с их полями, и Hibernate отслеживает их состояние.

В примере связь обязательна, поэтому используется внутреннее соединение `join fetch`. Для необязательной связи
`left join fetch` сохраняет в результате и основные объекты, у которых связанной записи нет. Другие ленивые связи,
например связь самого счёта с клиентом, автоматически от этого не загружаются.

**Выбирай этот способ**, когда сервису нужны сами сущности и заранее известная связь: например, для вызова доменных
методов или проверки состояния связанных объектов. После работы сервис всё равно возвращает DTO.

Особенно осторожно загружай коллекции. При соединении `Account → ledgerEntries` один счёт представлен несколькими
SQL-строками. При пагинации Hibernate может получить все строки и ограничить результат в памяти либо завершить запрос
ошибкой при соответствующей настройке. Для страницы счетов обычно сначала выбирают страницу их идентификаторов, затем
отдельным запросом загружают нужные связи. Это ограничение относится именно к коллекциям; соединение
`LedgerEntry → account` не размножает строки записей операций. Одновременная загрузка нескольких коллекций также может
резко увеличить число SQL-строк.

Подробнее: [
`join fetch` и его ограничения в Hibernate](https://docs.hibernate.org/orm/6.6/querylanguage/html_single/#join-fetch).

### 7.3. `@EntityGraph`: указать связи на методе репозитория

Граф загрузки описывает, какие связанные объекты нужны конкретному методу. Для простого условия не обязательно писать
JPQL вручную:

```java

@EntityGraph(attributePaths = "account")
List<LedgerEntryEntity> findByIdIn(List<UUID> ids);
```

Spring Data строит условие по имени метода, а `attributePaths = "account"` добавляет требование загрузить счёт. Здесь
`account` — имя Java-поля, а не столбца `account_id`. Аннотацию можно использовать и вместе с `@Query`, если условие
выборки сложнее.

Результатом остаются entity. Для этой простой связи Hibernate обычно использует SQL с соединением таблиц. Однако граф
описывает необходимые данные, а не точный SQL: число запросов проверяй по логам. Аннотация не является универсальной
гарантией одного запроса.

По умолчанию Spring Data использует граф типа `FETCH`: перечисленные атрибуты требуется загрузить, остальные
рассматриваются как ленивые, хотя реализация JPA вправе загрузить дополнительные данные. Тип `LOAD` сохраняет для
неуказанных атрибутов правила из отображения entity, включая `EAGER`.

**Выбирай этот способ**, когда нужны entity и удобно держать план загрузки рядом с методом репозитория. Например, один
метод читает только операции, а другой — операции вместе со счетами. Глобальное отображение связи при этом остаётся
`LAZY`.

Если включить в граф коллекцию, ограничения соединений и пагинации остаются: смена `join fetch` на `@EntityGraph` сама
по себе их не устраняет.

Подробнее: [графы загрузки в Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.entity-graph).

### 7.4. Batch fetching: догружать ленивые связи группами

При пакетной загрузке первоначальный запрос по-прежнему получает только записи операций. Когда код обращается к
незагруженному счёту, Hibernate может загрузить сразу несколько счетов, чьи незагруженные ссылки уже находятся в том же
persistence context.

Общая настройка Hibernate в Spring Boot:

```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 16
```

Для отдельного типа сущности можно использовать аннотацию Hibernate `org.hibernate.annotations.BatchSize`:

```java

@Entity
@Table(name = "account")
@BatchSize(size = 16)
public class AccountEntity {
    // Остальные поля и методы — как в разделе 2.
}
```

Это два способа настройки: общий размер по умолчанию и размер для конкретного типа. Для связи `LedgerEntry → account`
аннотация ставится на класс загружаемого счёта. На коллекции она задавала бы количество владельцев, чьи коллекции можно
загрузить группой, а не максимальное число элементов коллекции.

Вместо отдельных запросов для каждого счёта Hibernate выполняет запросы примерно такой формы:

```sql
select...
    from account
where id in (?, ?, ...)
```

Для 50 различных незагруженных счетов при размере группы 16 ориентир — один запрос операций и около четырёх запросов
счетов вместо 51 запроса. Это оценка: фактическое число зависит от уже загруженных объектов, кэша и того, какие связи
код действительно читает. Все ссылки должны обрабатываться в одном открытом контексте; после его закрытия настройка не
предотвращает `LazyInitializationException`.

**Выбирай этот способ**, когда условный обход ленивых связей оправдан, а единый запрос для всех случаев неудобен. Цена
компромисса — несколько обращений к БД и возможная загрузка счетов, которые затем не понадобятся. Если всем строкам
ответа нужны одни и те же поля, сначала рассмотри DTO-проекцию или явную загрузку связи.

Не путай эту настройку с `hibernate.jdbc.batch_size`: она группирует операции записи, а не загрузку ленивых связей.

Подробнее: [пакетная загрузка в Hibernate](https://docs.hibernate.org/orm/6.6/userguide/html_single/#fetching-batch).

### Как выбрать для Wallet Service

| Потребность сценария                                        | С чего начать                                |
|-------------------------------------------------------------|----------------------------------------------|
| Вернуть страницу истории операций                           | DTO-проекция `OperationItem` из раздела 8    |
| Получить entity с конкретной связью для обработки           | `join fetch` с ограниченным набором объектов |
| Задать нужные связи на методе репозитория                   | `@EntityGraph`                               |
| Уменьшить число запросов при условном обходе ленивых связей | Batch fetching с проверкой SQL               |

Проверяй не только число запросов, но и объём результата. Один запрос, загружающий огромную коллекцию, может оказаться
дороже двух небольших. Для `Page` отдельно учитывай запрос подсчёта общего количества: запрос данных плюс `count` —
нормальная ситуация, а не N+1.

## 8. Репозитории, JPQL и проекции

Spring Data JPA создаёт реализацию интерфейса репозитория. Методы по имени удобны для простых условий, но сложное имя
скрывает смысл запроса; лучше перейти к `@Query` или отдельному репозиторию.

```java
public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    Optional<AccountEntity> findByIdAndClientId(UUID id, UUID clientId);

    @Query("""
            select a
            from AccountEntity a
            where a.clientId = :clientId and a.status = :status
            order by a.id
            """)
    Page<AccountEntity> findPageByClient(
            @Param("clientId") UUID clientId,
            @Param("status") AccountStatus status,
            Pageable pageable
    );
}
```

JPQL работает с **именами классов и полей Java**, а не с именами таблиц и столбцов SQL. `from AccountEntity a`
правильно; `from account` — нативный SQL, а не JPQL. JPQL подходит для переносимых запросов к модели entity. Для
PostgreSQL-специфичных возможностей, оконных функций или точно контролируемого SQL допустим native query либо
JDBC/jOOQ — но результат и тестирование должны быть явными.

Для списка истории не загружай целые entity, если нужен только ответ API. Используй проекцию:

```java
public record OperationItem(
        UUID id,
        UUID operationId,
        OperationType operationType,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Instant createdAt
) {
}

public interface LedgerEntryRepository extends JpaRepository<LedgerEntryEntity, UUID> {
    @Query(value = """
            select new com.example.wallet.ledger.OperationItem(
                e.id, e.operationId, e.operationType, e.amount, e.balanceAfter, e.createdAt
            )
            from LedgerEntryEntity e
            where e.accountId = :accountId
              and (:operationType is null or e.operationType = :operationType)
            order by e.createdAt desc, e.id desc
            """,
            countQuery = """
                    select count(e)
                    from LedgerEntryEntity e
                    where e.accountId = :accountId
                      and (:operationType is null or e.operationType = :operationType)
                    """)
    Page<OperationItem> findOperationPage(
            @Param("accountId") UUID accountId,
            @Param("operationType") OperationType operationType,
            Pageable pageable
    );
}
```

Проекция уменьшает объём данных, не создаёт managed entity и не может случайно вызвать ленивую загрузку. Не делай один
универсальный «богатый» метод на все экраны: небольшой запрос, явно соответствующий use case, легче читать и проверять.

## 9. Пагинация

Spring Data использует `Pageable`. Устанавливай максимальный `size` на HTTP-границе согласно требованиям проекта: не
более 100. Сортировка истории должна быть стабильной — `createdAt desc, id desc`, иначе одинаковое время может вызвать
пропуски или повторения между страницами.

```java
Pageable pageable = PageRequest.of(
        page,
        Math.min(size, 100),
        Sort.by(DESC, "createdAt").and(Sort.by(DESC, "id"))
);
```

`Page<T>` обычно выполняет запрос данных и отдельный `count(*)`, чтобы вернуть общее количество. Для дорогого
бесконечного списка, когда итоговое число не нужно, используй `Slice<T>`: он сообщает только, есть ли следующая
страница. На следующей неделе будет cursor pagination — часто более подходящий вариант для большой постоянно пополняемой
истории операций.

## 10. Optimistic locking через `@Version`

Поле `@Version` защищает от ситуации, когда два запроса прочли одну версию кошелька и оба попытались записать разные
балансы. Hibernate добавляет версию в условие обновления:

```sql
update account
set balance = ?,
    version = ?
where id = ?
  and version = ?
```

Если первый запрос уже увеличил версию, второй обновит ноль строк и Hibernate выбросит `OptimisticLockException` (в
Spring обычно транслируется в исключение доступа к данным). Это предотвращает тихий lost update, но само по себе не
повторяет операцию и не превращает конфликт в понятный API-ответ.

Для Wallet Service нужно заранее выбрать стратегию после concurrency-теста и зафиксировать её в ADR. При optimistic
locking обычно ограниченно повторяют **всю** заранее определённую транзакцию: снова загружают состояние, снова проверяют
баланс и правила. Нельзя просто повторить последний `UPDATE`: исходное решение уже могло стать неверным. При исчерпании
попыток верни `409 CONCURRENT_UPDATE`.

`@Version` не заменяет идемпотентность. Два одинаковых сетевых запроса могут идти не одновременно, а один за другим, и
оба успешно изменить баланс. Для этого существует `Idempotency-Key` и отдельная запись `IdempotencyRecord`.

## 11. Что особенно важно для Wallet Service

1. Flyway создаёт схему и ограничения; Hibernate только проверяет её через `ddl-auto: validate`.
2. `Account` имеет `@Version`; реальная стратегия конкурентности проверяется тестом двух списаний `80.00` с баланса
   `100.00`.
3. Перевод выполняется в одной транзакции вместе с `Transfer`, двумя `LedgerEntry` и `OutboxEvent`.
4. Историю операций получай запросом с проекцией и постраничной сортировкой, а не через `account.getLedgerEntries()`.
5. Ledger, аудит и outbox не удаляются каскадом.
6. Entity остаются внутри persistence-слоя; контроллеры принимают и возвращают DTO.
7. Не включай Open Session in View для скрытого доступа к базе из JSON-сериализации.

## 12. Практика на 7–10 часов

1. Подключи `spring-boot-starter-data-jpa`, сохрани Flyway единственным владельцем схемы и выставь `ddl-auto: validate`.
2. Создай `LedgerEntryEntity`, `LedgerEntryEntity`, `LedgerEntryEntity` с явными таблицами, столбцами и
   `EnumType.STRING`. Добавь
   `@Version` счёту.
3. Реализуй получение кошелька владельцем через `findByIdAndClientId`. Контроллер должен вернуть `AccountResponse`, а не
   entity.
4. Реализуй сценарий пополнения: загрузка entity, доменная проверка, изменение баланса, создание ledger entry и outbox
   event в одной транзакции.
5. Для `GET /accounts/{accountId}/operations` сделай DTO-проекцию с фильтром `operationType` и сортировкой
   `createdAt desc, id desc`. Ограничь `size` значением 100.
6. Намеренно получи N+1: загрузи страницу ledger entries и в цикле обратись к ленивому `account`. Посчитай SQL-запросы в
   логах.
7. Замени этот код проекцией, `@EntityGraph` или `join fetch`, объясни в комментарии, почему выбранный вариант
   соответствует сценарию. Проверь, что количество запросов не зависит от размера страницы.

### Критерии готовности

- приложение не пытается создавать или менять таблицы через Hibernate;
- API не сериализует entity;
- тест демонстрирует N+1 до исправления и отсутствие N+1 после него;
- при конкурентном конфликте нет потерянного обновления;
- история операций не загружает все записи счёта в память;
- SQL-логи используются только в безопасном локальном или тестовом профиле.

## Самопроверка

1. Чем различаются JPA, Hibernate и Spring Data JPA?
2. Почему `save()` не обязательно отправляет SQL сразу?
3. Что различает managed и detached entity?
4. Почему `EAGER` — плохой универсальный ответ на `LazyInitializationException`?
5. Как N+1 выглядит в SQL-логах и почему `join fetch` не всегда подходит для страницы?
6. Почему ledger entries не должны удаляться через `CascadeType.REMOVE`?
7. Что именно добавляет Hibernate в SQL при `@Version`?
8. Почему `@Version` не заменяет `Idempotency-Key`?
9. В каких случаях для чтения лучше DTO-проекция, а не entity?
10. Почему `ddl-auto: update` не должен управлять схемой Wallet Service?

## Материалы

- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/reference/)
- [Spring Data: ключевые слова методов репозиториев](https://docs.spring.io/spring-data/jpa/reference/repositories/query-keywords-reference.html)
- [Hibernate ORM User Guide](https://docs.hibernate.org/orm/current/userguide/html_single/Hibernate_User_Guide.html)
- [Spring Framework: декларативные транзакции](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative.html)
