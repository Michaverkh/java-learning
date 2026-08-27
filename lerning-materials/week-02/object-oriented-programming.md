# Объектно-ориентированное программирование в Java

[← Вернуться к учебному плану](../java-backend-learning-plan.md)

## Что нужно понять

После изучения темы ты должен уметь:

1. различать класс, объект, ссылку и тип ссылки;
2. объяснять, что происходит при выполнении `new`;
3. проектировать состояние и поведение класса, а не просто складывать данные в поля;
4. использовать конструкторы и проверять инварианты объекта;
5. применять инкапсуляцию, наследование и полиморфизм;
6. выбирать между наследованием и композицией;
7. различать перегрузку и переопределение методов;
8. понимать роль `Object`, `equals`, `hashCode` и `toString`;
9. объяснять отличия объектной модели Java от TypeScript/JavaScript;
10. читать типичный объектно-ориентированный backend-код.

Знакомый синтаксис `class`, `extends`, `private` и `new` может создать впечатление, что Java-классы работают так же, как
TypeScript-классы. На уровне повседневного кода аналогии полезны, но модели типов и выполнения различаются:

- Java — статически типизированный язык с номинативной системой типов;
- каждый объект класса в Java создаётся в runtime JVM;
- Java-класс компилируется в bytecode и сам является runtime-типом;
- TypeScript в основном проверяет структурную совместимость и после компиляции удаляет типы;
- TypeScript-класс одновременно участвует в системе типов и создаёт JavaScript-конструктор;
- Java не использует JavaScript prototype chain как модель наследования объектов.

---

## 1. Класс, объект, экземпляр и ссылка

### Класс

Класс описывает:

- состояние будущих объектов — поля;
- допустимые операции — методы;
- правила создания — конструкторы;
- границы доступа — модификаторы visibility;
- отношения с другими типами — inheritance, interfaces и composition.

```java
import java.math.BigDecimal;

public class Account {
    private final String id;
    private BigDecimal balance;

    public Account(String id, BigDecimal initialBalance) {
        this.id = id;
        this.balance = initialBalance;
    }

    public void deposit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
```

`Account` здесь — тип и шаблон поведения. Он ещё не является конкретным счётом.

### Объект и экземпляр

Объект — созданная во время выполнения сущность со своим состоянием. В этом контексте слова «объект» и «экземпляр»
обычно используются как синонимы.

```java
Account primary = new Account("ACC-1", new BigDecimal("100.00"));
Account reserve = new Account("ACC-2", BigDecimal.ZERO);
```

Созданы два разных объекта `Account`. У каждого собственные `id` и `balance`.

### Ссылка

Переменная reference type обычно хранит не сам объект, а ссылку на него:

```java
Account first = new Account("ACC-1", BigDecimal.ZERO);
Account second = first;

second.deposit(new BigDecimal("50.00"));

System.out.println(first.getBalance()); // 50.00
```

`first` и `second` ссылаются на один объект. Присваивание ссылки не копирует объект.

Упрощённая схема:

```text
stack frame                         heap

first  ───────────────┐
                      ├───────────> Account { id: "ACC-1", balance: 50.00 }
second ───────────────┘
```

Это концептуальная модель, полезная для рассуждения. JVM может оптимизировать размещение и даже устранить отдельную
аллокацию, если наблюдаемое поведение программы не изменится.

### Аналогия с TypeScript

```typescript
const first = new Account("ACC-1", 0);
const second = first;

second.deposit(50);
console.log(first.balance); // 50
```

В обоих языках переменные ссылаются на один mutable object. Но Java не позволяет произвольно добавить объекту новое
поле во время выполнения, а допустимые члены определяются классом.

### `null`

Ссылка может не указывать на объект:

```java
Account account = null;
account.deposit(BigDecimal.TEN); // NullPointerException
```

В Java нет отдельного значения `undefined`. Для reference types исторически допустим `null`, если код, аннотации или
инструменты анализа не задают более строгий контракт.

---

## 2. Что происходит при `new`

Выражение:

```java
Account account = new Account("ACC-1", BigDecimal.ZERO);
```

можно мысленно разделить на этапы:

1. JVM выделяет память для объекта;
2. instance fields получают значения по умолчанию;
3. выполняются field initializers и instance initializer blocks;
4. вызывается конструктор родительского класса;
5. выполняется тело конструктора текущего класса;
6. выражение `new` возвращает ссылку на созданный объект;
7. ссылка присваивается переменной `account`.

Значения полей по умолчанию:

| Тип поля                 | Значение по умолчанию |
|---                       |---                    |
| `byte`, `short`, `int`   | `0`                    |
| `long`                   | `0L`                   |
| `float`                  | `0.0f`                 |
| `double`                 | `0.0d`                 |
| `char`                   | `'\u0000'`             |
| `boolean`                | `false`                |
| любой reference type     | `null`                 |

Локальные переменные автоматического значения не получают:

```java
int count;
System.out.println(count); // compile-time error
```

Компилятор требует явно присвоить локальной переменной значение до чтения.

### Объект не обязан создаваться только через публичный конструктор

Клиентский код обычно использует `new`, но класс может управлять созданием через static factory method:

```java
public final class Money {
    private static final Money ZERO = new Money(BigDecimal.ZERO);

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount;
    }

    public static Money zero() {
        return ZERO;
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }
}
```

```java
Money fee = Money.of("10.50");
Money zero = Money.zero();
```

Преимущества фабрики:

- у метода есть говорящее имя;
- можно вернуть cached instance;
- можно вернуть subtype;
- можно скрыть детали создания;
- не требуется создавать новый объект при каждом вызове.

### Фабрика может вернуть cached instance

`Cached instance` — это уже созданный объект, ссылку на который фабрика сохраняет и возвращает повторно. В примере
выше объект с нулевой суммой создаётся только один раз при инициализации класса:

```java
Money first = Money.zero();
Money second = Money.zero();

System.out.println(first == second); // true: обе переменные ссылаются на один объект
```

Если бы `zero()` каждый раз выполнял `new Money(BigDecimal.ZERO)`, суммы внутри объектов были бы одинаковыми, но это
были бы разные экземпляры. Кэширование позволяет не создавать множество эквивалентных объектов и иногда уменьшает
расход памяти и нагрузку на garbage collector.

Обычно переиспользуют объекты для часто встречающихся значений. Например, `Integer.valueOf(int)` может вернуть
заранее созданный экземпляр для небольших чисел, а `Boolean.valueOf(boolean)` возвращает один из двух экземпляров:
`Boolean.TRUE` или `Boolean.FALSE`.

Кэшировать безопаснее всего immutable-объекты. Если вернуть нескольким клиентам один mutable-объект, изменение через
одну ссылку неожиданно станет видно всем остальным клиентам:

```java
// Money immutable: amount нельзя изменить после создания объекта.
Money zero = Money.zero(); // общий экземпляр безопасно использовать повторно
```

Клиент не должен полагаться на то, что фабрика всегда возвращает тот же объект, если это не является явно
документированной частью контракта. Для сравнения значений value object обычно реализуют и используют `equals`, а
`==` оставляют для проверки идентичности ссылок.

### Фабрика может вернуть subtype

`Subtype` — более конкретный тип, который реализует интерфейс или наследуется от класса. Возвращаемым типом фабрики
может быть общий контракт, а фактический класс объекта фабрика выбирает по входным данным:

```java
interface PaymentProcessor {
    void process(Money amount);
}

final class CardPaymentProcessor implements PaymentProcessor {
    @Override
    public void process(Money amount) {
        System.out.println("Processing card payment");
    }
}

final class FastPaymentProcessor implements PaymentProcessor {
    @Override
    public void process(Money amount) {
        System.out.println("Processing fast payment");
    }
}

public final class PaymentProcessors {
    private PaymentProcessors() {
    }

    public static PaymentProcessor forMethod(String method) {
        return switch (method) {
            case "card" -> new CardPaymentProcessor();
            case "fast" -> new FastPaymentProcessor();
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        };
    }
}
```

Клиент видит только `PaymentProcessor`, но во время выполнения получает один из его subtypes:

```java
PaymentProcessor processor = PaymentProcessors.forMethod("fast");
processor.process(Money.of("10.50"));
```

Обычный конструктор `new CardPaymentProcessor()` всегда явно создаёт объект указанного класса. Фабрика же может:

- скрыть конкретные классы от клиента;
- выбирать реализацию по конфигурации или входным данным;
- позднее заменить реализацию, не меняя клиентский код, пока сохраняется контракт `PaymentProcessor`.

При этом клиент может вызывать только методы, объявленные в `PaymentProcessor`. Делать downcast до конкретной
реализации обычно не следует: это разрушает преимущество общего контракта и связывает клиентский код с деталями
фабрики.

---

## 3. Поля, методы и `this`

### Instance fields

Instance field принадлежит конкретному объекту:

```java
public class Account {
    private BigDecimal balance = BigDecimal.ZERO;
}
```

У каждого экземпляра свой `balance`.

### Instance methods

Instance method вызывается на объекте и может обращаться к его состоянию:

```java
public void deposit(BigDecimal amount) {
    balance = balance.add(amount);
}
```

Внутри метода неявно доступна ссылка `this` на текущий объект:

```java
public void deposit(BigDecimal amount) {
    this.balance = this.balance.add(amount);
}
```

Обе записи эквивалентны, пока имя параметра или локальной переменной не перекрывает имя поля.

### Что именно означает `this` в Java

`this` — ссылка на текущий экземпляр класса: объект, относительно которого сейчас выполняется instance method,
constructor или instance initializer. Например, при вызове:

```java
Account account = new Account("main");
account.deposit(new BigDecimal("100.00"));
```

внутри `deposit` выражение `this` ссылается на тот же объект, что и переменная `account` в вызывающем коде. Упрощённо
вызов можно мысленно читать так: «выполни `Account.deposit` для объекта `account`».

У `this` есть несколько важных свойств:

- ссылка предоставляется Java автоматически, её не объявляют как параметр;
- внутри instance method `this` не может быть `null`: метод сначала должен быть вызван относительно существующего
  объекта;
- присвоить другое значение в `this` нельзя;
- compile-time type выражения `this` — класс, внутри которого оно написано; runtime object при этом может быть его
  subtype;
- в static context текущего экземпляра нет, поэтому использовать `this` там нельзя.

```java
public class Account {
    public static void printCurrent() {
        System.out.println(this); // compile-time error
    }
}
```

`this` можно использовать не только для обращения к полям. Это обычная ссылка на объект, поэтому её можно передать в
другой метод или вернуть:

```java
public final class Account {
    private BigDecimal balance = BigDecimal.ZERO;

    public Account deposit(BigDecimal amount) {
        balance = balance.add(amount);
        return this;
    }

    public void registerIn(AccountRegistry registry) {
        registry.register(this);
    }
}
```

Возврат `this` позволяет строить цепочки вызовов:

```java
account
        .deposit(new BigDecimal("100.00"))
        .deposit(new BigDecimal("25.00"));
```

Такой fluent API удобен не всегда: он уместен, только если повторные вызовы читаются однозначно и ожидаемая mutability
объекта очевидна.

### `this` устраняет неоднозначность

```java
public class Account {
    private final String id;

    public Account(String id) {
        this.id = id;
    }
}
```

- `this.id` — поле объекта;
- `id` — параметр конструктора.

Если конфликта имён нет, квалификатор необязателен:

```java
public BigDecimal getBalance() {
    return balance;      // то же самое, что this.balance
}
```

Обычно `this.` явно пишут при перекрытии имени, а не добавляют механически перед каждым instance member.

### Java `this` и JavaScript `this` — не одна и та же модель

Главное различие: в Java значение `this` однозначно задаётся объектом, относительно которого вызван instance method.
Java-код не может отделить метод от объекта и произвольно подменить его `this`.

```java
Account first = new Account("first");
Account second = new Account("second");

first.printId();  // this == first
second.printId(); // this == second
```

У обычной функции JavaScript `this` в основном определяется **формой вызова**, а не местом объявления функции:

```javascript
"use strict";

const first = {
  id: "first",
  printId() {
    console.log(this.id);
  },
};

const second = { id: "second" };
const print = first.printId;

first.printId();          // this === first
print.call(second);       // this === second
print();                  // this === undefined; обращение к this.id завершится ошибкой
```

То есть одна и та же JS-функция может получить разные значения `this`. Методы Java не являются function values:
сохранить `first.printId` в переменную и затем вызвать его с `this == second` через аналог `call`, `apply` или `bind`
нельзя. Method reference `first::printId` уже привязан к receiver `first` и не меняет его при вызове:

```java
Runnable print = first::printId;
print.run(); // внутри printId this всё ещё ссылается на first
```

Отдельное правило JavaScript действует для arrow function: у неё нет собственного `this`, она захватывает его из
внешнего lexical context. В Java такого разделения на «обычные методы» и «arrow methods» нет. `this` внутри instance
method всегда означает текущий receiver, а внутри lambda expression сохраняет `this` окружающего объекта:

```java
public Runnable idPrinter() {
    return () -> System.out.println(this.id);
}
```

Здесь `this` — не объект лямбды, а экземпляр `Account`, на котором вызвали `idPrinter()`.

| Ситуация                         | Java `this`                                                      | JavaScript `this` у обычной функции                           |
|---                               |---                                                               |---                                                            |
| Откуда берётся значение          | из receiver instance method или создаваемого объекта              | в основном из формы вызова                                    |
| Можно ли подменить явно          | нет аналога `call`, `apply` или `bind`                            | да, через `call`, `apply` или `bind`                           |
| Вызов отдельно от объекта        | метод не является самостоятельной функцией                        | возможен; в strict mode `this` будет `undefined`               |
| `this` в static context          | отсутствует, обращение не компилируется                            | в static method класса обычно указывает на сам constructor     |
| `this` в lambda / arrow function | означает `this` окружающего экземпляра                            | arrow function лексически захватывает внешний `this`           |
| Может ли быть `null`/`undefined` | нет в выполняющемся instance method                               | может быть `undefined`; без strict mode возможен global object |

Не следует переносить в Java привычку «потерять `this` при передаче callback». Для Java method reference вида
`account::printId` receiver уже зафиксирован. Ближайшая практическая проблема в Java другая: передача `this` из
constructor может опубликовать ещё не полностью инициализированный объект — этот случай разобран ниже.

### `static` принадлежит классу

```java
public class Account {
    private static int createdCount;

    public Account() {
        createdCount++;
    }

    public static int getCreatedCount() {
        return createdCount;
    }
}
```

`createdCount` один на класс, а не один на объект:

```java
new Account();
new Account();

System.out.println(Account.getCreatedCount()); // 2
```

Static method не имеет `this` и не может напрямую обратиться к instance field.

| Java-член            | Кому принадлежит                       | Пример обращения           |
|---                   |---                                      |---                         |
| instance field       | конкретному объекту                     | `account.getBalance()`      |
| instance method      | вызывается относительно объекта         | `account.deposit(amount)`   |
| static field         | классу                                  | `Account.DEFAULT_LIMIT`     |
| static method        | классу                                  | `Money.of("10.00")`         |

В TypeScript различие instance/static выглядит похоже. В обоих языках static member не становится членом экземпляра.

---

## 4. Конструкторы

Конструктор создаёт корректное начальное состояние объекта:

```java
public class Account {
    private final String id;
    private BigDecimal balance;

    public Account(String id, BigDecimal initialBalance) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (initialBalance == null || initialBalance.signum() < 0) {
            throw new IllegalArgumentException("initialBalance must be non-negative");
        }

        this.id = id;
        this.balance = initialBalance;
    }
}
```

Конструктор:

- имеет то же имя, что и класс;
- не объявляет return type, даже `void`;
- вызывается при `new`;
- может иметь любую допустимую visibility;
- может быть перегружен;
- не наследуется и не переопределяется.

### Default constructor

Если конструктор не объявлен, компилятор добавляет конструктор без параметров:

```java
public class Account {
}
```

Упрощённо эквивалентно:

```java
public class Account {
    public Account() {
        super();
    }
}
```

Как только объявлен хотя бы один конструктор, compiler-generated no-args constructor исчезает:

```java
public class Account {
    public Account(String id) {
    }
}

new Account(); // compile-time error
```

Это отличается от необязательных параметров TypeScript: Java выбирает одну из явно доступных перегрузок.

### Перегрузка конструкторов и `this(...)`

```java
public class Account {
    private final String id;
    private final BigDecimal balance;

    public Account(String id) {
        this(id, BigDecimal.ZERO);
    }

    public Account(String id, BigDecimal initialBalance) {
        this.id = id;
        this.balance = initialBalance;
    }
}
```

Вызов `this(...)`:

- делегирует другому конструктору того же класса;
- должен быть первой инструкцией;
- помогает держать инициализацию в одном месте.

### `super(...)`

Конструктор наследника вызывает конструктор родителя:

```java
public class PremiumAccount extends Account {
    private final BigDecimal cashbackRate;

    public PremiumAccount(String id, BigDecimal cashbackRate) {
        super(id);
        this.cashbackRate = cashbackRate;
    }
}
```

`super(...)` тоже должен быть первой инструкцией. Нельзя одновременно начать конструктор с `this(...)` и `super(...)`.
Если явного вызова нет, compiler пытается вставить `super()`.

### Не публикуй частично созданный объект

Не следует передавать `this` во внешний код из конструктора:

```java
public Account(EventBus eventBus) {
    eventBus.register(this); // объект может быть ещё не полностью инициализирован
}
```

Внешний код может увидеть объект до завершения его инициализации. Это особенно опасно при concurrency.

### TypeScript-сравнение

```typescript
class Account {
  constructor(
    public readonly id: string,
    private balance: number = 0,
  ) {}
}
```

У Java нет TypeScript parameter properties. Поле и присваивание записываются явно. У Java также нет default parameters;
обычно применяются перегруженные конструкторы, factory methods или builder.

---

## 5. Инкапсуляция

Инкапсуляция — это не просто «сделать поля `private`». Её цель — спрятать представление состояния и разрешить только
операции, сохраняющие правила объекта.

### Слабая инкапсуляция: объект как мешок данных

```java
public class Account {
    public BigDecimal balance;
}
```

Клиент может нарушить правило:

```java
account.balance = new BigDecimal("-1000000");
```

### Формальная, но бесполезная инкапсуляция

```java
public class Account {
    private BigDecimal balance;

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
```

Поле private, но setter всё равно разрешает любое состояние.

### Инкапсуляция через бизнес-операции

```java
public class Account {
    private BigDecimal balance;

    public Account(BigDecimal initialBalance) {
        requireNonNegative(initialBalance);
        this.balance = initialBalance;
    }

    public void deposit(BigDecimal amount) {
        requirePositive(amount);
        balance = balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        requirePositive(amount);

        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("insufficient funds");
        }

        balance = balance.subtract(amount);
    }

    public BigDecimal getBalance() {
        return balance;
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }

    private static void requireNonNegative(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
    }
}
```

Теперь внешний код выражает намерение через `deposit` и `withdraw`, а объект сам защищает инвариант:

```text
balance >= 0
```

### Не возвращай mutable внутреннее состояние

```java
public class Customer {
    private final List<Account> accounts = new ArrayList<>();

    public List<Account> getAccounts() {
        return accounts; // клиент может вызвать clear()
    }
}
```

Безопаснее вернуть read-only view или копию:

```java
public List<Account> getAccounts() {
    return List.copyOf(accounts);
}
```

Это называется defensive copy. В TypeScript `readonly Account[]` запрещает изменение только на уровне type checker и
не создаёт runtime immutable collection.

### Инвариант

Инвариант — условие, которое должно оставаться истинным для любого наблюдаемого корректного состояния объекта.

Примеры:

- баланс не отрицателен;
- валюта суммы не меняется после создания;
- завершённый перевод нельзя выполнить повторно;
- счёт с состоянием `CLOSED` не принимает операции.

Хороший класс:

1. проверяет инвариант в конструкторе;
2. не предоставляет операции, которые позволяют его обойти;
3. повторно проверяет правила при каждом изменении состояния.

---

## 6. Наследование

Наследование выражает отношение «является разновидностью»:

```java
public class SavingsAccount extends Account {
}
```

`SavingsAccount` является `Account`, поэтому ссылке базового типа можно присвоить объект наследника:

```java
Account account = new SavingsAccount();
```

Java поддерживает:

- один непосредственный superclass для класса;
- реализацию нескольких interfaces;
- наследование доступных instance methods;
- переопределение разрешённых instance methods.

Java-класс не может `extends` несколько классов.

### Корень иерархии — `Object`

Если `extends` не указан, класс неявно наследуется от `java.lang.Object`:

```java
public class Account {
}
```

эквивалентно:

```java
public class Account extends Object {
}
```

Поэтому у любого обычного Java-объекта есть методы:

- `equals(Object other)`;
- `hashCode()`;
- `toString()`;
- `getClass()`;
- а также низкоуровневые методы coordination, которые редко следует вызывать напрямую.

### Что не наследуется

- конструкторы;
- private members как доступная часть API наследника;
- static methods в полиморфном смысле;
- package-private members родителя из другого package.

Private-поля физически являются частью состояния объекта-наследника, но код наследника не может обращаться к ним
напрямую.

### `super`

`super` позволяет обратиться к реализации родителя:

```java
@Override
public void withdraw(BigDecimal amount) {
    BigDecimal fee = calculateFee(amount);
    super.withdraw(amount.add(fee));
}
```

### Отношение `is-a` должно быть поведенческим

Одного совпадения полей мало. Наследник должен быть пригоден в любом месте, где ожидается родитель, и сохранять его
контракт.

Если `Account.withdraw` обещает разрешать любое списание в пределах баланса, наследник не должен неожиданно запрещать
все списания по нечётным дням, если это не предусмотрено контрактом базового типа.

Это практическое содержание Liskov Substitution Principle:

- не усиливать предусловия;
- не ослаблять постусловия;
- сохранять инварианты базового типа;
- не менять смысл операции неожиданным образом.

---

## 7. Переопределение и перегрузка

Эти механизмы часто путают, но они решаются в разное время.

### Переопределение — overriding

Наследник предоставляет новую реализацию instance method с совместимой сигнатурой:

```java
public class Account {
    public BigDecimal monthlyFee() {
        return new BigDecimal("100.00");
    }
}

public class PremiumAccount extends Account {
    @Override
    public BigDecimal monthlyFee() {
        return BigDecimal.ZERO;
    }
}
```

Аннотация `@Override` заставляет компилятор проверить намерение. Её следует использовать всегда.

Основные правила:

- имя и параметры должны соответствовать;
- return type может быть более конкретным;
- visibility нельзя сузить;
- checked exception нельзя расширить;
- `final` method переопределить нельзя;
- `static` method не переопределяется, а скрывается.

### Перегрузка — overloading

В одном типе существует несколько методов с одним именем и разными параметрами:

```java
public void deposit(BigDecimal amount) {
}

public void deposit(BigDecimal amount, String operationId) {
}
```

Перегрузка выбирается компилятором по declared types аргументов:

```java
public static void print(Account account) {
    System.out.println("Account");
}

public static void print(PremiumAccount account) {
    System.out.println("PremiumAccount");
}

Account account = new PremiumAccount();
print(account); // Account
```

Runtime object — `PremiumAccount`, но declared type переменной — `Account`. Поэтому compiler выбирает
`print(Account)`.

### Ключевое различие

| Механизм    | Что различается      | Когда выбирается реализация |
|---          |---                   |---                          |
| overloading | список параметров    | compile time                 |
| overriding  | реализация в subtype | runtime                      |

TypeScript позволяет описать overload signatures, но у функции остаётся одна JavaScript-реализация. В Java каждая
перегрузка имеет собственное тело или делегирует другой перегрузке.

---

## 8. Полиморфизм

Полиморфизм позволяет работать через общий контракт, не зная точный runtime class:

```java
public interface FeePolicy {
    BigDecimal calculateFor(BigDecimal amount);
}

public final class StandardFeePolicy implements FeePolicy {
    @Override
    public BigDecimal calculateFor(BigDecimal amount) {
        return amount.multiply(new BigDecimal("0.01"));
    }
}

public final class FreeFeePolicy implements FeePolicy {
    @Override
    public BigDecimal calculateFor(BigDecimal amount) {
        return BigDecimal.ZERO;
    }
}
```

```java
FeePolicy policy = new FreeFeePolicy();
BigDecimal fee = policy.calculateFor(new BigDecimal("1000.00"));
```

Здесь:

- declared type ссылки — `FeePolicy`;
- runtime type объекта — `FreeFeePolicy`;
- доступные методы проверяются по `FeePolicy`;
- реализация `calculateFor` выбирается по runtime type.

### Dynamic dispatch

```java
List<FeePolicy> policies = List.of(
    new StandardFeePolicy(),
    new FreeFeePolicy()
);

for (FeePolicy policy : policies) {
    System.out.println(policy.calculateFor(new BigDecimal("1000.00")));
}
```

Один и тот же вызов приводит к разному поведению. Это dynamic method dispatch.

Полиморфизм относится к переопределяемым instance methods. Он не применяется таким же образом к:

- полям;
- конструкторам;
- static methods;
- private methods.

### Поле не полиморфно

```java
class Parent {
    public String name = "parent";
}

class Child extends Parent {
    public String name = "child";
}

Parent value = new Child();
System.out.println(value.name); // parent
```

Field access определяется declared type ссылки. Поэтому скрывать поле родителя полем с тем же именем обычно не стоит.

---

## 9. Абстракция: interface и abstract class

Абстракция выделяет существенный контракт и скрывает детали реализации.

### Interface

```java
public interface AccountRepository {
    Account findById(String id);

    void save(Account account);
}
```

Interface хорошо подходит для:

- роли или capability;
- границы между слоями;
- нескольких взаимозаменяемых реализаций;
- dependency inversion;
- контрактов без общего mutable state.

Класс может реализовать несколько interfaces:

```java
public final class JpaAccountRepository
        implements AccountRepository, AutoCloseable {

    @Override
    public Account findById(String id) {
        // ...
        return null;
    }

    @Override
    public void save(Account account) {
        // ...
    }

    @Override
    public void close() {
        // ...
    }
}
```

### Abstract class

```java
public abstract class Account {
    private final String id;
    private BigDecimal balance;

    protected Account(String id, BigDecimal balance) {
        this.id = id;
        this.balance = balance;
    }

    public final String getId() {
        return id;
    }

    public abstract BigDecimal monthlyFee();
}
```

Abstract class:

- нельзя создать через `new`;
- может иметь fields и constructors;
- может содержать concrete и abstract methods;
- подходит для общей реализации тесно связанных типов.

### Сравнение

| Возможность              | interface                             | abstract class                 |
|---                       |---                                    |---                            |
| несколько на один класс | да                                    | нет, superclass только один   |
| instance state          | нет обычных instance fields           | да                             |
| constructor             | нет                                   | да                             |
| concrete behavior       | `default`, `static`, private methods  | обычные concrete methods      |
| типичное назначение     | контракт или capability               | общая база тесной иерархии    |

В TypeScript interface полностью исчезает после компиляции. Java interface остаётся JVM-типом: его можно увидеть через
reflection, проверить через `instanceof`, использовать в сигнатурах bytecode.

---

## 10. `final` и `sealed`

### Final field

```java
private final String id;
```

Ссылку в поле можно присвоить один раз. Но `final` не делает сам объект immutable:

```java
private final List<String> tags = new ArrayList<>();

public void addTag(String tag) {
    tags.add(tag); // допустимо
}
```

Нельзя присвоить полю новый список, но существующий список можно менять.

### Final method

```java
public final String getId() {
    return id;
}
```

Наследник не может переопределить method.

### Final class

```java
public final class Money {
}
```

От класса нельзя наследоваться. Это полезно для immutable value object и класса, контракт которого не рассчитан на
расширение.

### Sealed hierarchy

```java
public sealed interface TransactionResult
        permits Success, Rejected {
}

public record Success(String transactionId) implements TransactionResult {
}

public record Rejected(String reason) implements TransactionResult {
}
```

`sealed` ограничивает разрешённые реализации. Такая иерархия похожа по назначению на TypeScript discriminated union:

```typescript
type TransactionResult =
  | { kind: "success"; transactionId: string }
  | { kind: "rejected"; reason: string };
```

Но механика различается: Java использует номинативную иерархию runtime-типов, TypeScript — структурный union,
исчезающий после compilation.

---

## 11. Композиция вместо наследования

Композиция означает, что объект использует другой объект:

```java
public final class TransferService {
    private final FeePolicy feePolicy;
    private final AccountRepository accountRepository;

    public TransferService(
            FeePolicy feePolicy,
            AccountRepository accountRepository
    ) {
        this.feePolicy = feePolicy;
        this.accountRepository = accountRepository;
    }
}
```

`TransferService` не является `FeePolicy` или `AccountRepository`; он зависит от них.

### Почему композиция часто безопаснее

- зависимости видны в полях и constructor;
- компоненты можно заменять независимо;
- нет доступа к `protected` внутренностям;
- изменение superclass меньше влияет на subclasses;
- проще тестировать объект с fake или stub dependency;
- поведение можно собирать из небольших стратегий.

### Наследование уместно, когда

- отношение `is-a` действительно устойчиво;
- базовый тип специально спроектирован для расширения;
- контракт наследования документирован;
- subtype сохраняет поведение родителя;
- нужна полиморфная иерархия, а не только повторное использование кода.

### Наследование не стоит использовать только ради повторного использования

Плохо:

```java
public class TransferService extends ValidationUtils {
}
```

`TransferService` не является разновидностью `ValidationUtils`. Лучше dependency или отдельная функция/strategy.

---

## 12. Приведение типов и `instanceof`

### Upcasting

```java
PremiumAccount premium = new PremiumAccount();
Account account = premium;
```

Преобразование к базовому типу безопасно и обычно не требует явного cast.

### Downcasting

```java
Account account = loadAccount();
PremiumAccount premium = (PremiumAccount) account;
```

Compiler разрешит cast, но если объект не является `PremiumAccount`, в runtime возникнет `ClassCastException`.

Проверка с pattern matching:

```java
if (account instanceof PremiumAccount premium) {
    premium.applyCashback();
}
```

Если код постоянно выясняет concrete type через длинную цепочку `instanceof`, это может означать, что нужное поведение
следует перенести в полиморфный method.

Вместо:

```java
if (account instanceof PremiumAccount premium) {
    premium.applyPremiumFee();
} else if (account instanceof SavingsAccount savings) {
    savings.applySavingsFee();
}
```

предпочтительнее общий контракт:

```java
account.applyMonthlyFee();
```

Но `instanceof` не является ошибкой сам по себе. Он полезен на boundary, при разборе ограниченной sealed hierarchy и в
интеграционном коде.

---

## 13. Идентичность, `==`, `equals` и `hashCode`

### `==` для ссылок

Для объектов `==` проверяет, указывают ли ссылки на один экземпляр:

```java
Money first = new Money("10.00", "RUB");
Money second = new Money("10.00", "RUB");

System.out.println(first == second); // false
```

### `equals`

Логическое равенство задаётся методом `equals`:

```java
System.out.println(first.equals(second));
```

Без переопределения используется реализация `Object`, которая фактически проверяет identity.

Value objects должны определять равенство по значению. Один из простых вариантов — `record`:

```java
public record Money(BigDecimal amount, String currency) {
}
```

Record автоматически получает `equals`, `hashCode` и `toString` по компонентам.

### Контракт `equals`

Корректный `equals` должен быть:

- reflexive: `x.equals(x)` истинно;
- symmetric: `x.equals(y)` равно `y.equals(x)`;
- transitive;
- consistent, пока участвующее состояние не меняется;
- ложным для `null`.

### Контракт `hashCode`

Если `a.equals(b)` истинно, то `a.hashCode() == b.hashCode()` тоже должно быть истинно.

Обратное не требуется: разные объекты могут иметь одинаковый hash code.

Нарушение контракта ломает `HashMap` и `HashSet`:

```java
Set<Money> values = new HashSet<>();
values.add(first);

System.out.println(values.contains(second));
```

Если `equals` переопределён, а совместимый `hashCode` — нет, `contains` может вернуть неожиданный результат.

### TypeScript-сравнение

JavaScript/TypeScript не имеет встроенного переопределяемого аналога Java `equals`. `===` для объектов проверяет
identity. Равенство по значению обычно реализуют отдельной функцией или библиотекой.

---

## 14. Mutable и immutable objects

Mutable object меняет наблюдаемое состояние:

```java
account.deposit(BigDecimal.TEN);
```

Immutable object после создания не меняется:

```java
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    private void requireSameCurrency(Money other) {
        if (currency != other.currency) {
            throw new IllegalArgumentException("currency mismatch");
        }
    }
}
```

`add` возвращает новый `Money`, а исходные объекты не изменяет.

Для практической immutable-модели:

- не предоставляй mutating methods;
- делай поля `private final`;
- не отдавай mutable internals;
- делай defensive copies на входе и выходе;
- не разрешай наследование, если наследник сможет добавить mutable behavior;
- проверяй, immutable ли типы самих полей.

`final` на ссылке недостаточно, а `record` shallowly immutable: его компоненты нельзя переназначить, но компонент
`List` всё ещё может ссылаться на mutable list.

---

## 15. Номинативная Java против структурного TypeScript

TypeScript обычно считает типы совместимыми по структуре:

```typescript
class AccountId {
  constructor(public readonly value: string) {}
}

class CustomerId {
  constructor(public readonly value: string) {}
}

const accountId: AccountId = new CustomerId("42"); // допустимо по структуре
```

Java ориентируется на объявленные отношения типов:

```java
public record AccountId(String value) {
}

public record CustomerId(String value) {
}

AccountId accountId = new CustomerId("42"); // compile-time error
```

Совпадение полей и методов не создаёт отношение типов. Нужен общий interface, inheritance или явное преобразование.

### Сводное сравнение

| Концепция                  | Java                                        | TypeScript/JavaScript                               |
|---                        |---                                          |---                                                 |
| основная совместимость    | номинативная                                | преимущественно структурная                        |
| класс в runtime           | JVM-тип                                     | JavaScript constructor/function                    |
| interface в runtime       | существует как JVM-тип                      | удаляется при compilation                          |
| наследование классов      | superclass и dynamic dispatch               | prototype chain                                    |
| несколько базовых классов | нет                                          | нет, но есть mixin patterns                        |
| реализация интерфейсов    | явное `implements`                          | структура может совпасть без `implements`          |
| private member            | контролируется Java/JVM                     | `private` — type-level; `#field` — runtime private |
| optional field            | отдельного синтаксиса нет                    | `field?: T`                                        |
| отсутствие значения       | `null` для ссылок                            | `null` и `undefined`                               |
| object equality           | `==` identity, `equals` логическое равенство | `===` identity, общего overridable `equals` нет     |
| overload                  | несколько реализаций                         | несколько signatures и одна JS-реализация          |
| default parameters        | нет                                          | есть                                                |
| parameter properties      | нет                                          | есть                                                |
| discriminated union       | часто sealed hierarchy + records            | union типов                                         |
| final reference           | `final`                                      | `const` запрещает переназначить binding             |

### `final` и `const` — только приблизительная аналогия

```java
final List<String> names = new ArrayList<>();
names.add("Alice");     // допустимо
// names = new ArrayList<>(); // ошибка
```

```typescript
const names: string[] = [];
names.push("Alice"); // допустимо
// names = [];       // ошибка
```

Оба запрещают переназначение binding/reference, но не делают объект глубоко immutable.

### Java `private` и TypeScript `private`

TypeScript:

```typescript
class Account {
  private balance = 0;
}
```

Обычный `private` существует прежде всего для type checker. JavaScript runtime privacy выражается полем `#balance`.

Java:

```java
public class Account {
    private BigDecimal balance = BigDecimal.ZERO;
}
```

Visibility входит в метаданные JVM-класса и контролируется Java/JVM-инфраструктурой. Reflection и framework-механизмы
могут получать специальный доступ, но это не делает поле публичной частью контракта.

---

## 16. ООП в типичном Java backend

В enterprise-приложении разные виды классов играют разные роли:

```text
Controller  ──> Application Service ──> Domain Object
                         │
                         ├────────────> Repository interface
                         │                     ▲
                         └────────────> External gateway interface
                                               │
                                      Infrastructure implementations
```

Пример:

```java
public final class TransferService {
    private final AccountRepository accounts;
    private final FeePolicy feePolicy;

    public TransferService(
            AccountRepository accounts,
            FeePolicy feePolicy
    ) {
        this.accounts = accounts;
        this.feePolicy = feePolicy;
    }

    public void transfer(
            AccountId sourceId,
            AccountId targetId,
            Money amount
    ) {
        Account source = accounts.getRequired(sourceId);
        Account target = accounts.getRequired(targetId);
        Money fee = feePolicy.calculateFor(amount);

        source.withdraw(amount.add(fee));
        target.deposit(amount);

        accounts.save(source);
        accounts.save(target);
    }
}
```

Здесь одновременно используются:

- encapsulation: баланс меняется только через методы `Account`;
- abstraction: сервис зависит от `AccountRepository` и `FeePolicy`;
- polymorphism: runtime implementations repository и fee policy заменяемы;
- composition: dependencies переданы сервису через constructor;
- immutable value objects: `AccountId` и `Money`;
- mutable entity: `Account` управляет жизненным циклом баланса.

Не каждый класс обязан использовать наследование. В прикладном backend-коде композиция и interfaces часто встречаются
чаще, чем глубокие class hierarchies.

---

## 17. Частые ошибки

### Ошибка 1. Класс проектируется как TypeScript DTO

Поля, getters и setters создаются автоматически, но бизнес-правила остаются во внешних сервисах.

Следствие: любой код может собрать невозможное состояние. Для domain object лучше публиковать осмысленные операции.

### Ошибка 2. `new` считается глубоким копированием

```java
Account second = first;
```

Новый объект не создаётся. Копирование требует copy constructor, factory или явного mapping.

### Ошибка 3. `final` считается полной immutable-гарантией

`final List<T>` всё ещё может изменяться. Нужно учитывать mutability объекта по ссылке.

### Ошибка 4. `==` используется для value objects и строк

```java
if (currency == "RUB") {
}
```

Для объектов это identity comparison. Для строк используй:

```java
if ("RUB".equals(currency)) {
}
```

Для enum `==` корректен:

```java
if (currency == Currency.RUB) {
}
```

### Ошибка 5. Наследование применяется для повторного использования пары методов

Это создаёт ложное `is-a` отношение и сильную связанность. Сначала рассмотри composition.

### Ошибка 6. Overloading принимается за runtime polymorphism

Overload выбирается compiler по declared types. Override выбирается runtime по фактическому классу объекта.

### Ошибка 7. Базовый класс вызывает overridable method из constructor

```java
public Account() {
    validate(); // может вызвать override наследника до его инициализации
}
```

Наследник ещё не закончил инициализацию, поэтому override может увидеть default values своих полей.

### Ошибка 8. Downcast используется вместо общего контракта

Если клиенту постоянно нужен конкретный subtype, возможно, базовая abstraction выбрана неверно.

### Ошибка 9. У `equals` нет согласованного `hashCode`

Объект начинает некорректно вести себя в hash-based collections.

### Ошибка 10. Interface создаётся для каждого класса

Interface полезен, когда есть контракт, boundary или необходимость заменять реализацию. Механическое создание
`AccountService` + `AccountServiceImpl` без второй реализации или архитектурной границы добавляет шум.

---

## 18. Мини-практика: объектная модель Wallet Service

### Задание 1. Инкапсулированный `Account`

Создай `Account`, у которого:

- immutable `AccountId`;
- immutable `Currency`;
- private `balance`;
- конструктор запрещает отрицательный initial balance;
- `deposit` принимает только положительную сумму той же валюты;
- `withdraw` не позволяет уйти в минус;
- отсутствует `setBalance`.

Проверь:

```java
Account account = new Account(
    new AccountId("ACC-1"),
    Money.rubles("1000.00")
);

account.deposit(Money.rubles("500.00"));
account.withdraw(Money.rubles("300.00"));

// ожидаемый баланс: 1200.00 RUB
```

### Задание 2. Identity против value equality

Создай два независимых `Money` с одинаковыми значениями и проверь:

```java
first == second
first.equals(second)
first.hashCode() == second.hashCode()
```

Реализуй `Money` сначала обычным final class, затем record. Сравни объём кода.

### Задание 3. Полиморфная комиссия

Создай interface:

```java
public interface FeePolicy {
    Money calculateFor(Money amount);
}
```

Реализации:

- `NoFeePolicy`;
- `FixedFeePolicy`;
- `PercentageFeePolicy`.

`TransferService` должен зависеть только от `FeePolicy`, не использовать `instanceof` и не знать concrete
implementation.

### Задание 4. Наследование против композиции

Реализуй premium account двумя способами:

1. `PremiumAccount extends Account`;
2. обычный `Account` с переданной `FeePolicy`.

Ответь:

- где проще заменить правило комиссии;
- где меньше связанность;
- действительно ли premium account нарушает поведение обычного account;
- требуется ли вообще subtype для бизнес-модели.

### Задание 5. Overload и override

Предскажи результат до запуска:

```java
class Account {
    String type() {
        return "account";
    }
}

class PremiumAccount extends Account {
    @Override
    String type() {
        return "premium";
    }
}

static String describe(Account account) {
    return "Account: " + account.type();
}

static String describe(PremiumAccount account) {
    return "PremiumAccount: " + account.type();
}

Account account = new PremiumAccount();
System.out.println(describe(account));
```

Объясни отдельно:

1. какая перегрузка `describe` выбрана compiler;
2. какая реализация `type` выбрана runtime.

### Задание 6. Defensive copy

Создай `Customer` со списком счетов. Добейся, чтобы такой код не менял внутреннее состояние:

```java
List<Account> accounts = customer.getAccounts();
accounts.clear();
```

Проверь отличие `Collections.unmodifiableList(accounts)` от `List.copyOf(accounts)` при последующем изменении исходного
списка внутри `Customer`.

---

## 19. Вопросы для самопроверки

1. Чем класс отличается от объекта?
2. Что хранится в переменной reference type?
3. Создаёт ли `Account second = first` новый объект?
4. Какие этапы проходит объект при `new`?
5. Когда compiler создаёт default constructor?
6. Почему после объявления конструктора с параметрами `new Account()` может перестать компилироваться?
7. Чем instance member отличается от static member?
8. Почему наличие private fields и публичных setters ещё не гарантирует хорошую инкапсуляцию?
9. Что такое инвариант объекта?
10. Какие members участвуют в dynamic dispatch?
11. Чем overriding отличается от overloading?
12. Почему overload выбирается по declared type, а override — по runtime type?
13. Что Java-класс наследует от `Object`?
14. Почему при переопределении `equals` обычно нужно переопределить `hashCode`?
15. Чем reference identity отличается от value equality?
16. Почему `final List<T>` не обязательно immutable?
17. Когда abstract class уместнее interface?
18. Почему Java interface сильнее отличается от TypeScript interface, чем кажется по синтаксису?
19. Что означает номинативная типизация Java?
20. Когда inheritance предпочтительнее composition?
21. Почему вызов overridable method из constructor опасен?
22. Когда downcast указывает на проблему дизайна?
23. Чем sealed hierarchy Java похожа на discriminated union TypeScript?
24. Почему domain object часто не должен иметь универсальный setter для каждого поля?

## Краткий итог

- Класс задаёт структуру, поведение и правила создания, а объект является конкретным runtime-экземпляром.
- Переменная reference type хранит ссылку; присваивание ссылки не копирует объект.
- Конструктор должен создавать объект сразу в корректном состоянии и защищать его инварианты.
- Инкапсуляция выражается осмысленными операциями и сокрытием представления, а не механическим набором getters/setters.
- Наследование моделирует устойчивое отношение `is-a`; для сборки поведения обычно безопаснее composition.
- Overloading выбирается в compile time, overriding и полиморфный instance method — в runtime.
- Java использует номинативные runtime-типы, а TypeScript в основном использует структурные compile-time-типы.
- Для объектов `==` проверяет identity, а логическое равенство задаётся `equals` вместе с согласованным `hashCode`.
- `final` запрещает переназначение ссылки, но не делает объект глубоко immutable.
- В backend-коде ООП чаще всего проявляется через encapsulated domain objects, interfaces на границах и constructor
  composition.
