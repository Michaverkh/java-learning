# Maven: lifecycle сборки

## Зачем backend-разработчику понимать lifecycle

Maven в Java-проекте не просто заменяет ручной вызов `javac`. Он:

- определяет структуру исходников и ресурсов;
- формирует compile- и runtime-classpath;
- скачивает и кэширует зависимости;
- компилирует production- и test-код;
- запускает проверки и тесты;
- собирает JAR или WAR;
- публикует артефакты;
- даёт IDE и CI единую модель проекта.

После изучения темы нужно уметь:

- по команде Maven понимать, какие действия будут выполнены;
- выбирать команду для компиляции, тестирования или полной проверки;
- отличать lifecycle, phase и plugin goal;
- исследовать сборку незнакомого проекта;
- использовать Maven Wrapper вместо случайной глобальной версии Maven.

Главная идея:

```text
выбираем фазу → Maven выполняет все предшествующие фазы этого lifecycle
```

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

В Maven эта последовательность представлена фазами lifecycle. Конкретную
работу на фазах выполняют goals Maven-плагинов.

## Стандартная структура Maven-проекта

По умолчанию Maven понимает следующую структуру:

```text
project/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
│       ├── java/
│       └── resources/
└── target/
```

| Элемент                       | Назначение                                                      |
|---                            |---                                                             |
| `pom.xml`                     | модель проекта, зависимости и конфигурация plugins              |
| `settings.xml`                | пользовательские или корпоративные настройки Maven             |
| `target/`                     | стандартный каталог результатов сборки                         |
| `mvnw`, `mvnw.cmd`, `.mvn/`   | Maven Wrapper и его конфигурация                                |

## Lifecycle, phase и goal

### Lifecycle

Lifecycle — упорядоченная последовательность фаз. Maven имеет три встроенных
lifecycle:

| Lifecycle   | Назначение                                                       |
|---          |---                                                              |
| `default`   | компиляция, тестирование, упаковка и публикация проекта           |
| `clean`     | удаление результатов предыдущей сборки                           |
| `site`      | создание и публикация сайта с документацией проекта              |

`clean` не является первой фазой `default`: это отдельный lifecycle. Команда:

```shell
./mvnw clean verify
```

просит Maven выполнить фазу `clean` из lifecycle `clean`, а затем дойти до
`verify` в lifecycle `default`.

### Phase

Phase — именованный этап lifecycle. Она описывает намерение, например
«скомпилировать» или «проверить», а конкретную работу выполняют plugin goals.

Основные фазы `default` lifecycle:

| Фаза                     | Что означает                                                          |
|---                       |---                                                                     |
| `validate`               | проверить корректность и полноту модели проекта                        |
| `compile`                | скомпилировать production-код                                          |
| `test`                   | запустить unit-тесты                                                    |
| `package`                | собрать JAR, WAR или другой артефакт                                   |
| `verify`                 | выполнить дополнительные проверки собранного проекта                  |
| `install`                | положить артефакт в локальный Maven repository                         |
| `deploy`                 | опубликовать артефакт в удалённом repository                           |

Между ними есть дополнительные фазы. Для integration-тестов особенно важна
последовательность:

```text
package
  → pre-integration-test
  → integration-test
  → post-integration-test
  → verify
```

Если вызвать фазу, Maven выполнит все предшествующие фазы того же lifecycle:

```shell
./mvnw package
```

Это не команда «только создать JAR»: перед упаковкой Maven также компилирует
код и запускает unit-тесты.

### Plugin goal

Goal — конкретное действие Maven-плагина. Форма прямого вызова:

```text
plugin-prefix:goal
```

Примеры:

```shell
./mvnw compiler:compile
./mvnw surefire:test
./mvnw dependency:tree
```

Связь понятий:

```text
lifecycle
└── phase
    └── один или несколько привязанных plugin goals
```

Для проекта с packaging `jar` типичные стандартные привязки выглядят так:

| Phase       | Типичный goal                  |
|---          |---                             |
| `compile`   | `compiler:compile`              |
| `test`      | `surefire:test`                 |
| `package`   | `jar:jar`                       |
| `install`   | `install:install`               |
| `deploy`    | `deploy:deploy`                 |

Точные plugins, их версии и дополнительные executions определяются effective
POM. Phase — стабильный пользовательский интерфейс сборки, а goals — реальная
реализация этапа.

### Packaging влияет на привязки

Поле `<packaging>` определяет тип основного артефакта и стандартные goal
bindings:

```xml
<packaging>jar</packaging>
```

Для `jar` фаза `package` создаёт JAR, для `war` — WAR. Для агрегирующего
родительского проекта часто используется:

```xml
<packaging>pom</packaging>
```

Одинаковая команда `./mvnw package` может выполнять разную конкретную работу
для разных packaging, сохраняя одинаковое намерение.

## Минимальный `pom.xml`

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

POM задаёт модель проекта. Разработчик не описывает вручную цепочку
`javac → junit → jar`: стандартные lifecycle bindings предоставляют plugins.

Версии здесь иллюстративны. В реальном проекте нужно использовать версии,
зафиксированные его parent POM или секциями dependency/plugin management.

## Частые команды Maven

| Команда                            | Результат                                                        |
|---                                 |---                                                               |
| `./mvnw compile`                   | компиляция production-кода                                       |
| `./mvnw test`                      | компиляция main/test-кода и запуск unit-тестов                    |
| `./mvnw package`                   | тесты и создание артефакта                                       |
| `./mvnw verify`                    | полный набор локальных проверок до `verify`                       |
| `./mvnw install`                   | проверки и установка артефакта в локальный repository            |
| `./mvnw deploy`                    | проверки и публикация в удалённый repository                     |
| `./mvnw clean verify`              | чистая сборка до `verify`                                        |
| `./mvnw dependency:tree`           | дерево зависимостей                                               |
| `./mvnw help:effective-pom`         | итоговый POM с inheritance, profiles и defaults                  |
| `./mvnw help:active-profiles`       | активные Maven profiles                                          |

На Windows wrapper запускается так:

```powershell
.\mvnw.cmd verify
```

В Git Bash, Linux и macOS:

```shell
./mvnw verify
```

### `test` не всегда означает все тесты

Обычная схема Maven:

- Maven Surefire Plugin запускает unit-тесты на фазе `test`;
- Maven Failsafe Plugin запускает integration-тесты через goals
  `integration-test` и `verify`.

Конкретный проект может использовать другие соглашения, profiles или plugins.
Поэтому нужно смотреть POM, effective POM и вывод сборки.

Для полной локальной проверки обычно безопаснее выбрать:

```shell
./mvnw verify
```

Но окончательную команду перед merge определяет конкретный проект и его CI.

## Аналогия с TypeScript/Node.js

| Maven                         | Приблизительная аналогия в TypeScript/Node.js                    |
|---                            |---                                                               |
| lifecycle                    | стандартизированный pipeline сборки                              |
| phase                        | стандартизированный этап pipeline                                |
| plugin goal                  | конкретная команда инструмента внутри этапа                       |
| plugin                       | пакет, добавляющий build logic и соглашения                       |
| Maven Wrapper                | project-local запуск зафиксированной версии инструмента           |
| `pom.xml`                    | зависимости и декларативная модель build pipeline                 |
| `target/`                    | `dist/`, `build/` или другой каталог генерируемых outputs          |

Аналогия ограничена: `npm run build` не имеет универсальной семантики между
проектами, поскольку это произвольная команда из `package.json`. Maven
lifecycle стандартизирован и одинаково именует основные намерения сборки.

## Maven Wrapper

Maven Wrapper хранит сведения о нужной проекту версии Maven и при необходимости
загружает её:

```text
mvnw
mvnw.cmd
.mvn/wrapper/...
```

Преимущества:

- разработчики и CI используют согласованную версию;
- не требуется глобально установленный Maven;
- обновление версии становится версионируемым изменением;
- сборка лучше воспроизводится в разных окружениях.

Обычное правило:

```text
есть wrapper → запускай wrapper
```

Wrapper scripts и supporting files нужно хранить в репозитории.

## Как читать незнакомую Maven-сборку

1. Найти `mvnw`, `.mvn/` и корневой `pom.xml`.
2. Посмотреть `groupId`, `artifactId`, `version` и `packaging`.
3. Найти parent POM и секцию `modules`.
4. Изучить `properties`, `dependencyManagement` и `dependencies`.
5. Изучить `pluginManagement`, `plugins`, executions и profiles.
6. Выполнить безопасные диагностические команды:

```shell
./mvnw --version
./mvnw help:effective-pom
./mvnw help:active-profiles
./mvnw dependency:tree
```

7. Сверить локальную verification-команду с `README`, CI-конфигурацией и
   contribution guide.

Effective POM важен: видимый `pom.xml` может наследовать большую часть
конфигурации от parent и активных profiles.

## Типичные ловушки

### Считать `package` единственной нужной командой

Integration-тесты или quality gates могут быть привязаны к `verify`. Перед
merge нужно выполнять команду, принятую проектом.

### Вызывать goal вместо lifecycle phase без причины

```shell
./mvnw compiler:compile
```

прямо запускает goal, но может обойти другие действия lifecycle. Для обычной
сборки стабильнее выражать намерение фазой:

```shell
./mvnw compile
```

Прямой вызов goal полезен для самостоятельных диагностических действий вроде
`dependency:tree`.

### Всегда начинать с `clean`

`clean` полезен для проверки сборки с нуля и диагностики устаревших
результатов, но замедляет обратную связь. Он не должен автоматически
предшествовать каждой локальной команде.

### Использовать глобальную версию Maven

Команда `mvn` может запустить версию, отличающуюся от версии CI и проекта.
Если есть wrapper, предпочтителен `mvnw`.

### Полагаться только на кнопку IDE

IDE может использовать другой JDK или собственный build path. При странном
поведении нужно воспроизвести сборку wrapper-командой:

```shell
java -version
./mvnw --version
```

### Путать JDK Maven и target Java version

JDK, на котором работает Maven, и версия Java, для которой компилируется
приложение, — связанные, но разные настройки. `maven.compiler.release` или
Maven Toolchains Plugin помогают зафиксировать target или выбранный JDK, но
требования самого Maven к JDK нужно проверять отдельно.

## Диагностика по симптомам

| Симптом                                           | Что проверить сначала                                         |
|---                                                |---                                                             |
| не запустились integration-тесты                  | Failsafe executions, phases, profiles, naming conventions      |
| используется неожиданная версия plugin            | parent POM, plugin management, effective POM                   |
| IDE собирает, wrapper падает                      | JDK, generated sources, IDE-only configuration                 |
| локально работает, CI падает                      | wrapper, JDK, profiles/properties, env, clean checkout          |
| после смены ветки видны старые outputs            | конфигурацию сборки; затем диагностический `clean`              |

## Практическое задание

### Часть 1. Исследование

Выбери Maven-проект и ответь:

1. Какая версия Java используется для компиляции?
2. Какая версия Maven зафиксирована wrapper?
3. Где находятся main/test sources и outputs?
4. Какая команда запускает unit-тесты?
5. Есть ли отдельные integration-тесты?
6. Какая команда соответствует полной проверке перед merge?
7. Какие дополнительные goals добавлены plugins?

### Часть 2. Выполнение lifecycle

Последовательно выполни:

```shell
./mvnw compile
./mvnw test
./mvnw package
./mvnw verify
./mvnw dependency:tree
./mvnw help:effective-pom
```

После каждой команды проверь `target/` и вывод Maven. Зафиксируй:

- какие phases и goals выполнились;
- на какой фазе появился JAR;
- какие тесты были запущены;
- что добавилось после `verify`.

### Часть 3. Добавление проверки

- подключи простой quality plugin;
- привяжи его goal к `verify`;
- докажи по логу, что `test` его не запускает, а `verify` запускает.

## Что важно запомнить

1. Maven имеет три встроенных lifecycle: `default`, `clean` и `site`.
2. Вызов phase выполняет все предыдущие phases того же lifecycle.
3. Реальную работу выполняют plugin goals, привязанные к phases.
4. Packaging влияет на стандартные goal bindings.
5. Для полной локальной проверки обычно выбирают `verify`, но правило задаёт
   конкретный проект.
6. `clean` — отдельный lifecycle, а не первая фаза `default`.
7. Если проект содержит Maven Wrapper, следует запускать именно его.

## Самопроверка

Ответь без подсказки:

1. Чем Maven lifecycle отличается от phase?
2. Чем phase отличается от plugin goal?
3. Почему `./mvnw package` запускает тесты?
4. Почему `clean` не является первой фазой `default` lifecycle?
5. Что дополнительно даёт `verify` по сравнению с `package`?
6. Как packaging влияет на Maven lifecycle?
7. Почему постоянный `clean` обычно нежелателен?
8. Зачем нужен Maven Wrapper?
9. Какие команды помогут исследовать незнакомую сборку?

## Официальная документация

- [Maven: Introduction to the Build Lifecycle](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)
- [Maven: Maven in Five Minutes](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html)
- [Maven Wrapper](https://maven.apache.org/wrapper/)
