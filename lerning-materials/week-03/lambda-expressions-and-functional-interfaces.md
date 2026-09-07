# Лямбда-выражения и функциональные интерфейсы в Java

[← Вернуться к учебному плану](../java-backend-learning-plan.md)

Лямбда-выражение позволяет передать поведение как значение: условие проверки, преобразование, действие, способ создания
объекта или правило вычисления результата. Но в Java функция не является самостоятельным типом. Каждая лямбда получает
тип конкретного **функционального интерфейса**.

Для Wallet Service это пригодится, например, чтобы передать правило проверки счёта, преобразователь доменного события в
outbox-событие или отложенное действие аудита. При этом лямбда не должна скрывать основную бизнес-логику перевода: если
поведение имеет важное доменное имя, несколько ветвей или собственные зависимости, обычный именованный метод либо класс
часто читается лучше.

## Что уже разобрано

В этом разделе не повторяются:

- устройство интерфейсов, `default`-методы и отличие интерфейса от абстрактного класса — см.
  [«Проектирование типов, nullability и generics»](../week-02/type-design-nullability-and-generics.md);
- изменяемость объектов и различие между `final`-ссылкой и неизменяемым объектом — см.
  [«Объектно-ориентированное программирование»](../week-02/object-oriented-programming.md#14-mutable-и-immutable-objects);
- `Comparator` и способы построения порядка — см.
  [раздел Collections Framework](collections-framework.md#8-comparable-и-comparator).

Stream API будет разобран отдельно. Здесь примеры используют обычные методы и коллекции, чтобы сначала стало понятно,
что представляет собой сама лямбда.

## Что нужно понять

После изучения темы ты должен уметь:

1. объяснить, почему у лямбды нет типа без контекста;
2. распознать функциональный интерфейс и объявить собственный с `@FunctionalInterface`;
3. выбрать между `Predicate`, `Function`, `Consumer`, `Supplier` и `UnaryOperator`;
4. читать и писать краткую и блочную формы лямбд;
5. использовать композицию стандартных функциональных интерфейсов;
6. объяснить требование `final` или `effectively final` для захваченной локальной переменной;
7. отличать изменение захваченной ссылки от изменения объекта по этой ссылке;
8. заменить подходящую лямбду ссылкой на метод и не ухудшить читаемость;
9. понимать ограничения checked-исключений, перегрузок и вывода типов;
10. выбирать между лямбдой, именованным методом и отдельным классом в backend-коде.

---

## 1. Главная модель: лямбде нужен целевой тип

В TypeScript функция имеет тип вида `(account: Account) => boolean`. В Java похожая сигнатура сама по себе не является
типом. Нужен интерфейс, единственный абстрактный метод которого имеет подходящую сигнатуру:

```java
Predicate<Account> isActive = account -> account.status() == AccountStatus.ACTIVE;
```

Компилятор видит тип слева — `Predicate<Account>` — и знает, что требуется реализация метода:

```java
boolean test(Account account);
```

Поэтому параметр `account` имеет тип `Account`, а результат должен быть совместим с `boolean`.

Одна и та же форма лямбды может соответствовать разным интерфейсам:

```java
Predicate<Account> predicate = account -> account.isActive();
Function<Account, Boolean> function = account -> account.isActive();
```

Обе переменные можно вызвать с объектом `Account`, но их типы и имена методов различаются:

```java
boolean first = predicate.test(account);
Boolean second = function.apply(account);
```

`Predicate<Account>` лучше выражает смысл условия. `Function<Account, Boolean>` технически подходит, но хуже сообщает
намерение и использует объект-обёртку `Boolean` в сигнатуре результата.

### Где компилятор берёт целевой тип

Контекстом может быть:

- тип переменной;
- тип параметра метода;
- тип возвращаемого значения;
- явное приведение типа.

```java
Predicate<Account> active = account -> account.isActive();

accounts.removeIf(account -> account.isClosed());

public Predicate<Account> canDebit() {
    return account -> account.isActive() && account.balance().signum() > 0;
}

var positive = (Predicate<BigDecimal>) amount -> amount.signum() > 0;
```

Последний пример показывает важное ограничение: это не компилируется:

```java
// var positive = amount -> amount.signum() > 0;
```

`var` просит компилятор вывести тип из правой части, а лямбда, наоборот, просит получить свой тип из контекста. Без
явного функционального интерфейса возникает замкнутый круг.

---

## 2. Что такое функциональный интерфейс

Функциональный интерфейс — интерфейс с **одним абстрактным методом**. Такой метод также называют SAM: *single abstract
method*. Английское сокращение полезно знать, потому что оно встречается в документации и сообщениях компилятора.

```java
@FunctionalInterface
public interface TransferRule {
    boolean isSatisfiedBy(TransferContext context);
}
```

Теперь интерфейс можно реализовать лямбдой:

```java
TransferRule accountsAreDifferent = context ->
        !context.sourceAccountId().equals(context.targetAccountId());
```

Вызов выполняется через имя абстрактного метода:

```java
if (!accountsAreDifferent.isSatisfiedBy(context)) {
    throw new SameAccountTransferException();
}
```

### Зачем нужна `@FunctionalInterface`

Аннотация не превращает интерфейс в функциональный: подходящий интерфейс остаётся функциональным и без неё. Аннотация
просит компилятор проверить намерение автора.

```java
@FunctionalInterface
public interface TransferRule {
    boolean isSatisfiedBy(TransferContext context);

    // Если добавить второй абстрактный метод, компиляция завершится ошибкой.
    // String errorCode();
}
```

Для собственного контракта, предназначенного для лямбд, аннотацию почти всегда стоит добавлять. Она не даст случайно
сломать всех пользователей интерфейса вторым абстрактным методом.

### Какие методы не нарушают правило одного абстрактного метода

`default`, `static` и `private`-методы могут иметь реализацию и поэтому не увеличивают число абстрактных методов:

```java
@FunctionalInterface
public interface TransferRule {
    boolean isSatisfiedBy(TransferContext context);

    default TransferRule and(TransferRule other) {
        return context -> this.isSatisfiedBy(context) && other.isSatisfiedBy(context);
    }

    static TransferRule alwaysAllow() {
        return context -> true;
    }
}
```

Публичные методы, совпадающие с методами `Object`, также не считаются отдельными абстрактными операциями для определения
функционального интерфейса:

```java
@FunctionalInterface
public interface AccountFormatter {
    String format(Account account);

    boolean equals(Object other);
}
```

Основной функциональный контракт здесь всё равно `format`.

### Наследование тоже учитывается

Компилятор рассматривает весь итоговый интерфейс, включая унаследованные методы:

```java
interface Validator<T> {
    boolean isValid(T value);
}

@FunctionalInterface
interface AccountValidator extends Validator<Account> {
    // Унаследован ровно один абстрактный метод isValid(Account).
}
```

Если после учёта наследования остаются два несовместимых абстрактных метода, интерфейс не является функциональным.

### Когда создавать собственный интерфейс

Стандартного интерфейса достаточно, если сигнатура и смысл очевидны:

```java
Predicate<Account> isActive = Account::isActive;
```

Собственный интерфейс оправдан, когда важен доменный смысл, имя операции, checked-исключение или несколько логически
связанных параметров:

```java
@FunctionalInterface
public interface ExchangeRateProvider {
    BigDecimal rate(Currency source, Currency target, Instant at)
            throws RateUnavailableException;
}
```

Однако не следует создавать `AccountPredicate`, который полностью повторяет `Predicate<Account>` и ничего не добавляет.

---

## 3. Синтаксис лямбда-выражений

Общая форма:

```text
(параметры) -> выражение
(параметры) -> { блок операторов }
```

### Ноль, один и несколько параметров

```java
Supplier<UUID> idGenerator = () -> UUID.randomUUID();

Predicate<BigDecimal> isPositive = amount -> amount.signum() > 0;

BiPredicate<Account, Currency> hasCurrency =
        (account, currency) -> account.currency() == currency;
```

Круглые скобки можно опустить только для одного параметра без явно записанного типа и без аннотации.

```java
Predicate<BigDecimal> first = amount -> amount.signum() > 0;
Predicate<BigDecimal> second = (BigDecimal amount) -> amount.signum() > 0;
```

У нескольких параметров скобки обязательны.

### Выражение и блок

Если справа одно выражение, его значение становится результатом автоматически:

```java
Function<Account, AccountId> getId = account -> account.id();
```

Фигурные скобки создают блок. Для результата нужен явный `return`:

```java
Function<Account, String> describe = account -> {
    String owner = account.clientId().toString();
    return owner + ": " + account.balance();
};
```

Для `void`-совместимого действия `return` не нужен:

```java
Consumer<OutboxEvent> publish = event -> {
    brokerClient.publish(event);
    metrics.incrementPublishedEvents();
};
```

Если тело разрастается, лучше дать поведению имя:

```java
Consumer<OutboxEvent> publish = this::publishAndRecordMetrics;
```

### Явные типы и `var` в параметрах

Обычно типы параметров выводятся из функционального интерфейса. Java также допускает явный тип или `var`:

```java
Predicate<BigDecimal> first = (BigDecimal amount) -> amount.signum() > 0;
Predicate<BigDecimal> second = (var amount) -> amount.signum() > 0;
```

`var` полезен главным образом, когда нужна аннотация параметра:

```java
Predicate<String> nonBlank = (@Deprecated var value) -> !value.isBlank();
```

В одном списке нельзя смешивать стили:

```java
// Нельзя: (var account, Currency currency) -> ...
```

Либо типы выведены у всех параметров, либо у всех записаны явные типы, либо у всех используется `var`.

---

## 4. Основные стандартные функциональные интерфейсы

Большинство повседневных контрактов находится в пакете `java.util.function`.

| Интерфейс             | Абстрактный метод    | Смысл                                                | Пример для Wallet Service               |
|-----------------------|----------------------|------------------------------------------------------|-----------------------------------------|
| `Predicate<T>`        | `boolean test(T)`    | проверить условие                                    | активен ли счёт                         |
| `Function<T, R>`      | `R apply(T)`         | преобразовать `T` в `R`                              | превратить событие в payload            |
| `Consumer<T>`         | `void accept(T)`     | выполнить действие без возвращаемого значения        | записать событие в аудит                |
| `Supplier<T>`         | `T get()`            | предоставить значение без входных параметров         | создать UUID или получить текущее время |
| `UnaryOperator<T>`    | `T apply(T)`         | преобразовать `T` в тот же тип `T`                    | нормализовать описание перевода         |
| `BiPredicate<T, U>`   | `boolean test(T, U)` | проверить условие для двух значений                   | совпадают ли валюты                     |
| `BiFunction<T, U, R>` | `R apply(T, U)`      | преобразовать два значения в результат                | создать ledger entry из счёта и суммы   |
| `BiConsumer<T, U>`    | `void accept(T, U)`  | выполнить действие с двумя значениями                 | добавить пару «ключ — значение»         |
| `BinaryOperator<T>`   | `T apply(T, T)`      | объединить два `T` в один `T`                         | сложить две денежные суммы               |

Первые пять интерфейсов — обязательный минимум этой темы. Варианты с префиксом `Bi` и `BinaryOperator` полезно уметь
узнавать при чтении кода.

### `Predicate<T>`: условие

```java
Predicate<Account> isActive = account -> account.status() == AccountStatus.ACTIVE;
Predicate<Account> hasFunds = account -> account.balance().signum() > 0;

if (isActive.test(account)) {
    // Счёт активен.
}
```

Предикаты можно объединять. `and` и `or` вычисляются с коротким замыканием, как `&&` и `||`:

```java
Predicate<Account> canDebit = isActive.and(hasFunds);
Predicate<Account> unavailable = isActive.negate();
```

В `isActive.and(hasFunds)` второй предикат не вызывается, если первый вернул `false`.

### `Function<T, R>`: преобразование

```java
Function<Account, AccountId> toId = Account::id;
Function<AccountId, String> toText = AccountId::toString;

Function<Account, String> accountIdAsText = toId.andThen(toText);
```

Порядок композиции:

```java
f.andThen(g) // сначала f, затем g: g(f(value))
f.compose(g) // сначала g, затем f: f(g(value))
```

Это частый источник ошибок, поэтому для сложной цепочки полезны говорящие имена промежуточных функций.

`Function.identity()` возвращает переданное значение без изменений:

```java
Function<Account, Account> sameAccount = Function.identity();
```

### `Consumer<T>`: действие

```java
Consumer<AuditRecord> saveAudit = auditRepository::save;
saveAudit.accept(record);
```

`Consumer` обычно означает побочный эффект: запись, отправку сообщения, изменение внешнего объекта. Два действия можно
выполнить последовательно:

```java
Consumer<AuditRecord> saveAudit = auditRepository::save;
Consumer<AuditRecord> saveAndCount =
        saveAudit.andThen(ignored -> auditMetrics.increment());
```

Если первое действие выбросит исключение, второе не выполнится.

### `Supplier<T>`: отложенное получение значения

```java
Supplier<UUID> idGenerator = UUID::randomUUID;
Supplier<Instant> now = clock::instant;

UUID id = idGenerator.get();
```

`Supplier` не обязательно создаёт новый объект. Он лишь обещает вернуть значение при вызове. Это может быть вычисление,
чтение кэша или обращение к внешней зависимости. Повторные вызовы могут вернуть разные значения:

```java
UUID first = idGenerator.get();
UUID second = idGenerator.get(); // Обычно другой UUID.
```

В тестируемом коде `Supplier<UUID>` иногда удобен как узкая зависимость, но для значимой инфраструктурной роли лучше
именованный интерфейс `IdGenerator`: он яснее выражает назначение.

### `UnaryOperator<T>`: преобразование без смены типа

```java
UnaryOperator<String> normalizeDescription = description ->
        description == null ? "" : description.trim();
```

`UnaryOperator<T>` наследует `Function<T, T>`. Он точнее описывает случай, когда вход и результат одного типа.

### Специализированные интерфейсы для примитивов

Generics не работают с примитивами, поэтому `Predicate<Integer>` принимает `Integer`, а не `int`. Для часто используемых
операций есть специализированные варианты:

```java
IntPredicate positive = value -> value > 0;
IntSupplier attempts = retryCounter::get;
ToLongFunction<String> length = String::length;
```

Они помогают избежать лишних преобразований между примитивом и объектом-обёрткой. В обычном бизнес-коде не нужно
заменять ими все интерфейсы ради предполагаемой оптимизации. Но они особенно уместны в больших вычислениях и primitive
streams.

Для денежных значений специализированного интерфейса нет: `BigDecimal` является объектом, и для него используются
обычные generic-интерфейсы.

---

## 5. Лямбда как реализация поведения

Рассмотрим метод, который требует правило проверки:

```java
public Account requireAccount(AccountId id, Predicate<Account> requirement) {
    Account account = accountRepository.findById(id)
            .orElseThrow(AccountNotFoundException::new);

    if (!requirement.test(account)) {
        throw new AccountNotActiveException(id);
    }

    return account;
}
```

Клиент передаёт поведение:

```java
Account source = requireAccount(sourceId, Account::isActive);
```

Это делает метод гибким, но у примера есть недостаток: для любого неуспешного предиката выбрасывается
`AccountNotActiveException`. Если разные правила требуют разных ошибок, один `Predicate` скрывает важную часть
контракта. Возможные решения:

- оставить отдельные явно названные проверки;
- передать также фабрику исключения;
- создать доменный объект правила, который содержит нужную семантику.

Например:

```java
public Account requireAccount(
        AccountId id,
        Predicate<Account> requirement,
        Function<AccountId, RuntimeException> exceptionFactory
) {
    Account account = accountRepository.findById(id)
            .orElseThrow(AccountNotFoundException::new);

    if (!requirement.test(account)) {
        throw exceptionFactory.apply(id);
    }

    return account;
}
```

Вызов:

```java
Account source = requireAccount(
        sourceId,
        Account::isActive,
        AccountNotActiveException::new
);
```

Такой вариант полезен как учебный пример сразу двух функциональных интерфейсов. В production-коде его следует выбирать
только если он действительно упрощает несколько сценариев, а не создаёт абстракцию ради самой абстракции.

---

## 6. Захват переменных и `effectively final`

Лямбда может использовать значения, объявленные во внешней области видимости:

```java
Currency requiredCurrency = Currency.RUB;

Predicate<Account> hasRequiredCurrency =
        account -> account.currency() == requiredCurrency;
```

Говорят, что лямбда **захватила** локальную переменную `requiredCurrency`.

### Что означает `effectively final`

Локальная переменная может быть захвачена, если она:

- явно объявлена `final`; или
- не объявлена `final`, но после присваивания ни разу не изменяется — то есть является *effectively final*.

Предыдущий пример компилируется, хотя ключевого слова `final` нет. Этот не компилируется:

```java
Currency requiredCurrency = Currency.RUB;

Predicate<Account> hasRequiredCurrency =
        account -> account.currency() == requiredCurrency;

// requiredCurrency = Currency.USD;
```

Если раскомментировать последнее присваивание, `requiredCurrency` перестанет быть effectively final, в том числе если
присваивание находится после объявления лямбды.

### Почему существует это ограничение

Локальная переменная живёт в стеке вызова метода. Объект с поведением лямбды может использоваться после завершения этого
метода. Поэтому лямбда фактически сохраняет значение захваченной локальной переменной, а не общую изменяемую ячейку
стека.

Требование effectively final не даёт создать иллюзию, что последующее присваивание локальной переменной автоматически
увидит уже созданная лямбда. Оно также убирает часть неочевидных ошибок при выполнении кода в другом потоке. Но само по
себе это требование **не делает захваченный объект неизменяемым или потокобезопасным**.

### Ссылка неизменна, объект может изменяться

```java
List<AccountId> selectedIds = new ArrayList<>();

Consumer<Account> remember = account -> selectedIds.add(account.id());
```

Переменной `selectedIds` не присваивается новая ссылка, поэтому она effectively final. Но лямбда изменяет сам список.

Аналогично разрешён массив-обёртка:

```java
int[] count = {0};
Consumer<Account> increment = account -> count[0]++;
```

Это компилируется, но обычно является плохим способом обойти ограничение. Состояние становится менее очевидным, а при
параллельном выполнении появляется гонка данных. Лучше вернуть вычисленный результат, использовать явный объект с
понятной ответственностью либо подходящий потокобезопасный счётчик, если параллелизм действительно нужен.

### Поля объекта захватываются иначе

Ограничение effectively final относится к локальным переменным и параметрам метода. Поле объекта можно изменять:

```java
public final class RetryTracker {
    private int attempts;

    public Runnable incrementAction() {
        return () -> attempts++;
    }
}
```

Но разрешение компилятора не означает безопасность. Если действие вызывается из нескольких потоков, обычный `int` не
обеспечивает атомарность и видимость. Это уже вопрос Java concurrency, а не лямбд.

### Параметры метода тоже должны быть effectively final

```java
public Predicate<Account> hasCurrency(Currency currency) {
    // currency = normalize(currency); // После этого захват был бы запрещён.
    return account -> account.currency() == currency;
}
```

Если нужна нормализация, создай отдельную переменную и больше её не меняй:

```java
public Predicate<Account> hasCurrency(Currency currency) {
    Currency normalizedCurrency = Objects.requireNonNull(currency);
    return account -> account.currency() == normalizedCurrency;
}
```

---

## 7. Область видимости и `this`

Лямбда не создаёт новую область видимости для `this`. Внутри неё `this` означает тот же объект, что и снаружи:

```java
public final class AuditService {
    private final AuditRepository repository;

    public Consumer<AuditRecord> saver() {
        return record -> this.repository.save(record);
    }
}
```

Анонимный класс ведёт себя иначе: его `this` ссылается на экземпляр самого анонимного класса.

```java
Consumer<AuditRecord> saver = new Consumer<>() {
    @Override
    public void accept(AuditRecord record) {
        // this — экземпляр анонимного Consumer, а не AuditService.
        repository.save(record);
    }
};
```

Также внутри лямбды нельзя повторно объявить локальную переменную с именем, уже занятым во внешней области:

```java
Account account = loadAccount();

// Нельзя: параметр лямбды конфликтует с внешней переменной account.
// Predicate<Account> active = account -> account.isActive();
```

Это отличается от тела обычного метода и помогает помнить: лямбда лексически является частью окружающего кода.

---

## 8. Ссылки на методы

Ссылка на метод — сокращённая запись лямбды, которая только вызывает уже существующий метод. Она также нуждается в
целевом функциональном интерфейсе.

### Статический метод

```java
Function<String, AccountId> parse = AccountId::parse;
// То же по смыслу: value -> AccountId.parse(value)
```

### Метод конкретного объекта

```java
Supplier<Instant> now = clock::instant;
// То же по смыслу: () -> clock.instant()
```

Объект `clock` уже известен при создании ссылки.

### Метод произвольного объекта конкретного типа

```java
Predicate<Account> active = Account::isActive;
// То же по смыслу: account -> account.isActive()
```

Первый аргумент функционального интерфейса становится получателем вызова метода.

Если у метода есть собственный аргумент, он идёт следом:

```java
BiPredicate<String, String> startsWith = String::startsWith;

boolean result = startsWith.test("TRANSFER_COMPLETED", "TRANSFER");
```

По смыслу это `(text, prefix) -> text.startsWith(prefix)`.

### Ссылка на конструктор

```java
Supplier<ArrayList<AccountId>> emptyList = ArrayList::new;
Function<String, AccountId> accountId = AccountId::new;
```

Какой конструктор выбирается, определяется сигнатурой функционального интерфейса.

### Ссылка на конструктор массива

```java
IntFunction<AccountId[]> accountIdArray = AccountId[]::new;

AccountId[] ids = accountIdArray.apply(10);
```

### Когда ссылка на метод улучшает код

Хорошо:

```java
Predicate<Account> active = Account::isActive;
Consumer<AuditRecord> save = auditRepository::save;
```

Лямбда иногда яснее, потому что показывает роли аргументов:

```java
BiFunction<Account, BigDecimal, LedgerEntry> entryFactory =
        (account, amount) -> createLedgerEntry(account, amount);
```

Вариант `this::createLedgerEntry` короче, но при чтении без подсказки IDE может быть менее очевидно, какой параметр куда
передаётся. Ссылка на метод — средство выразительности, а не обязательная замена каждой подходящей лямбды.

---

## 9. Checked-исключения

Лямбда может выбрасывать только те checked-исключения, которые допускает абстрактный метод её функционального
интерфейса.

```java
Function<Path, String> readText = path -> Files.readString(path);
```

Этот код не компилируется: `Files.readString` может выбросить `IOException`, а `Function.apply` не объявляет его.

Есть три распространённых решения.

### Обработать исключение внутри

```java
Function<Path, String> readText = path -> {
    try {
        return Files.readString(path);
    } catch (IOException exception) {
        throw new UncheckedIOException("Не удалось прочитать " + path, exception);
    }
};
```

Так исходная причина сохраняется. Преобразование в unchecked-исключение должно соответствовать контракту слоя, а не
делаться автоматически только ради компиляции.

### Объявить собственный функциональный интерфейс

```java
@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Exception> {
    R apply(T value) throws E;
}

ThrowingFunction<Path, String, IOException> readText = Files::readString;
```

Это полезно на контролируемой границе, но собственные `Throwing...`-интерфейсы могут быстро размножиться. Сначала следует
проверить, действительно ли вызывающему коду нужно знать конкретный checked-тип.

### Вынести операцию в именованный метод

```java
private String readTextUnchecked(Path path) {
    try {
        return Files.readString(path);
    } catch (IOException exception) {
        throw new UncheckedIOException("Не удалось прочитать " + path, exception);
    }
}
```

После этого место использования остаётся компактным:

```java
Function<Path, String> readText = this::readTextUnchecked;
```

Не следует применять приёмы, которые незаметно «протаскивают» checked-исключение как unchecked. Такой код разрушает
ожидания вызывающей стороны и усложняет диагностику.

---

## 10. Перегрузки и вывод типов

Поскольку тип лямбды зависит от контекста, перегруженные методы иногда создают неоднозначность.

```java
void register(Predicate<Account> rule) { }
void register(Function<Account, Boolean> rule) { }

// Неоднозначно:
// register(account -> account.isActive());
```

Компилятор не может выбрать перегрузку только по форме лямбды. Можно задать тип явно:

```java
register((Predicate<Account>) account -> account.isActive());
```

Но лучше не проектировать API с перегрузками, которые отличаются только похожими функциональными интерфейсами. Разные
имена методов обычно дают более понятный контракт.

### Тип результата тоже участвует в выборе

Лямбда должна быть совместима и по параметрам, и по результату. Например:

```java
Function<Account, AccountId> id = account -> account.id();
Consumer<Account> printer = account -> System.out.println(account.id());
```

Блочная лямбда, возвращающая значение, должна возвращать совместимое значение по всем путям выполнения:

```java
Function<Account, String> label = account -> {
    if (account.isActive()) {
        return "ACTIVE";
    }
    return "UNAVAILABLE";
};
```

Если во второй ветке нет `return` и выполнение может дойти до конца блока, код не скомпилируется.

### Overload-ловушка со ссылками на методы

Если имя метода перегружено, целевой тип определяет нужную версию:

```java
Function<String, Integer> parseDecimal = Integer::parseInt;
BiFunction<String, Integer, Integer> parseWithRadix = Integer::parseInt;
```

Одна ссылка `Integer::parseInt` соответствует разным перегрузкам благодаря разным типам слева.

---

## 11. Лямбда и анонимный класс — не одно и то же

До появления лямбд функциональный интерфейс часто реализовывали анонимным классом:

```java
Predicate<Account> active = new Predicate<>() {
    @Override
    public boolean test(Account account) {
        return account.isActive();
    }
};
```

Лямбда короче:

```java
Predicate<Account> active = Account::isActive;
```

Но различие не только в синтаксисе:

- `this` в лямбде относится к внешнему объекту, в анонимном классе — к самому анонимному объекту;
- лямбда не вводит новую область видимости для локальных имён;
- у анонимного класса есть собственный фактический класс и можно объявить дополнительные поля и методы;
- способ создания объекта лямбды является деталью JVM, на которую нельзя полагаться.

Не следует сравнивать лямбды через `==`, использовать их фактический класс как часть логики или рассчитывать, что два
одинаково написанных выражения дадут один объект:

```java
Predicate<Account> first = Account::isActive;
Predicate<Account> second = Account::isActive;

// Результат first == second не является полезным контрактом программы.
```

Обычно JVM связывает и создаёт реализацию лямбды через механизм `invokedynamic`. Для прикладного кода важен контракт
функционального интерфейса, а не имя сгенерированного класса или конкретная стратегия выделения объектов.

Лямбды также не следует считать автоматически сериализуемыми. Даже если целевой интерфейс расширяет `Serializable`,
формат и захваченное окружение делают такое решение хрупким. Для сообщений, кэша или БД передавай данные, а поведение
восстанавливай обычным кодом приложения.

---

## 12. Побочные эффекты и чистые функции

Java не требует, чтобы лямбда была чистой. Она может менять состояние, писать в БД, логировать и выбрасывать исключения:

```java
Consumer<Transfer> save = transferRepository::save;
```

Однако функции без наблюдаемых побочных эффектов легче тестировать и комбинировать:

```java
Function<Transfer, OutboxEvent> toEvent = transfer ->
        OutboxEvent.transferCompleted(
                transfer.id(),
                transfer.completedAt()
        );
```

Полезная практическая граница:

- `Predicate` и `Function` обычно ожидаются как вычисления без скрытого изменения состояния;
- `Consumer` явно сообщает, что главное — действие;
- `Supplier` может скрывать I/O, поэтому имя переменной или собственного интерфейса должно раскрывать стоимость и смысл.

Это не правило компилятора. Например, `Predicate<Account>` технически может одновременно записать аудит, но такой
предикат удивляет: повторная проверка условия внезапно создаёт вторую запись.

```java
Predicate<Account> surprising = account -> {
    auditRepository.save(AuditRecord.checked(account.id()));
    return account.isActive();
};
```

Проверку и действие лучше разделить явно.

---

## 13. Сопоставление с TypeScript и JavaScript

| Java                                           | Приблизительная аналогия в TypeScript                                  |
|------------------------------------------------|------------------------------------------------------------------------|
| `Predicate<Account>`                           | `(account: Account) => boolean`                                        |
| `Function<Account, AccountDto>`                | `(account: Account) => AccountDto`                                     |
| `Consumer<OutboxEvent>`                        | `(event: OutboxEvent) => void`                                         |
| `Supplier<Instant>`                            | `() => Instant`                                                        |
| `UnaryOperator<String>`                        | `(value: string) => string`                                            |
| `account -> account.isActive()`                | `(account) => account.isActive()`                                      |
| `Account::isActive`                            | `(account) => account.isActive()`                                      |
| `clock::instant`                               | `() => clock.instant()`                                                |
| `AccountId::new`                               | `(value) => new AccountId(value)`                                      |
| захваченная локальная переменная               | переменная из лексического окружения функции                           |
| требование `effectively final`                 | прямого аналога нет: JS-замыкание может менять внешнюю переменную       |

### Главное различие типов

TypeScript использует структурную типизацию: функции с совместимыми параметрами и результатом обычно совместимы по
форме. Java требует конкретный номинальный тип интерфейса. Два интерфейса с одинаковым методом остаются разными типами:

```java
@FunctionalInterface
interface AccountRule {
    boolean test(Account account);
}

@FunctionalInterface
interface TransferSourceRule {
    boolean test(Account account);
}

AccountRule first = Account::isActive;
TransferSourceRule second = Account::isActive;

// Нельзя: second = first;
```

Одну и ту же лямбду можно отдельно адаптировать к обоим интерфейсам, но готовый объект одного интерфейсного типа не
становится реализацией другого.

### Захват переменных отличается

В JavaScript замыкание захватывает доступ к переменной, поэтому увидит её новое значение:

```typescript
let currency = "RUB";
const getCurrency = () => currency;
currency = "USD";
console.log(getCurrency()); // USD
```

Java запрещает аналогичное переприсваивание локальной переменной, захваченной лямбдой. При этом обе платформы позволяют
менять содержимое захваченного объекта, если сам объект изменяемый.

### `this` похож на стрелочную функцию

По поведению `this` Java-лямбда ближе к стрелочной функции TypeScript/JavaScript: она использует `this` внешнего
контекста. Анонимный класс Java, напротив, создаёт собственный `this`.

---

## 14. Лямбды в типичном Java backend

### Отложенное создание исключения

`Optional.orElseThrow` принимает `Supplier` и создаёт исключение только при отсутствии значения:

```java
Account account = accountRepository.findById(accountId)
        .orElseThrow(() -> new AccountNotFoundException(accountId));
```

Если конструктору не нужны аргументы, подходит ссылка:

```java
Account account = optionalAccount.orElseThrow(AccountNotFoundException::new);
```

### Обновление значения в `Map`

Методы коллекций принимают функциональные интерфейсы:

```java
Map<AccountId, List<LedgerEntry>> entriesByAccount = new HashMap<>();

entriesByAccount
        .computeIfAbsent(entry.accountId(), ignored -> new ArrayList<>())
        .add(entry);
```

Функция вызывается только если ключ ещё не связан со значением. Для конструктора без параметров нельзя написать здесь
`ArrayList::new`: `computeIfAbsent` передаёт в функцию ключ, а нужный конструктор списка не принимает `AccountId`.

### Конфигурация повторного действия

```java
public <T> T retry(int maxAttempts, Supplier<T> action) {
    RuntimeException lastFailure = null;

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
        try {
            return action.get();
        } catch (TemporaryConflictException exception) {
            lastFailure = exception;
        }
    }

    throw lastFailure;
}
```

Вызов:

```java
Transfer transfer = retry(3, () -> transferService.transfer(command));
```

Для реального денежного перевода такого общего метода недостаточно: требования проекта разрешают повтор только для
заранее определённых временных конфликтов, а идемпотентность и границы транзакции должны быть явно проверены. Пример
показывает передачу действия, но не является готовой политикой повторов.

### Тестовые зависимости

```java
UUID fixedId = UUID.fromString("11111111-1111-1111-1111-111111111111");
Supplier<UUID> idGenerator = () -> fixedId;
```

Так можно получить детерминированное значение в unit-тесте. Для времени в Java обычно лучше передавать `Clock`, потому
что это стандартная именованная абстракция с готовыми реализациями `Clock.fixed` и `Clock.systemUTC`.

---

## 15. Когда лямбда не подходит

Лямбда удобна, если поведение:

- короткое и понятно на месте использования;
- имеет очевидные вход и результат;
- не требует сложного состояния;
- используется как параметр конкретной операции.

Предпочти именованный метод, если:

- выражение не помещается в одну понятную мысль;
- поведение повторяется;
- важен хороший stack trace и удобная точка останова;
- нужна отдельная документация или unit-тест именно этого алгоритма.

Предпочти отдельный класс, если:

- поведение имеет зависимости и жизненный цикл;
- существует несколько взаимосвязанных операций;
- нужны состояние, конфигурация или разные реализации через DI;
- это значимая доменная стратегия.

Например, политика права клиента инициировать перевод может начинаться с простого `Predicate<TransferContext>`. Но когда
в неё входят роль, принадлежность кошелька, статус клиента и аудит отказа, именованный `TransferAuthorizationPolicy`
обычно лучше выражает архитектуру и проще тестируется.

---

## 16. Частые ошибки

### Ошибка 1. Использовать `Function<T, Boolean>` вместо `Predicate<T>`

`Predicate<T>` точнее выражает условие и предоставляет `and`, `or`, `negate`.

### Ошибка 2. Считать лямбду самостоятельным типом

Запись `var rule = account -> account.isActive()` невозможна без целевого функционального интерфейса.

### Ошибка 3. Добавить второй абстрактный метод

После этого интерфейс перестаёт быть функциональным. `@FunctionalInterface` обнаружит ошибку сразу.

### Ошибка 4. Обходить effectively final через изменяемый массив

`int[] count = {0}` компилируется, но скрывает изменение состояния и не обеспечивает потокобезопасность.

### Ошибка 5. Считать захваченный объект неизменяемым

Неизменность ссылки не запрещает `list.add(...)` и не защищает список от гонок данных.

### Ошибка 6. Помещать неожиданный побочный эффект в `Predicate`

Проверка условия может вызываться несколько раз. Запись в БД или отправка события внутри неё удивляет и затрудняет
тестирование.

### Ошибка 7. Путать `compose` и `andThen`

Проверяй порядок: `f.andThen(g)` означает `g(f(value))`.

### Ошибка 8. Игнорировать checked-исключение

`Function.apply` не разрешает `IOException`. Исключение нужно обработать, осмысленно преобразовать или отразить в
собственном контракте.

### Ошибка 9. Создавать неоднозначные перегрузки

Перегрузки с `Predicate<T>` и `Function<T, Boolean>` затрудняют вывод типа лямбды. Разные имена методов яснее.

### Ошибка 10. Делать длинную бизнес-операцию одной лямбдой

Перевод денег с несколькими инвариантами, сохранением ledger и событием требует явных шагов и доменных имён, а не
многострочного `Consumer`.

### Ошибка 11. Считать ссылку на метод всегда более читаемой

Если она скрывает порядок аргументов или указывает на перегруженный метод, обычная лямбда может быть понятнее.

### Ошибка 12. Полагаться на идентичность или класс реализации лямбды

Контрактом является функциональный интерфейс. Реализация, повторное использование экземпляра и имя класса — детали JVM.

---

## 17. Практика для Wallet Service

Задания рассчитаны на доменный MVP без Spring и базы данных. Stream API здесь использовать не нужно: цель — научиться
работать с функциональными интерфейсами отдельно от потоковой обработки коллекций.

### Задание 1. Правила доступности счёта

Создай:

```java
Predicate<Account> isActive;
Predicate<Account> hasRubCurrency;
Predicate<Account> hasPositiveBalance;
```

Собери `canDebit` через композицию. Проверь:

- активный рублёвый счёт с положительным балансом;
- заблокированный счёт;
- счёт с нулевым балансом;
- короткое замыкание композиции с помощью небольшого тестового счётчика вызовов.

Важно: итоговое правило не заменяет проверку конкретной суммы списания и не является полной реализацией перевода.

### Задание 2. Валидатор команды перевода

Объяви собственный интерфейс:

```java
@FunctionalInterface
public interface TransferValidator {
    void validate(TransferContext context);
}
```

Реализуй отдельными лямбдами проверки требований проекта:

- исходный и целевой кошельки различаются;
- оба кошелька активны;
- валюты совпадают;
- сумма больше нуля и имеет не более двух знаков после запятой;
- клиент не заблокирован.

Добавь `default`-метод `andThen`, который возвращает составной валидатор. Убедись, что выполнение прекращается на первой
ошибке и сохраняется конкретный тип доменного исключения.

### Задание 3. Фабрика ledger entries

Создай `BiFunction<Account, BigDecimal, LedgerEntry>` для подготовки записи ledger. Затем сравни её с собственным
интерфейсом:

```java
@FunctionalInterface
public interface LedgerEntryFactory {
    LedgerEntry create(Account account, BigDecimal signedAmount);
}
```

Ответь письменно:

- какой вариант лучше сообщает доменный смысл;
- где должны появляться `operationId`, `createdAt` и `balanceAfter`;
- не стало ли два аргумента недостаточно для корректного контракта.

Если данных недостаточно, замени набор параметров отдельным `LedgerEntryDraft` и объясни решение.

### Задание 4. Генератор идентификаторов и время

Сделай небольшой сервис создания `Transfer`, принимающий:

```java
Supplier<UUID> idGenerator;
Clock clock;
```

В production-конфигурации используй `UUID::randomUUID` и `Clock.systemUTC()`. В unit-тесте передай фиксированные UUID и
`Clock.fixed(...)`. Докажи тестом, что `id`, `createdAt` и `completedAt` детерминированы.

### Задание 5. Преобразование события

Реализуй:

```java
Function<TransferCompleted, OutboxEvent> toOutboxEvent;
```

Затем вынеси тело в именованный метод и используй ссылку на него. Сравни две записи по:

- видимости входа и результата;
- удобству точки останова;
- понятности формирования payload;
- сохранению запрета на персональные данные и секреты в событии.

### Задание 6. Захват переменной

Напиши метод, возвращающий `Predicate<LedgerEntry>` для заданного `AccountId`. Затем:

1. попробуй переприсвоить параметр после объявления лямбды и изучи ошибку компилятора;
2. исправь код через отдельную нормализованную переменную;
3. захвати изменяемый список и добавляй в него идентификаторы;
4. объясни, почему последний вариант разрешён, но опасен при параллельном выполнении.

### Задание 7. Checked-исключение при экспорте

Попробуй присвоить `Files::readString` переменной типа `Function<Path, String>`. Зафиксируй ошибку компиляции. Реализуй
два решения:

- преобразование `IOException` в `UncheckedIOException` с сохранением причины;
- собственный `ThrowingFunction<Path, String, IOException>`.

Сравни, кто и где обязан обрабатывать ошибку в каждом варианте.

### Задание 8. Рефакторинг без самоцели

Возьми проверку перевода из доменного MVP и сделай две версии:

- явная последовательность именованных методов;
- композиция функциональных интерфейсов.

Оцени не количество строк, а:

- понятно ли, какое правило нарушено;
- удобно ли поставить breakpoint;
- сохраняется ли тип доменного исключения;
- очевиден ли порядок проверок;
- проще ли добавить новое правило.

Оставь в проекте более читаемый вариант и запиши причину выбора коротким комментарием в учебном README или заметке.

---

## 18. Вопросы для самопроверки

1. Почему выражение `account -> account.isActive()` нельзя присвоить `var` без приведения типа?
2. Как компилятор определяет тип параметра лямбды?
3. Что делает интерфейс функциональным?
4. Учитываются ли `default`, `static` и методы `Object` как дополнительные абстрактные методы?
5. Зачем ставить `@FunctionalInterface`, если лямбда работает и без неё?
6. Чем `Predicate<Account>` лучше `Function<Account, Boolean>` для проверки условия?
7. В каком порядке выполняются `function.andThen(other)` и `function.compose(other)`?
8. Что произойдёт со вторым предикатом в `first.and(second)`, если первый вернул `false`?
9. Что означает `effectively final`?
10. Почему можно изменить список, захваченный лямбдой, но нельзя присвоить локальной переменной новый список?
11. Делает ли effectively final захваченное состояние потокобезопасным?
12. На какой объект указывает `this` внутри лямбды и внутри анонимного класса?
13. Чем `clock::instant` отличается по форме от `Clock::instant`?
14. Как первый параметр используется в ссылке `String::startsWith`?
15. Почему `Files::readString` нельзя напрямую использовать как `Function<Path, String>`?
16. Почему перегрузки `register(Predicate<T>)` и `register(Function<T, Boolean>)` неудобны для лямбд?
17. Можно ли полагаться на `==` или фактический класс двух лямбд?
18. Когда собственный функциональный интерфейс лучше стандартного?
19. Почему побочный эффект внутри `Predicate` может привести к дефекту?
20. Когда именованный класс или метод лучше лямбды?

## Краткий итог

- Лямбда в Java получает тип только из контекста функционального интерфейса.
- Функциональный интерфейс имеет один итоговый абстрактный метод; `@FunctionalInterface` фиксирует это намерение.
- `Predicate`, `Function`, `Consumer`, `Supplier` и `UnaryOperator` покрывают большинство простых сценариев.
- Стандартные интерфейсы предоставляют композицию: `and`, `or`, `negate`, `compose`, `andThen`.
- Локальная переменная, захваченная лямбдой, должна быть `final` или effectively final.
- Неизменность захваченной ссылки не означает неизменность объекта и не обеспечивает потокобезопасность.
- Ссылка на метод — компактная форма передачи существующего метода, если она сохраняет ясность.
- Checked-исключения ограничены контрактом абстрактного метода функционального интерфейса.
- Лямбда подходит для короткого локального поведения; значимая бизнес-логика часто заслуживает собственного имени и
  типа.

## Официальные материалы

- [Java Language Specification: Lambda Expressions](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27)
- [Java Language Specification: Functional Interfaces](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.8)
- [Package `java.util.function`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html)
- [Dev.java: Lambda Expressions](https://dev.java/learn/writing-lambda-expressions/)
- [Dev.java: Using Lambda Expressions](https://dev.java/learn/lambdas/)
