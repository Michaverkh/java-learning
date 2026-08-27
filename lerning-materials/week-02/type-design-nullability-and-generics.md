# Проектирование типов в Java: специальные виды типов, nullability и generics

[← Вернуться к учебному плану](../java-backend-learning-plan.md)

Этот раздел связывает три практических вопроса:

1. Как выбрать форму Java-типа: `interface`, `abstract class`, обычный `final`-класс, `sealed`-иерархию, `record` или
   `enum`?
2. Как выразить обязательное и отсутствующее значение через `null` и `Optional`?
3. Как безопасно переиспользовать алгоритмы и контейнеры через generics с учётом инвариантности и type erasure?

Материал рассчитан на разработчика с опытом TypeScript. Синтаксические сходства здесь обманчивы: Java использует
номинативные runtime-типы, а TypeScript — преимущественно структурные типы, исчезающие при компиляции.

## Что уже разобрано

Не будем повторять материал из предыдущих разделов:

- базовые `interface`, `abstract`, `final`, `sealed`, наследование и полиморфизм — в разделе
  [«Объектно-ориентированное программирование в Java»](object-oriented-programming.md);
- reference types, базовое поведение `null`, wrapper-типы и ограничение generics на primitive types — в разделе
  [«Примитивы и reference types в Java»](primitives-and-reference-types.md).

Здесь эти конструкции рассматриваются как единая система проектирования контрактов для backend-кода.

## Что нужно понять

После раздела должно получаться:

1. выбирать между `interface`, `abstract class`, `record`, `enum`, `sealed` и обычным классом;
2. отличать неизменяемую ссылку `final` от immutable object;
3. моделировать закрытый набор вариантов через `enum` или sealed hierarchy;
4. использовать `Optional<T>` как контракт отсутствующего результата, а не как замену всем `null`;
5. читать и писать generic types и generic methods;
6. объяснять инвариантность `List<T>` и применять PECS;
7. находить последствия type erasure в runtime, reflection, массивах и overload-методах;
8. отличать generics Java от структурной и более выразительной type-level системы TypeScript.

---

## 1. Карта выбора типа

Начинай не с ключевого слова, а с вопроса: что должен гарантировать тип?

| Требование                                                                 | Обычно подходит                       |
|----------------------------------------------------------------------------|---------------------------------------|
| Несколько несвязанных реализаций одного поведения                          | `interface`                           |
| Общая реализация, состояние и защищённый constructor для наследников       | `abstract class`                      |
| Класс не предназначен для наследования                                     | `final class`                         |
| Известен закрытый набор разных вариантов с собственными данными            | `sealed interface` + `record`         |
| Компактный value object или неизменяемый переносчик данных                  | `record`                              |
| Закрытый набор именованных singleton-значений                              | `enum`                                |
| Сущность с изменяемым состоянием, identity и жизненным циклом               | обычный или `final` class             |
| Метод может не найти значение                                              | `Optional<T>` в return type           |
| Один алгоритм работает с несколькими типами при сохранении type safety      | generic method или generic class      |

Это эвристика, а не автоматическое правило. Например, `record` синтаксически компактен, но mutable JPA entity с identity
и жизненным циклом обычно не является record по смыслу.

---

## 2. `interface`, `abstract`, `final` и `sealed` как границы расширения

### `interface`: контракт между модулями

Java interface задаёт номинативный runtime-контракт. Класс должен явно объявить `implements`; совпадения формы методов
недостаточно.

```java
public interface AccountRepository {
    Optional<Account> findById(AccountId id);

    void save(Account account);
}
```

Инфраструктурная реализация явно присоединяется к контракту:

```java
public final class InMemoryAccountRepository implements AccountRepository {
    private final Map<AccountId, Account> accounts = new HashMap<>();

    @Override
    public Optional<Account> findById(AccountId id) {
        return Optional.ofNullable(accounts.get(id));
    }

    @Override
    public void save(Account account) {
        accounts.put(account.id(), account);
    }
}
```

В отличие от TypeScript interface, Java interface:

- существует в bytecode и runtime;
- участвует в `instanceof`, reflection и dynamic dispatch;
- может содержать `default`, `static` и private methods;
- может объявлять только константы как fields: они неявно `public static final`;
- не хранит instance state и не имеет constructor;
- может быть generic, sealed или обычным расширяемым контрактом.

`default` method помогает эволюционировать интерфейс без немедленного изменения всех реализаций:

```java
public interface FeePolicy {
    Money calculateFor(Money amount);

    default boolean isFreeFor(Money amount) {
        return calculateFor(amount).isZero();
    }
}
```

Но default method не должен превращать interface в свалку общей бизнес-логики. Если логика зависит от общего mutable
state или сложных protected hooks, возможно, нужен отдельный collaborator или abstract class.

### `abstract class`: частично реализованная база

Abstract class полезен, когда подтипы действительно образуют одну иерархию и разделяют состояние или алгоритм:

```java
public abstract class Operation {
    private final OperationId id;
    private final Instant createdAt;

    protected Operation(OperationId id, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public final OperationId id() {
        return id;
    }

    public final Instant createdAt() {
        return createdAt;
    }

    public abstract Money signedAmount();
}
```

У Java нет множественного наследования классов, поэтому выбор base class занимает единственный слот `extends`. Для
архитектурной границы чаще достаточно interface; abstract class стоит вводить ради настоящей общей модели, а не ради
экономии нескольких строк.

### Три значения `final`

Контекст меняет смысл `final`:

| Где используется       | Что запрещает                                      | Чего не гарантирует                         |
|------------------------|----------------------------------------------------|---------------------------------------------|
| variable или field     | повторное присваивание переменной/ссылки           | глубокую immutability объекта               |
| method                 | override в наследнике                              | отсутствие других способов изменить объект  |
| class                  | создание subclass                                  | immutability экземпляров                    |

```java
final List<String> tags = new ArrayList<>();
tags.add("urgent");       // допустимо: объект изменяется
// tags = List.of();       // ошибка: ссылка переназначается
```

В domain-коде `final class` часто является хорошим default: наследование запрещено, пока для него не найдено осмысленное
отношение `is-a` и стабильный контракт расширения.

### `sealed`: контролируемое расширение

Обычный interface открыт: новую реализацию может добавить другой модуль. Sealed type перечисляет допустимые прямые
подтипы:

```java
public sealed interface TransferResult
        permits TransferSucceeded, TransferRejected {
}

public record TransferSucceeded(TransferId transferId)
        implements TransferResult {
}

public record TransferRejected(RejectionReason reason)
        implements TransferResult {
}
```

Каждый разрешённый прямой subtype должен выбрать дальнейшую политику:

- `final` — ветка закрыта;
- `sealed` — разрешён следующий ограниченный набор;
- `non-sealed` — ветка снова открыта для произвольного наследования.

Records неявно `final`, поэтому хорошо подходят для листьев sealed hierarchy.

В Java 21 pattern matching for `switch` позволяет обработать варианты без ручных cast:

```java
static String message(TransferResult result) {
    return switch (result) {
        case TransferSucceeded(var transferId) ->
                "Transfer completed: " + transferId;
        case TransferRejected(var reason) ->
                "Transfer rejected: " + reason;
    };
}
```

Compiler знает закрытый набор direct subtypes и проверяет exhaustiveness. Это ближайшая Java-аналогия TypeScript
discriminated union, но не точная копия: варианты Java являются настоящими runtime-классами.

```typescript
type TransferResult =
  | { kind: "succeeded"; transferId: string }
  | { kind: "rejected"; reason: RejectionReason };
```

### Когда sealed type не нужен

Не закрывай extension point, если реализации должны добавляться независимо: JDBC drivers, плагины, внешние адаптеры,
стратегии интеграции. `sealed` особенно полезен для конечных domain-состояний и результатов, которыми владеет один
модуль.

---

## 3. `record`: value-oriented data carrier

Record объявляет состояние через список components:

```java
public record Money(BigDecimal amount, Currency currency) {
}
```

Compiler создаёт:

- private final fields для components;
- public accessors `amount()` и `currency()`;
- canonical constructor;
- `equals`, `hashCode` и `toString` на основе components.

Record остаётся классом. Он может реализовать interfaces, иметь methods, static fields и static methods. Он не может
расширить другой пользовательский класс и не может быть superclass, потому что неявно `final`.

### Когда выбирать record

`record` подходит, когда класс нужен в первую очередь для хранения набора данных, объект после создания не должен
меняться, а два объекта с одинаковыми данными должны считаться одинаковыми.

Например, `Money` — это значение. Две суммы с одинаковыми количеством и валютой логично считать равными:

```java
public record Money(BigDecimal amount, Currency currency) {
}
```

Простая проверка:

> Если объект можно описать как «несколько значений, которые вместе образуют одно значение», вероятно, подходит
> `record`.

Типичные примеры:

- value objects: `Money`, `AccountId`, `TransferId`;
- DTO запросов и ответов: `TransferRequest`, `TransactionResult`;
- неизменяемые результаты операций;
- domain events как варианты закрытой sealed hierarchy.

`record` обычно не подходит в следующих случаях:

- состояние объекта должно изменяться, например баланс кошелька после `deposit` и `withdraw`;
- у объекта важна собственная identity: два кошелька с одинаковым балансом всё равно являются разными кошельками;
- класс должен быть родителем для других классов;
- это изменяемая JPA entity с ORM-жизненным циклом;
- не все поля должны участвовать в `equals` или правила равенства существенно сложнее сравнения компонентов;
- используемый serializer или framework не поддерживает нужный constructor contract.

| Ситуация                               | Что обычно выбрать |
|----------------------------------------|--------------------|
| Неизменяемый набор данных              | `record`           |
| DTO запроса или ответа                 | часто `record`     |
| Value object вроде `Money` или `Email` | часто `record`     |
| Объект с изменяемым состоянием         | обычный класс      |
| Entity с уникальной identity           | обычный класс      |
| JPA entity                             | обычный класс      |
| Нужен superclass                       | обычный класс      |
| Нужны сложные правила равенства        | чаще обычный класс |

Коротко: **`record` — для значений и данных; обычный класс — для сущностей с состоянием, identity и жизненным циклом.**
При этом record может содержать методы и проверки. Важен не запрет на поведение, а смысл объекта.

### Record не гарантирует глубокую immutability

Ссылка на component неизменяема, объект по ссылке может оставаться mutable:

```java
public record Batch(List<Operation> operations) {
}

List<Operation> source = new ArrayList<>();
Batch batch = new Batch(source);
source.add(operation); // содержимое batch фактически изменилось
```

Защитная копия закрывает эту дыру:

```java
public record Batch(List<Operation> operations) {
    public Batch {
        operations = List.copyOf(operations);
    }
}
```

### Compact constructor и инварианты

Compact constructor позволяет проверять и нормализовать components без повторения списка параметров:

```java
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");

        if (amount.scale() > 2) {
            throw new IllegalArgumentException("amount must have at most 2 fraction digits");
        }

        amount = amount.setScale(2);
    }

    public static Money rubles(String amount) {
        return new Money(new BigDecimal(amount), Currency.RUB);
    }
}
```

Присваивания fields выполняются compiler после тела compact constructor. Поэтому параметры можно валидировать и
нормализовать, но нельзя вручную писать `this.amount = ...` внутри compact constructor.

### Осторожно с `BigDecimal` в record equality

Сгенерированный `equals` использует `BigDecimal.equals`, который учитывает scale:

```java
new BigDecimal("10.0").equals(new BigDecimal("10.00")); // false
```

Если domain считает эти суммы равными, record должен нормализовать scale в constructor либо получить явно написанные
`equals/hashCode`. Нельзя нормализовать только в `equals`: одинаковая стратегия обязана сохранять его контракт и
согласованность с `hashCode`.

---

## 4. `enum`: закрытый набор singleton-экземпляров

Java enum — не строковой union и не набор числовых constants. Каждый элемент является единственным экземпляром enum
class:

```java
public enum AccountStatus {
    ACTIVE,
    BLOCKED,
    CLOSED
}
```

Поэтому enum корректно сравнивать через `==`:

```java
if (account.status() == AccountStatus.ACTIVE) {
    // операция разрешена
}
```

Enum может хранить fields и реализовывать поведение:

```java
public enum AccountStatus {
    ACTIVE(true),
    BLOCKED(false),
    CLOSED(false);

    private final boolean operationsAllowed;

    AccountStatus(boolean operationsAllowed) {
        this.operationsAllowed = operationsAllowed;
    }

    public boolean operationsAllowed() {
        return operationsAllowed;
    }
}
```

Не стоит помещать в enum всю изменчивую бизнес-логику. Простое поведение, не зависящее от внешних сервисов, уместно;
интеграции и сложные workflows лучше оставить domain/application services.

### `name()`, `toString()` и внешний контракт

`name()` возвращает идентификатор constant, например `"ACTIVE"`. Не используй `ordinal()` как значение базы данных или
API: добавление и перестановка constants изменит номера.

Для стабильного внешнего кода лучше объявить явное поле:

```java
public enum Currency {
    RUB("RUB"),
    USD("USD");

    private final String code;

    Currency(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
```

Не принимай пользовательский ввод через голый `valueOf` без boundary-validation: неизвестное значение приводит к
`IllegalArgumentException`, а API обычно должен вернуть контролируемую validation error.

### Enum collections

Для sets и maps с enum keys стандартная библиотека предоставляет специализированные структуры:

```java
Set<AccountStatus> mutableStatuses =
        EnumSet.of(AccountStatus.ACTIVE, AccountStatus.BLOCKED);

Map<AccountStatus, String> labels = new EnumMap<>(AccountStatus.class);
labels.put(AccountStatus.ACTIVE, "Активен");
```

`EnumSet` и `EnumMap` яснее выражают намерение и обычно эффективнее общих `HashSet`/`HashMap` для enum.

### `enum` против sealed hierarchy

| Вопрос                                               | `enum`                                   | sealed hierarchy                           |
|------------------------------------------------------|------------------------------------------|--------------------------------------------|
| У каждого варианта только одно значение              | да                                       | нет, создаются разные экземпляры           |
| Варианты несут разные наборы данных                  | неудобно                                 | естественно через разные records/classes   |
| Нужны простые именованные состояния                  | отлично                                  | избыточно                                  |
| Нужен `SUCCESS(id)` или `REJECTED(reason)`           | неудобно                                 | отлично                                    |
| Закрытый набор проверяется compiler                  | да                                       | да                                         |

---

## 5. `null`: допустимое runtime-значение без достаточного type-контракта

У любого reference type переменная потенциально может содержать `null`, если внешние средства анализа не доказывают
обратное:

```java
Account account = null; // компилируется
```

В TypeScript с `strictNullChecks` типы `Account` и `Account | null` различаются. В стандартной type system Java такой
границы нет: signature `Account find(...)` сама по себе не сообщает compiler, вернёт ли метод `null`.

Поэтому backend-код должен делать nullability частью договора:

- обязательные constructor parameters проверяются через `Objects.requireNonNull`;
- методы не возвращают `null` без явно принятой convention/annotation;
- collections возвращаются пустыми, а не `null`;
- отсутствие одиночного результата выражается через `Optional<T>`, когда это улучшает API;
- на внешней границе JSON/DB nullability валидируется и преобразуется в domain contract.

```java
public Account(AccountId id, Currency currency) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.currency = Objects.requireNonNull(currency, "currency must not be null");
}
```

`Objects.requireNonNull` не добавляет новый вид type safety. Он превращает скрытую позднюю ошибку в раннее нарушение
контракта с понятным местом возникновения.

### Nullability annotations

В enterprise-проектах встречаются `@Nullable`, `@NonNull`, package-level defaults и статический анализ IDE, SpotBugs,
NullAway или Checker Framework. Они полезны, но конкретные annotations зависят от стека проекта. Не смешивай несколько
семейств annotations без общей настройки анализаторов.

---

## 6. `Optional<T>`: ноль или одно значение

`Optional<T>` — контейнер, который либо содержит non-null `T`, либо пуст. Его главное назначение — return type метода,
где отсутствие результата является нормальным сценарием.

```java
public interface AccountRepository {
    Optional<Account> findById(AccountId id);
}
```

Сигнатура вынуждает caller выбрать поведение для отсутствующего счёта:

```java
Account account = repository.findById(accountId)
        .orElseThrow(() -> new AccountNotFoundException(accountId));
```

### Создание Optional

```java
Optional<Account> empty = Optional.empty();
Optional<Account> known = Optional.of(account);          // null запрещён
Optional<Account> maybe = Optional.ofNullable(rawValue); // null -> empty
```

`Optional` сам не должен быть `null`. Иначе появляется два представления отсутствия и весь контракт теряет смысл.

```java
// Плохо
Optional<Account> findById(AccountId id) {
    return null;
}
```

### Преобразование без ручной распаковки

```java
Optional<Currency> currency = repository.findById(accountId)
        .filter(Account::isActive)
        .map(Account::currency);
```

- `map` преобразует `T` в `U` и возвращает `Optional<U>`;
- `flatMap` нужен, если функция уже возвращает `Optional<U>`;
- `filter` оставляет значение только при выполнении predicate;
- `or` лениво предоставляет другой `Optional`;
- `stream` удобно соединяет Optional со Stream API.

```java
Optional<Client> client = repository.findById(accountId)
        .flatMap(account -> clientRepository.findById(account.clientId()));
```

С `map` здесь получился бы `Optional<Optional<Client>>`.

### `orElse` выполняет аргумент заранее

```java
Account account = repository.findById(id)
        .orElse(loadFallbackAccount());
```

`loadFallbackAccount()` вызывается до `orElse`, даже если Optional заполнен. Для дорогого или имеющего side effect
fallback используй lazy supplier:

```java
Account account = repository.findById(id)
        .orElseGet(this::loadFallbackAccount);
```

### Почему `get()` обычно является smell

```java
if (account.isPresent()) {
    return account.get();
}
```

Такой код превращает Optional в усложнённую null-check. Обычно яснее `orElseThrow`, `map`, `flatMap`, `ifPresent` или
явное ветвление через `isEmpty`, если ветка действительно сложная.

### Где Optional обычно не нужен

| Контекст                         | Предпочтительный контракт                                      |
|----------------------------------|----------------------------------------------------------------|
| обязательный parameter           | обычный `T` + ранняя проверка                                  |
| optional parameter               | отдельный method/command type или явно принятая nullability     |
| field domain entity              | модель состояния предметной области, не wrapper по умолчанию    |
| элемент collection               | не класть `Optional<T>` в `List` без особой причины             |
| collection return type           | пустая collection                                               |
| REST DTO                         | учитывать семантику JSON и поддержку serializer                 |
| JPA entity field                 | учитывать mapping; обычно nullable field, не `Optional` field   |
| одиночный результат поиска       | `Optional<T>`                                                   |

`Optional` как parameter заставляет caller создавать обёртку и часто маскирует неясный API:

```java
// Неудачный API
void createAccount(Optional<String> description) { }
```

В application command может быть уместнее nullable field с явной boundary-семантикой либо отдельный value type. Для
PATCH нужно отличать как минимум «поле отсутствовало» от «поле передали со значением», а иногда ещё от «поле передали как
null». Один `Optional` не всегда моделирует все три состояния.

### Optional и TypeScript

| Java                                      | TypeScript                                   |
|-------------------------------------------|----------------------------------------------|
| `Optional<Account>`                       | чаще `Account | undefined`                   |
| `map`                                     | optional chaining + преобразование           |
| `orElse(defaultValue)`                    | `value ?? defaultValue`                      |
| `orElseGet(supplier)`                     | lazy fallback через явное ветвление/функцию  |
| `orElseThrow(...)`                        | проверка и `throw`                           |
| отдельный runtime object                  | union существует только в type system        |

Optional не является полным аналогом `T | null | undefined`: в нём нет различия между `null` и `undefined`, а сам
Optional является Java-объектом со своим API.

---

## 7. Основы Java generics

Generics позволяют параметризовать class, interface, method или constructor типами:

```java
public interface Repository<ID, ENTITY> {
    Optional<ENTITY> findById(ID id);

    void save(ENTITY entity);
}
```

Конкретизация сохраняет связь между ID и entity на этапе компиляции:

```java
Repository<AccountId, Account> accounts = ...;

Optional<Account> account = accounts.findById(accountId);
// accounts.findById(transferId); // compile-time error
```

Без generics пришлось бы принимать/возвращать `Object`, вручную cast и обнаруживать часть ошибок в runtime.

### Generic method

Type parameter метода объявляется перед return type:

```java
public static <T> T requirePresent(Optional<T> value, Supplier<String> message) {
    return value.orElseThrow(() -> new NoSuchElementException(message.get()));
}
```

`T` принадлежит методу, а не содержащему его классу. Обычно compiler выводит type argument из аргументов вызова.

### Bounds

Bound ограничивает допустимые type arguments и сообщает compiler доступные операции:

```java
public static <T extends Comparable<? super T>> T max(List<T> values) {
    return values.stream()
            .max(Comparator.naturalOrder())
            .orElseThrow();
}
```

`extends` в type parameter означает upper bound как для классов, так и для interfaces. При нескольких bounds класс,
если он есть, указывается первым:

```java
<T extends BaseEvent & Auditable & Serializable>
```

### Primitive type нельзя использовать как type argument

```java
List<Integer> attempts = new ArrayList<>();
// List<int> attempts; // compile-time error
```

Используется wrapper и при необходимости autoboxing. Для некоторых числовых сценариев есть primitive specializations:
`IntStream`, `OptionalInt`, `ToIntFunction<T>` и другие.

### Не используй raw types

```java
List raw = new ArrayList();
raw.add("text");
raw.add(42);
```

Raw type существует ради совместимости со старым Java-кодом и отключает часть type safety. Новый код должен использовать
`List<?>`, если element type действительно неизвестен, либо конкретный `List<T>`.

---

## 8. Инвариантность и wildcards

Пусть `PremiumAccount extends Account`. Из этого не следует, что `List<PremiumAccount>` является subtype
`List<Account>`:

```java
List<PremiumAccount> premiumAccounts = new ArrayList<>();
// List<Account> accounts = premiumAccounts; // compile-time error
```

Если бы присваивание было разрешено, через `accounts` можно было бы добавить обычный `Account`, нарушив настоящий тип
списка:

```java
accounts.add(new Account(...));
PremiumAccount premium = premiumAccounts.get(0); // тип больше не гарантирован
```

Java generic types по умолчанию инвариантны. Wildcard задаёт безопасный взгляд на параметризованный тип.

### Upper-bounded wildcard: producer

```java
public static Money total(List<? extends Operation> operations) {
    Money result = Money.zero(Currency.RUB);

    for (Operation operation : operations) {
        result = result.add(operation.amount());
    }

    return result;
}
```

Метод принимает `List<Deposit>`, `List<Withdrawal>` или `List<Operation>` и безопасно читает элементы как `Operation`.
Добавлять конкретные операции нельзя: compiler не знает настоящий subtype списка. Добавить можно только `null`, но это
не является полезной практикой.

### Lower-bounded wildcard: consumer

```java
public static void addAuditEntries(
        List<? super AuditEntry> target,
        List<AuditEntry> source
) {
    target.addAll(source);
}
```

`target` может быть `List<AuditEntry>` или `List<Object>`. В него безопасно записывать `AuditEntry`, но прочитать элемент
можно лишь как `Object`.

### PECS

Мнемоника:

- Producer Extends — источник значений: `? extends T`;
- Consumer Super — приёмник значений: `? super T`.

Стандартный пример уже виден в API collections:

```java
public static <T> void copy(
        List<? super T> dest,
        List<? extends T> src
)
```

Если структура и читает, и принимает `T`, часто нужен инвариантный `List<T>` без wildcard.

### `<?>` — неизвестный, но единый тип

`List<?>` не равно `List<Object>`:

- `List<Object>` действительно принимает любой объект;
- `List<?>` означает список некоторого неизвестного element type;
- из `List<?>` безопасно читать `Object`, но нельзя добавлять обычные значения.

```java
static int sizeOf(List<?> values) {
    return values.size();
}
```

Используй `<?>`, когда алгоритму не важно, что за element type находится внутри.

---

## 9. Java generics против TypeScript generics

В обоих языках generics связывают несколько позиций одного API и сохраняют информацию о типах для checker. Но окружающие
type systems различаются.

| Свойство                          | Java                                                               | TypeScript                                                       |
|-----------------------------------|--------------------------------------------------------------------|------------------------------------------------------------------|
| Основная модель типов             | номинативная для classes/interfaces                                | преимущественно структурная                                     |
| Результат компиляции              | bytecode с erased type arguments и служебными casts/bridge methods | JavaScript без TS types                                          |
| Runtime generic argument          | обычно недоступен у экземпляра                                     | отсутствует                                                      |
| Variance                          | инвариантность + use-site wildcards                                | структурная совместимость; variance выводится/может аннотироваться|
| Upper bound                       | `<T extends Bound>`                                                 | `<T extends Constraint>`                                         |
| Lower bound                       | `? super T`                                                        | прямого эквивалента wildcard нет                                 |
| Union/intersection                | intersection bounds; нет общих union types                         | `A | B`, `A & B`                                                 |
| Conditional/mapped types          | нет                                                                | есть                                                             |
| `keyof`, indexed access           | нет                                                                | есть                                                             |
| Primitive type arguments          | нельзя                                                             | можно использовать `number`, `boolean` и другие TS types         |
| Runtime-проверка `instanceof`     | проверяет реальный class/interface, но не erased argument           | работает только с JS constructor/prototype, не с interface       |
| Несовместимость по имени          | обычна: требуется `extends`/`implements`                            | одинаковая структура часто совместима                            |

### Структурность меняет интуицию

TypeScript:

```typescript
interface HasId {
  id: string;
}

function readId<T extends HasId>(value: T): string {
  return value.id;
}

readId({ id: "ACC-1", balance: 100 }); // подходит по форме
```

Java требует объявленной номинативной связи:

```java
public interface HasId {
    String id();
}

public static <T extends HasId> String readId(T value) {
    return value.id();
}
```

Класс с методом `String id()` не подойдёт, пока явно не объявит `implements HasId`.

### Не переносить сложный type-level стиль TypeScript буквально

В TypeScript generic API может вычислять новые типы через conditional, mapped, template literal и indexed access types.
Java generics предназначены прежде всего для type-safe контейнеров, алгоритмов и связей между номинативными типами.
Попытка эмулировать TypeScript type-level programming множеством Java-иерархий часто ухудшает API.

---

## 10. Type erasure

Java compiler проверяет parameterized types, но JVM в большинстве случаев работает с их erasure.

Упрощённо:

```java
public final class Box<T> {
    private T value;

    public T get() {
        return value;
    }
}
```

После erasure `T` без bound превращается в `Object`; compiler сохраняет type safety на местах использования и при
необходимости вставляет cast:

```java
Box<Account> box = ...;
Account account = box.get();
```

Концептуально runtime-вызов близок к:

```java
Account account = (Account) box.get();
```

Это модель для понимания, а не буквальный Java source, который обязательно генерируется как отдельный файл.

### Правило erasure

- `List<String>` и `List<Account>` стираются до `List`;
- unbounded `T` стирается до `Object`;
- `T extends Account` стирается до `Account`;
- при нескольких bounds используется erasure левого bound;
- parameterized method signatures также стираются.

### Что доступно в runtime

```java
List<String> names = new ArrayList<>();
List<Integer> numbers = new ArrayList<>();

System.out.println(names.getClass() == numbers.getClass()); // true
```

Оба объекта имеют runtime class `ArrayList`. Проверить конкретный argument нельзя:

```java
if (value instanceof List<?>) { }
// if (value instanceof List<String>) { } // compile-time error
```

`List<?>` является reifiable type: runtime может проверить, что объект является каким-то `List`, не выясняя element
type.

### Ограничения, вызванные erasure

#### Нельзя создать `new T()`

```java
public static <T> T create() {
    // return new T(); // compile-time error
}
```

Runtime не знает constructor конкретного `T`. Передай factory:

```java
public static <T> T create(Supplier<T> factory) {
    return factory.get();
}
```

#### Нельзя создать обычный generic array

```java
// T[] values = new T[10];
// List<String>[] groups = new List<String>[10];
```

Arrays reified и проверяют component type в runtime, а parameterized element type стёрт. Обычно используй `List<T>`.

#### Нельзя перегрузить методы только type arguments

```java
// После erasure обе сигнатуры — process(List)
void process(List<Account> accounts) { }
void process(List<Transfer> transfers) { }
```

Compiler отвергнет name clash. Разные имена (`processAccounts`, `processTransfers`) выражают намерение яснее.

#### Нельзя обращаться к class literal parameterized type

```java
Class<List> rawListClass = List.class;
// Class<List<String>> type = List<String>.class;
```

### Reflection: важная оговорка

Фраза «generics полностью исчезают» слишком груба. Class file может хранить generic metadata объявлений, и reflection
может прочитать, например, тип field `List<String>`. Но обычный объект `new ArrayList<String>()` не несёт надёжный runtime
token своего type argument.

```java
Field field = Report.class.getDeclaredField("accountIds");
Type declaredType = field.getGenericType(); // может описывать List<AccountId>
```

Это metadata позиции объявления, а не восстановление element type произвольного `List` в runtime.

### Bridge methods

Erasure иногда меняет erased signature при override. Compiler генерирует synthetic bridge method, чтобы сохранить
полиморфизм:

```java
interface Converter<T> {
    T convert(String value);
}

final class AccountIdConverter implements Converter<AccountId> {
    @Override
    public AccountId convert(String value) {
        return new AccountId(UUID.fromString(value));
    }
}
```

У интерфейсного метода erased return type — `Object`; implementation возвращает `AccountId`. Bytecode может содержать
служебный bridge, делегирующий настоящему методу. Обычно это заметно только в reflection, stack traces или при изучении
bytecode.

### Heap pollution

Heap pollution возникает, когда переменная parameterized type ссылается на объект с несовместимым содержимым. Частый
источник — raw types и unchecked casts:

```java
List<String> names = new ArrayList<>();
List raw = names;
raw.add(42);

String first = names.get(0); // ClassCastException здесь, далеко от причины
```

Не подавляй `unchecked` warning без локального доказательства безопасности. Чем шире scope `@SuppressWarnings`, тем легче
скрыть настоящий дефект.

Generic varargs также требуют осторожности, потому что varargs реализован через array. `@SafeVarargs` допустим только
там, где автор метода гарантирует отсутствие небезопасной записи или утечки массива.

### Как передать type information явно

Для простого класса передают `Class<T>`:

```java
public static <T> T parse(String json, Class<T> type) {
    // serializer использует type как runtime token
    throw new UnsupportedOperationException();
}
```

Но `Class<List<Account>>` получить нельзя. Библиотеки сериализации используют super type token — например,
`TypeReference<List<Account>>` или аналог конкретной библиотеки. Anonymous subclass сохраняет generic declaration в
metadata, откуда библиотека читает вложенный тип.

---

## 11. Связный пример для Wallet Service

Смоделируем результат перевода, lookup repository и reusable application service.

### Value types и enum

```java
public record AccountId(UUID value) {
    public AccountId {
        Objects.requireNonNull(value, "value must not be null");
    }
}

public record TransferId(UUID value) {
    public TransferId {
        Objects.requireNonNull(value, "value must not be null");
    }
}

public enum Currency {
    RUB,
    USD
}

public enum RejectionReason {
    SOURCE_NOT_FOUND,
    TARGET_NOT_FOUND,
    ACCOUNT_NOT_ACTIVE,
    CURRENCY_MISMATCH,
    INSUFFICIENT_FUNDS
}
```

Отдельные nominal IDs не дают случайно передать `TransferId` вместо `AccountId`, хотя оба содержат `UUID`. В TypeScript
для подобной гарантии понадобились бы branded types: обычные `{ value: string }` структурно совместимы.

### Закрытый результат с разными payload

```java
public sealed interface TransferResult
        permits TransferSucceeded, TransferRejected {
}

public record TransferSucceeded(TransferId transferId)
        implements TransferResult {
}

public record TransferRejected(RejectionReason reason)
        implements TransferResult {
}
```

`enum` описывает причины без дополнительных данных. Sealed hierarchy описывает два результата, каждый со своим payload.

### Generic repository и Optional

```java
public interface Repository<ID, ENTITY> {
    Optional<ENTITY> findById(ID id);

    void save(ENTITY entity);
}

public interface AccountRepository extends Repository<AccountId, Account> {
}
```

`Optional<Account>` сообщает, что отсутствие счёта нормально для lookup. `save` требует настоящий `Account`, поэтому
`Optional<Account>` как parameter там не нужен.

### Обработка результата

```java
public final class TransferResponseMapper {
    public String code(TransferResult result) {
        return switch (result) {
            case TransferSucceeded ignored -> "TRANSFER_CREATED";
            case TransferRejected(var reason) -> switch (reason) {
                case SOURCE_NOT_FOUND, TARGET_NOT_FOUND -> "ACCOUNT_NOT_FOUND";
                case ACCOUNT_NOT_ACTIVE -> "ACCOUNT_NOT_ACTIVE";
                case CURRENCY_MISMATCH -> "CURRENCY_MISMATCH";
                case INSUFFICIENT_FUNDS -> "INSUFFICIENT_FUNDS";
            };
        };
    }
}
```

Exhaustive switches заставят обработать новый subtype или enum constant при расширении domain-модели.

---

## 12. Частые ошибки

### Ошибка 1. Создавать interface для каждого implementation

`AccountService` + `AccountServiceImpl` без второй реализации, architectural boundary или тестовой причины добавляет имя,
но не обязательно добавляет полезную абстракцию.

### Ошибка 2. Использовать abstract class только ради переиспользования кода

Наследование создаёт сильную связь и занимает единственный `extends`. Часто отдельный collaborator лучше выражает
реальную зависимость.

### Ошибка 3. Считать `final` синонимом immutable

`final List<T>` запрещает переназначить ссылку, но не запрещает менять список.

### Ошибка 4. Считать любой record immutable

Record с `List`, массивом, mutable entity или `Date` component может менять наблюдаемое состояние. Нужны defensive copies
и immutable components.

### Ошибка 5. Сохранять enum через `ordinal()`

Перестановка constants меняет данные. Храни стабильное строковое имя или явно заданный code.

### Ошибка 6. Возвращать `null` вместо `Optional.empty()`

Получается двойной nullable-контракт и caller снова рискует получить NPE.

### Ошибка 7. Вызывать `Optional.get()` без проверки

Это nullable-доступ с другим exception. Выбери явное поведение: `orElseThrow`, fallback или преобразование.

### Ошибка 8. Использовать `orElse` с дорогим fallback

Аргумент вычисляется eagerly. Используй `orElseGet` для lazy evaluation.

### Ошибка 9. Возвращать `null` collection

Пустой `List` уже естественно выражает отсутствие элементов. `null` добавляет caller лишнюю ветку без нового смысла.

### Ошибка 10. Путать `List<?>` и `List<Object>`

Первый — список неизвестного element type; второй — список, в который можно добавить любой object.

### Ошибка 11. Использовать `? extends T` и затем пытаться добавлять `T`

Настоящий список может быть `List<SpecificSubtype>`. Upper-bounded wildcard безопасен прежде всего для чтения.

### Ошибка 12. Игнорировать raw type warning

Raw types позволяют загрязнить heap и перенести `ClassCastException` далеко от причины.

### Ошибка 13. Считать, что reflection всегда знает `T`

Reflection может увидеть generic declaration field или superclass, но произвольный экземпляр `List<T>` обычно не знает
свой runtime argument.

### Ошибка 14. Проверять `instanceof List<String>`

После erasure runtime различает `List`, но не `List<String>` и `List<Account>`.

---

## 13. Практика для Wallet Service

### Задание 1. Value types

Создай records:

```java
AccountId(UUID value)
TransferId(UUID value)
Money(BigDecimal amount, Currency currency)
```

Требования:

- components не принимают `null`;
- `Money.amount` положителен для command либо вынеси положительную сумму в отдельный `PositiveMoney`;
- scale нормализован и согласован с `equals/hashCode`;
- проверь, что `AccountId` нельзя передать туда, где требуется `TransferId`.

### Задание 2. Enum и переходы состояния

Реализуй `AccountStatus`: `ACTIVE`, `BLOCKED`, `CLOSED`.

- денежные операции разрешены только для `ACTIVE`;
- `CLOSED` нельзя вернуть в `ACTIVE`;
- не используй `ordinal()`;
- напиши exhaustive switch, возвращающий разрешённые команды для каждого статуса.

### Задание 3. Sealed result

Создай sealed `TransferResult` с вариантами:

- `Succeeded(TransferId id)`;
- `Rejected(RejectionReason reason)`;
- `Duplicate(TransferId originalId)`.

Преобразуй результат в стабильный API code через exhaustive switch. Добавь четвёртый subtype и посмотри, какие места
перестанут компилироваться.

### Задание 4. Optional boundary

Реализуй:

```java
Optional<Account> findById(AccountId id);
Account getRequired(AccountId id);
```

Первый метод repository сообщает об обычном отсутствии. Второй application/domain helper преобразует отсутствие в
`AccountNotFoundException`. Не возвращай `null` и не вызывай `get()`.

### Задание 5. Generic repository

Создай:

```java
interface Repository<ID, ENTITY>
```

и две специализации для `Account` и `Transfer`. Добейся compile-time ошибки при попытке искать account по `TransferId`.
Объясни, почему runtime не сможет проверить `repository instanceof Repository<AccountId, Account>`.

### Задание 6. PECS

Напиши метод, копирующий domain events:

```java
static <E extends DomainEvent> void appendAll(
        Collection<? super E> target,
        Collection<? extends E> source
)
```

Проверь вызовы с `List<TransferCompleted>` и `List<DomainEvent>`. Для каждой collection объясни, почему используется
`extends` или `super`.

### Задание 7. Исследование erasure

Скомпилируй маленький generic class и исследуй его через:

```text
javap -c -p -s YourClass.class
```

Найди:

- erased descriptors;
- вставленный cast на месте использования;
- bridge method после реализации generic interface конкретным типом.

### Задание 8. Heap pollution

В отдельном экспериментальном тесте смешай `List<String>` и raw `List`, получи отложенный `ClassCastException`, затем
устрани raw type. Такой код не должен попасть в реализацию Wallet Service.

---

## 14. Вопросы для самопроверки

1. Почему класс с подходящими methods не реализует Java interface автоматически?
2. Когда abstract class лучше interface?
3. Какие три разных ограничения выражает `final`?
4. Чем sealed interface отличается от обычного interface?
5. Почему record не гарантирует глубокую immutability?
6. Почему `BigDecimal` component требует решения о scale до использования сгенерированного record equality?
7. Почему enum сравнивают через `==`?
8. Почему нельзя хранить enum через `ordinal()`?
9. Когда sealed hierarchy лучше enum?
10. Почему signature `Account findById(...)` не сообщает Java compiler о nullability?
11. Почему `Optional` чаще подходит для return type, чем для parameter или field?
12. Чем отличаются `Optional.of` и `Optional.ofNullable`?
13. Почему `orElseGet` может быть правильнее `orElse`?
14. Почему `List<PremiumAccount>` не является `List<Account>`?
15. Что значит PECS?
16. Чем `List<?>` отличается от `List<Object>`?
17. Что происходит с unbounded `T` после erasure?
18. Почему разрешён `instanceof List<?>`, но запрещён `instanceof List<String>`?
19. Почему нельзя перегрузить `process(List<Account>)` и `process(List<Transfer>)`?
20. Что такое heap pollution?
21. Зачем compiler создаёт bridge methods?
22. В каком смысле reflection видит generics, а в каком — нет?
23. Какие возможности TypeScript generics нельзя напрямую перенести в Java?
24. Почему отдельные records `AccountId` и `TransferId` полезнее общего `UUID`?

---

## Краткий итог

- `interface` задаёт runtime-контракт; `abstract class` добавляет общие state и implementation.
- `final` управляет переназначением и наследованием, но сам по себе не гарантирует immutability.
- `sealed` закрывает набор подтипов и вместе с records моделирует варианты с разными payload.
- `record` генерирует value-oriented boilerplate, но требует явных инвариантов и defensive copies.
- `enum` — набор singleton-экземпляров; для внешнего хранения нельзя полагаться на `ordinal()`.
- `null` остаётся допустимым значением reference type; обязательность нужно защищать контрактом и проверками.
- `Optional<T>` полезен прежде всего как return type для нормального сценария «результат не найден».
- Java generics номинативны, инвариантны и используют wildcards; TypeScript generics работают внутри структурной и более
  выразительной type system.
- PECS: producer — `extends`, consumer — `super`.
- Из-за erasure runtime обычно различает `List`, но не `List<String>` и `List<Account>`.
- Raw types и unchecked casts разрушают compile-time guarantees и могут привести к heap pollution.

## Официальные материалы

- [Java SE 21: `Optional`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html)
- [Dev.java: Records](https://dev.java/learn/records/)
- [Dev.java: Generics](https://dev.java/learn/generics/intro/)
- [Java Language Specification 21: Types, Values, and Variables](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html)
- [Java Language Specification 21: Classes](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html)
