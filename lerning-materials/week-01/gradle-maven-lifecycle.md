# Gradle и Maven: lifecycle сборки

## Зачем backend-разработчику понимать lifecycle

Build tool в Java-проекте не просто заменяет ручной вызов `javac`. Он:

- определяет структуру исходников и ресурсов;
- формирует compile- и runtime-classpath;
- скачивает и кэширует зависимости;
- компилирует production- и test-код;
- запускает проверки и тесты;
- собирает JAR;
- публикует артефакты;
- даёт IDE и CI единую модель проекта.

После изучения темы нужно уметь:

- по команде сборки понять, какие действия реально будут выполнены;
- выбрать минимальную команду для компиляции, тестирования или полной проверки;
- отличить Maven phase, plugin goal и Gradle task;
- исследовать сборку незнакомого проекта;
- объяснить, почему задача была выполнена, пропущена или взята из кэша;
- использовать Maven Wrapper или Gradle Wrapper вместо случайной глобальной
  версии инструмента.

Главная идея:

```text
Maven:  выбираем фазу заранее определённого lifecycle
Gradle: выбираем задачи, а Gradle строит граф их зависимостей
```

Это похожие по назначению, но разные модели. Нельзя механически переносить все
термины одного инструмента в другой.

## Что такое build lifecycle

Build lifecycle — модель перехода проекта от исходников к проверенному и,
возможно, опубликованному артефакту:

```text
исходники
   ↓
компиляция
   ↓
тестирование
   ↓
упаковка
   ↓
дополнительные проверки
   ↓
публикация
```

У Maven эта последовательность явно представлена фазами lifecycle. У Gradle
конкретная работа представлена задачами и связями между ними.

## Общая структура Java-проекта

Оба инструмента по умолчанию понимают стандартную структуру:

```text
project/
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
│       ├── java/
│       └── resources/
└── ...
```

Различаются основные конфигурационные файлы:

| Maven                                  | Gradle                                                        |
|---                                     |---                                                            |
| `pom.xml`                              | `build.gradle.kts` или `build.gradle`                          |
| XML-модель проекта                     | Kotlin DSL или Groovy DSL                                      |
| `settings.xml` — настройки окружения   | `settings.gradle.kts`/`settings.gradle` — состав сборки         |
| результат обычно в `target/`           | результат обычно в `build/`                                   |
| wrapper: `mvnw`, `mvnw.cmd`, `.mvn/`   | wrapper: `gradlew`, `gradlew.bat`, `gradle/wrapper/`            |

## Maven: lifecycle, phase и goal

В Maven нужно различать три понятия.

### Lifecycle

Lifecycle — упорядоченная последовательность фаз. Maven имеет три встроенных
lifecycle:

| Lifecycle | Назначение                                                    |
|---        |---                                                           |
| `default` | компиляция, тестирование, упаковка и публикация проекта       |
| `clean`   | удаление результатов предыдущей сборки                        |
| `site`    | создание и публикация сайта с документацией проекта           |

`clean` не является первой фазой `default`. Это отдельный lifecycle. Поэтому
команда:

```shell
./mvnw clean verify
```

просит Maven выполнить фазу `clean` из lifecycle `clean`, а затем дойти до
`verify` в lifecycle `default`.

### Phase

Phase — именованный этап lifecycle. Сама фаза описывает намерение, например
«скомпилировать» или «проверить», но конкретную работу выполняют plugin goals.

Основные фазы `default` lifecycle:

| Фаза                    | Что означает                                                        |
|---                      |---                                                                   |
| `validate`              | проверить, что проект корректен и содержит нужную информацию         |
| `compile`               | скомпилировать production-код                                        |
| `test`                  | запустить unit-тесты                                                  |
| `package`               | собрать JAR, WAR или другой артефакт                                  |
| `verify`                | выполнить дополнительные проверки собранного проекта                 |
| `install`               | положить артефакт в локальный Maven repository                        |
| `deploy`                | опубликовать артефакт в удалённом repository                          |

Между перечисленными фазами существуют дополнительные фазы. Особенно важна
группа для integration-тестов:

```text
package
  → pre-integration-test
  → integration-test
  → post-integration-test
  → verify
```

Полезная практическая последовательность выглядит так:

```text
validate
  → compile
  → test
  → package
  → integration-test
  → verify
  → install
  → deploy
```

Если вызвать фазу, Maven выполнит все предшествующие фазы этого lifecycle:

```shell
./mvnw package
```

Команда не означает «только создать JAR». Maven пройдёт предыдущие фазы,
включая `compile` и `test`.

Аналогично:

```shell
./mvnw verify
```

выполнит компиляцию, unit-тесты, упаковку, integration-тесты, если они
настроены, и проверки, привязанные к `verify`.

### Plugin goal

Goal — конкретное действие Maven-плагина. Общая форма прямого вызова:

```text
plugin-prefix:goal
```

Примеры:

```shell
./mvnw compiler:compile
./mvnw surefire:test
./mvnw dependency:tree
```

Conceptual mapping:

```text
lifecycle
└── phase
    └── один или несколько привязанных plugin goals
```

Например, для проекта с packaging `jar` стандартные привязки концептуально
выглядят так:

| Phase      | Типичный goal                                                    |
|---         |---                                                                |
| `compile`  | `compiler:compile`                                                 |
| `test`     | `surefire:test`                                                    |
| `package`  | `jar:jar`                                                          |
| `install`  | `install:install`                                                  |
| `deploy`   | `deploy:deploy`                                                    |

Точные плагины, их версии и дополнительные executions определяются effective
POM. Поэтому phase — стабильный пользовательский интерфейс сборки, а goals —
реальная реализация этапа.

### Packaging влияет на привязки

Поле `<packaging>` в `pom.xml` определяет тип основного артефакта и стандартные
goal bindings:

```xml
<packaging>jar</packaging>
```

Для `jar` фаза `package` создаёт JAR. Для `war` та же фаза создаёт WAR. Для
агрегирующего родительского проекта часто используется:

```xml
<packaging>pom</packaging>
```

Поэтому одинаковая команда `./mvnw package` может выполнять немного разную
работу в разных типах проектов, сохраняя одинаковое намерение.

### Минимальный `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="
             http://maven.apache.org/POM/4.0.0
             https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>wallet-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.13.4</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.5.3</version>
            </plugin>
        </plugins>
    </build>
</project>
```

Здесь POM задаёт модель проекта. Пользователь не пишет вручную шаги
`javac → junit → jar`: стандартные lifecycle bindings предоставляют плагины.

Версии в учебном примере иллюстративны. В реальном проекте нужно использовать
версии, зафиксированные его parent POM или dependency/plugin management.

### Частые команды Maven

| Команда                             | Результат                                                   |
|---                                  |---                                                          |
| `./mvnw compile`                    | компиляция production-кода                                  |
| `./mvnw test`                       | компиляция main/test-кода и unit-тесты                      |
| `./mvnw package`                    | тесты и создание артефакта                                  |
| `./mvnw verify`                     | полный набор локальных проверок до `verify`                 |
| `./mvnw install`                    | проверки и установка артефакта в локальный repository       |
| `./mvnw deploy`                     | проверки и публикация в удалённый repository                |
| `./mvnw clean verify`               | чистая сборка до `verify`                                   |
| `./mvnw dependency:tree`            | дерево зависимостей                                         |
| `./mvnw help:effective-pom`          | итоговый POM с inheritance, profiles и defaults             |

На Windows wrapper запускается как:

```powershell
.\mvnw.cmd verify
```

В Git Bash или Linux/macOS:

```shell
./mvnw verify
```

### `test` не всегда означает все тесты

Обычная схема Maven:

- Maven Surefire Plugin запускает unit-тесты на фазе `test`;
- Maven Failsafe Plugin запускает integration-тесты через goals
  `integration-test` и `verify`.

Конкретный проект может использовать другие соглашения, профили или плагины.
Поэтому нельзя по одному имени файла гарантировать, когда тест будет запущен.
Нужно смотреть конфигурацию POM и вывод сборки.

Для CI обычно безопаснее выбрать:

```shell
./mvnw verify
```

а не останавливаться на `package`, если проект привязывает важные проверки к
более поздним фазам.

## Gradle: задачи и их граф

Gradle не заставляет каждую сборку проходить один универсальный линейный
lifecycle. Основная единица работы — task.

Примеры задач:

```text
compileJava
processResources
classes
compileTestJava
test
jar
assemble
check
build
clean
```

Задачи связаны зависимостями. Если задача `build` зависит от `check` и
`assemble`, Gradle сначала выполнит необходимую часть графа:

```text
build
├── assemble
│   └── jar
│       └── classes
│           ├── compileJava
│           └── processResources
└── check
    └── test
        ├── testClasses
        └── classes
```

Это упрощённый граф. Конкретный проект может добавить code style, static
analysis, coverage, integration tests, генерацию кода и другие задачи.

### Три фазы выполнения Gradle

Термин «phase» у Gradle означает не то же самое, что Maven phase. Каждый запуск
Gradle проходит три этапа:

| Этап              | Что делает Gradle                                                     |
|---                 |---                                                                      |
| Initialization    | находит settings, проекты и included builds                            |
| Configuration     | вычисляет модель сборки, регистрирует задачи и их связи                 |
| Execution         | выполняет выбранные задачи и необходимые зависимости                   |

Например:

```shell
./gradlew test
```

не означает, что Gradle пропустит initialization и configuration. Это
означает, что после построения модели в execution phase целевой задачей будет
`test` и её task dependencies.

Configuration Cache может повторно использовать результат configuration для
совместимых сборок, но логическая модель трёх этапов остаётся полезной.

### Откуда появляются задачи

Задачи добавляются:

- Gradle core;
- подключёнными plugins;
- build scripts проекта;
- convention plugins организации.

Минимальный `build.gradle.kts`:

```kotlin
plugins {
    java
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
}

tasks.test {
    useJUnitPlatform()
}
```

Применение `java` plugin создаёт стандартные source sets, configurations и
задачи для Java-проекта. Сам файл не перечисляет вручную каждый вызов
компилятора.

Как и в Maven-примере, версия зависимости иллюстративна. Проект может
централизовать версии через version catalog, platform/BOM или convention
plugin.

### Рабочие и lifecycle tasks

Условно задачи удобно разделить на две группы.

Рабочая задача выполняет конкретное действие:

- `compileJava` компилирует main-код;
- `test` запускает тесты;
- `jar` создаёт архив;
- `clean` удаляет build directory.

Lifecycle task выражает более крупное намерение и часто сама почти не делает
работы, а зависит от других задач:

- `assemble` собирает выходные артефакты;
- `check` запускает проверки;
- `build` зависит от `assemble` и `check`.

```text
./gradlew build
```

означает: выбрать задачу `build`, построить граф её зависимостей и выполнить
нужные задачи. Это не команда «пройти все известные задачи проекта».

Например, `publish` или `bootRun` обычно не становятся частью `build`
автоматически.

### Зависимость задач и порядок выполнения

В Kotlin DSL можно зарегистрировать задачу лениво:

```kotlin
val verifyArchitecture by tasks.registering {
    group = "verification"
    description = "Checks architecture rules"

    doLast {
        println("Architecture verified")
    }
}

tasks.check {
    dependsOn(verifyArchitecture)
}
```

Теперь:

```shell
./gradlew check
```

включит `verifyArchitecture` в task graph.

`dependsOn` означает и необходимость задачи, и порядок: зависимость должна
успешно выполниться раньше зависящей задачи.

Другие связи имеют иную семантику:

| Связь              | Смысл                                                               |
|---                 |---                                                                    |
| `dependsOn`        | задача необходима для выполнения текущей                              |
| `mustRunAfter`     | если обе выбраны, задаёт строгий порядок, но не добавляет зависимость  |
| `shouldRunAfter`   | задаёт предпочтительный порядок                                       |
| `finalizedBy`      | после задачи нужно запустить finalizer, например cleanup              |

Распространённая ошибка — использовать `mustRunAfter`, ожидая, что первая
задача автоматически попадёт в граф. Не попадёт: ordering rule не равен task
dependency.

### Как исследовать граф и задачи

```shell
./gradlew tasks
./gradlew tasks --all
./gradlew help --task test
./gradlew build --dry-run
./gradlew build --info
```

| Команда                         | Для чего нужна                                            |
|---                              |---                                                        |
| `tasks`                         | основные задачи по группам                                |
| `tasks --all`                   | более полный список задач                                 |
| `help --task <name>`            | детали выбранной задачи                                   |
| `--dry-run`                     | показать план задач без выполнения их actions             |
| `--info`                        | подробнее объяснить ход сборки и причины пропусков         |
| `dependencies`                  | показать зависимости configurations                       |
| `dependencyInsight`             | объяснить выбор конкретной версии зависимости             |

На Windows:

```powershell
.\gradlew.bat build --dry-run
```

В Git Bash или Linux/macOS:

```shell
./gradlew build --dry-run
```

### Почему Gradle пишет `UP-TO-DATE`, `FROM-CACHE`, `SKIPPED`

Gradle моделирует inputs и outputs задач. Если входы и выходы не изменились,
повторять работу не обязательно.

Основные статусы:

| Статус/сообщение | Что обычно означает                                                |
|---               |---                                                                   |
| `UP-TO-DATE`     | inputs/outputs не изменились, локальный результат актуален           |
| `FROM-CACHE`     | output восстановлен из build cache                                  |
| `SKIPPED`        | задача пропущена по условию или не имела выполняемых actions         |
| `NO-SOURCE`      | для задачи не найдено исходных файлов                                |
| `FAILED`         | action задачи завершился ошибкой                                     |

Incremental build — возможность не выполнять актуальные задачи или обработать
только изменившиеся inputs, если task это поддерживает.

Build Cache — переиспользование outputs задачи, вычисленных ранее, возможно
даже другой машиной при настроенном remote cache.

Configuration Cache — переиспользование результата configuration phase.

Это три связанные с производительностью, но разные возможности:

```text
incremental/up-to-date → нужно ли повторять работу task
build cache            → можно ли взять готовые outputs task
configuration cache    → можно ли повторно не конфигурировать build
```

### Нужен ли `clean`

Обычная Gradle-сборка не должна требовать постоянного `clean`:

```shell
./gradlew build
```

Incremental-механизмы сохраняют актуальность outputs. Команда:

```shell
./gradlew clean build
```

удаляет `build/`, лишает последующую сборку части локальных преимуществ и
обычно работает медленнее.

`clean` полезен:

- для диагностики подозрения на stale generated outputs;
- для проверки воспроизводимости сборки с нуля;
- когда конкретный plugin неверно описывает inputs/outputs;
- если этого явно требует процесс проекта.

Но «всегда добавлять clean на всякий случай» маскирует ошибки build logic и
ухудшает feedback loop.

Та же практическая осторожность применима к Maven: `clean` нужен не перед
каждым `test`, хотя Maven и Gradle имеют разные механизмы инкрементальности.

### Multi-project build

В Gradle задача может быть адресована конкретному проекту:

```shell
./gradlew :wallet-domain:test
./gradlew :wallet-api:build
```

Если `wallet-api` зависит от production-кода `wallet-domain`, Gradle добавит
необходимые задачи зависимого проекта в граф.

Команда без project path может выбрать одноимённые задачи в подходящих
subprojects. В enterprise-репозитории полезно всегда смотреть фактический task
graph, а не угадывать область выполнения по короткому имени.

## Maven и Gradle: сопоставление без ложной эквивалентности

Приблизительное соответствие намерений:

| Намерение                    | Maven                              | Gradle                                  |
|---                           |---                                 |---                                      |
| удалить outputs              | `./mvnw clean`                     | `./gradlew clean`                       |
| скомпилировать main-код      | `./mvnw compile`                   | `./gradlew classes`                     |
| запустить unit-тесты         | `./mvnw test`                      | `./gradlew test`                        |
| собрать артефакты            | `./mvnw package`                   | `./gradlew assemble`                    |
| выполнить проверки           | `./mvnw verify`                    | `./gradlew check`                       |
| собрать и проверить          | `./mvnw verify`                    | `./gradlew build`                       |
| установить локально          | `./mvnw install`                   | зависит от publishing setup             |
| опубликовать                 | `./mvnw deploy`                    | обычно `./gradlew publish`              |
| дерево зависимостей          | `./mvnw dependency:tree`           | `./gradlew dependencies`                |

Это соответствие намерений, а не гарантия одинаковой работы:

- Maven выполняет все фазы до выбранной;
- Gradle выполняет выбранные задачи и транзитивные task dependencies;
- plugins проекта могут добавить проверки и изменить граф/bindings;
- `install`, `deploy` и `publish` особенно зависят от конфигурации проекта.

## Аналогия с TypeScript/Node.js

| Java build tools                         | Приблизительная аналогия в TypeScript/Node.js                         |
|---                                       |---                                                                     |
| Maven phase                              | стандартизированный этап pipeline                                     |
| Maven plugin goal                        | конкретная команда инструмента внутри этапа                            |
| Gradle task                              | npm script с явными inputs, outputs и зависимостями от других tasks    |
| Gradle task graph                        | вычисляемый dependency graph для build pipeline                        |
| Maven/Gradle plugin                      | пакет, который добавляет build logic и соглашения                      |
| Wrapper                                  | зафиксированный project-local способ запустить нужную версию tool      |
| `pom.xml`/`build.gradle.kts`             | одновременно зависимости и модель build pipeline                       |
| `target/`/`build/`                       | `dist/`, `build/` или другой каталог генерируемых outputs               |

Аналогия ограничена. `npm run build` не имеет универсальной семантики между
проектами: это произвольная строка из `package.json`. Maven lifecycle гораздо
более стандартизирован, а Gradle tasks являются объектами модели сборки, а не
только shell-командами.

## Wrapper: почему запускать нужно через проект

Wrapper хранит сведения о версии build tool, нужной проекту, и при
необходимости загружает её.

Maven:

```text
mvnw
mvnw.cmd
.mvn/wrapper/...
```

Gradle:

```text
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
```

Преимущества wrapper:

- разработчики и CI используют согласованную версию;
- не нужно надеяться на глобально установленный Maven/Gradle;
- upgrade версии становится версионируемым изменением проекта;
- сборка воспроизводится ближе к ожидаемому окружению.

Обычное правило:

```text
есть wrapper → запускай wrapper
```

Важно коммитить wrapper scripts и supporting files. Wrapper JAR в Gradle —
часть механизма запуска, а не обычная application dependency.

## Как читать незнакомую Maven-сборку

1. Найти `mvnw`, `.mvn/` и корневой `pom.xml`.
2. Посмотреть `groupId`, `artifactId`, `version`, `packaging`.
3. Найти parent POM и секции `modules`.
4. Изучить `properties`, `dependencyManagement`, `dependencies`.
5. Изучить `pluginManagement`, `plugins`, executions и profiles.
6. Выполнить безопасные диагностические команды:

```shell
./mvnw --version
./mvnw help:effective-pom
./mvnw help:active-profiles
./mvnw dependency:tree
```

7. Запустить принятый в репозитории verification command, обычно указанный в
   `README`, CI-конфигурации или contribution guide.

Effective POM важен, потому что видимый `pom.xml` может наследовать большую
часть конфигурации от parent и активных profiles.

## Как читать незнакомую Gradle-сборку

1. Найти `gradlew`, `gradle/wrapper/` и `settings.gradle(.kts)`.
2. Определить root project, subprojects и included builds.
3. Посмотреть корневые и модульные `build.gradle(.kts)`.
4. Найти применённые plugins, version catalog и convention plugins.
5. Выполнить:

```shell
./gradlew --version
./gradlew projects
./gradlew tasks
./gradlew build --dry-run
./gradlew dependencies
```

6. Для непонятной задачи использовать:

```shell
./gradlew help --task taskName
./gradlew taskName --info
```

7. Сверить локальную команду с CI pipeline.

В больших проектах существенная build logic часто находится не в корневом
скрипте, а в `buildSrc`, included build вроде `build-logic` или опубликованном
convention plugin.

## Типичные ловушки

### Считать `package` единственной нужной Maven-командой

Важные integration tests или quality gates могут быть привязаны к `verify`.
Перед merge нужно выполнять команду, принятую проектом.

### Вызывать Maven goal вместо lifecycle phase без причины

```shell
./mvnw compiler:compile
```

прямо запускает goal, но может обойти другие действия lifecycle. Для обычной
сборки стабильнее выражать намерение фазой:

```shell
./mvnw compile
```

Прямой вызов goal полезен для диагностических и самостоятельных целей вроде
`dependency:tree`.

### Считать `gradle build` последовательностью всех задач

`build` выполняет только свой dependency graph. Задача, которая не связана с
`build`, выполнена не будет.

### Путать task dependency и ordering rule

`mustRunAfter` определяет порядок только тогда, когда обе задачи уже выбраны.
Чтобы задача стала обязательной частью графа, обычно нужен `dependsOn`.

### Всегда начинать с `clean`

Это замедляет feedback loop и может скрыть некорректно описанные inputs и
outputs. Сначала следует запускать обычную инкрементальную сборку.

### Использовать глобальную версию инструмента

Команды `mvn` и `gradle` могут запустить версии, отличающиеся от CI и проекта.
Предпочтительны `mvnw` и `gradlew`.

### Полагаться только на кнопку IDE

IDE может использовать другой JDK или собственный build path. При странном
поведении нужно воспроизвести сборку wrapper-командой из терминала и сравнить:

```shell
java -version
./mvnw --version
```

или:

```shell
java -version
./gradlew --version
```

### Путать build tool JDK и target Java version

JDK, на котором работает Gradle/Maven, и Java version, для которой
компилируется приложение, — связанные, но разные настройки. Gradle toolchains
и `maven.compiler.release` помогают зафиксировать compiler target, но нужно
отдельно проверять требования к JDK самого build tool.

## Диагностика по симптомам

| Симптом                                             | Что проверить сначала                                      |
|---                                                  |---                                                          |
| Maven не запустил integration tests                 | Failsafe executions, phases, profiles, naming conventions   |
| Maven использует неожиданную версию plugin          | parent POM, plugin management, effective POM                |
| Gradle не выполнил пользовательскую задачу          | связана ли она через `dependsOn`, фактический task graph     |
| Gradle постоянно выполняет неизменённую задачу      | объявлены ли inputs/outputs, поддерживает ли task caching    |
| Gradle пишет `NO-SOURCE`                            | source sets и пути к исходникам                             |
| IDE собирает, wrapper падает                        | JDK, generated sources, IDE-only configuration              |
| локально работает, CI падает                        | wrapper, JDK, profiles/properties, env, clean checkout       |
| после смены ветки видны странные старые outputs     | корректность build logic; затем диагностический `clean`      |

## Практическое задание

### Часть 1. Исследование

Выбери Maven- или Gradle-проект и ответь:

1. Какая версия Java используется для компиляции?
2. Какая версия build tool зафиксирована wrapper?
3. Где находятся main/test sources и outputs?
4. Какая команда запускает unit-тесты?
5. Есть ли отдельные integration tests?
6. Какая команда соответствует полной проверке перед merge?
7. Какие дополнительные quality tasks/goals добавлены plugins?

### Часть 2. Maven

Для Maven-проекта последовательно выполни:

```shell
./mvnw compile
./mvnw test
./mvnw package
./mvnw verify
./mvnw dependency:tree
./mvnw help:effective-pom
```

После каждой команды проверь `target/` и вывод Maven. Зафиксируй:

- какие phases/goals выполнились;
- на какой фазе появился JAR;
- какие тесты были запущены;
- что добавилось после `verify`.

### Часть 3. Gradle

Для Gradle-проекта выполни:

```shell
./gradlew tasks
./gradlew build --dry-run
./gradlew classes
./gradlew test
./gradlew assemble
./gradlew check
./gradlew build
./gradlew build
```

Сравни первый и второй запуск `build`. Найди `UP-TO-DATE`, `FROM-CACHE`,
`NO-SOURCE` или другие статусы и объясни каждый из них.

### Часть 4. Добавление проверки

Maven-вариант:

- подключи простой quality plugin;
- привяжи его goal к `verify`;
- докажи по логу, что `test` его не запускает, а `verify` запускает.

Gradle-вариант:

- зарегистрируй `verifyArchitecture`;
- свяжи её с `check` через `dependsOn`;
- сравни `build --dry-run` до и после изменения.

## Что важно запомнить

1. Maven имеет три встроенных lifecycle: `default`, `clean` и `site`.
2. Вызов Maven phase выполняет все предыдущие phases того же lifecycle.
3. Реальную работу Maven выполняют plugin goals, привязанные к phases.
4. Packaging влияет на стандартные goal bindings.
5. Для полной локальной Maven-проверки обычно выбирают `verify`, но
   окончательное правило задаёт проект.
6. Gradle выполняет task graph, а не универсальный линейный lifecycle.
7. Initialization, configuration и execution в Gradle не являются аналогами
   Maven phases.
8. `build` — lifecycle task, обычно зависящая от `assemble` и `check`.
9. `dependsOn` добавляет необходимую задачу в граф; `mustRunAfter` сам по себе
   этого не делает.
10. `UP-TO-DATE`, Build Cache и Configuration Cache решают разные задачи.
11. `clean` не должен быть автоматическим префиксом каждой локальной сборки.
12. Если проект содержит wrapper, следует запускать именно его.

## Самопроверка

Ответь без подсказки:

1. Чем Maven lifecycle отличается от phase?
2. Чем phase отличается от plugin goal?
3. Почему `./mvnw package` запускает тесты?
4. Почему `clean` не является первой фазой `default` lifecycle?
5. Что дополнительно даёт `verify` по сравнению с `package`?
6. Как packaging влияет на Maven lifecycle?
7. Какие три этапа проходит каждый запуск Gradle?
8. Почему Gradle `build` не означает «выполнить все задачи»?
9. Чем `assemble`, `check` и `build` отличаются друг от друга?
10. Почему `mustRunAfter` не заменяет `dependsOn`?
11. Чем `UP-TO-DATE` отличается от `FROM-CACHE`?
12. Почему постоянный `clean` обычно нежелателен?
13. Зачем нужны Maven Wrapper и Gradle Wrapper?
14. Какие команды помогут исследовать незнакомую сборку?

## Официальная документация

- [Maven: Introduction to the Build Lifecycle](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)
- [Maven: Maven in Five Minutes](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html)
- [Maven Wrapper](https://maven.apache.org/wrapper/)
- [Gradle: Build Lifecycle](https://docs.gradle.org/current/userguide/build_lifecycle.html)
- [Gradle: Java Plugin](https://docs.gradle.org/current/userguide/java_plugin.html)
- [Gradle: Controlling Task Execution](https://docs.gradle.org/current/userguide/controlling_task_execution.html)
- [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html)

[← Вернуться к учебному плану](../java-backend-learning-plan.md)
