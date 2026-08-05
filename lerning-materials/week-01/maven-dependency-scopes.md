# Maven dependency scopes

## Зачем нужны scopes

Java-код редко существует без внешних библиотек. Но не каждая библиотека нужна в один и тот же момент:

- API логирования нужно при компиляции production-кода и при запуске;
- JDBC-драйвер обычно не нужен компилятору, но нужен запущенному приложению;
- JUnit нужен только для компиляции и запуска тестов;
- Servlet API может быть нужен компилятору, но в production его предоставляет контейнер.

Maven dependency scope отвечает сразу на два вопроса:

1. **В какие classpath проекта попадёт зависимость?**
2. **Будет ли она передана транзитивно проектам-потребителям?**

Это две независимые идеи. Если помнить только «`test` — для тестов», будет сложно диагностировать ошибки вида
`ClassNotFoundException`, лишние библиотеки в приложении и конфликты транзитивных версий.

После изучения темы нужно уметь:

- выбрать scope по тому, где класс реально используется и кто предоставляет его реализацию;
- отличить compile-time dependency от runtime dependency;
- предсказать, что попадёт в classpath production-кода и тестов;
- понять, какие зависимости увидит потребитель библиотеки;
- исследовать граф через `dependency:tree`;
- отличить `<dependencies>` от `<dependencyManagement>`;
- объяснить назначение BOM, optional dependency и exclusions.

## Минимальная модель: четыре classpath

Удобно мысленно разделять не один classpath, а несколько:

```text
main compile classpath
    нужен javac при компиляции src/main/java

main runtime classpath
    нужен JVM при запуске production-кода

test compile classpath
    нужен javac при компиляции src/test/java

test runtime classpath
    нужен JVM при запуске тестов
```

Тестовый код обычно видит production-код и его зависимости, поэтому test classpath шире main classpath.

Scope не является Java-модификатором и не запрещает импорт на уровне языка. Он управляет тем, предоставит ли Maven
соответствующий JAR компилятору или JVM на конкретном этапе.

## Краткая таблица scopes

| Scope       | Main compile | Main runtime | Test compile/run | Передаётся потребителю | Основной смысл                                        |
|---          |---           |---           |---               |---                     |---                                                     |
| `compile`   | да           | да           | да                | да                      | библиотека нужна production-коду всегда                |
| `provided`  | да           | нет          | да                | нет                     | в runtime библиотеку предоставляет окружение           |
| `runtime`   | нет          | да           | да                | да                      | реализация нужна при запуске, но не для компиляции      |
| `test`      | нет          | нет          | да                | нет                     | библиотека нужна только тестам                          |
| `system`    | да           | нет          | да                | нет                     | локальный JAR по явному пути; почти всегда антипаттерн  |
| `import`    | неприменимо  | неприменимо  | неприменимо       | неприменимо             | импорт списка управляемых зависимостей из BOM           |

`compile` используется по умолчанию: отсутствие `<scope>` означает `<scope>compile</scope>`.

## `compile`: обычная production-зависимость

```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.17</version>
</dependency>
```

`compile` подходит, если production-код:

- импортирует типы зависимости;
- должен иметь эту зависимость при запуске;
- и потребитель вашей библиотеки также должен получить её транзитивно.

Например:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TransferService {
    private static final Logger log = LoggerFactory.getLogger(TransferService.class);
}
```

`slf4j-api` нужен и `javac`, и JVM. Если `wallet-core` публикуется как библиотека, его пользователям также нужна эта
часть публичного или внутреннего runtime-графа, поэтому `compile` — естественный выбор.

Не следует явно писать `compile` в каждом блоке только ради многословности: это default scope.

## `provided`: компилируемся с API, но runtime предоставляет среда

```xml
<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
    <version>6.1.0</version>
    <scope>provided</scope>
</dependency>
```

Классы доступны при компиляции main- и test-кода, но Maven не включает зависимость в обычный runtime classpath и не
передаёт её транзитивно потребителю.

Контракт выглядит так:

```text
компилятор: «вот API, код можно проверить»
runtime:    «реализацию обязан предоставить контейнер или платформа»
```

Классический пример — WAR, который разворачивается во внешнем servlet container. Контейнер уже предоставляет Servlet
API; упаковка ещё одной копии может привести к конфликту классов.

`provided` нужно использовать только при наличии реального runtime-контракта. Если запустить код обычной командой
`java` без обещанного контейнера, можно получить:

```text
NoClassDefFoundError: jakarta/servlet/...
```

В Spring Boot с embedded server нельзя автоматически считать servlet-зависимости `provided`: способ упаковки и
запуска отличается. Нужно смотреть конфигурацию конкретного приложения, а не переносить правило из WAR-проекта.

## `runtime`: компилятор знает контракт, JVM получает реализацию

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.7</version>
    <scope>runtime</scope>
</dependency>
```

Production-код обычно работает с JDBC API из JDK:

```java
import java.sql.Connection;
import java.sql.DriverManager;

Connection connection = DriverManager.getConnection(url, user, password);
```

Типы PostgreSQL-драйвера напрямую не импортируются, поэтому драйвер не нужен для компиляции main-кода. Но при запуске
JVM должна найти реализацию JDBC driver через механизм service provider.

Другие типичные примеры:

- конкретная реализация logging facade;
- реализация SPI, загружаемая во время выполнения;
- database driver, если его типы не используются напрямую.

Если main-код начинает импортировать класс из runtime-зависимости, сборка должна упасть на `compile`. Обычно это
полезный сигнал: либо код слишком сильно связан с конкретной реализацией, либо выбран неверный scope.

## `test`: зависимость не является частью приложения

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.13.4</version>
    <scope>test</scope>
</dependency>
```

Зависимость доступна при компиляции и запуске тестов, но:

- main-код её не видит;
- в production runtime она не нужна;
- потребители опубликованной библиотеки её не получают.

Типичные примеры:

- JUnit;
- AssertJ;
- Mockito;
- test fixtures;
- библиотеки для поднятия тестового окружения.

Если класс из `src/main/java` импортирует JUnit, Maven должен обнаружить ошибку уже при компиляции production-кода. Это
не ограничение JUnit, а результат раздельных classpath.

## `system`: локальный файл вместо repository

```xml
<dependency>
    <groupId>com.vendor</groupId>
    <artifactId>legacy-sdk</artifactId>
    <version>1.0.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/legacy-sdk.jar</systemPath>
</dependency>
```

По доступности `system` похож на `provided`, но Maven не разрешает артефакт через local/remote repositories, а берёт
JAR по указанному пути.

Проблемы:

- сборка зависит от файловой системы конкретной машины;
- артефакт не скачивается автоматически;
- сложнее контролировать checksum, происхождение и версию;
- ломается воспроизводимость CI и developer environment;
- Maven-модель зависимостей обходится стороной.

Практическое правило: **не использовать `system` в обычном проекте**. Закрытый vendor JAR лучше опубликовать во
внутренний Maven repository с нормальными `groupId`, `artifactId` и `version`.

## `import`: специальный scope для BOM

`import` можно применять только:

- внутри `<dependencyManagement>`;
- к зависимости с `<type>pom</type>`.

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>3.5.4</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Такой блок не добавляет Spring Boot библиотеки в classpath. Он импортирует BOM — согласованный каталог версий. После
этого конкретную зависимость всё равно нужно объявить:

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

Версия берётся из импортированного BOM.

Название `import` может вводить в заблуждение: это не аналог Java `import` и не способ импортировать библиотеку в код.

## `<dependencies>` и `<dependencyManagement>` — разные операции

```text
dependencies
    добавляет зависимость в граф проекта

dependencyManagement
    задаёт правила для зависимости, если она появится в графе
```

Пример:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>42.7.7</version>
            <scope>runtime</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Сам по себе этот блок не добавляет драйвер. В дочернем модуле или ниже в том же POM достаточно написать:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
```

Maven подставит управляемые `version` и `scope`. В enterprise multi-module проекте это помогает централизованно
согласовать версии и не копировать их по модулям.

`dependencyManagement` также может переопределить версию транзитивной зависимости. Его правила имеют приоритет над
обычным выбором версии по близости в дереве.

## Транзитивность: зависимость зависимости

Пусть проект `wallet-app` зависит от `transfer-client`, а тот — от `jackson-databind`:

```text
wallet-app
└── transfer-client
    └── jackson-databind
```

`transfer-client` — direct dependency, `jackson-databind` — transitive dependency. Maven разрешает полный граф, поэтому
не требуется вручную копировать все зависимости каждой библиотеки.

Но итоговый scope транзитивной зависимости вычисляется из двух scopes:

- scope прямой зависимости A в текущем проекте;
- scope зависимости B внутри POM артефакта A.

Упрощённая официальная матрица:

| Scope A \ Scope B | `compile`  | `provided` | `runtime`  | `test`     |
|---                 |---         |---         |---          |---         |
| `compile`          | `compile`  | исключена  | `runtime`   | исключена  |
| `provided`         | `provided` | исключена  | `provided`  | исключена  |
| `runtime`          | `runtime`  | исключена  | `runtime`   | исключена  |
| `test`             | `test`     | исключена  | `test`      | исключена  |

Матрицу не обязательно заучивать. Полезнее понимать правила:

- `provided` и `test` зависимости библиотеки не должны незаметно становиться обязанностью её потребителя;
- более узкий контекст прямой зависимости сужает и её транзитивное поддерево;
- реальный результат всегда можно проверить через `dependency:tree`.

## Как Maven выбирает версию при конфликте

Представим граф:

```text
wallet-app
├── library-a
│   └── common-utils:1.5
└── library-b
    └── adapter
        └── common-utils:2.0
```

По правилу **nearest definition** Maven выберет `common-utils:1.5`: он расположен ближе к корню. Если конфликтующие
версии находятся на одинаковой глубине, побеждает зависимость, встретившаяся первой в порядке деклараций.

Полагаться на случайный порядок нежелательно. Если версия важна, её фиксируют явно:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>common-utils</artifactId>
            <version>2.0</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Или импортируют BOM платформы. В текущем POM собственный `dependencyManagement` приоритетнее унаследованного, а
управляемая версия приоритетнее обычной dependency mediation.

Важно: Maven выбирает одну версию артефакта с одинаковыми coordinates. Это не гарантирует бинарную совместимость.
Сборка может пройти, а runtime завершиться `NoSuchMethodError`, если выбранная версия не содержит ожидаемого метода.

## `optional`: зависимость нужна мне, но не моему потребителю автоматически

```xml
<dependency>
    <groupId>com.vendor</groupId>
    <artifactId>fraud-provider-sdk</artifactId>
    <version>4.2.0</version>
    <optional>true</optional>
</dependency>
```

Для текущего проекта зависимость остаётся обычной. Но если другой проект подключит наш артефакт, optional dependency
не будет передана ему транзитивно. Потребитель, которому нужна соответствующая возможность, объявляет её сам.

Это полезно для библиотеки с необязательными интеграциями:

```text
wallet-client
├── core API
├── интеграция с Provider A (optional)
└── интеграция с Provider B (optional)
```

`optional` и `scope` решают разные задачи:

- scope определяет classpath и общие правила транзитивности;
- optional обрывает передачу конкретной зависимости следующему потребителю.

Если большая библиотека требует много optional-флагов, возможно, её лучше разделить на отдельные модули.

## `exclusions`: удалить нежелательную ветку графа

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>legacy-client</artifactId>
    <version>3.0.0</version>
    <exclusions>
        <exclusion>
            <groupId>commons-logging</groupId>
            <artifactId>commons-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

Exclusion действует на конкретный путь в графе, а не глобально на весь проект. Та же библиотека может прийти по другой
ветке.

Использовать exclusions стоит осознанно:

- зависимость ошибочно объявлена upstream-библиотекой;
- нужно удалить конфликтующую реализацию;
- уязвимый или запрещённый артефакт заменяется другим.

Не стоит лечить непонятный конфликт случайными exclusions. Сначала нужно увидеть полный граф и понять, почему артефакт
появился.

## Аналогии с npm

Полного соответствия между Maven и npm нет: Maven scopes моделируют compile/test/runtime classpath, а Node.js обычно
исполняет исходный JavaScript или результат отдельной TypeScript-сборки.

| Maven                         | Приблизительная аналогия в TypeScript/Node.js                                       |
|---                            |---                                                                                  |
| `compile`                     | production `dependencies`, нужные коду и его потребителю                            |
| `runtime`                     | production dependency, загружаемая только во время исполнения                       |
| `test`                        | `devDependencies`, используемые тестами                                              |
| `provided`                    | частично напоминает `peerDependencies`: реализацию предоставляет окружение           |
| `optional=true`               | необязательная интеграция, но это не эквивалент npm `optionalDependencies`            |
| `dependencyManagement`        | централизованная политика версий; отдалённо напоминает overrides/resolutions         |
| BOM с `import`                | импорт согласованного каталога версий без установки всех пакетов                     |
| transitive dependencies       | зависимости зависимостей в `node_modules`                                            |
| `dependency:tree`             | `npm ls` или `pnpm why`                                                              |

Главное различие с `peerDependencies`: `provided` управляет classpath текущего Maven-проекта и означает, что runtime
предоставляет среда. `peerDependencies` прежде всего выражает контракт npm-пакета с пакетом, устанавливаемым рядом
потребителем. Аналогия полезна для первого приближения, но не для механического выбора scope.

## Как выбирать scope

Последовательность решения:

```text
Используются ли типы зависимости в src/main/java?
├── да
│   ├── runtime гарантированно предоставляет контейнер/платформа? → provided
│   └── иначе                                                   → compile
└── нет
    ├── нужна ли зависимость запущенному production-коду?       → runtime
    ├── нужна ли она только тестам?                              → test
    └── возможно, зависимость вообще не нужна
```

Дополнительный вопрос для автора библиотеки:

```text
Должен ли потребитель автоматически получить эту зависимость?
├── да  → обычная транзитивная зависимость подходящего scope
└── нет → рассмотреть provided, test или optional — по реальной семантике
```

Не следует выбирать scope только ради уменьшения JAR. Сначала выражается корректный контракт, затем проверяется
упаковка конкретным Maven/Spring Boot plugin.

## Диагностика графа

### Полное дерево

На Windows:

```powershell
.\mvnw.cmd dependency:tree
```

На Linux/macOS:

```shell
./mvnw dependency:tree
```

Типичная строка:

```text
org.postgresql:postgresql:jar:42.7.7:runtime
```

Поля:

```text
groupId : artifactId : type : version : scope
```

### Фильтрация по артефакту

```powershell
.\mvnw.cmd dependency:tree -Dincludes=org.slf4j
```

### Показать конфликтующие и исключённые версии

```powershell
.\mvnw.cmd dependency:tree -Dverbose
```

Поддержка отдельных деталей вывода зависит от версии Maven Dependency Plugin, но в дереве часто можно увидеть:

```text
(omitted for conflict with 2.0.17)
```

### Effective POM

```powershell
.\mvnw.cmd help:effective-pom
```

Команда помогает понять, откуда реально пришли:

- версия;
- scope;
- parent POM;
- импортированный BOM;
- настройки plugin;
- profile.

В enterprise-проекте чтения одного локального `pom.xml` часто недостаточно.

## Compile-time и runtime ошибки как подсказки

| Симптом                          | Вероятная причина                                                       |
|---                               |---                                                                      |
| `package ... does not exist`     | зависимости нет в compile classpath или неверно выбраны coordinates      |
| `cannot find symbol`             | нужный тип отсутствует в compile classpath либо несовместима версия       |
| `ClassNotFoundException`         | код явно пытался загрузить класс, отсутствующий в runtime classpath       |
| `NoClassDefFoundError`           | JVM не смогла определить уже используемый класс из-за runtime classpath   |
| `NoSuchMethodError`              | класс найден, но runtime-версия бинарно несовместима с версией компиляции |
| тест проходит, приложение падает | test classpath содержит библиотеку, которой нет в production runtime      |

Последний случай особенно коварен: тестовая зависимость может случайно маскировать ошибку production-конфигурации.

## Частые ошибки

### Всё объявлять как `compile`

Сборка может работать, но контракт становится неточным: тестовые инструменты и runtime-реализации попадают не туда,
куда должны, а потребители получают лишние транзитивные зависимости.

### Путать `provided` и `runtime`

```text
provided: нужен компилятору, отсутствует в runtime
runtime:  не нужен main-компилятору, присутствует в runtime
```

Это почти противоположные scopes.

### Ожидать, что `dependencyManagement` добавит библиотеку

Он только управляет параметрами зависимости. Для появления в графе нужна декларация в `<dependencies>` или
транзитивный путь.

### Ставить `test` production-драйверу из-за Testcontainers

Если приложение в production подключается к PostgreSQL, драйвер является production runtime dependency, даже если
PostgreSQL также поднимается в тестах.

### Использовать `systemPath` для внутреннего JAR

Это делает сборку зависимой от машины. Правильнее опубликовать артефакт во внутренний repository.

### Слепо копировать scope из другого проекта

Один проект собирает WAR для внешнего контейнера, другой — executable JAR с embedded server. Одинаковая библиотека
может иметь разный runtime-контракт.

## Практическое задание

Создать небольшой Maven-проект со следующими зависимостями:

1. `slf4j-api` без явного scope;
2. PostgreSQL JDBC driver со scope `runtime`;
3. JUnit Jupiter со scope `test`.

Затем проверить:

1. Production-код компилируется с импортом `org.slf4j.Logger`.
2. Production-код не компилируется с прямым импортом класса PostgreSQL driver.
3. Test-код видит JUnit и PostgreSQL driver.
4. Main-код не видит JUnit.
5. `dependency:tree` показывает ожидаемые scopes.
6. В effective POM видны версии, полученные из parent POM или BOM, если они используются.

Дополнительное упражнение:

- добавить две библиотеки, которые транзитивно требуют разные версии одного артефакта;
- предсказать победившую версию;
- проверить результат через `dependency:tree -Dverbose`;
- зафиксировать нужную версию в `dependencyManagement`;
- повторно проверить дерево.

## Checklist для code review

- У каждого нестандартного scope понятен runtime-контракт?
- Не используется ли `provided` без среды, действительно предоставляющей библиотеку?
- Не импортирует ли main-код классы из `runtime` или `test` dependency?
- Не попали ли test tools в production-граф?
- Действительно ли `optional` выражает необязательную возможность библиотеки?
- У каждого exclusion есть объяснимая причина?
- Версии централизованы в parent POM или BOM?
- Конфликты проверены через `dependency:tree`, а не угаданы?
- Сборка воспроизводима без локальных `systemPath`?

## Итоговая ментальная модель

```text
scope
├── выбирает classpath
│   ├── main compile
│   ├── main runtime
│   ├── test compile
│   └── test runtime
└── ограничивает транзитивность

dependencyManagement
└── управляет версией/scope, но не добавляет dependency

BOM + import
└── переносит согласованный набор managed dependencies

optional
└── не передаёт dependency следующему потребителю автоматически

exclusion
└── удаляет dependency из конкретной ветки графа
```

Короткое правило для повседневной работы:

```text
compile  — нужен коду и при компиляции, и при запуске
provided — нужен компилятору, runtime обещает окружение
runtime  — компилятору не нужен, JVM нужен
test     — нужен только тестам
system   — избегать
import   — только BOM внутри dependencyManagement
```

## Материалы

- [Maven: Introduction to the Dependency Mechanism](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html)
- [Maven: Optional Dependencies and Dependency Exclusions](https://maven.apache.org/guides/introduction/introduction-to-optional-and-excludes-dependencies.html)
- [Maven Dependency Plugin: dependency:tree](https://maven.apache.org/plugins/maven-dependency-plugin/tree-mojo.html)
