# Надёжные границы Java-кода: исключения, ресурсы, время, метаданные и защитные копии

[← Вернуться к учебному плану](../java-backend-learning-plan.md)

Обычный backend-код редко ломается в счастливом сценарии. Сложность возникает на границах: пришёл повреждённый CSV,
файл нельзя прочитать, кошелёк не соответствует бизнес-правилу, внешний сервис недоступен, время операции нужно
правильно сохранить и показать, а изменяемый объект не должен незаметно разрушить инвариант.

Этот раздел объединяет темы недели в один вопрос: **как сделать границу модуля понятной, безопасной и диагностируемой**.
Исключение сообщает, что нормальный путь невозможно продолжить. `try-with-resources` освобождает внешний ресурс даже
при этом исключении. `java.time` не даёт потерять смысл времени. Аннотации и reflection позволяют фреймворкам увидеть
метаданные кода. Defensive copy не выпускает изменяемое внутреннее состояние наружу.

## Что уже разобрано

Здесь не повторяются:

- `null`, `Optional`, immutable value objects и `record` — в
  [проектировании типов](../week-02/type-design-nullability-and-generics.md);
- неизменяемые снимки `List.copyOf` и отличие от глубокой неизменяемости — в
  [Collections Framework](../week-03/collections-framework.md).

Теперь эти идеи применяются к ошибкам, I/O и публичным контрактам модулей.

## Что нужно уметь после раздела

1. Отличать ожидаемую альтернативу результата от ошибки и выбирать `Optional`, результатный тип или исключение.
2. Читать цепочку причин (`cause`), не терять её при переводе технической ошибки в понятную ошибку слоя.
3. Закрывать файлы, потоки и JDBC-ресурсы через `try-with-resources`.
4. Моделировать ошибки Wallet Service без `catch (Exception)` и без утечки SQL-деталей наружу.
5. Выбирать `Instant`, `LocalDate`, `LocalDateTime`, `OffsetDateTime`, `Duration` и `Clock` по смыслу данных.
6. Понимать, как Spring и библиотеки используют аннотации и reflection, не превращая доменную логику в ручной reflection.
7. Защищать поля типа массива, `Date` и mutable-объектов защитными копиями.

---

## 1. Сначала определить: это результат или ошибка?

Не каждое отсутствие данных — исключение. У репозитория отсутствие кошелька при поиске может быть нормальным
результатом:

```java
Optional<Account> findById(AccountId id);
```

Но команда `withdraw` не может успешно завершиться, если средств недостаточно. Это нарушение условия операции, поэтому
её естественно выразить исключением:

```java
public void withdraw(Money amount) {
    if (balance.isLessThan(amount)) {
        throw new InsufficientFundsException(id, balance, amount);
    }

    balance = balance.subtract(amount);
}
```

Полезная эвристика:

| Ситуация | Как обычно выразить |
|---|---|
| Допустимы и «найдено», и «не найдено» | `Optional<T>` или пустая коллекция |
| Есть несколько ожидаемых исходов, вызывающий обязан выбрать ветку | закрытый result type, например sealed hierarchy |
| Метод не способен выполнить обещанный контракт | исключение |
| Некорректное значение передано в локальный API программистом | чаще `IllegalArgumentException` |
| Сбой инфраструктуры: сеть, файл, БД | техническое исключение, при необходимости переведённое на границе слоя |

Не применяй исключение для обычного управления потоком: «не найдено» не должно приводить к `try/catch` на каждом
вызове. Но и не возвращай `null`: он отложит ошибку до случайного `NullPointerException` далеко от причины.

## 2. Иерархия исключений и checked / unchecked

Все ошибочные объекты наследуют `Throwable`. Обычно прикладной код имеет дело с двумя ветками:

```text
Throwable
├── Error                 // ошибка JVM или среды; обычно не перехватывается приложением
└── Exception
    ├── RuntimeException  // unchecked
    └── IOException       // checked; один из примеров
```

`Error` (`OutOfMemoryError`, `StackOverflowError`) означает, что среда может быть в ненадёжном состоянии. Не пытайся
превратить его в обычный HTTP-ответ общим `catch (Throwable)`.

Главное различие **не в важности, неожиданности или возможности восстановления**. Оно в том, что проверяет compiler
у вызывающего кода:

| Вид | Базовый класс | Что потребует compiler от вызывающего кода | Примеры |
|---|---|---|---|
| Checked exception | `Exception`, но не `RuntimeException` | `catch` **или** объявить `throws` | `IOException`, `SQLException` |
| Unchecked exception | `RuntimeException` и его потомки | Ничего; `catch` и `throws` возможны, но необязательны | `IllegalArgumentException`, `NullPointerException`, `InsufficientFundsException` |

### Checked exceptions

Checked exception должен быть либо обработан, либо объявлен в `throws`:

```java
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

String readCsv(Path path) throws IOException {
    return Files.readString(path);
}
```

Если убрать `throws IOException`, этот метод **не скомпилируется**: `Files.readString` объявляет checked `IOException`.
Есть второй вариант — обработать исключение именно там, где код знает, что делать:

```java
String readOptionalCsv(Path path) {
    try {
        return Files.readString(path);
    } catch (IOException exception) {
        return ""; // допустимо лишь если пустой текст — осмысленное решение контракта
    }
}
```

То есть checked exception становится частью контракта метода: увидев `throws IOException`, вызывающий разработчик
обязан принять решение. Для файла это нередко разумно: можно выбрать другой путь, показать понятную ошибку или
выполнить ограниченный повтор. Но checked exception в глубокой domain-цепочке часто создаёт шум: каждый метод вынужден
только прокидывать его дальше.

### Unchecked exceptions

`RuntimeException` не требуется указывать в `throws`. Она подходит для нарушения инварианта, ошибки аргумента или
ошибки, которую нельзя разумно исправить в точке каждого промежуточного вызова.

```java
int percentage(int part, int total) {
    if (total <= 0) {
        throw new IllegalArgumentException("total must be positive");
    }

    return part * 100 / total;
}

int value = percentage(25, 0); // код компилируется, затем выбросится IllegalArgumentException
```

У метода нет ни `throws IllegalArgumentException`, ни обязательного `try/catch`. В отличие от `IOException`, compiler
не заставляет обработать вызов. `throws IllegalArgumentException` можно написать для документации, но это не меняет
проверку compiler.

Отдельный unchecked тип полезен, когда вызывающей границе нужно отличить бизнес-отказ от технической ошибки:

```java
public final class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(AccountId accountId, Money balance, Money requested) {
        super("Insufficient funds for account " + accountId
                + ": balance=" + balance + ", requested=" + requested);
    }
}

void withdraw(Account account, Money amount) {
    if (account.balance().isLessThan(amount)) {
        throw new InsufficientFundsException(account.id(), account.balance(), amount);
    }
}
```

Для Wallet Service domain-исключения обычно будут unchecked: их перехватит HTTP-граница и сопоставит со стабильным
кодом `INSUFFICIENT_FUNDS` и статусом `409`, а не каждый метод между `Account` и controller. Пользователь может
пополнить счёт и повторить запрос, но это не делает исключение checked: checked/unchecked — решение о принуждении со
стороны compiler, а не классификация ошибки по смыслу.

### Одно и то же действие рядом

```java
Path path = Path.of("operations.csv");

// Checked: следующая строка не скомпилируется без catch или throws IOException.
String csv = Files.readString(path);

// Unchecked: следующая строка компилируется без catch и throws.
int result = percentage(25, 0);
```

В обоих случаях во время выполнения возможна ошибка. Разница лишь в том, что для checked-варианта Java заранее
заставляет вызывающего явно отразить этот риск в коде.

### Не строить «зоопарк» классов

Иерархия нужна, когда у вариантов есть общее действие. Например, HTTP-слой может отобразить все ожидаемые отказы
домена, но не инфраструктурные сбои:

```java
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}

public final class AccountNotActiveException extends DomainException { /* ... */ }
public final class InsufficientFundsException extends DomainException { /* ... */ }
```

Не создавай класс на каждую вариацию текста. Если способ обработки одинаков и полезных структурированных данных нет,
достаточно одного типа с понятным сообщением или result type. И наоборот, не проверяй текст `exception.getMessage()`:
текст — для человека, машинный код должен жить в явной модели ошибки API.

## 3. Причина ошибки, stack trace и перевод между слоями

Stack trace отвечает на вопрос «каким путём программа пришла к ошибке». Он формируется при создании исключения и
включает класс, метод и строку. В production-логах он нужен вместе с correlation ID, но клиенту Wallet Service нельзя
возвращать stack trace, SQL и пути файлов: это и небезопасно, и нестабильно.

Иногда граница слоя должна заменить техническую деталь понятным исключением. Обязательно передай исходную причину
в `cause`:

```java
public List<ImportedOperation> importFile(Path path) {
    try {
        return readOperations(path);
    } catch (IOException exception) {
        throw new OperationImportException("Cannot read operations file: " + path, exception);
    }
}
```

Без второго аргумента диагностика потеряет исходный `IOException` и место его возникновения. Полезная цепочка будет
такой: `OperationImportException` → `IOException` → конкретная ошибка файловой системы.

Перехватывай исключение только если есть одна из причин:

- восстановить работу: повторить ограниченное число раз только временный сбой;
- добавить осмысленный контекст и сохранить `cause`;
- освободить ресурс — предпочтительнее доверить это `try-with-resources`;
- преобразовать ошибку на границе слоя, например domain exception в HTTP problem response.

Иначе исключение лучше не ловить: оно поднимется до места, которое действительно умеет принять решение. Вот почему
`catch (Exception)` почти всегда слишком широк: он смешивает validation, бизнес-отказы, ошибки программирования и
сбой инфраструктуры. Нередко такой блок ещё и скрывает проблему:

```java
try {
    processTransfer(request);
} catch (Exception exception) {
    return false; // причина и различие сценариев потеряны
}
```

Исключение: это допустимо в самом верхнем техническом контуре, например в boundary фонового worker-а, который должен
залогировать ошибку, отметить задачу неуспешной и продолжить получать следующие сообщения. Даже там нужны явные
правила логирования, retry и метрик.

## 4. Ресурсы и `try-with-resources`

Файл, сокет, JDBC connection, statement, result set и поток ввода используют ресурс вне JVM или ограниченный пул. Сборщик
мусора освобождает память, но не заменяет своевременный `close()` для таких объектов.

Если ресурс реализует `AutoCloseable`, используй `try-with-resources`:

```java
try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
    return reader.lines()
            .map(this::parseLine)
            .toList();
}
```

После выхода из блока Java вызовет `reader.close()` и при успехе, и при исключении. Если ресурсов несколько, они
закрываются в обратном порядке объявления:

```java
try (Connection connection = dataSource.getConnection();
     PreparedStatement statement = connection.prepareStatement(sql);
     ResultSet result = statement.executeQuery()) {
    // читаем result
}
```

Не закрывай `DataSource`: им владеет контейнер приложения. Закрывают именно взятый `Connection`. Аналогично, код должен
закрывать ресурс, который он сам открыл или получил во владение по контракту; нельзя бездумно закрывать поток,
принадлежащий вызывающему коду.

### Suppressed exceptions

При ошибке в теле `try` и ещё одной ошибке в `close()` первичной остаётся ошибка тела. Ошибка закрытия сохраняется в
`exception.getSuppressed()`. Это важнее ручного `finally`, где легко случайно заменить исходную ошибку ошибкой `close`.

Поток `reader.lines()` нельзя возвращать из блока: чтение stream произойдёт позже, когда reader уже закрыт. Верни
собранный список либо передай stream в обработчик, который закончит работу внутри блока.

## 5. CSV-импорт: частичный успех — это контракт, а не случайный `catch`

CSV — внешняя граница: файл может не открыться, строка может быть синтаксически неверной, а корректный синтаксис может
нарушать правило Wallet Service. Эти случаи требуют разных решений.

```java
public record ImportIssue(int lineNumber, String message) { }

public record ImportReport(
        List<ImportedOperation> operations,
        List<ImportIssue> issues
) {
    public ImportReport {
        operations = List.copyOf(operations);
        issues = List.copyOf(issues);
    }
}
```

Читатель файла может выбросить `OperationImportException` с `IOException` в качестве `cause`: весь импорт продолжить
невозможно. Ошибку одной строки лучше вернуть в `ImportReport` и продолжить остальные строки. Например, строка с
`amount=-10.00` может дать issue с номером строки и нейтральным описанием правила. Это не исключение, если контракт
импорта обещает частичный успех.

Парсер должен отдельно проверять количество колонок, дату, валюту, `BigDecimal` и бизнес-ограничения суммы. Не делай
так:

```java
catch (Exception ignored) {
    // строка потеряна, пользователь не узнает почему
}
```

Собери точную ошибку в отчёт. Если CSV потенциально содержит персональные или платёжные данные, не включай всю исходную
строку в production-логи; используй номер строки, поле и безопасное описание.

## 6. Время: выбирать тип по смыслу, а не по удобству

`java.time` является современным API времени. Его основные типы immutable и потокобезопасны. Почти всегда он лучше
старых `java.util.Date` и `Calendar`.

| Что моделируется | Тип | Пример |
|---|---|---|
| Точка на общей временной шкале | `Instant` | `createdAt`, момент проведения перевода |
| Календарный день без времени и часового пояса | `LocalDate` | дата рождения, дата банковского отчёта |
| Время на часах без часового пояса | `LocalTime` | время закрытия операционного дня |
| Локальные дата и время, но без информации о зоне | `LocalDateTime` | ввод пользователем «2026-09-21 10:00» до выбора зоны |
| Время с указанным смещением UTC | `OffsetDateTime` | API-представление `2026-09-21T10:00:00+03:00` |
| Локальное время в зоне с правилами переходов | `ZonedDateTime` | расписание «в 09:00 Europe/Moscow» |
| Продолжительность между моментами | `Duration` | TTL idempotency record: 24 часа |
| Период в календарных единицах | `Period` | один месяц, два дня |

Для `createdAt`, `expiresAt` и `occurredAt` Wallet Service храни в Java `Instant`, а в PostgreSQL — момент времени с
timezone-поддержкой (`timestamptz`). Клиентский ISO-8601 с offset можно разобрать через `OffsetDateTime`, затем
преобразовать в `Instant`. Сумма «суток» и «24 часов» не всегда равна в зоне с переходом на летнее время, поэтому для
технического TTL обычно подходит `Duration`, а для календарного правила — `Period` или вычисление в нужной зоне.

### Время должно быть тестируемым

Вызов `Instant.now()` внутри domain-метода делает тест зависимым от реальных часов. Введи `Clock` как зависимость:

```java
public final class TransferService {
    private final Clock clock;

    public TransferService(Clock clock) {
        this.clock = clock;
    }

    public Transfer create(/* ... */) {
        return Transfer.createdAt(Instant.now(clock));
    }
}
```

В тесте: `Clock.fixed(Instant.parse("2026-09-21T08:00:00Z"), ZoneOffset.UTC)`. Production-конфигурация передаёт
`Clock.systemUTC()`. UTC в storage и логах уменьшает неоднозначность; преобразование в локальную зону обычно происходит
на UI или на специально определённой API-границе.

## 7. Аннотации: метаданные, а не поведение сами по себе

Аннотация — структурированная метка у класса, метода, поля, параметра или другого элемента программы. Например,
`@Override` помогает compiler проверить, что метод действительно переопределяет родительский:

```java
@Override
public String toString() {
    return id.toString();
}
```

Аннотация не исполняет код сама. Её читают compiler, annotation processor или runtime-библиотека. Поэтому важно
`@Retention`:

| Retention | Кто видит | Пример применения |
|---|---|---|
| `SOURCE` | только compiler | подсказки статического анализа |
| `CLASS` | попадает в bytecode, но обычно не доступна через reflection | инструменты при сборке |
| `RUNTIME` | доступна программе во время работы | Spring `@Component`, JUnit `@Test` |

`@Target` ограничивает, где метка допустима, например только на method или type. В прикладном Java-коде особенно часто
встретятся `@Override`, `@Deprecated`, `@SuppressWarnings`, а позднее Spring- и validation-аннотации. Не добавляй
`@SuppressWarnings` широко: сначала устрани причину или сузь подавление до конкретного предупреждения и места.

## 8. Reflection: runtime-исследование типов с ценой сложности

### Что это такое и где находится

Reflection — стандартный механизм Java, с помощью которого программа **во время выполнения** получает описание
классов и может работать с их конструкторами, методами, полями, аннотациями и generic-декларациями. Это не отдельный
фреймворк и не Maven-зависимость.

Основные API входят в Java SE и поставляются вместе с JDK/JRE:

- `java.lang.Class` — объект с описанием загруженного класса; например, `Account.class` имеет тип `Class<Account>`;
- `java.lang.reflect` — `Method`, `Field`, `Constructor`, `Parameter` и типы для generic-метаданных;
- `java.lang.annotation` — чтение аннотаций, если они имеют `RetentionPolicy.RUNTIME`.

JVM хранит и загружает metadata из `.class`-файлов. Reflection API — стандартная библиотечная поверхность Java, через
которую код получает доступ к этой metadata с соблюдением правил доступа JVM. Поэтому точный ответ: **reflection —
часть платформы Java; API находится в стандартной библиотеке и опирается на информацию JVM.** Spring, Jackson и JUnit
не являются reflection, а используют его как один из механизмов своей работы.

Без reflection вызов известен compiler заранее:

```java
account.withdraw(amount);
```

Compiler проверяет наличие метода, тип `amount`, visibility и возвращаемый тип. С reflection имя метода и сам вызов
могут определяться только во время выполнения:

```java
Method method = Account.class.getMethod("withdraw", Money.class);
method.invoke(account, amount);
```

Во втором случае compiler не может проверить строку `"withdraw"` и связь arguments с методом так же строго. Опечатка
проявится при запуске как `NoSuchMethodException`; исключение из `withdraw` будет обёрнуто в
`InvocationTargetException`. Это цена гибкости.

### Какие сущности можно получить

```java
Class<Account> type = Account.class;

Constructor<?>[] constructors = type.getConstructors(); // public constructors
Method[] methods = type.getDeclaredMethods();           // методы, объявленные в Account
Field[] fields = type.getDeclaredFields();              // поля, объявленные в Account
```

`getMethod` и `getMethods` работают с public methods, включая унаследованные. `getDeclaredMethod` и похожие методы
ищут declaration именно в выбранном классе и могут вернуть non-public member. Это **не означает**, что private field
можно безусловно прочитать: обычная проверка доступа сохраняется. Принудительное снятие ограничений через
`setAccessible(true)` может быть запрещено Java modules и почти всегда сигнализирует, что application-коду стоит
пересмотреть дизайн.

Небольшой пример чтения собственной runtime-аннотации:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CsvRecord {
}

boolean markedForImport(Class<?> type) {
    return type.isAnnotationPresent(CsvRecord.class);
}
```

Аннотация здесь только данные. Решение «является ли класс CSV-записью» принимает код, который прочитал аннотацию через
reflection.

### Где reflection применяется в реальных проектах

Обычно reflection находится в framework или библиотеке, а не в доменной логике приложения.

| Задача | Как reflection помогает | Пример |
|---|---|---|
| Внедрение зависимостей | находит классы и constructors, читает `@Component` / `@Autowired`, создаёт и связывает объекты | Spring Application Context |
| Web API | определяет controller methods, путь, параметры и annotations; вызывает нужный method при HTTP-запросе | Spring MVC |
| Сериализация | читает поля, accessors, constructors, `@JsonProperty`; создаёт DTO из JSON | Jackson |
| ORM | видит entity, id и mapping-аннотации; создаёт entity и читает/устанавливает состояние | JPA provider, например Hibernate |
| Тесты | находит test methods и lifecycle-методы, вызывает их, передаёт параметры | JUnit 5 |
| Валидация | читает `@NotNull`, `@Size` и другие ограничения полей или параметров | Jakarta Bean Validation |
| Общие инструменты | генерирует документацию, mapper, CLI-команду или registry из описания класса | OpenAPI, mapper-библиотеки, внутренние платформы |

Например, при запуске Spring Boot component scanning находит классы, затем Spring читает их annotations и constructors,
чтобы построить dependency graph. При HTTP-запросе Spring MVC сопоставляет путь с controller method, собирает arguments
и вызывает method. Реальные детали оптимизированы: framework кэширует найденные metadata, может создать proxy или
использовать `MethodHandle`. Не нужно представлять, что на каждый запрос он заново перебирает все классы приложения.

Важно отделять понятия:

- Spring — фреймворк, который часто использует reflection, но также содержит DI container, AOP, web-инфраструктуру и
  многое другое;
- Hibernate — ORM, который использует reflection для mapping, но ещё строит SQL, управляет persistence context и
  dirty checking;
- аннотация — metadata, а не исполняемое поведение;
- reflection — способ прочитать metadata и динамически обратиться к коду, но не единственный: возможны generated code,
  proxy, `MethodHandle` и явные interfaces.

### Когда писать reflection самому

Самостоятельная reflection оправдана в небольшом инфраструктурном слое, если код должен работать с неизвестными на
момент компиляции классами: универсальный сериализатор, test runner, плагинная система, генератор документации или
конвенциональный mapper. Этот код стоит изолировать, кэшировать найденные metadata и покрывать тестами на неверные
declarations.

Для доменной логики Wallet Service она обычно плохой выбор:

- compiler перестаёт проверять часть вызовов и безопасные переименования;
- ошибки появляются только во время выполнения;
- private-доступ может нарушить инкапсуляцию и модульные границы;
- код сложнее читать, отлаживать и тестировать;
- reflection обычно медленнее прямого вызова, хотя для редкой стартовой настройки это часто несущественно.

Предпочитай обычный interface, явную зависимость, `switch` по sealed hierarchy или registry. Когда необходимо понять
generic type через reflection, помни type erasure: `List<Account>` в большинстве runtime-мест выглядит лишь как
`List`; конкретный тип может сохраниться в declaration поля или метода, но не в обычном объекте списка.

## 9. Defensive copies: защищать контракт на входе и выходе

`final` запрещает переназначить поле, но не делает изменяемый объект неизменяемым. `List.copyOf` создаёт безопасный
снимок структуры списка, но не копирует глубоко его элементы. Для массива или старого `Date` нужна отдельная defensive
copy.

```java
public final class ImportRequest {
    private final byte[] content;

    public ImportRequest(byte[] content) {
        this.content = Arrays.copyOf(
                Objects.requireNonNull(content, "content must not be null"),
                content.length
        );
    }

    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
```

Копия на входе не даёт вызывающему изменить состояние после constructor. Копия на выходе не выдаёт внутренний массив.
Для `Date` используй `new Date(date.getTime())`; для mutable-объекта — явную копию или, лучше, замени тип на immutable
value object. Делать deep copy всего графа обычно дорого и расплывчато: копируй ровно изменяемые части, которые
участвуют в инварианте или не должны переходить во владение клиента.

В `ImportReport` выше `List.copyOf` уместен, потому что `ImportedOperation` и `ImportIssue` должны быть immutable.
Если бы элемент содержал `byte[]`, одной копии списка было бы недостаточно: сам элемент обязан защитить массив.

## 10. Практика для Wallet Service

Выполни импорт без Spring и базы данных. Цель — не реализовать универсальный CSV-парсер, а точно провести границы
ошибок и владения данными.

### Задание 1. Контракт импорта

В пакете `src/com/example/exercises/week_4/relable_boundaries` реализуй
`OperationCsvImporter#importOperations(Path path): ImportReport`. Имя `import` использовать нельзя: это ключевое слово
Java. Полная формулировка, формат CSV, пример файла и критерии готовности находятся в
[README упражнения](../../src/com/example/exercises/week_4/relable_boundaries/README.md).

`ImportReport` должен отдельно хранить успешные `ImportedOperation` и ошибки строк. Каждая успешная операция содержит
неизменяемый идентификатор, `Money`, `Instant occurredAt` и тип операции. Формат строк:
`operationId;occurredAt;amount;currency;type`. Одна повреждённая строка не должна отменять разбор остальных:
результат такой строки — структурированный `ImportIssue` с номером строки и именем поля.

### Задание 2. Разделить сбои

Обработай сценарии:

| Сценарий | Ожидаемый результат |
|---|---|
| файла нет или его нельзя прочитать | `OperationImportException` с исходным `IOException` как `cause` |
| неверное число колонок | `ImportIssue` с номером строки |
| не разбирается дата или сумма | `ImportIssue` с номером строки и названием поля |
| сумма `≤ 0` или более двух знаков после запятой | `ImportIssue`, а не тихое округление |
| одна строка неверна, остальные корректны | корректные строки остаются в отчёте |

### Задание 3. Ресурсы и время

Открой файл только через `try-with-resources`. Передай `Clock` в importer или в фабрику отчёта, добавь в отчёт
`importedAt`. В тесте используй `Clock.fixed`; тест не должен зависеть от текущего времени или timezone машины.

### Задание 4. Негативные тесты

Напиши unit-тесты как минимум для отсутствующего файла, повреждённой даты, отрицательной суммы, слишком точной суммы,
частичного успеха и сохранённого `cause`. Проверяй тип исключения, `getCause()` и структурированные поля issue, а не
точный текст stack trace.

### Задание 5. Defensive copy

Добавь в `ImportedOperation` вложение `byte[] sourceHash` либо отдельный учебный класс. Докажи двумя тестами, что
изменение исходного массива после constructor и изменение массива, полученного через accessor, не меняют объект.

### Задание 6. Минимальная annotation / reflection-практика

Создай `@CsvRecord` с `RUNTIME` retention и `TYPE` target. Напиши маленькую функцию, которая принимает `Class<?>` и
проверяет наличие аннотации. Затем объясни, почему доменное правило «сумма больше нуля» не следует переносить в
reflection: явная проверка в `Money` проще, проверяется compiler и видна при чтении кода.

## 11. Вопросы для самопроверки

1. Почему `Optional` не является заменой исключению при невозможности выполнить команду?
2. В чём разница между checked и unchecked exception? Почему она не равна «ожидаемая и неожиданная ошибка»?
3. Почему `Error` и `Throwable` не нужно перехватывать в обычном прикладном коде?
4. Когда следует ловить исключение, а когда лучше дать ему подняться выше?
5. Почему при новом исключении на границе слоя важно передать `cause`?
6. Что такое suppressed exception и почему `try-with-resources` сохраняет его корректнее ручного `finally`?
7. Кто владеет `Connection`, взятым у `DataSource`, и кто владеет самим `DataSource`?
8. Почему нельзя вернуть `Stream<String>` из `reader.lines()` после закрытия reader?
9. Как отличить ошибку строки CSV от ошибки, из-за которой невозможно импортировать весь файл?
10. Почему для времени создания перевода подходит `Instant`, а для дня отчёта — `LocalDate`?
11. Как `Clock` делает тесты детерминированными?
12. Исполняет ли аннотация поведение сама по себе? Что меняет `RetentionPolicy.RUNTIME`?
13. Почему ручная reflection обычно не годится для доменной логики?
14. Чем defensive copy массива на входе отличается от copy на выходе?
15. Почему `List.copyOf` не защищает от изменения массива внутри элемента списка?
16. Почему `catch (Exception)` почти всегда слишком широк и где может быть оправдан?

## Краткий итог

- Исключение выражает невозможность выполнить контракт, а не нормальную ветку результата.
- Domain-исключение должно содержать полезный контекст; при оборачивании технической ошибки всегда сохраняй `cause`.
- `try-with-resources` — стандартный способ закрывать принадлежащие коду I/O- и JDBC-ресурсы.
- Частично повреждённый CSV лучше выражать отчётом с ошибками строк, а недоступный файл — ошибкой всего импорта.
- Время выбирают по смыслу: для факта операции — `Instant`, для тестов — инъекция `Clock`.
- Аннотации являются метаданными; reflection нужна главным образом инфраструктуре, а не скрытой бизнес-логике.
- Defensive copies защищают владение изменяемыми данными на входе и выходе API.

## Официальные материалы

- [Java Exceptions](https://dev.java/learn/exceptions/)
- [Класс `Throwable`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Throwable.html)
- [Интерфейс `AutoCloseable`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/AutoCloseable.html)
- [Пакет `java.time`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/package-summary.html)
- [Класс `Clock`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Clock.html)
- [Пакет `java.lang.annotation`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/annotation/package-summary.html)
- [Reflection API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/package-summary.html)
- *Effective Java*, главы 9–10; исключения подробно рассматриваются в главе 10, а рекомендации по defensive copies — также в главах о конструкторах и методах.
