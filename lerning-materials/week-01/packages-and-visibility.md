# Пакеты и visibility в Java

[← Вернуться к учебному плану](../java-backend-learning-plan.md)

## Что нужно понять

После изучения темы ты должен уметь:

1. объяснить, зачем Java нужны пакеты и как полное имя класса связано с каталогами;
2. использовать `package`, `import` и fully qualified name;
3. различать `public`, `protected`, package-private и `private`;
4. отдельно определять доступность top-level типа и его членов;
5. предсказывать доступ к `protected`-члену из другого пакета;
6. проектировать пакет как границу реализации, а не просто как папку;
7. читать ошибки компиляции, связанные с пакетами и visibility.

Здесь **visibility** означает доступность типа, поля, метода или конструктора из другой
части программы. В Java доступ контролируется на этапе компиляции.

## 1. Что такое package

Package — это пространство имён, в котором находятся Java-типы: классы, интерфейсы,
`enum`, `record` и аннотации.

```java
package com.example.wallet.account;

public class AccountService {
}
```

Полное, или fully qualified, имя класса:

```text
com.example.wallet.account.AccountService
```

Оно состоит из:

```text
package name                       simple class name
com.example.wallet.account       + AccountService
```

Два класса могут иметь одинаковое простое имя, если находятся в разных пакетах:

```text
java.util.Date
java.sql.Date
```

Для JVM это два разных типа.

### Package — не просто каталог

Исходник обычно располагают по пути, повторяющему имя пакета:

```text
src/main/java/
└── com/
    └── example/
        └── wallet/
            └── account/
                └── AccountService.java
```

```java
package com.example.wallet.account;
```

Соответствие пути пакету — стандартная договорённость build tools и IDE. Само имя
пакета задаётся декларацией `package`, а не положением файла.

Технически `javac` может получить файл из другого каталога, но намеренно нарушать
соответствие не следует: IDE, Maven, навигация и другие разработчики ожидают
стандартную структуру.

### Package — не объект

В отличие от модуля ES, пакет:

- нельзя присвоить переменной;
- нельзя импортировать целиком как namespace-объект;
- не имеет runtime-объекта с экспортами;
- прежде всего группирует имена и задаёт одну из границ доступа.

### Пакеты не образуют иерархию доступа

Точки в имени создают удобную структуру имён, но не отношение «родитель — ребёнок»:

```text
com.example.wallet
com.example.wallet.account
com.example.wallet.account.internal
```

Это три разных пакета. Класс из `com.example.wallet.account.internal` не получает
доступ к package-private членам `com.example.wallet.account`.

Это одна из наиболее частых ошибок при чтении Java-кода.

## 2. Декларация `package`

Декларация пакета, если она есть, находится в начале compilation unit:

```java
package com.example.wallet.account;

import java.math.BigDecimal;

public class Account {
    private BigDecimal balance;
}
```

До `package` могут находиться комментарии. После неё идут `import`, затем объявления
типов.

В одном `.java`-файле может быть только одна декларация `package`. Все top-level типы
из этого файла принадлежат указанному пакету.

### Unnamed package

Если `package` отсутствует, тип попадает в unnamed package:

```java
public class Main {
}
```

Это допустимо для однофайловых экспериментов, но не для production-кода:

- код из именованного пакета не может импортировать тип из unnamed package;
- структура плохо масштабируется;
- Maven- и Spring-проекты используют именованные пакеты.

## 3. Именование пакетов

Обычные правила:

- только нижний регистр;
- обратное доменное имя организации;
- затем продукт, модуль или feature;
- без дефисов, потому что `-` недопустим в Java-идентификаторе.

```text
com.example.wallet
com.example.wallet.account
com.example.wallet.transfer
```

Типичные неудачные варианты:

```text
Com.Example.Wallet     // верхний регистр
com.example.my-wallet // дефис
utils                  // слишком общее имя и высокий риск конфликтов
```

Не нужно механически делать отдельный пакет для каждого класса. Пакет должен
объединять тесно связанный код и помогать выражать архитектурную границу.

## 4. `import`: короткое имя вместо полного

Без импорта можно использовать полное имя:

```java
package com.example.wallet.account;

public class Account {
    private java.math.BigDecimal balance;
}
```

С импортом имя в файле становится короче:

```java
package com.example.wallet.account;

import java.math.BigDecimal;

public class Account {
    private BigDecimal balance;
}
```

`import`:

- не загружает класс в память;
- не устанавливает библиотеку;
- не копирует код;
- не делает недоступный тип доступным;
- только позволяет компилятору сопоставить короткое имя с полным.

Это ближе к удобству разрешения имени, чем к выполнению ES-модуля.

### Что не требуется импортировать

Без явного импорта доступны:

1. типы того же пакета;
2. типы из `java.lang`, например `String`, `Object`, `System`, `Integer`;
3. типы, к которым обращаются по полному имени.

```java
package com.example.wallet.account;

public class AccountService {
    private Account account; // Account находится в том же пакете
    private String name;     // java.lang.String импортирован неявно
}
```

### Wildcard import

```java
import java.util.*;
```

Такой импорт делает доступными короткие имена public-типов непосредственно из
`java.util`, но не из вложенных пакетов:

```java
import java.util.*; // не импортирует java.util.concurrent.ConcurrentHashMap
```

Wildcard не влияет на runtime и обычно не ухудшает производительность. Его минусы —
менее явные зависимости файла и возможные конфликты имён. В enterprise-коде стиль
обычно задаётся IDE, Checkstyle или командными соглашениями.

### Конфликт одинаковых имён

Два типа с одинаковым простым именем нельзя одновременно импортировать обычным
способом:

```java
import java.util.Date;
import java.sql.Date; // конфликт
```

Один из типов нужно записывать полным именем:

```java
import java.util.Date;

public class Report {
    private Date createdAt;
    private java.sql.Date businessDate;
}
```

У Java нет import alias, аналогичного такому TypeScript-коду:

```typescript
import { Date as SqlDate } from "./sql";
```

### Static import

Обычный импорт относится к типу:

```java
import java.math.BigDecimal;
```

Статический импорт позволяет обращаться по короткому имени к доступному static-члену:

```java
import static java.math.RoundingMode.HALF_UP;
import static org.junit.jupiter.api.Assertions.assertEquals;

assertEquals(expected, actual);
amount.setScale(2, HALF_UP);
```

Использовать его стоит там, где короткая запись остаётся понятной. Особенно часто он
встречается в тестах.

## 5. Два уровня проверки доступа

В выражении:

```java
account.debit(amount);
```

компилятор проверяет как минимум:

1. доступен ли тип `Account`;
2. доступен ли метод `debit`.

Public-метод внутри недоступного package-private класса не делает этот класс частью
публичного API:

```java
class InternalAccount {        // package-private top-level class
    public void debit() {      // public, но тип снаружи пакета недоступен
    }
}
```

Всегда анализируй внешний уровень, а затем внутренний.

## 6. Модификаторы доступа

### Общая таблица

Эта таблица описывает доступ к **членам класса**: полям, методам, конструкторам и
вложенным типам. Для top-level классов и интерфейсов действуют более узкие правила,
описанные сразу после таблицы.

| Модификатор                       | Тот же класс | Тот же пакет | Подкласс в другом пакете | Остальной код |
|---                                |---           |---            |---                       |---            |
| `public`                          | да           | да            | да                        | да            |
| `protected`                       | да           | да            | да, с ограничением        | нет           |
| без модификатора, package-private | да           | да            | нет                       | нет           |
| `private`                         | да           | нет           | нет                       | нет           |

«Без модификатора» — это не ключевое слово `default`. В Java нет модификатора
доступа `default`; `default` используется, например, для методов интерфейса и ветки
`switch`.

### Где какие модификаторы разрешены

Для top-level типа доступны только:

```text
public
package-private (модификатор отсутствует)
```

Так нельзя:

```java
private class Account {   // ошибка компиляции для top-level типа
}

protected class Account { // ошибка компиляции для top-level типа
}
```

Члены класса и вложенные типы могут быть `public`, `protected`, package-private или
`private`.

## 7. `public`

`public` означает доступ из любого места, где доступен содержащий тип:

```java
package com.example.wallet.api;

public class WalletService {
    public void transfer() {
    }
}
```

Но `public` не означает «можно вызвать всегда». Могут мешать:

- недоступность внешнего типа;
- отсутствие класса в compile classpath;
- границы Java Platform Module System;
- сигнатура с недоступным типом;
- обычные правила `static`/instance и типов.

### Один public top-level тип на файл

В одном файле может быть не более одного `public` top-level типа, а имя файла должно
совпадать с его именем:

```java
// WalletService.java
public class WalletService {
}
```

Рядом разрешены package-private top-level типы:

```java
public class WalletService {
}

class TransferValidator {
}
```

На практике значимые типы обычно хранят по одному в файле.

## 8. Package-private

Если модификатор отсутствует, доступ разрешён только коду того же пакета:

```java
package com.example.wallet.transfer;

class TransferValidator {
    boolean isAllowed() {
        return true;
    }
}
```

И класс, и метод здесь package-private.

Это полезный способ скрыть детали реализации feature:

```text
com.example.wallet.transfer
├── TransferService.java       // public фасад
├── TransferValidator.java     // package-private
└── TransferLimitPolicy.java   // package-private
```

Внешний код зависит от `TransferService`, но не от внутренних деталей. При этом
классы одного пакета могут удобно взаимодействовать без повсеместных `public`.

### Package-private и тесты

Если тест объявлен в том же пакете, он видит package-private код, даже если физически
лежит в другом source root:

```text
src/main/java/com/example/wallet/transfer/TransferValidator.java
src/test/java/com/example/wallet/transfer/TransferValidatorTest.java
```

Оба файла должны начинаться одинаково:

```java
package com.example.wallet.transfer;
```

Доступ определяется именем пакета, а не каталогом `main` или `test`.

Это не означает, что любой private-метод нужно ослаблять ради теста. Обычно тестируют
наблюдаемое поведение через API класса, а package-private используют, если это
осмысленная граница дизайна.

## 9. `private`

`private` ограничивает доступ телом top-level класса, который содержит объявление:

```java
public class Account {
    private long balanceInCents;

    private void validateAmount(long amountInCents) {
    }
}
```

Другой экземпляр того же класса доступ не ограничивает:

```java
public class Account {
    private long balanceInCents;

    boolean hasSameBalance(Account other) {
        return balanceInCents == other.balanceInCents; // допустимо
    }
}
```

`private` относится к классу, а не к конкретному объекту.

Современный компилятор также обеспечивает доступ между enclosing-классом и его
вложенными классами согласно правилам языка:

```java
public class Wallet {
    private long balance;

    private static class Auditor {
        static long readBalance(Wallet wallet) {
            return wallet.balance;
        }
    }
}
```

## 10. `protected`: два разных правила одновременно

`protected` часто понимают неточно как «видно только наследникам». На самом деле
доступ разрешён:

1. всему коду того же пакета — почти как package-private;
2. подклассам из других пакетов — но с дополнительным ограничением на объект,
   через который выполняется доступ.

### В том же пакете

```java
package com.example.wallet.core;

public class Account {
    protected long balance;
}
```

```java
package com.example.wallet.core;

class AccountPrinter {
    long read(Account account) {
        return account.balance; // допустимо: тот же пакет, наследование не нужно
    }
}
```

### В другом пакете

```java
package com.example.wallet.core;

public class Account {
    protected long balance;
}
```

```java
package com.example.wallet.premium;

import com.example.wallet.core.Account;

public class PremiumAccount extends Account {
    long ownBalance() {
        return this.balance; // допустимо
    }

    long anotherPremiumBalance(PremiumAccount other) {
        return other.balance; // допустимо
    }

    long arbitraryAccountBalance(Account other) {
        return other.balance; // ошибка компиляции
    }
}
```

Почему последний вариант запрещён? В другом пакете `PremiumAccount` получает
`protected`-доступ как наследник к своей наследуемой части API, а не право читать
внутреннее состояние любого `Account`.

Упрощённое практическое правило для подкласса из другого пакета:

> Доступ идёт через `this`, `super` или ссылку, тип которой является этим подклассом
> либо его подклассом, но не через произвольную ссылку базового типа.

Конструктор имеет дополнительные нюансы: `protected`-конструктор из другого пакета
можно вызвать через `super(...)` при создании подкласса, но это не превращает его в
общедоступную фабрику базового класса.

### Почему `protected` стоит применять осторожно

Protected-член становится частью контракта для наследников. Изменить его позже
сложнее, чем private-деталь. Для состояния обычно безопаснее:

```java
private long balance;

protected long balance() {
    return balance;
}
```

Метод сохраняет контроль над инвариантами лучше, чем `protected`-поле.

## 11. Конструкторы и visibility

Доступ конструктора определяет, кто может создавать экземпляры:

```java
public final class Money {
    private final long cents;

    private Money(long cents) {
        this.cents = cents;
    }

    public static Money ofCents(long cents) {
        if (cents < 0) {
            throw new IllegalArgumentException("cents must be non-negative");
        }
        return new Money(cents);
    }
}
```

Private-конструктор позволяет направить создание объектов через factory method,
проверить инварианты или реализовать utility-класс.

Package-private конструктор удобен, если экземпляры должен создавать только код
конкретного пакета:

```java
public class Transfer {
    Transfer() {
    }
}
```

Если конструктор вообще не объявлен, компилятор создаёт default constructor с той же
visibility, что и класс. Если объявить любой конструктор самостоятельно, неявный
конструктор без параметров больше не генерируется.

## 12. Интерфейсы, records и вложенные типы

### Интерфейсы

Обычный метод интерфейса без тела неявно `public abstract`:

```java
public interface TransferRepository {
    void save(); // public abstract
}
```

Реализация не может сузить доступ:

```java
public class JdbcTransferRepository implements TransferRepository {
    @Override
    public void save() {
    }
}
```

Поля интерфейса неявно `public static final`. Современная Java также разрешает
`private`-методы интерфейса для повторного использования внутри `default` и
`static`-методов.

### Records

Visibility record-компонентов выражается через сгенерированные accessor-методы:

```java
public record Money(long cents) {
}
```

Метод `cents()` является `public`, потому что record — `public`. Поля компонентов
при этом являются private final деталями реализации.

### Вложенные типы

Для nested-классов доступны все четыре уровня:

```java
public class TransferService {
    public static class Request {
    }

    private static class ValidationResult {
    }
}
```

При обращении снова проверяются оба уровня: доступность `TransferService`, затем
доступность `Request` или `ValidationResult`.

## 13. Переопределение и сужение доступа

Переопределяющий метод не может быть менее доступным, чем родительский:

```java
public class BaseService {
    protected void validate() {
    }
}
```

Допустимо расширить доступ:

```java
public class WalletService extends BaseService {
    @Override
    public void validate() {
    }
}
```

Недопустимо сузить:

```java
public class WalletService extends BaseService {
    @Override
    private void validate() { // ошибка компиляции
    }
}
```

`private`-метод родителя не переопределяется: подкласс его не видит. Метод с тем же
именем в подклассе будет новым методом.

## 14. Сравнение с TypeScript

| Java                              | Приблизительная аналогия в TypeScript/Node.js                                             |
|---                                |---                                                                                         |
| `package`                         | namespace для имён плюс граница package-private доступа; точного аналога нет                |
| `import com.example.Account`      | импорт имени типа, но без выполнения модуля как в ESM                                      |
| `public`                          | `public`, однако Java проверяет доступ и у top-level типа                                   |
| `private`                         | TypeScript `private` ближе всего; Java также применяет правила к nested-классам              |
| package-private                   | точного аналога нет; доступ определяется одинаковым именем пакета                           |
| `protected`                       | похож на TypeScript `protected`, но в Java код того же пакета тоже имеет доступ              |
| `java.lang`                       | небольшой набор автоматически доступных типов; это не аналог глобального объекта Node.js    |
| fully qualified class name        | полный путь пространства имён до типа                                                       |

Важное различие: файлы ES-модулей являются явной границей экспорта. В Java
`public class` публичен независимо от имени файла как модуля; границы формируются
пакетами, модификаторами, classpath и, если используется JPMS, Java-модулями.

TypeScript `private` существует в системе типов и в зависимости от target обычно
остаётся обычным JavaScript-свойством. Java `private` записывается в метаданные
класса и контролируется JVM-инфраструктурой, хотя reflection при специальных
условиях может обходить обычную проверку. Это не повод проектировать API с расчётом
на reflection.

## 15. Package visibility и Java modules — разные границы

Не смешивай:

- package: пространство имён и граница package-private;
- Maven module: единица сборки и зависимостей;
- JPMS module: языковая/runtime-граница с `module-info.java`;
- Spring component scan: механизм поиска bean-классов.

В JPMS public-класс может быть недоступен другому модулю, если его пакет не
экспортирован:

```java
module com.example.wallet {
    exports com.example.wallet.api;
}
```

Пакет `com.example.wallet.internal` здесь не экспортируется. Но это следующий слой
архитектуры: базовые правила `public`/`protected`/package-private/`private` всё равно
сначала должны быть понятны.

## 16. Практический дизайн пакетов

На раннем этапе Wallet Service можно организовать по feature:

```text
com.example.wallet
├── WalletApplication.java
├── account/
│   ├── Account.java
│   ├── AccountService.java
│   └── AccountRepository.java
└── transfer/
    ├── Transfer.java
    ├── TransferService.java
    ├── TransferRepository.java
    └── TransferValidator.java
```

Не каждый класс обязан быть `public`. Например:

```java
package com.example.wallet.transfer;

public class TransferService {
    private final TransferValidator validator = new TransferValidator();

    public void transfer() {
        validator.validate();
    }
}
```

```java
package com.example.wallet.transfer;

final class TransferValidator {
    void validate() {
    }
}
```

`TransferService` — API feature, а `TransferValidator` — изменяемая деталь пакета.

Полезная эвристика:

1. начинай с минимально необходимой visibility;
2. делай `public` только то, от чего действительно должен зависеть внешний код;
3. предпочитай private-поля;
4. не используй `protected` только ради тестирования;
5. следи, чтобы public API не возвращал внутренние package-private типы;
6. воспринимай package-private как инструмент архитектуры, а не случайное отсутствие
   слова `public`.

## 17. Частые ошибки

### Ошибка 1. Подпакет считается частью родительского пакета

```text
com.example.wallet
com.example.wallet.internal
```

Для package-private доступа они никак не связаны.

### Ошибка 2. `import` воспринимается как разрешение доступа

Импортировать можно только тип, который уже доступен. `import` не обходит
package-private или private.

### Ошибка 3. `protected` считается доступным через любой объект базового типа

Для наследника из другого пакета тип ссылки имеет значение. Перечитай пример с
`PremiumAccount`.

### Ошибка 4. Всё объявляется `public`

Так детали реализации превращаются в неявный контракт. Чем шире доступность, тем
дороже рефакторинг.

### Ошибка 5. Пакет путают с source root

`src/main/java` и `src/test/java` — разные source roots, но классы под ними могут
принадлежать одному Java-пакету.

### Ошибка 6. Имя каталога считается источником истины

Источник истины для принадлежности исходника к пакету — декларация `package`.
Структура каталогов должна ей соответствовать по соглашению.

### Ошибка 7. Public API раскрывает недоступный тип

```java
package com.example.wallet.transfer;

class TransferDetails {
}

public class TransferService {
    public TransferDetails details() {
        return new TransferDetails();
    }
}
```

Метод формально `public`, но клиент из другого пакета не может нормально использовать
его возвращаемый тип. Такие сигнатуры — признак плохо спроектированной границы API.

## 18. Как разбирать ошибку доступа

Если компилятор сообщает, что тип или член недоступен:

1. выпиши package вызывающего класса;
2. выпиши package целевого класса;
3. проверь visibility самого top-level класса;
4. проверь visibility нужного конструктора, поля или метода;
5. для `protected` проверь наследование, package и тип ссылки слева от точки;
6. проверь, нет ли конфликта импортов;
7. только затем проверяй classpath и JPMS.

Типичные сообщения:

```text
Account is not public in com.example.wallet; cannot be accessed from outside package
```

```text
balance has protected access in Account
```

```text
The type java.sql.Date is already defined in a single-type-import
```

Компилятор обычно прямо указывает, какой уровень проверки не прошёл.

## 19. Мини-практика

Создай структуру:

```text
src/
├── com/example/wallet/account/
│   ├── Account.java
│   └── AccountService.java
└── com/example/wallet/report/
    └── AccountReport.java
```

### Задание 1. Package-private граница

Сделай `Account` package-private, а `AccountService` — public. Попытайся создать
`Account` из `AccountReport`. Зафиксируй сообщение компилятора.

Затем сделай `Account` public, но оставь его конструктор package-private. Сравни новую
ошибку с предыдущей: теперь тип доступен, а операция создания — нет.

### Задание 2. Импорты

В одном классе используй одновременно:

```text
java.util.Date
java.sql.Date
```

Один тип импортируй, для второго используй fully qualified name.

### Задание 3. `protected`

Создай `BaseAccount` с `protected long balance` в пакете `account` и
`PremiumAccount extends BaseAccount` в пакете `premium`.

Проверь компиляцией:

```java
this.balance
premiumAccount.balance
baseAccount.balance
```

Объясни, почему третье выражение не компилируется внутри `PremiumAccount`, если
`baseAccount` имеет тип `BaseAccount`.

### Задание 4. Минимальная visibility

Спроектируй пакет `transfer` из четырёх классов:

```text
TransferService
Transfer
TransferValidator
TransferLimitPolicy
```

Оставь публичным только API, который нужен пакету `report`. Обоснуй visibility каждого
типа, конструктора и метода.

## 20. Вопросы для самопроверки

1. Чем имя пакета отличается от пути к исходному файлу?
2. Является ли `com.example.wallet.internal` подпакетом с особыми правами относительно
   `com.example.wallet`?
3. Что именно делает `import`?
4. Какие типы доступны без явного импорта?
5. Какие модификаторы разрешены top-level классу?
6. Чем package-private отличается от `protected`?
7. Почему `protected`-поле может быть доступно классу, который вообще не является
   наследником?
8. Почему наследник из другого пакета не всегда может обратиться к protected-члену
   через ссылку базового типа?
9. Может ли public-метод оказаться практически недоступным внешнему коду?
10. Как package-private помогает уменьшить публичный API feature?

## Краткий итог

- Package задаёт пространство имён и границу package-private доступа.
- Подпакеты — самостоятельные пакеты без привилегий относительно «родителя».
- `import` сокращает имя, но не загружает класс и не меняет доступ.
- Доступ нужно проверять на каждом уровне: внешний тип, вложенный тип или член.
- `public` открыт всем, `private` — только содержащему классу, package-private —
  пакету.
- `protected` объединяет доступ для пакета и специальный доступ для наследников.
- Хороший дизайн начинает с минимальной visibility и публикует только устойчивый API.
