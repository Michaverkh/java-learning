# Примитивы и reference types в Java

[← Вернуться к учебному плану](../java-backend-learning-plan.md)

## Что нужно понять

После изучения темы ты должен уметь:

1. назвать восемь примитивных типов Java и выбрать подходящий тип для данных;
2. отличать значение примитива от ссылки на объект;
3. объяснять, что именно копируется при присваивании и передаче аргумента в метод;
4. понимать, почему Java всегда передаёт аргументы по значению;
5. предсказывать результат integer arithmetic, numeric promotion, overflow и деления целых чисел;
6. отличать `double` от `BigDecimal` и не использовать binary floating point для денег;
7. понимать ограничения `char` при работе с Unicode;
8. безопасно пользоваться wrapper-типами и autoboxing/unboxing;
9. объяснять различия `==`, `equals` и сравнения массивов;
10. выбирать между primitive и wrapper type в полях, DTO, generics и nullable-контрактах.

В TypeScript тип `number` покрывает почти всю обычную числовую арифметику, а `string` представляет текст. В Java выбор
типа заметно сильнее влияет на допустимые значения, арифметику, возможность `null`, generics и поведение при сравнении.

---

## 1. Две группы типов

Типы Java делятся на две большие группы:

- **primitive types** — восемь встроенных типов, значение хранится непосредственно в переменной;
- **reference types** — классы, интерфейсы, массивы, enum, record и другие типы, переменная которых хранит ссылку на
  объект либо `null`.

```java
int attempts = 3;                 // primitive type
BigDecimal balance = BigDecimal.ZERO; // reference type
String accountId = "ACC-1";       // reference type
int[] operationCodes = {10, 20};  // reference type
```

`String`, массив и любой созданный тобой класс — reference types. Даже если `String` выглядит в коде почти как
встроенный тип, примитивом он не является.

| Свойство                    | Primitive type                                      | Reference type                                       |
|---                          |---                                                  |---                                                  |
| Что содержит переменная     | само значение                                       | ссылку на объект или `null`                           |
| Может ли быть `null`        | нет                                                 | да                                                    |
| Создаётся ли через `new`    | нет                                                 | обычно да, но есть литералы и фабрики                  |
| Есть ли instance methods    | нет                                                 | да                                                    |
| Допустим в `List<T>`        | нет: `List<int>` запрещён                           | да: `List<Integer>`                                   |
| Присваивание                | копирует значение                                   | копирует ссылку                                       |
| `==`                       | сравнивает значения                                 | обычно сравнивает identity объектов                   |
| Пример                      | `int`, `boolean`, `double`                          | `String`, `Integer`, `Account`, `int[]`               |

Формулировка «примитив находится в stack, объект находится в heap» полезна только как первое приближение и не является
определением типов. Примитив может быть полем объекта, а JVM вправе оптимизировать размещение данных. Семантически важно
не физическое место в RAM, а то, что выражение primitive type даёт значение, а выражение reference type — ссылочное
значение.

---

## 2. Восемь примитивных типов

### Целочисленные типы

| Тип       | Размер     | Диапазон                                                        | Типичное применение                         |
|---        |---         |---                                                              |---                                          |
| `byte`    | 8 бит      | от `-128` до `127`                                               | компактные binary-данные                    |
| `short`   | 16 бит     | от `-32_768` до `32_767`                                         | редко нужен в прикладном коде               |
| `int`     | 32 бита    | от `-2_147_483_648` до `2_147_483_647`                            | обычные целые числа                         |
| `long`    | 64 бита    | от `-9_223_372_036_854_775_808` до `9_223_372_036_854_775_807`    | большие счётчики, идентификаторы, epoch time |

Все эти типы знаковые и используют two's complement. В Java нет обычных unsigned primitive types, хотя стандартная
библиотека содержит отдельные методы для unsigned-интерпретации значений.

```java
int retryCount = 3;
long transactionId = 9_000_000_000L;
```

Суффикс `L` нужен, потому что целочисленный литерал без суффикса по умолчанию имеет тип `int`. Предпочтителен заглавный
`L`: строчную `l` легко спутать с цифрой `1`.

### Floating-point типы

| Тип       | Размер     | Приблизительная точность            | Типичное применение                    |
|---        |---         |---                                   |---                                     |
| `float`   | 32 бита    | 6–7 значащих десятичных цифр         | графика, большие массивы измерений     |
| `double`  | 64 бита    | 15–16 значащих десятичных цифр       | научные вычисления, метрики            |

Оба типа следуют IEEE 754 и хранят двоичное приближение числа:

```java
double result = 0.1 + 0.2;
System.out.println(result); // 0.30000000000000004
```

Литерал с десятичной точкой по умолчанию имеет тип `double`. Для `float` требуется суффикс `F`:

```java
float ratio = 0.75F;
double preciseRatio = 0.75;
```

### `boolean`

`boolean` принимает только `true` или `false`:

```java
boolean active = true;
```

Java не выполняет truthy/falsy-преобразования:

```java
int count = 1;

if (count) { // compile-time error
}
```

Нужно записать условие явно:

```java
if (count > 0) {
}
```

### `char`

`char` — 16-битная UTF-16 code unit, а не гарантированно целый Unicode-символ:

```java
char currencySign = '₽';
char latinA = '\u0041';
```

Одинарные кавычки создают `char`, двойные — `String`:

```java
char letter = 'A';
String text = "A";
```

Некоторые символы за пределами Basic Multilingual Plane представлены парой `char`, называемой surrogate pair:

```java
String emoji = "😀";

System.out.println(emoji.length());                  // 2 code units
System.out.println(emoji.codePointCount(0, emoji.length())); // 1 code point
```

Поэтому `char` не является полным аналогом «одного пользовательского символа». Для корректной обработки Unicode
используй API code points: `codePoints()`, `codePointAt()` и `codePointCount()`. Даже один code point не всегда равен
одному видимому grapheme cluster.

---

## 3. Литералы

Разделители `_` улучшают читаемость и не влияют на значение:

```java
int maxConnections = 10_000;
long nanosPerSecond = 1_000_000_000L;
double feeRate = 0.01;
```

Целые числа можно записывать в разных системах счисления:

```java
int decimal = 255;
int hexadecimal = 0xFF;
int binary = 0b1111_1111;
int octal = 0377;
```

С ведущим нулём литерал становится восьмеричным, поэтому в прикладном коде такая запись легко приводит к ошибке:

```java
int value = 010; // 8, а не 10
```

Специальные escape sequences:

```java
char newline = '\n';
char tab = '\t';
char quote = '\'';
char backslash = '\\';
```

---

## 4. Значения по умолчанию и локальные переменные

Поля класса и элементы массива получают default values:

| Тип                                           | Default value |
|---                                            |---            |
| `byte`, `short`, `int`, `long`                | `0`            |
| `float`, `double`                             | `0.0`          |
| `char`                                        | `'\u0000'`     |
| `boolean`                                     | `false`        |
| любой reference type                          | `null`         |

```java
public class AccountState {
    private int failedAttempts; // 0
    private boolean blocked;    // false
    private String reason;      // null
}
```

Локальная переменная default value не получает. Компилятор использует definite assignment analysis:

```java
int amount;
System.out.println(amount); // compile-time error: variable might not have been initialized
```

Это не означает, что локальная переменная содержит случайный «мусор», доступный Java-коду. Программа просто не
скомпилируется до гарантированного присваивания.

```java
int amount;
if (isPremium) {
    amount = 100;
} else {
    amount = 50;
}
System.out.println(amount); // обе ветки присваивают значение
```

---

## 5. Присваивание: копия значения против копии ссылки

### Primitive type

При присваивании примитива копируется значение:

```java
int first = 10;
int second = first;

second = 20;

System.out.println(first);  // 10
System.out.println(second); // 20
```

После присваивания переменные независимы.

### Reference type

При присваивании reference type копируется ссылка, а не объект:

```java
List<String> first = new ArrayList<>();
first.add("deposit");

List<String> second = first;
second.add("withdraw");

System.out.println(first); // [deposit, withdraw]
```

Обе ссылки указывают на один mutable object:

```text
first  ──────┐
             ├────> ArrayList ["deposit", "withdraw"]
second ──────┘
```

Переназначение одной ссылки не меняет вторую:

```java
second = new ArrayList<>();
second.add("transfer");

System.out.println(first);  // [deposit, withdraw]
System.out.println(second); // [transfer]
```

Теперь переменные указывают на разные объекты.

### Поверхностная копия тоже может разделять вложенные объекты

```java
List<Account> original = new ArrayList<>();
original.add(account);

List<Account> copy = new ArrayList<>(original);
```

Создан новый список, но оба списка содержат ссылки на тот же `account`. Изменение структуры `copy` не меняет структуру
`original`, а изменение самого `account` наблюдается через оба списка. Это shallow copy.

---

## 6. Java всегда использует pass-by-value

В Java нет pass-by-reference для переменных. Метод всегда получает копию значения аргумента:

- для примитива копируется primitive value;
- для reference type копируется reference value.

### Передача примитива

```java
static void increment(int value) {
    value++;
}

int count = 10;
increment(count);
System.out.println(count); // 10
```

Метод изменил только свою локальную копию.

### Передача ссылки

```java
static void addOperation(List<String> operations) {
    operations.add("deposit");
}

List<String> operations = new ArrayList<>();
addOperation(operations);
System.out.println(operations); // [deposit]
```

Копия ссылки указывает на тот же список, поэтому метод может изменить объект.

### Переназначение параметра

```java
static void replace(List<String> operations) {
    operations = new ArrayList<>();
    operations.add("replacement");
}

List<String> operations = new ArrayList<>();
operations.add("original");

replace(operations);
System.out.println(operations); // [original]
```

Метод переназначил только локальную копию ссылки. Переменная вызывающего кода осталась прежней.

Удобная проверка: если бы Java передавала саму переменную по ссылке, `replace` смог бы заставить переменную вызывающего
кода указывать на новый список. Но этого не происходит.

---

## 7. Арифметика целых чисел

### Деление отбрасывает дробную часть

```java
int result = 5 / 2;
System.out.println(result); // 2
```

Если нужен floating-point result, хотя бы один операнд должен быть `double` или `float`:

```java
double result = 5.0 / 2;
System.out.println(result); // 2.5
```

Преобразование после деления уже не вернёт потерянную часть:

```java
double wrong = (double) (5 / 2); // 2.0
double correct = (double) 5 / 2; // 2.5
```

### Integer promotion

При арифметических операциях `byte`, `short` и `char` повышаются как минимум до `int`:

```java
byte left = 10;
byte right = 20;

// byte sum = left + right; // compile-time error: результат имеет тип int
int sum = left + right;
```

Из-за этого на первый взгляд очевидный код не компилируется:

```java
short amount = 100;
// amount = amount + 1; // result is int
amount += 1;            // compound assignment включает неявное narrowing conversion
```

`amount += 1` ведёт себя приблизительно как `amount = (short) (amount + 1)`, поэтому может молча переполниться.

### Overflow

Integer overflow не создаёт исключение:

```java
int max = Integer.MAX_VALUE;
int overflowed = max + 1;

System.out.println(overflowed); // -2147483648
```

Результат оборачивается по диапазону two's complement. Если переполнение должно считаться ошибкой, используй exact
methods:

```java
int result = Math.addExact(Integer.MAX_VALUE, 1);
// ArithmeticException: integer overflow
```

Также доступны `subtractExact`, `multiplyExact`, `incrementExact`, `decrementExact`, `toIntExact`.

### Деление на ноль

Поведение зависит от категории числа:

```java
int integerResult = 1 / 0;       // ArithmeticException
double floatingResult = 1.0 / 0; // Infinity
double undefined = 0.0 / 0.0;    // NaN
```

Для floating point существуют `Double.POSITIVE_INFINITY`, `Double.NEGATIVE_INFINITY` и `Double.NaN`.

Особенность `NaN`:

```java
double value = Double.NaN;
System.out.println(value == value);      // false
System.out.println(Double.isNaN(value)); // true
```

---

## 8. Расширяющие и сужающие преобразования

### Widening conversion

Расширяющее преобразование обычно выполняется неявно:

```java
int count = 100;
long longCount = count;
double measuredCount = longCount;
```

Но «более широкий тип» не всегда означает точное сохранение значения. `long` содержит до 64 бит целочисленной
точности, а `double` — 53 бита значащей двоичной части:

```java
long original = 9_007_199_254_740_993L;
double converted = original;
long restored = (long) converted;

System.out.println(restored == original); // false
```

### Narrowing conversion

Сужающее преобразование требует явного cast и может потерять данные:

```java
long source = 3_000_000_000L;
int narrowed = (int) source;

System.out.println(narrowed); // отрицательное значение
```

Cast говорит компилятору «я принимаю это преобразование», но не делает его безопасным. Для проверенного преобразования
из `long` в `int`:

```java
int value = Math.toIntExact(source); // ArithmeticException, если значение не помещается
```

### Константные выражения

Компилятор разрешает присвоить подходящий compile-time constant более узкому типу:

```java
byte valid = 100;
// byte invalid = 128; // compile-time error
```

Но переменная `int`, даже содержащая `100`, уже не является таким неявным преобразованием:

```java
int source = 100;
// byte value = source; // compile-time error
byte value = (byte) source;
```

---

## 9. Почему деньги — не `double`

`double` оптимизирован для диапазона и скорости binary floating-point arithmetic, а не для точного представления
десятичных денежных сумм:

```java
double balance = 0.1 + 0.2;
System.out.println(balance == 0.3); // false
```

Для денег обычно используется `BigDecimal`:

```java
BigDecimal first = new BigDecimal("0.1");
BigDecimal second = new BigDecimal("0.2");
BigDecimal result = first.add(second);

System.out.println(result); // 0.3
```

Не создавай `BigDecimal` из неточного `double`:

```java
BigDecimal wrong = new BigDecimal(0.1);
BigDecimal correct = new BigDecimal("0.1");
BigDecimal alsoCorrect = BigDecimal.valueOf(0.1);
```

`BigDecimal` — immutable reference type. Операция возвращает новый объект:

```java
BigDecimal balance = new BigDecimal("100.00");
balance.add(new BigDecimal("50.00"));

System.out.println(balance); // 100.00

balance = balance.add(new BigDecimal("50.00"));
System.out.println(balance); // 150.00
```

Для деления иногда нужно явно задать scale и rounding mode:

```java
BigDecimal average = total.divide(
    BigDecimal.valueOf(count),
    2,
    RoundingMode.HALF_EVEN
);
```

`BigDecimal.equals` учитывает и значение, и scale:

```java
BigDecimal left = new BigDecimal("1.0");
BigDecimal right = new BigDecimal("1.00");

System.out.println(left.equals(right));        // false
System.out.println(left.compareTo(right) == 0); // true
```

Для domain value object `Money` заранее определи правила scale, rounding и equality, а не разбрасывай их по сервисам.

---

## 10. Reference types

К reference types относятся:

- class types: `String`, `BigDecimal`, `Account`;
- interface types: `List<String>`, `Runnable`;
- array types: `int[]`, `Account[]`;
- enum types;
- record types;
- type variables: `T`;
- специальные формы вроде annotation types.

```java
Account account = new Account();
List<String> operations = new ArrayList<>();
int[] codes = new int[10];
```

У переменной есть declared type, а у объекта — runtime type:

```java
List<String> operations = new ArrayList<>();
```

- declared type переменной — `List<String>`;
- runtime class объекта — `ArrayList`.

Declared type определяет, какие операции разрешит компилятор. Runtime type участвует в dynamic dispatch
переопределённых instance methods.

### Массивы — объекты

```java
int[] amounts = new int[3];

System.out.println(amounts.length);    // 3
System.out.println(amounts[0]);        // 0
System.out.println(amounts.getClass()); // class [I
```

Переменная массива является ссылкой, массив создаётся как объект, имеет runtime type и может быть `null`. Его элементы
получают default values.

### `String` — immutable reference type

```java
String accountId = "ACC";
accountId.concat("-1");

System.out.println(accountId); // ACC
```

Как и `BigDecimal`, `String` не меняется: метод возвращает новый объект.

```java
accountId = accountId.concat("-1");
System.out.println(accountId); // ACC-1
```

---

## 11. `null`

`null` — специальное значение, означающее отсутствие ссылки на объект:

```java
String reason = null;
```

Примитив не может быть `null`:

```java
// int count = null; // compile-time error
```

Вызов instance method или доступ к instance field через `null` приводит к `NullPointerException`:

```java
reason.length(); // NullPointerException
```

Проверять значение безопаснее так:

```java
if (reason != null) {
    System.out.println(reason.length());
}
```

Для equality удобен `Objects.equals`, который корректно обрабатывает `null`:

```java
boolean same = Objects.equals(left, right);
```

Для обязательных аргументов можно fail fast:

```java
public Account(String id) {
    this.id = Objects.requireNonNull(id, "id must not be null");
}
```

`Optional<T>` может выражать отсутствие результата в return type, но обычно не должен механически заменять каждое
nullable field или parameter. Эта тема подробно рассматривается отдельно.

---

## 12. Wrapper-типы

Для каждого примитива есть reference wrapper:

| Primitive type | Wrapper type |
|---             |---           |
| `byte`         | `Byte`        |
| `short`        | `Short`       |
| `int`          | `Integer`     |
| `long`         | `Long`        |
| `float`        | `Float`       |
| `double`       | `Double`      |
| `char`         | `Character`   |
| `boolean`      | `Boolean`     |

Wrapper нужен, когда требуется объект:

- в generics и collections;
- для nullable-значения;
- в некоторых serialization, reflection и framework API;
- для utility methods и constants.

```java
List<Integer> retryCounts = List.of(1, 2, 3);
Integer optionalLimit = null;
int max = Integer.MAX_VALUE;
int parsed = Integer.parseInt("42");
```

Wrapper-объекты immutable.

### Autoboxing и unboxing

Компилятор может автоматически преобразовать primitive в wrapper:

```java
Integer boxed = 42; // Integer.valueOf(42)
```

И wrapper в primitive:

```java
Integer boxed = 42;
int primitive = boxed; // boxed.intValue()
```

Удобство скрывает реальные операции, аллокации и риск `null`.

### Unboxing `null`

```java
Integer retryCount = null;
int count = retryCount; // NullPointerException
```

Та же проблема может быть спрятана в арифметике или условии:

```java
Integer count = null;
int next = count + 1; // unboxing, затем NullPointerException

Boolean enabled = null;
if (enabled) {        // unboxing, затем NullPointerException
}
```

Для tri-state значения `Boolean` иногда осмыслен: `true`, `false`, «не задано». Но такой контракт должен быть явным.

### Wrapper cache и ловушка `==`

```java
Integer first = 100;
Integer second = 100;
System.out.println(first == second); // часто true

Integer third = 1000;
Integer fourth = 1000;
System.out.println(third == fourth); // обычно false
```

`Integer.valueOf` обязан кэшировать как минимум значения от `-128` до `127`; реализации могут кэшировать больше.
Поэтому результат identity comparison зависит от объектов, а не от числового равенства.

Используй:

```java
Objects.equals(first, second);
// или после безопасного unboxing:
first.intValue() == second.intValue();
```

Не используй `new Integer(...)`: старые wrapper-конструкторы deprecated for removal. Предпочитай autoboxing,
`Integer.valueOf` или `Integer.parseInt`.

---

## 13. `==`, `equals` и массивы

### Примитивы

Для числовых примитивов `==` сравнивает числовые значения после numeric promotion:

```java
int left = 10;
long right = 10L;
System.out.println(left == right); // true
```

Для floating point сохраняются правила IEEE 754, включая `NaN` и signed zero.

### Ссылки

Для reference types `==` проверяет, указывают ли ссылки на один и тот же объект:

```java
String first = new String("ACC-1");
String second = new String("ACC-1");

System.out.println(first == second);      // false
System.out.println(first.equals(second)); // true
```

`equals` — instance method. Его смысл определяет класс. `String`, wrapper types, `BigDecimal`, records и collections
предоставляют value-oriented equality, но обычный класс наследует identity-based реализацию `Object.equals`, пока её
не переопределили.

String pool иногда заставляет ошибочный код выглядеть рабочим:

```java
String first = "ACC-1";
String second = "ACC-1";
System.out.println(first == second); // true: одинаковые string literals интернируются
```

Для содержания строк всё равно используй `equals`.

### Массивы

Массивы не переопределяют `Object.equals` для сравнения элементов:

```java
int[] first = {1, 2, 3};
int[] second = {1, 2, 3};

System.out.println(first.equals(second));        // false
System.out.println(Arrays.equals(first, second)); // true
```

Для вложенных массивов используй `Arrays.deepEquals`:

```java
int[][] first = {{1, 2}, {3, 4}};
int[][] second = {{1, 2}, {3, 4}};

System.out.println(Arrays.deepEquals(first, second)); // true
```

---

## 14. `final` для primitive и reference type

Для примитива `final` запрещает присвоить новое значение:

```java
final int maxRetries = 3;
// maxRetries = 4; // compile-time error
```

Для reference type `final` запрещает переназначить ссылку, но не запрещает менять объект:

```java
final List<String> operations = new ArrayList<>();
operations.add("deposit"); // допустимо

// operations = new ArrayList<>(); // compile-time error
```

```text
final reference ─────> mutable object
     нельзя              менять можно
 переназначить
```

`final` reference не делает объект immutable. Для immutability нужны соответствующий дизайн класса, отсутствие
изменяющих методов и defensive copies mutable state.

---

## 15. Generics не принимают primitive types

Такой код запрещён:

```java
// List<int> values = new ArrayList<>();
```

Нужен wrapper type:

```java
List<Integer> values = new ArrayList<>();
values.add(10); // autoboxing
int first = values.get(0); // unboxing
```

Для большого количества чисел это может увеличить расход памяти и добавить boxing overhead. Специализированные primitive
arrays и stream types избегают части накладных расходов:

```java
int[] values = {1, 2, 3};
IntStream stream = Arrays.stream(values);
int sum = stream.sum();
```

В стандартной библиотеке есть `IntStream`, `LongStream`, `DoubleStream`, а также `OptionalInt`, `OptionalLong`,
`OptionalDouble`.

```java
OptionalInt max = IntStream.of(10, 20, 30).max();
```

`Optional<int>` невозможен, потому что type argument должен быть reference type.

---

## 16. Выбор между primitive и wrapper

По умолчанию используй primitive, если отсутствие значения не является частью контракта:

```java
private int retryCount;
private boolean active;
```

Используй wrapper, когда нужны generics или осмысленный `null`:

```java
private Integer requestedLimit; // null означает «клиент не передал поле»
```

| Ситуация                                    | Обычно выбрать         | Почему                                                |
|---                                          |---                     |---                                                    |
| обязательный счётчик                        | `int`                   | нет `null`, проще и дешевле                            |
| обязательный флаг                           | `boolean`               | ровно два состояния                                   |
| поле может быть не передано в PATCH DTO      | `Integer` / `Boolean`   | `null` может означать «нет изменения»                  |
| элемент `List`                              | wrapper                 | generics не принимают primitive types                  |
| ID из БД до сохранения может отсутствовать   | `Long`                  | различает «ещё нет ID» и числовое значение             |
| обязательный domain ID после создания        | `long` или value object | отсутствие значения недопустимо                        |

В framework-коде важно различать:

```java
public record UpdateSettingsRequest(
    Boolean notificationsEnabled
) {
}
```

Здесь `null` может означать «поле отсутствовало», а `false` — «клиент явно отключил уведомления». Если заменить тип на
`boolean`, десериализатор может свести оба состояния к `false`, в зависимости от настроек и контракта.

Для ясного API лучше документировать required/optional поля и проверять их через Bean Validation или на границе
приложения, а не надеяться на случайный `NullPointerException` при unboxing.

---

## 17. Сопоставление с TypeScript/Node.js

| Java                                      | Приблизительная аналогия в TypeScript/JavaScript                                      |
|---                                        |---                                                                                    |
| `int`, `long`, `float`, `double`           | в обычном TS чаще всего один `number`, но его runtime-модель отличается                |
| `long`                                    | не аналог `bigint`: диапазон фиксирован 64 битами и возможен overflow                  |
| `BigInteger`                              | ближе к `bigint` по произвольной точности, но это immutable class                      |
| `boolean`                                 | `boolean`, но без truthy/falsy coercion                                                |
| `char`                                    | отдельного типа нет; JS string также использует UTF-16 code units                     |
| `String`                                  | `string`, но в Java это reference type и class                                         |
| `null` у reference type                   | часть runtime-модели; нет отдельного Java-аналога `undefined`                         |
| `Integer`                                 | wrapper object для `int`, нужен для generics и nullable-значений                       |
| `Account second = first`                  | `const second = first`: обе переменные указывают на один объект                        |
| primitive assignment                      | похоже на присваивание `number`, `boolean`, `bigint`                                   |
| Java pass-by-value reference              | практически похоже на передачу JS object reference value                              |
| `final Account account`                   | ближе к `const account`: нельзя переназначить binding, объект может остаться mutable    |

### Главное отличие чисел

Обычный JavaScript `number` — IEEE 754 double. Java заставляет выбирать между несколькими fixed-width primitive types:

```typescript
const value: number = 5 / 2; // 2.5
```

```java
int value = 5 / 2;       // 2
double other = 5.0 / 2;  // 2.5
```

JavaScript `number` точно представляет целые только до `2^53 - 1`. Java `long` хранит все целые своего 64-битного
диапазона точно, но переполняется за его границей.

### Главное сходство ссылок

```typescript
const first = { balance: 100 };
const second = first;
second.balance = 200;

console.log(first.balance); // 200
```

```java
Account first = new Account(100);
Account second = first;
second.setBalance(200);

System.out.println(first.getBalance()); // 200
```

В обоих случаях копируется значение, указывающее на один объект. Термин «передача объекта по ссылке» популярен, но для
Java неточен: передаётся по значению копия ссылки.

---

## 18. Практический backend-пример

Рассмотрим входящий запрос:

```java
public record CreateTransferRequest(
    long sourceAccountId,
    long targetAccountId,
    BigDecimal amount,
    Boolean notifyRecipient
) {
}
```

Выбор типов выражает разные свойства:

- `sourceAccountId` и `targetAccountId` — обязательные primitive values, `null` невозможен;
- `amount` — reference type, потому что точное decimal number реализовано классом;
- `notifyRecipient` — wrapper, если API различает `true`, `false` и «поле не передано».

Проверка на границе:

```java
public CreateTransferRequest {
    Objects.requireNonNull(amount, "amount must not be null");

    if (sourceAccountId <= 0 || targetAccountId <= 0) {
        throw new IllegalArgumentException("account ids must be positive");
    }
    if (amount.signum() <= 0) {
        throw new IllegalArgumentException("amount must be positive");
    }
}
```

Нормализация optional flag:

```java
boolean shouldNotify = Boolean.TRUE.equals(request.notifyRecipient());
```

Эта запись:

- возвращает `true` только для `Boolean.TRUE`;
- возвращает `false` для `false` и `null`;
- не вызывает unboxing `null`.

Такое объединение `false` и `null` допустимо, только если соответствует контракту. Если состояния семантически
различаются, их нужно обрабатывать отдельно.

---

## 19. Частые ошибки

### Ошибка 1. Ожидать дробный результат от integer division

```java
double ratio = successful / total;
```

Если оба операнда имеют целочисленные типы, дробная часть теряется до присваивания. Преобразуй один операнд:

```java
double ratio = (double) successful / total;
```

### Ошибка 2. Использовать `double` для денег

Binary floating point не хранит большинство десятичных дробей точно. Используй `BigDecimal` и явно задай правила
rounding.

### Ошибка 3. Считать cast проверкой диапазона

```java
int value = (int) longValue;
```

Cast может молча потерять старшие биты. Для проверки используй `Math.toIntExact`.

### Ошибка 4. Не учитывать integer overflow

Переполнение `int` и `long` не выбрасывает exception. Для критичных вычислений используй `Math.*Exact`, проверку границ
или `BigInteger`.

### Ошибка 5. Говорить, что Java передаёт объекты по ссылке

Java передаёт копию reference value. Метод может изменить объект, но не может переназначить переменную вызывающего кода.

### Ошибка 6. Сравнивать wrapper-типы через `==`

Wrapper cache делает баг нестабильным: маленькие значения могут выглядеть равными, большие — нет. Используй `equals`,
`Objects.equals` или безопасный unboxing.

### Ошибка 7. Допустить unboxing `null`

Арифметика, comparison и `if (nullableBoolean)` могут неявно распаковать wrapper и выбросить `NullPointerException`.

### Ошибка 8. Считать `final` глубокой immutability

`final` запрещает переназначить переменную, но mutable object по ссылке всё ещё можно менять.

### Ошибка 9. Считать `char` любым одиночным символом

`char` хранит одну UTF-16 code unit. Emoji и часть других Unicode symbols требуют двух `char`, а видимый grapheme
может состоять из нескольких code points.

### Ошибка 10. Сравнивать массивы через `equals`

У массивов `equals` проверяет identity. Для содержимого используй `Arrays.equals` или `Arrays.deepEquals`.

### Ошибка 11. Создать `BigDecimal` из `double`

```java
new BigDecimal(0.1)
```

Так в decimal object фиксируется уже существующая двоичная погрешность. Используй строку или `BigDecimal.valueOf`.

### Ошибка 12. Делать wrapper обязательным полем без причины

`Integer` или `Boolean` добавляет третье состояние `null`. Если оно не имеет domain-смысла, primitive type делает
инвариант сильнее.

---

## 20. Мини-практика

### Задание 1. Предскажи результат

Не запускай код до ответа:

```java
int total = 5;
int parts = 2;

double first = total / parts;
double second = (double) total / parts;

System.out.println(first);
System.out.println(second);
```

Объясни, на каком этапе теряется дробная часть.

### Задание 2. Найди overflow

```java
int dailyLimit = 2_000_000_000;
int days = 2;
long totalLimit = dailyLimit * days;
```

Почему `long` не спасает результат? Исправь выражение двумя способами:

1. с `long` arithmetic;
2. с обнаружением overflow через `Math.multiplyExact`.

### Задание 3. Pass-by-value

Предскажи результат:

```java
static void update(int count, List<String> operations) {
    count++;
    operations.add("deposit");
    operations = new ArrayList<>();
    operations.add("replacement");
}

int count = 0;
List<String> operations = new ArrayList<>();

update(count, operations);

System.out.println(count);
System.out.println(operations);
```

Нарисуй значения двух параметров метода сразу после вызова.

### Задание 4. Wrapper и `null`

Напиши метод:

```java
static boolean isEnabled(Boolean value)
```

Он должен возвращать `true` только для `Boolean.TRUE` и не выбрасывать `NullPointerException`.

Затем напиши второй метод, который различает все три состояния и возвращает enum:

```java
enum FlagState {
    ENABLED,
    DISABLED,
    NOT_PROVIDED
}
```

### Задание 5. Денежная сумма

Реализуй immutable `Money`, который:

- хранит `BigDecimal amount` и `Currency currency`;
- запрещает `null`;
- нормализует scale до двух знаков с выбранным rounding mode;
- складывается только с той же валютой;
- не меняет текущий объект;
- корректно определяет equality.

Объясни, должен ли `new BigDecimal("1.0")` быть равен `new BigDecimal("1.00")` внутри твоего domain type.

### Задание 6. Unicode

Для строк `"A"`, `"₽"` и `"😀"` выведи:

- `length()`;
- количество code points;
- значения `char` по индексам;
- code points в hexadecimal form.

Объясни, почему обход через `charAt(i)` не всегда равен обходу символов.

### Задание 7. Массивы

Сравни два независимых `int[]` с одинаковыми элементами через:

- `==`;
- `equals`;
- `Arrays.equals`.

Затем повтори эксперимент для `int[][]` с `Arrays.equals` и `Arrays.deepEquals`.

---

## 21. Вопросы для самопроверки

1. Какие восемь primitive types существуют в Java?
2. Является ли `String` примитивом?
3. Может ли primitive variable содержать `null`?
4. Что копируется при присваивании одного примитива другому?
5. Что копируется при присваивании одной reference variable другой?
6. Почему изменение mutable object видно через две разные ссылки?
7. Почему переназначение параметра метода не переназначает переменную вызывающего кода?
8. Что означает утверждение «Java всегда pass-by-value» для reference types?
9. Каков результат `5 / 2` и почему?
10. Почему `byte + byte` имеет тип `int`?
11. Что происходит при переполнении `int`?
12. Какие методы помогают обнаружить integer overflow?
13. Всегда ли widening conversion сохраняет значение точно?
14. Почему cast из `long` в `int` может быть опасен?
15. Почему `double` не подходит для денежных сумм?
16. Как правильно создать `BigDecimal` из десятичного литерала?
17. Почему результат `BigDecimal.add` нужно присвоить переменной?
18. Чем `BigDecimal.equals` отличается от `compareTo`?
19. Почему один emoji может занимать два `char`?
20. Чем autoboxing отличается от unboxing?
21. Где возникает `NullPointerException` при работе с `Integer`?
22. Почему `Integer` нельзя надёжно сравнивать через `==`?
23. Почему `List<int>` не компилируется?
24. Когда `Integer` уместнее `int`?
25. Делает ли `final List<T>` список immutable?
26. Как сравнить содержимое одномерных и вложенных массивов?
27. Чем declared reference type отличается от runtime type объекта?
28. Какие значения по умолчанию получают поля и элементы массива?
29. Почему локальную переменную нужно инициализировать явно?
30. Чем Java `long` отличается от JavaScript `bigint`?

## Краткий итог

- В Java восемь primitive types; все остальные обычные типы являются reference types.
- Primitive variable содержит значение, reference variable — ссылку на объект либо `null`.
- Присваивание примитива копирует значение; присваивание ссылки копирует ссылку и не создаёт объект.
- Java всегда передаёт аргументы по значению, в том числе копирует reference value.
- Integer arithmetic имеет fixed-width semantics: целочисленное деление отбрасывает дробь, а overflow обычно
  оборачивает значение без exception.
- `double` хранит двоичное приближение; для денег обычно нужен immutable `BigDecimal`.
- `char` — одна UTF-16 code unit, а не гарантированно один Unicode character.
- Wrapper types позволяют использовать generics и `null`, но добавляют autoboxing, identity traps и риск unboxing
  `null`.
- Для primitive values `==` сравнивает значения, для references — identity; value equality объектов обычно задаёт
  `equals`.
- `final` reference нельзя переназначить, но это не делает объект immutable.
