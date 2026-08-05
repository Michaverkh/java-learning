# Структура Java-проекта

[← Вернуться к учебному плану](../java-backend-learning-plan.md)

## Что нужно понять в этой теме

После изучения главы ты должен уметь:

1. отличать структуру репозитория от структуры Java packages;
2. находить production-код, тесты, ресурсы, зависимости и точку входа;
3. объяснять стандартную структуру Maven-проекта;
4. понимать, откуда Maven берёт исходники и куда складывает результаты;
5. отличать обычный односоставный проект от multi-module build;
6. ориентироваться в структуре Spring Boot-приложения;
7. быстро исследовать незнакомый enterprise-репозиторий.

Главная идея: у Java-проекта нет единственной структуры, заданной самим языком.
Нужно различать несколько уровней:

```text
репозиторий
├── build-модель: Maven, модули, зависимости
├── наборы исходников: main, test и дополнительные каталоги
├── packages: логические пространства имён Java
└── артефакты сборки: .class, JAR, отчёты, сгенерированный код
```

Язык Java определяет классы и packages, но каталоги `src/main/java`,
`src/test/java` и `src/main/resources` — это соглашения Maven. Они стали
стандартом де-факто благодаря принципу convention over configuration.

## Минимальный проект без build tool

Для ручной компиляции достаточно такой структуры:

```text
hello-java/
├── src/
│   └── com/
│       └── example/
│           ├── GreetingService.java
│           └── Main.java
└── out/
```

`Main.java`:

```java
package com.example;

public class Main {
    public static void main(String[] args) {
        GreetingService service = new GreetingService();
        System.out.println(service.greet("Java"));
    }
}
```

Компиляция и запуск:

```shell
javac -d out src/com/example/GreetingService.java src/com/example/Main.java
java -cp out com.example.Main
```

Здесь имена `src` и `out` выбраны разработчиком. Для `javac` они не являются
специальными: пути явно переданы в командной строке.

С ростом проекта вручную приходится решать всё больше задач:

- перечислять исходные файлы;
- составлять compile и runtime classpath;
- отдельно компилировать тесты;
- копировать ресурсы;
- скачивать и обновлять зависимости;
- собирать JAR;
- запускать проверки и генерировать отчёты.

Именно поэтому production-проекты используют build tools. В этом учебном
проекте используется только Maven: примеры и задания не дублируются для
другого build tool.

## Стандартная структура Maven-проекта

Типичный односоставный проект выглядит так:

```text
wallet-service/
├── .gitignore
├── README.md
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/
│   └── wrapper/
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── example/
    │   │           └── wallet/
    │   │               ├── Main.java
    │   │               └── WalletService.java
    │   └── resources/
    │       ├── application.yml
    │       └── db/
    │           └── migration/
    │               └── V1__create_wallet.sql
    └── test/
        ├── java/
        │   └── com/
        │       └── example/
        │           └── wallet/
        │               └── WalletServiceTest.java
        └── resources/
            └── application-test.yml
```

### Назначение основных путей

| Путь                         | Назначение                                                                         |
|---                           |---                                                                                 |
| `src/main/java`              | production Java-код                                                                |
| `src/main/resources`         | ресурсы production-приложения                                                      |
| `src/test/java`              | тестовый Java-код                                                                  |
| `src/test/resources`         | ресурсы, доступные при запуске тестов                                               |
| `pom.xml`                    | Maven-модель проекта, зависимости и plugins                                        |
| `mvnw`, `.mvn/wrapper`       | Maven Wrapper                                                                      |
| `target`                     | стандартный каталог результатов Maven                                              |

## Почему путь повторяет package

Файл:

```text
src/main/java/com/example/wallet/WalletService.java
```

обычно начинается так:

```java
package com.example.wallet;
```

Корень исходников — `src/main/java`. Часть пути после этого корня соответствует
package:

```text
корень исходников   package path
src/main/java/      com/example/wallet/WalletService.java
                    └──────┬─────────┘
                      com.example.wallet
```

У класса есть полное, или binary, имя:

```text
com.example.wallet.WalletService
```

Важно различать:

- `src/main/java` не входит в package;
- `com.example.wallet` — package, а не просто папка для группировки;
- `WalletService` — имя класса;
- полный путь файла и полное имя класса связаны через source root.

Компилятор технически может прочитать исходник из неожиданного пути, если файл
передан явно. Но стандартное соответствие пути и package необходимо для IDE,
build tools, навигации и нормальной работы команды.

### Именование packages

Обычно package начинается с перевёрнутого доменного имени организации:

```text
com.example.wallet
ru.company.payments
```

Далее идут имя продукта, bounded context или feature:

```text
com.example.wallet.account
com.example.wallet.transfer
com.example.wallet.ledger
```

Имена packages пишутся строчными буквами. Дефисы использовать нельзя.

Подробно правила доступа между классами будут разобраны в теме «Пакеты и
visibility». Здесь достаточно помнить: package влияет не только на путь, но и
на полное имя класса и package-private/protected доступ.

## Наборы main и test: не просто каталоги

Maven разделяет основной и тестовый код на стандартные наборы каталогов.
Термин **source set** можно встретить в общей литературе о сборке, но для
работы с Maven важнее понимать его compile и test classpath, стандартные
каталоги и lifecycle phases.

У каждого набора есть:

- Java-исходники;
- ресурсы;
- compile classpath;
- runtime classpath;
- отдельный каталог скомпилированных результатов.

Упрощённая модель:

```text
src/main/java ────────► main classes ──┐
src/main/resources ───► main resources ├──► production JAR
                                       │
src/test/java ─────────► test classes  ─┤──► test runtime
src/test/resources ────► test resources┘
```

Тестовый код обычно видит:

- скомпилированный `main`;
- main dependencies;
- test dependencies;
- test resources.

Production-код не должен зависеть от тестового кода. Поэтому класс из
`src/main/java` не может просто импортировать helper из `src/test/java`.

### Дополнительные каталоги исходников

В enterprise-проекте могут встречаться:

```text
src/integrationTest/java
src/integrationTest/resources
src/contractTest/java
src/testFixtures/java
```

Эти имена не имеют стандартного значения для Maven. Их может подключать plugin
в `pom.xml`, например Build Helper Maven Plugin. Увидев нестандартный каталог,
нужно проверить effective POM и конфигурацию plugins, а не угадывать его
classpath и lifecycle.

## Production-код и тесты

Тест часто помещают в тот же package, что и проверяемый класс:

```text
src/main/java/com/example/wallet/Money.java
src/test/java/com/example/wallet/MoneyTest.java
```

Одинаковый package не означает одинаковый набор исходников. Файлы
компилируются в разные каталоги и имеют разные classpath.

Такое расположение позволяет тесту проверять package-private поведение. Но
тестировать детали реализации только ради покрытия не стоит: основной
контракт обычно лучше проверять через public API.

Пример:

```java
package com.example.wallet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WalletServiceTest {
    @Test
    void returnsCurrentBalance() {
        WalletService service = new WalletService();

        assertEquals("0.00", service.balance().toPlainString());
    }
}
```

Тестовый класс и метод в JUnit 5 не обязаны быть `public`.

## Ресурсы

Ресурс — файл, который нужен приложению во время выполнения, но не
компилируется `javac`:

```text
src/main/resources/
├── application.yml
├── messages.properties
├── templates/
│   └── receipt.html
└── db/
    └── migration/
        └── V1__init.sql
```

Maven Resources Plugin копирует ресурсы в output, а Maven JAR Plugin включает
main resources в JAR. Их затем ищут через classpath.

Например:

```java
try (var input = Main.class.getResourceAsStream("/messages.properties")) {
    // чтение ресурса
}
```

Важное различие:

```text
classpath resource:  /messages.properties
filesystem path:     C:\app\config\messages.properties
```

Ресурс внутри JAR может не быть обычным файлом файловой системы. Поэтому код,
который превращает каждый classpath resource в `File`, часто ломается после
упаковки приложения.

`src/test/resources` добавляется в test runtime, но не должен попадать в
production JAR. Там удобно хранить:

- тестовые конфигурации;
- JSON/CSV fixtures;
- SQL для тестовых сценариев;
- ожидаемые ответы.

Секреты, реальные пароли и токены нельзя коммитить ни в main, ни в test
resources.

## Результаты сборки

Maven складывает генерируемые файлы в `target/`.

Упрощённый пример:

```text
target/
├── classes/
├── test-classes/
├── surefire-reports/
└── wallet-service.jar
```

Точная структура зависит от Maven plugins и их версий. Содержимое `target`:

- генерируются повторно;
- обычно добавляются в `.gitignore`;
- не являются местом для ручного редактирования;
- могут безопасно исчезнуть при `clean`.

То же относится к IDE outputs вроде `out/`. Если исправление внесено только в
сгенерированный файл, следующая сборка его перезапишет.

## Generated sources

Некоторые инструменты создают Java-код:

- OpenAPI Generator;
- protobuf/gRPC;
- jOOQ;
- annotation processors;
- генераторы metamodel.

Сгенерированный код может находиться, например, здесь:

```text
target/generated-sources/...
```

Конфигурация Maven plugin в `pom.xml` должна зарегистрировать этот путь как
source directory и связать генерацию с нужной lifecycle phase.

Правила работы:

1. сначала найти генератор и его входные данные;
2. не исправлять generated output вручную;
3. менять OpenAPI/schema/template или настройку генератора;
4. выяснить по правилам конкретного репозитория, коммитится ли output.

Чаще build outputs не коммитят, но у некоторых проектов generated code
намеренно версионируется. Универсального правила здесь нет.

## Точка входа

В обычном Java-приложении ищи:

```java
public static void main(String[] args) {
    // ...
}
```

В Spring Boot:

```java
package com.example.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WalletApplication {
    public static void main(String[] args) {
        SpringApplication.run(WalletApplication.class, args);
    }
}
```

Точка входа объясняет, как запускается процесс, но не обязательно показывает
бизнес-flow. В Spring после `SpringApplication.run` управление переходит к
framework: он создаёт application context, находит beans и запускает
встроенный web server.

У библиотеки `main` может отсутствовать. Её результатом является JAR,
предназначенный для подключения другим приложением.

## Структура Spring Boot-приложения

Небольшие учебные примеры часто группируют код по техническим слоям:

```text
com.example.wallet
├── controller
├── service
├── repository
├── dto
├── entity
└── config
```

Это **package-by-layer**. Плюс — структура сразу знакома. Минус — одна feature
размазана по всему дереву, а packages со временем становятся большими.

Для растущего приложения обычно удобнее группировка по feature:

```text
com.example.wallet
├── WalletApplication.java
├── account
│   ├── AccountController.java
│   ├── AccountService.java
│   ├── AccountRepository.java
│   └── Account.java
├── transfer
│   ├── TransferController.java
│   ├── TransferService.java
│   ├── TransferRepository.java
│   └── Transfer.java
└── ledger
    ├── LedgerService.java
    └── LedgerRepository.java
```

Это **package-by-feature**. Внутри большой feature можно выделить свои
подпакеты:

```text
transfer/
├── api/
├── application/
├── domain/
└── infrastructure/
```

Выбор структуры — архитектурное решение, а не требование Spring. Для
enterprise-кода важнее:

- ясные границы ответственности;
- контролируемое направление зависимостей;
- отсутствие общего пакета `util`, превращённого в свалку;
- близость изменяемого вместе кода;
- возможность понять feature без обхода всего репозитория.

### Где размещать стартовый класс Spring Boot

`@SpringBootApplication` включает component scanning своего package и
подпакетов. Поэтому стартовый класс обычно размещают в верхнем package:

```text
com.example.wallet.WalletApplication
com.example.wallet.account...
com.example.wallet.transfer...
```

Если положить его в `com.example.wallet.bootstrap`, соседний package
`com.example.wallet.account` не будет автоматически находиться ниже него.
Проблему можно настроить явно, но правильное размещение root-класса обычно
проще.

## Модуль, package и JAR — разные вещи

Слово «модуль» в Java-коде перегружено:

| Понятие                         | Что это                                                                            |
|---                              |---                                                                                 |
| Java package                    | пространство имён классов и граница части правил visibility                        |
| Maven module                    | проект с собственным `pom.xml` внутри reactor build                                |
| JPMS module                     | модуль Java Platform Module System с `module-info.java`                            |
| JAR                             | архив классов и ресурсов; артефакт сборки                                           |
| архитектурный модуль            | логическая граница приложения; может контролироваться только соглашениями          |

Один Maven module обычно создаёт один основной JAR, но это не закон языка.
Внутри JAR может быть много packages. JPMS module тоже не равен автоматически
Maven-модулю, хотя проекты часто стараются сопоставить их.

## Multi-module project

Когда части системы требуют отдельных build boundaries, структура может быть
такой:

```text
wallet/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/
│   └── wrapper/
├── wallet-domain/
│   ├── pom.xml
│   └── src/
│       ├── main/java/
│       └── test/java/
├── wallet-application/
│   ├── pom.xml
│   └── src/
│       ├── main/java/
│       └── test/java/
└── wallet-api/
    ├── pom.xml
    └── src/
        ├── main/java/
        └── test/java/
```

Корневой aggregator/parent `pom.xml` перечисляет дочерние modules и хранит
общую конфигурацию. Каждый module имеет свой `pom.xml`, исходники, зависимости,
тесты и outputs.

Не стоит создавать много build-модулей только ради красивого дерева:

- усложняется конфигурация;
- появляется больше межмодульных зависимостей;
- сборка и публикация требуют дополнительных решений;
- циклические зависимости становятся build-ошибками.

Package — дешёвая логическая группировка. Отдельный build module — более
сильная физическая граница.

## Сопоставление с React/TypeScript-проектом

| Java/Maven                    | Приблизительная аналогия в TypeScript/Node.js                                         |
|---                            |---                                                                                    |
| `src/main/java`               | `src` production-приложения                                                           |
| `src/test/java`               | тесты в `test`, `tests` или рядом с исходниками                                        |
| `src/main/resources`          | assets/config/templates, копируемые сборщиком                                          |
| package                       | namespace плюс часть правил доступа; не равен ES module                               |
| import Java-типа              | named import, но разрешение и runtime-модель существенно отличаются                    |
| `pom.xml`                     | часть ролей `package.json`, lock/config build pipeline                                 |
| Maven module                  | workspace package в monorepo                                                           |
| `target`                      | `dist`/`build`                                                                         |
| JAR                           | публикуемый package или bundle по назначению, но бинарного Java-формата                 |
| набор main/test               | отдельная группа исходников со своим classpath и этапами Maven lifecycle                  |
| classpath                     | пути разрешения runtime/compile dependencies, но не алгоритм Node.js module resolution  |

Главное отличие от типичного frontend-проекта: tests в стандартной Java-модели
физически отделены от main source tree, а resources копируются в classpath
рядом с `.class`. Package объявляется внутри исходника и является частью
полного имени типа.

## Что обычно хранится в корне репозитория

Кроме build-файлов, в enterprise-проекте часто есть:

```text
.
├── .github/                    # GitHub Actions и настройки GitHub
├── .gitlab-ci.yml              # GitLab CI
├── config/                     # конфигурация quality tools
├── docker/                     # вспомогательные container-файлы
├── docs/                       # документация и ADR
├── .mvn/                       # Maven Wrapper и project-level настройки Maven
├── docker-compose.yml
├── Dockerfile
├── README.md
└── CODEOWNERS
```

Названия не стандартизированы. README и CI-конфигурация часто точнее
показывают настоящий способ сборки, чем предположения по дереву каталогов.

Файлы IDE вроде `.idea/`, `*.iml`, `.classpath` и `.project` могут быть
полностью или частично исключены из Git — это решение команды. Они не должны
быть единственным источником build-конфигурации.

## Как Maven видит стандартную структуру

При стандартной конфигурации команды концептуально делают следующее:

```text
compile main:
    src/main/java + main compile dependencies
        → target/classes

process resources:
    src/main/resources
        → target/classes

compile test:
    src/test/java + main output + test dependencies
        → target/test-classes

run test:
    test classes + main classes + resources + test runtime dependencies
        → reports and test results

package:
    main classes + main resources
        → JAR
```

Точные задачи и фазы разобраны в главе
[Maven lifecycle](maven-lifecycle.md).

## Типичные ошибки

### Считать любой каталог внутри `src` package

`main`, `java` и `test` обычно не входят в package. Package path начинается
после настроенного source root.

### Положить Java-файл прямо в `src/main`

Maven Compiler Plugin по умолчанию компилирует `.java` из `src/main/java`.
Файл в другом месте может быть невидим сборке, даже если IDE позволяет его
открыть.

### Не синхронизировать package и путь

После перемещения файла нужно обновить `package` и imports. Безопаснее
использовать IDE refactoring, особенно если класс широко используется.

### Импортировать production-код из test tree

Направление видимости обратное: tests зависят от main. Общий production-код
должен находиться в main, а переиспользуемые тестовые helpers — в test fixtures
или специальном test-support module.

### Редактировать `target/` или generated sources

Изменение исчезнет при следующей генерации или `clean`. Нужно найти настоящий
source либо вход генератора.

### Полагаться на текущую рабочую директорию для ресурсов

Путь `src/main/resources/file.txt` существует в checkout, но обычно отсутствует
в установленном приложении. Runtime-код должен понимать, читает ли он
classpath resource или внешний файл.

### Считать package архитектурной защитой

Package помогает организовать код и ограничить часть доступа, но public-классы
в соседних packages всё ещё можно связать как угодно. Сильные границы требуют
дисциплины, архитектурных тестов, build modules или JPMS — в зависимости от
задачи.

### Делить приложение на модули слишком рано

Много маленьких Maven-модулей не гарантируют хорошую архитектуру.
Сначала нужно понять области ответственности и направление зависимостей.

### Искать только `Main.java`

Приложение может запускаться через Spring Boot class, plugin, test, application
server или container entrypoint. Библиотека вообще не обязана иметь `main`.

## Как исследовать незнакомый Java-репозиторий

Двигайся снаружи внутрь:

1. Прочитай `README`, `CONTRIBUTING` и локальные инструкции для разработчика.
2. Найди Maven Wrapper и корневой `pom.xml`.
3. Найди root build и список Maven modules.
4. Установи используемые версии Java и Maven.
5. Найди `src/main`, `src/test` и нестандартные каталоги исходников.
6. Найди entry point, но не считай его центром всей логики.
7. Посмотри main/test dependencies и plugins.
8. Найди конфигурацию, migrations, generated sources и integration tests.
9. Сверь локальные команды с CI pipeline.
10. Только после этого прослеживай конкретный business-flow.

Полезные команды:

```powershell
# обзор файлов верхнего уровня
Get-ChildItem -Force

# Maven-модули
Get-ChildItem -Recurse -File -Filter pom.xml

# точки входа
rg -n "static void main|SpringApplication\.run" .

# объявления packages
rg -n "^package " src

# тесты JUnit
rg -n "@Test|@ParameterizedTest|@SpringBootTest" .
```

Для Maven:

```shell
./mvnw help:effective-pom
./mvnw dependency:tree
```

В большом репозитории поиск следует ограничивать исходниками, чтобы не
сканировать `target`, `.git` и внешние зависимости.

## Разбор текущего учебного проекта

Сейчас этот репозиторий имеет упрощённую IDE-ориентированную структуру:

```text
java-learning/
├── java-learning.iml
├── src/
│   ├── broCode/
│   │   └── Main.java
│   └── com/
│       └── example/
│           ├── Main.java
│           └── WalletGreetingService.java
└── out/
    └── production/
        └── java-learning/
```

Из неё можно сделать выводы:

- `src` настроен как единый source root;
- `out` содержит скомпилированные IDE outputs;
- `java-learning.iml` описывает IntelliJ IDEA module;
- стандартных `src/main/java` и `src/test/java` пока нет;
- `pom.xml` и Maven Wrapper пока отсутствуют;
- сборка зависит от конфигурации IDE или ручных команд.

Для первых экспериментов это допустимо. Для дальнейшего учебного backend-
проекта полезно перейти к стандартной Maven-структуре, чтобы сборка была
воспроизводима из терминала и в CI.

Целевая минимальная структура может выглядеть так:

```text
java-learning/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/
│   └── wrapper/
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── example/
    │   │           ├── Main.java
    │   │           └── WalletGreetingService.java
    │   └── resources/
    └── test/
        ├── java/
        │   └── com/
        │       └── example/
        │           └── WalletGreetingServiceTest.java
        └── resources/
```

Эта глава только объясняет переход. Перемещать текущие файлы стоит отдельным
изменением вместе с настройкой Maven, чтобы проект не оказался
между двумя структурами.

## Практическое задание

### Часть 1. Карта текущего репозитория

Для каждого элемента определи его роль:

```text
src/com/example/Main.java
src/com/example/WalletGreetingService.java
out/production/java-learning
java-learning.iml
lerning-materials
```

Ответь:

1. Какой каталог является source root?
2. Какой package у каждого класса?
3. Где лежат `.class`-файлы?
4. Какие файлы являются исходными, а какие сгенерированными?
5. Можно ли воспроизвести IDE-сборку одной зафиксированной wrapper-командой?

### Часть 2. Спроектировать стандартную структуру

Не перемещая файлы, нарисуй целевое дерево Maven-проекта:

- production-код;
- unit-тесты;
- main/test resources;
- `pom.xml`;
- wrapper;
- outputs.

Для каждого каталога подпиши, должен ли он храниться в Git.

### Часть 3. Найти элементы реального backend-проекта

В рабочем backend-репозитории найди:

1. root build;
2. список Maven modules;
3. основной каталог исходников;
4. unit и integration tests;
5. Spring Boot entry point;
6. application configuration;
7. database migrations;
8. generated sources;
9. production JAR;
10. команду полной проверки в CI.

### Часть 4. Проверка понимания classpath resource

Положи `greeting.txt` в `src/main/resources`, прочитай его через
`getResourceAsStream`, затем:

1. запусти код из IDE;
2. собери JAR;
3. убедись, что ресурс находится внутри JAR;
4. запусти JAR из другой рабочей директории.

Код не должен зависеть от пути `src/main/resources/greeting.txt` на диске.

## Что важно запомнить

1. Java package, source directory и build module — разные понятия.
2. `src/main/java` и `src/test/java` — соглашения Maven, а не синтаксис
   языка Java.
3. Package path начинается после source root.
4. Main и test компилируются раздельно и имеют разные classpath.
5. Resources копируются в output и читаются из classpath.
6. `target/` и `out/` обычно содержат генерируемые результаты.
7. Generated source исправляют через вход генератора, а не вручную.
8. Spring Boot root class обычно размещают над сканируемыми packages.
9. Package-by-feature чаще лучше масштабируется, чем единый набор технических
   packages, но структура должна следовать границам конкретного приложения.
10. Maven module, JPMS module и архитектурный модуль не
    являются синонимами.
11. Wrapper и CI показывают воспроизводимый способ сборки лучше, чем кнопка IDE.
12. Незнакомый репозиторий нужно читать от build boundary к business-flow.

## Самопроверка

Ответь без подсказки:

1. Что в Java-проекте задаёт язык, а что является соглашением build tool?
2. Почему `src/main/java` не входит в package?
3. Как из пути файла определить его ожидаемый package?
4. Может ли production-код импортировать класс из `src/test/java`?
5. Чем test resources отличаются от main resources?
6. Почему classpath resource не всегда можно представить как `File`?
7. Куда Maven складывает результаты сборки?
8. Почему нельзя исправлять класс внутри `target/generated-sources` вручную?
9. Чем package отличается от Maven module?
10. Чем Maven module отличается от JPMS module?
11. Где лучше размещать класс с `@SpringBootApplication` и почему?
12. В чём trade-off package-by-layer и package-by-feature?
13. Как понять назначение нестандартного `src/integrationTest`?
14. Какие файлы покажут реальную команду проверки перед merge?
15. Что в структуре текущего репозитория мешает воспроизводимой CLI-сборке?

## Официальная документация

- [Maven: Introduction to the Standard Directory Layout](https://maven.apache.org/guides/introduction/introduction-to-the-standard-directory-layout.html)
- [Maven: Maven in Five Minutes](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html)
- [Spring Boot: Structuring Your Code](https://docs.spring.io/spring-boot/reference/using/structuring-your-code.html)
- [Java: Packages](https://dev.java/learn/packages/)
- [Java API: Class.getResourceAsStream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html#getResourceAsStream(java.lang.String))

[← Вернуться к учебному плану](../java-backend-learning-plan.md)
