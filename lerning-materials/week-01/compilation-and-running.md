# Компиляция и запуск Java-программ

[← Вернуться к учебному плану](../java-backend-learning-plan.md)

## Что нужно понять в этой теме

После изучения главы ты должен уметь:

1. объяснить, чем компиляция `javac` отличается от запуска `java`;
2. вручную скомпилировать один или несколько классов;
3. запустить класс из package, правильно указав classpath;
4. передать JVM-опции, system properties и аргументы приложения;
5. собрать и запустить исполняемый JAR;
6. отличить ошибку компиляции от ошибки загрузки класса и ошибки выполнения;
7. понять, какие из этих действий скрывают Gradle, Maven и IntelliJ IDEA.

Связанные понятия `JDK`, `JVM`, bytecode и classpath подробно разобраны в главе
[JDK, JVM, bytecode, classpath](../jdk-jvm-bytecode-classpath.md). Здесь фокус
сделан на командах и практической модели работы.

## Ментальная модель

Обычный путь Java-программы состоит из двух явно разделённых этапов:

```text
исходный код                 результат компиляции             выполнение

src/com/example/Main.java    out/com/example/Main.class
            │                            │
            │ javac                      │ java запускает JVM
            ▼                            ▼
      проверка типов  ──────────►  JVM bytecode  ──────────►  работающий процесс
```

Для TypeScript-разработчика приблизительная аналогия выглядит так:

| Java                         | Приблизительная аналогия в TypeScript/Node.js                                      |
|---                           |---                                                                                 |
| `javac`                      | `tsc`                                                                              |
| `java`                       | `node`                                                                             |
| `.class`                     | результат сборки, но в виде бинарного bytecode, а не текстового `.js`              |
| полное имя `com.demo.Main`   | имя модуля вместе с его namespace; прямого эквивалента в Node.js нет               |
| classpath                    | пути поиска кода и зависимостей; отдалённо напоминает module resolution            |
| JVM-опции                    | опции runtime наподобие `node --max-old-space-size=...`                            |
| аргументы после имени класса | аргументы после имени JS-файла, доступные приложению через `process.argv`          |

Аналогия не полная. `tsc` обычно превращает TypeScript в JavaScript, который
остаётся исходным текстом для V8. `javac` создаёт бинарный bytecode с описанием
классов, полей, методов и инструкциями для JVM.

## Минимальная программа

Создадим файл `src/com/example/Main.java`:

```java
package com.example;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, Java!");
    }
}
```

Структура каталогов:

```text
hello-java/
└── src/
    └── com/
        └── example/
            └── Main.java
```

Первая строка объявляет package:

```java
package com.example;
```

Полное имя класса — `com.example.Main`. Именно это имя, а не путь к
`Main.class`, передаётся команде `java`.

## Компиляция с помощью `javac`

Из корня `hello-java` выполним:

```shell
javac -d out src/com/example/Main.java
```

Здесь:

- `javac` — компилятор из JDK;
- `-d out` — каталог, в который нужно записать результат;
- `src/com/example/Main.java` — компилируемый исходный файл.

После компиляции получится:

```text
hello-java/
├── out/
│   └── com/
│       └── example/
│           └── Main.class
└── src/
    └── com/
        └── example/
            └── Main.java
```

`javac` сам создаёт внутри `out` каталоги, соответствующие package. Поэтому
лучше использовать `-d`, а не складывать `.class` рядом с исходниками.

### Что делает компилятор

Упрощённо `javac`:

1. читает и разбирает исходный код;
2. строит синтаксическое и семантическое представление программы;
3. разрешает имена типов, полей и методов;
4. проверяет типы и правила языка;
5. при необходимости применяет обработчики аннотаций;
6. генерирует `.class`-файлы с bytecode и метаданными.

Компилятор не запускает `main`. Следующая программа может успешно
скомпилироваться и упасть только при выполнении:

```java
public class Main {
    public static void main(String[] args) {
        int divisor = args.length;
        System.out.println(10 / divisor);
    }
}
```

Для `javac` деление корректно по типам. Если запустить программу без аргументов,
во время выполнения возникнет `ArithmeticException`.

### Полезные опции `javac`

```shell
javac --release 21 -encoding UTF-8 -Xlint:all -d out src/com/example/Main.java
```

| Опция                    | Назначение                                                                          |
|---                       |---                                                                                 |
| `-d out`                 | записать `.class`-файлы в `out`                                                     |
| `--release 21`           | компилировать для API и формата class-файлов указанной версии Java                  |
| `-encoding UTF-8`        | явно задать кодировку исходных файлов                                               |
| `-Xlint:all`             | включить рекомендуемые предупреждения компилятора                                   |
| `-cp path`               | указать compile classpath с уже скомпилированными классами и JAR-зависимостями      |
| `-parameters`            | сохранить в bytecode имена параметров методов для reflection                       |
| `-g`                     | добавить отладочную информацию; обычно она и так генерируется                       |

`--release 21` надёжнее, чем сочетание старых `-source` и `-target`: оно
ограничивает не только синтаксис и версию bytecode, но и доступный API целевой
Java-платформы.

Предупреждение — не всегда ошибка. Например, unchecked-операция может дать
warning, но компиляция продолжится. В enterprise-проектах сборку иногда
настраивают так, чтобы отдельные предупреждения считались ошибками.

## Запуск с помощью `java`

Скомпилированный класс запускается так:

```shell
java -cp out com.example.Main
```

Результат:

```text
Hello, Java!
```

Разберём команду:

- `java` создаёт новый процесс JVM;
- `-cp out` задаёт корень, относительно которого JVM ищет классы;
- `com.example.Main` — полное имя стартового класса;
- JVM ищет файл `com/example/Main.class` внутри `out`;
- затем JVM находит и вызывает метод `main`.

Запускать так неправильно:

```shell
java out/com/example/Main.class
```

В обычном class mode команда `java` ожидает бинарное имя класса, а не путь к
`.class`-файлу.

### Почему classpath равен `out`, а не `out/com/example`

JVM преобразует полное имя:

```text
com.example.Main
```

в относительный путь:

```text
com/example/Main.class
```

Значит, в classpath должен лежать каталог, внутри которого начинается `com`.
Для файла `out/com/example/Main.class` таким корнем является `out`.

## Точка входа `main`

Классический метод запуска:

```java
public static void main(String[] args) {
    // ...
}
```

Значение каждой части:

- `public` — launcher должен иметь доступ к методу;
- `static` — метод вызывается без создания экземпляра `Main`;
- `void` — метод не возвращает код завершения;
- `main` — специальное имя, которое ищет launcher;
- `String[] args` — аргументы командной строки приложения.

Эквивалентная запись с varargs:

```java
public static void main(String... args) {
    // ...
}
```

Если сигнатура неверна, класс может скомпилироваться, но launcher не найдёт
точку входа. Например, это обычный instance-метод, а не entry point:

```java
public void main(String[] args) {
    // ...
}
```

### Код завершения процесса

Нормальное завершение `main` даёт процессу exit code `0`. Необработанное
исключение обычно приводит к ненулевому коду и печати stack trace в standard
error.

Явно завершить процесс можно так:

```java
System.exit(2);
```

Для обычного управления потоком внутри бизнес-кода `System.exit` использовать
не стоит: он завершает всю JVM, а не только текущий метод или поток.

## Аргументы приложения, JVM-опции и system properties

Порядок частей команды имеет значение:

```text
java [опции JVM] [способ запуска и entry point] [аргументы приложения]
```

Пример:

```shell
java -Xms128m -Xmx512m -Dapp.mode=dev -cp out com.example.Main wallet-42 --verbose
```

Здесь:

| Фрагмент                    | Кто его обрабатывает                                                              |
|---                          |---                                                                               |
| `-Xms128m`                  | JVM: начальный размер heap                                                        |
| `-Xmx512m`                  | JVM: максимальный размер heap                                                     |
| `-Dapp.mode=dev`            | JVM создаёт system property `app.mode`                                            |
| `-cp out`                   | launcher/JVM: classpath приложения                                                |
| `com.example.Main`          | launcher: стартовый класс                                                         |
| `wallet-42 --verbose`       | приложение: элементы массива `args`                                               |

Прочитать значения можно так:

```java
package com.example;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        String mode = System.getProperty("app.mode", "prod");

        System.out.println("mode = " + mode);
        System.out.println("args = " + Arrays.toString(args));
    }
}
```

Важно не путать:

- `-Dapp.mode=dev` — system property внутри конкретной JVM;
- переменную окружения можно прочитать через `System.getenv("APP_MODE")`;
- `--verbose` после имени класса — обычная строка в `args`; Java сама не
  превращает её во флаг и не парсит CLI.

Если поставить `-Xmx512m` после имени класса, оно уже не будет JVM-опцией, а
попадёт приложению в `args`.

## Компиляция нескольких классов

Пусть есть два файла:

```text
src/
└── com/
    └── example/
        ├── Main.java
        └── GreetingService.java
```

`GreetingService.java`:

```java
package com.example;

public class GreetingService {
    public String greet(String name) {
        return "Hello, " + name + "!";
    }
}
```

`Main.java`:

```java
package com.example;

public class Main {
    public static void main(String[] args) {
        GreetingService service = new GreetingService();
        String name = args.length == 0 ? "Java" : args[0];

        System.out.println(service.greet(name));
    }
}
```

Можно явно передать оба файла:

```shell
javac -d out src/com/example/Main.java src/com/example/GreetingService.java
java -cp out com.example.Main Codex
```

PowerShell-вариант для компиляции всех исходников:

```powershell
$sources = Get-ChildItem -Path src -Recurse -Filter *.java
javac -d out $sources.FullName
```

Bash-вариант:

```bash
javac -d out $(find src -name "*.java")
```

Для настоящего проекта перечислением исходников и classpath занимается build
tool. Ручные команды полезны, чтобы понять модель и диагностировать проблемы,
но не должны заменять Gradle или Maven в повседневной разработке.

## Compile classpath и runtime classpath

Допустим, `Main` использует класс из `lib/example.jar`.

Компиляция на Windows:

```shell
javac -cp "lib/example.jar" -d out src/com/example/Main.java
```

Запуск на Windows:

```shell
java -cp "out;lib/example.jar" com.example.Main
```

На Linux и macOS разделителем элементов classpath будет `:`:

```shell
java -cp "out:lib/example.jar" com.example.Main
```

Зависимость должна быть доступна на обоих этапах:

```text
исходный код
    │
    │ javac использует compile classpath
    ▼
bytecode
    │
    │ JVM использует runtime classpath
    ▼
работающее приложение
```

Если JAR был доступен при компиляции, но отсутствует при запуске, компиляция
пройдёт успешно, а приложение может упасть с `NoClassDefFoundError`.

## Запуск исходного файла без отдельного `javac`

Для небольшого самостоятельного файла можно использовать source-file mode:

```shell
java Hello.java
```

Например:

```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello!");
    }
}
```

Команда `java Hello.java` компилирует исходник в памяти и сразу запускает его.
Отдельный `.class` рядом с файлом не создаётся.

Это удобно для:

- экспериментов;
- коротких учебных примеров;
- небольших скриптоподобных программ.

Это не отменяет обычную сборку проекта. Source-file mode не следует мысленно
смешивать с запуском скомпилированного класса:

```shell
java Hello.java   # source-file mode: передаём путь к исходнику
java Hello        # class mode: передаём имя уже скомпилированного класса
```

## Сборка и запуск JAR

JAR — ZIP-архив с `.class`-файлами, ресурсами и метаданными в `META-INF`.

### Обычный JAR

После компиляции:

```shell
javac -d out src/com/example/Main.java
jar --create --file app.jar -C out .
```

Посмотреть содержимое:

```shell
jar --list --file app.jar
```

Класс из обычного JAR можно запустить через classpath:

```shell
java -cp app.jar com.example.Main
```

### Исполняемый JAR

Чтобы запускать JAR без явного имени класса, нужно записать `Main-Class` в
manifest:

```shell
jar --create --file app.jar --main-class com.example.Main -C out .
java -jar app.jar
```

Концептуально внутри архива появятся:

```text
META-INF/MANIFEST.MF
com/example/Main.class
```

Manifest будет содержать entry point:

```text
Main-Class: com.example.Main
```

Аргументы приложения передаются после JAR:

```shell
java -Xmx512m -Dapp.mode=dev -jar app.jar wallet-42
```

При `java -jar app.jar` параметр `-cp` не используют для произвольного
добавления зависимостей: launcher строит classpath по правилам запуска JAR и
его manifest. На практике Gradle, Maven или Spring Boot plugin собирают
дистрибутив, manifest либо специальный executable/fat JAR.

## Что происходит при запуске JVM

После команды:

```shell
java -cp out com.example.Main
```

упрощённая последовательность такая:

1. операционная система запускает Java launcher;
2. создаётся процесс JVM и настраивается runtime;
3. JVM находит стартовый класс через classpath;
4. class loader загружает его bytecode;
5. JVM проверяет и связывает класс;
6. выполняется статическая инициализация класса;
7. launcher вызывает `main`;
8. JVM интерпретирует bytecode и JIT-компилирует горячие участки;
9. процесс живёт, пока работают non-daemon threads или пока его не завершат.

Из завершения `main` не всегда следует немедленное завершение приложения. Если
был создан работающий non-daemon thread, JVM продолжит жить:

```java
public class Main {
    public static void main(String[] args) {
        Thread worker = new Thread(() -> {
            while (true) {
                // фоновая работа
            }
        });

        worker.start();
        System.out.println("main завершён, но JVM ещё работает");
    }
}
```

Это важно для backend-приложений: серверные потоки продолжают принимать
запросы после завершения стартового метода Spring Boot.

## Три разных времени возникновения ошибок

### 1. Ошибка компиляции

```java
int amount = "100";
```

`javac` обнаружит несовместимые типы, `.class` не будет создан:

```text
error: incompatible types: String cannot be converted to int
```

Исправлять нужно исходный код или compile classpath.

### 2. Ошибка загрузки или связывания

```text
Error: Could not find or load main class com.example.Main
```

Код мог быть корректным, но launcher не нашёл стартовый класс. Проверяем:

- создан ли `out/com/example/Main.class`;
- указан ли `-cp out`;
- совпадает ли package с каталогами и полным именем;
- запускается ли `com.example.Main`, а не просто `Main`.

Другие ошибки этого этапа:

- `UnsupportedClassVersionError` — bytecode новее, чем запускающая JVM;
- `NoClassDefFoundError` — требуемого класса нет в runtime classpath либо его
  инициализация ранее завершилась ошибкой;
- `NoSuchMethodError` — во время выполнения загружена несовместимая версия
  класса.

### 3. Ошибка выполнения

```text
Exception in thread "main" java.lang.ArithmeticException: / by zero
    at com.example.Main.main(Main.java:6)
```

Класс найден и `main` уже выполнялся. Нужно читать exception, message и stack
trace, начиная с мест собственного кода.

Такое разделение ускоряет диагностику:

```text
javac не создал class?          → compile-time проблема
java не нашла/не связала class? → classpath или совместимость
main начал выполняться и упал?  → runtime проблема
```

## Частые ловушки

### Запускается не тот класс

Вместо:

```shell
java -cp out Main
```

для `package com.example` нужно:

```shell
java -cp out com.example.Main
```

### В classpath указан package, а не его корень

Неправильно:

```shell
java -cp out/com/example com.example.Main
```

Правильно:

```shell
java -cp out com.example.Main
```

### Опция JVM стоит после entry point

```shell
java -cp out com.example.Main -Xmx512m
```

Здесь `-Xmx512m` — аргумент приложения. JVM-опции нужно ставить раньше:

```shell
java -Xmx512m -cp out com.example.Main
```

### Версии `java` и `javac` различаются

Проверка:

```shell
java -version
javac -version
```

Также полезно проверить, откуда взяты команды.

PowerShell:

```powershell
Get-Command java
Get-Command javac
```

Bash:

```bash
which java
which javac
```

IDE может использовать один JDK, Gradle daemon — другой, а терминал — третий.
Из-за этого проект собирается в одном месте и падает с
`UnsupportedClassVersionError` в другом.

### Старые `.class` маскируют проблему

После изменения package, сигнатур или структуры проекта в каталоге сборки
могут остаться старые файлы. Поэтому build tools имеют задачу `clean`.
Удалять каталог результата вручную безопасно только когда точно известно, что
он содержит исключительно генерируемые артефакты.

## Где здесь Gradle, Maven и IntelliJ IDEA

В реальном backend-проекте обычно не вызывают `javac` для каждого файла
вручную.

Build tool:

- находит исходники по соглашениям проекта;
- формирует compile и runtime classpath;
- скачивает зависимости;
- вызывает Java compiler;
- копирует ресурсы;
- запускает тесты;
- собирает JAR;
- обеспечивает воспроизводимую сборку.

Типичные команды:

```shell
./gradlew compileJava
./gradlew test
./gradlew build
```

или:

```shell
./mvnw compile
./mvnw test
./mvnw package
```

IntelliJ IDEA выполняет похожие действия через конфигурацию проекта и Run
Configuration. Поля `Main class`, `VM options`, `Program arguments`,
`Environment variables` и classpath соответствуют частям уже разобранной
команды:

```text
java [VM options] -cp [classpath] [Main class] [Program arguments]
```

Понимание ручного запуска помогает читать Run Configuration, CI-команду,
Docker `ENTRYPOINT`, параметры Kubernetes-контейнера и production-логи.

## Практическое задание

### Шаг 1. Один класс

Создай `src/com/example/Main.java`, скомпилируй его в `out` и запусти:

```shell
javac --release 21 -encoding UTF-8 -Xlint:all -d out src/com/example/Main.java
java -cp out com.example.Main
```

### Шаг 2. Аргументы и настройки

Сделай так, чтобы программа:

- принимала идентификатор кошелька первым аргументом;
- читала `app.mode` из system property;
- читала `APP_REGION` из environment variable;
- печатала все три значения.

Пример запуска в PowerShell:

```powershell
$env:APP_REGION = "local"
java -Dapp.mode=dev -cp out com.example.Main wallet-42
```

### Шаг 3. Несколько классов

Вынеси форматирование сообщения в `WalletGreetingService`. Скомпилируй оба
файла и проверь структуру `out`.

### Шаг 4. Исполняемый JAR

Собери JAR с `Main-Class`:

```shell
jar --create --file wallet-cli.jar --main-class com.example.Main -C out .
java -Dapp.mode=dev -jar wallet-cli.jar wallet-42
```

### Шаг 5. Намеренно сломай запуск

Последовательно воспроизведи и объясни:

1. compile error из-за несовместимых типов;
2. `Could not find or load main class` из-за неверного `-cp`;
3. runtime exception внутри `main`;
4. JVM-опцию, случайно переданную после имени класса.

Цель — не просто увидеть ошибки, а научиться по моменту их появления выбирать
правильное направление диагностики.

## Что важно запомнить

1. `javac` компилирует `.java` в `.class`; `java` запускает JVM и выполняет
   скомпилированный код.
2. `-d` задаёт корень каталога с результатами компиляции.
3. JVM запускает класс по полному имени, например `com.example.Main`.
4. Classpath содержит корни поиска, а не путь непосредственно к стартовому
   `.class`.
5. JVM-опции стоят до entry point, аргументы приложения — после него.
6. Compile classpath и runtime classpath решают разные задачи.
7. `java File.java` — удобный source-file mode, но не замена сборке проекта.
8. Исполняемый JAR содержит `Main-Class` в manifest и запускается через
   `java -jar`.
9. Ошибки компиляции, загрузки/связывания и выполнения нужно диагностировать
   по-разному.
10. Gradle, Maven и IDE автоматизируют те же базовые этапы, а не заменяют
    модель выполнения Java.

## Самопроверка

Попробуй ответить без подсказки:

1. Почему `javac` и `java` — две разные команды?
2. Какой файл JVM будет искать для имени `com.example.Main`?
3. Почему для `out/com/example/Main.class` нужно указать `-cp out`?
4. Чем `java Main.java` отличается от `java Main`?
5. Что попадёт в `args` в команде
   `java -Xmx512m -cp out com.example.Main one two`?
6. Что произойдёт, если `-Xmx512m` поставить после `com.example.Main`?
7. Чем system property отличается от environment variable?
8. Почему успешная компиляция не гарантирует успешный запуск?
9. В чём разница между compile classpath и runtime classpath?
10. Зачем нужен `Main-Class` в manifest?
11. Почему JVM может продолжить работать после завершения `main`?
12. Какие три группы ошибок нужно различать при диагностике?

[← Вернуться к учебному плану](../java-backend-learning-plan.md)
