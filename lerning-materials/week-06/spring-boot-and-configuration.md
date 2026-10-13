# Неделя 6. Spring Boot и конфигурация

[← Вернуться к учебному плану](../java-backend-learning-plan.md) · [Требования Wallet Service](../../src/main/java/com/example/wallet/technical-requirements.md)

## Цель и отправная точка

Научиться объяснять запуск Spring Boot, выбирать зависимости и передавать настройки в приложение так, чтобы ошибки
конфигурации обнаруживались при старте. Практический результат — Wallet Service с профилями `local` и `test`
и проверяемой настройкой максимальной суммы перевода.

[IoC, DI, beans и жизненный цикл](../week-05/ioc-di-spring-context.md) уже разобраны. Здесь используем эти знания, не
повторяя устройство контейнера. HTTP-контроллеры, транзакции и безопасность относятся к следующим неделям.

Материал привязан к текущему репозиторию: Java 21, Spring Boot **4.1.1**, Maven. В `pom.xml` уже есть
`spring-boot-starter-parent` и `spring-boot-starter`, а в `com.example.wallet` — `WalletApplication`. Примеры ниже —
материал для выполнения практики, а не описание уже внесённых изменений в приложение.

## 1. Что Spring Boot добавляет к Spring

Spring Framework предоставляет контейнер и инструменты для разработки приложения. Spring Boot собирает вокруг них
согласованный способ запуска: управление версиями библиотек, готовые наборы зависимостей, автоматическую настройку
инфраструктуры, загрузку параметров и упаковку запускаемого приложения.

Для разработчика на Node.js полезна такая аналогия: нужно отдельно выбрать библиотеки, связать их, прочитать переменные
окружения, настроить журналирование и запустить сервер. Boot берёт типичные решения на себя, но код перевода денег и
правила работы кошелька по-прежнему пишете вы.

Разделяйте три механизма:

| Механизм              | За что отвечает                                   | Пример для Wallet Service                 |
|-----------------------|---------------------------------------------------|-------------------------------------------|
| Зависимости Maven     | Какие библиотеки доступны приложению              | Spring MVC и встроенный Tomcat            |
| Автоконфигурация Boot | Какие инфраструктурные beans создать при условиях | Инфраструктура обработки HTTP             |
| Настройки приложения  | Какие значения использовать                       | Порт сервера, максимальная сумма перевода |

Добавление строки `server.port: 8081` само по себе не превращает консольное приложение в сервер. Для этого нужны
соответствующие библиотеки и конфигурация. Аналогично, наличие библиотеки в `pom.xml` ещё не означает, что любое её
поведение включено.

## 2. Точка входа и `@SpringBootApplication`

Текущая точка входа уже выглядит так:

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

`main` вызывает обычный Java-метод. Boot подготавливает окружение с настройками, создаёт контекст, регистрирует
компоненты приложения и подходящую автоконфигурацию. Если приложение является веб-приложением, при запуске контекста
поднимается встроенный сервер.

`@SpringBootApplication` объединяет:

- `@SpringBootConfiguration` — помечает основной класс конфигурации, специализируя `@Configuration`;
- `@EnableAutoConfiguration` — включает подбор автоконфигураций;
- `@ComponentScan` — включает поиск компонентов начиная с пакета основного класса.

Это объясняет, почему `com.example.wallet.transfers.TransferService` находится автоматически. Классы упражнений в
соседнем `com.example.exercises` не входят в эту область. Расширять сканирование до всего `com.example`
ради одного потерянного bean опасно: в контекст могут попасть учебные компоненты из других задач.

Источник: [аннотация
`@SpringBootApplication`](https://docs.spring.io/spring-boot/reference/using/using-the-springbootapplication-annotation.html).

## 3. Starters, версии и запускаемый JAR

Starter — зависимость, которая приводит нужный набор библиотек для определённой задачи. Версии управляемых зависимостей
согласует родительский POM Boot; starter не заменяет эту функцию.

| Зависимость                      | Назначение на этой неделе                     |
|----------------------------------|-----------------------------------------------|
| `spring-boot-starter`            | Базовый запуск Boot и журналирование          |
| `spring-boot-starter-webmvc`     | Spring MVC и встроенный Tomcat                |
| `spring-boot-starter-validation` | Проверка ограничений конфигурации             |
| `spring-boot-starter-test`       | Базовые средства тестирования; область `test` |

Для Boot 4 используем `spring-boot-starter-webmvc`. В учебниках по Boot 3 часто встречается
`spring-boot-starter-web`; не переносите названия зависимостей и импорты между версиями механически. Состав наборов
проверяется в [документации Boot](https://docs.spring.io/spring-boot/reference/using/build-systems.html).

Фрагменты для текущего `pom.xml`: зависимости добавляются в существующий `dependencies`, а не во второй блок. Версии
здесь не нужны, поскольку уже подключён родительский POM Boot 4.1.1.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

Текущий `spring-boot-starter` можно оставить: Maven разрешит повторяющиеся транзитивные зависимости. PostgreSQL, JPA,
Flyway и Security сейчас не нужны: каждый из этих наборов добавляет собственные условия запуска.

В существующий `build` добавьте `plugins`:

```xml
<plugins>
    <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
            <mainClass>com.example.wallet.WalletApplication</mainClass>
        </configuration>
    </plugin>
</plugins>
```

Основной класс указан явно, поскольку в учебном репозитории есть и другие методы `main`. С родительским POM Boot
объявленный плагин подключает переупаковку JAR на этапе `package`: внутрь попадают зависимости и загрузчик. Один лишь
`spring-boot-starter-parent` без объявления плагина не делает обычный JAR исполняемым Boot-архивом.
Подробнее: [упаковка запускаемого архива](https://docs.spring.io/spring-boot/maven-plugin/packaging.html).

После выполнения этих изменений команды из корня текущего репозитория:

```powershell
mvn clean package
java -jar target/java-learning-1.0-SNAPSHOT.jar --spring.profiles.active=local
```

Альтернативный запуск во время разработки:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

При веб-запуске процесс продолжает слушать порт; остановка — `Ctrl+C`. До добавления веб-зависимости приложение может
успешно создать контекст и завершиться, когда в нём нет удерживающей процесс работы. Это не обязательно ошибка.

## 4. Автоконфигурация: откуда берутся beans

Boot проверяет условия: доступен ли класс библиотеки, какой тип приложения запускается, есть ли уже нужный bean, какие
параметры заданы. Часто встречаются `@ConditionalOnClass`, `@ConditionalOnMissingBean` и
`@ConditionalOnProperty`.

Например, после подключения MVC starter Boot может настроить веб-инфраструктуру. Но он не придумает
`TransferService` по названию пакета: это ваш компонент и ваши правила.

Если вы объявляете собственный bean, конкретная автоконфигурация может отступить благодаря условию
`@ConditionalOnMissingBean`. Это не универсальное правило «пользовательский bean всегда заменяет любой другой»:
смотрите условия нужной конфигурации. Сканирование компонентов и выбор автоконфигураций — разные механизмы.

Для диагностики запустите:

```powershell
java -jar target/java-learning-1.0-SNAPSHOT.jar --spring.profiles.active=local --debug
```

В отчёте условий найдите совпавшие и несовпавшие конфигурации. Отрицательный результат сам по себе не ошибка:
для приложения без базы данных многие настройки базы не должны применяться. `--debug` также не означает, что абсолютно
все сообщения всех библиотек будут печататься на уровне `DEBUG`.

При неожиданном поведении сначала выясните, какая зависимость и какое условие его включили, затем меняйте конфигурацию.
Исключать автоконфигурации наугад — плохой способ исправить неверный набор зависимостей.

Источник: [автоконфигурация](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html).

## 5. `application.yml` и размещение ресурсов

Проект использует стандартную структуру Maven, которую также создаёт Initializr: код приложения и тесты разделены, а у
каждого набора есть своя папка ресурсов. Переопределять пути в `pom.xml` не требуется.

Для практики используйте:

```text
src/
├── main/
│   ├── java/com/example/wallet/        # Код приложения
│   └── resources/
│       ├── application.yml
│       └── application-local.yml
└── test/
    ├── java/com/example/wallet/
    │   └── configuration/
    │       └── TransferPropertiesTest.java  # Добавляется на шаге 4
    └── resources/
        └── application-test.yml
```

Maven компилирует основной код в `target/classes`, тесты — в `target/test-classes` и копирует туда соответствующие
ресурсы. Тестовый код и тестовые ресурсы не попадают в основной JAR. Учебные упражнения находятся в
`src/main/java/com/example/exercises`, рядом с пакетом приложения.

Не кладите `application.yml` рядом с `WalletApplication.java`: исходники Java и ресурсы проходят разные этапы сборки.
Проверьте результат в `target/classes/application.yml`.

Пример `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: wallet-service

server:
  port: 8080

logging:
  level:
    root: INFO
    com.example.wallet: INFO
```

Вложенность YAML задаётся пробелами. `spring.application.name` — полное имя вложенного свойства. `.yml` и `.yaml`
поддерживаются; выберите одно расширение для проекта. Не создавайте параллельные `application.properties` и YAML с
пересекающимися значениями: это усложняет поиск источника настройки.

## 6. Профили `local` и `test`

Профиль задаёт группу условий и дополнительных настроек. Базовый файл остаётся общим, профильный уточняет значения.

`src/main/resources/application-local.yml`:

```yaml
wallet:
  transfer:
    max-amount: "100000.00"

logging:
  level:
    com.example.wallet: DEBUG
```

`src/test/resources/application-test.yml`:

```yaml
wallet:
  transfer:
    max-amount: "1000.00"

logging:
  level:
    com.example.wallet: WARN
```

Здесь числа лимитов выбраны **только для упражнения**. Технические требования не задают максимальную сумму;
дополнительное правило вводим для практики из учебного плана. Оно не заменяет проверку остатка и другие инварианты.

`local` активируется параметром запуска выше. `test` будет включён через `@ActiveProfiles("test")` в тесте. Само имя
файла `application-test.yml` не активирует профиль, и запуск `mvn test` тоже не включает его автоматически. Maven
profile и Spring profile относятся к разным механизмам: сборке и настройке приложения соответственно.

Тестовый файл находится в ресурсах тестов, поэтому не попадёт в основной JAR. Для запуска собранного JAR с профилем
`test` пришлось бы предоставить настройки отдельно; команды этой главы запускают JAR с `local`.

Не помещайте `spring.profiles.active` в `application-local.yml`: выбор активного профиля должен происходить вне его
собственного файла. При нескольких активных профилях конфликтующие значения зависят от порядка; для этой практики
включайте один профиль за раз.

`@Profile("local")` на классе конфигурации ограничивает регистрацию beans. Применяйте это, когда меняется состав
объектов, например нужен локальный имитатор внешней системы. Для разных числовых значений лимита достаточно файлов. Не
создавайте разные `TransferService` для каждого окружения: бизнес-правила должны оставаться общими.

Источник: [профили Spring Boot](https://docs.spring.io/spring-boot/reference/features/profiles.html).

## 7. Типизированная конфигурация через `@ConfigurationProperties`

В TypeScript конструкция `process.env.MAX_AMOUNT as string` не проверяет значение во время выполнения. Нужно отдельно
преобразовать строку и проверить ограничения. В Boot эти шаги можно сосредоточить в объекте настроек.

Создайте `TransferProperties.java` в текущем пакете `com.example.wallet.configuration`:

```java
package com.example.wallet.configuration;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "wallet.transfer", ignoreUnknownFields = false)
public record TransferProperties(
        @NotNull
        @DecimalMin(value = "0.00", inclusive = false)
        @Digits(integer = 17, fraction = 2)
        BigDecimal maxAmount
) {
}
```

Для этого примера нужен `spring-boot-starter-validation` из раздела 3. Используются аннотации `jakarta.validation`, а не
старые `javax.validation`.

Boot связывает `wallet.transfer.max-amount` с `maxAmount`, создавая объект с `BigDecimal`. Для такого `record`
с одним конструктором дополнительная `@ConstructorBinding` не требуется. Значение `"100000.00"` в YAML записано строкой,
чтобы явно сохранить десятичную запись на входе преобразования.

Проверки имеют отдельные назначения:

- `@NotNull` — настройка обязательна; остальные ограничения не заменяют проверку отсутствия;
- `@DecimalMin` — положительный лимит;
- `@Digits` — не больше 17 цифр целой части и двух дробных, в соответствии с будущим `numeric(19, 2)`;
- `@Validated` — включает проверку объекта настроек при связывании;
- `ignoreUnknownFields = false` — помогает обнаружить опечатку в нашем небольшом пространстве `wallet.transfer`.

Последняя опция не проверяет все настройки Boot. Например, ошибочный ключ вне `wallet.transfer` не обязан привести к той
же ошибке. Держите этот префикс за одним объектом настроек.

Семантика аннотации: [
`ConfigurationProperties` API](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/context/properties/ConfigurationProperties.html).

### Зарегистрировать объект настроек

Добавьте к `WalletApplication` аннотацию и импорт:

```java
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WalletApplication {
    // Существующий main остаётся здесь.
}
```

Это фрагмент изменения существующего класса. Сканирование найдёт `TransferProperties` во вложенном пакете. Одна
`@ConfigurationProperties` сама по себе не регистрирует объект. Альтернатива — явная
`@EnableConfigurationProperties(TransferProperties.class)` на конфигурации; выбирайте один способ. Не добавляйте к этому
`record` `@Component`: создание через связывание параметров конструктора и обычная регистрация компонента — разные пути.

Источник: [
`ConfigurationPropertiesScan` API](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/context/properties/ConfigurationPropertiesScan.html).

### Передать настройку в проверку перевода

Отдельный небольшой компонент позволяет не перегружать пример конструктором существующего `TransferService`:

```java
package com.example.wallet.transfers;

import java.math.BigDecimal;

import com.example.wallet.configuration.TransferProperties;
import org.springframework.stereotype.Component;

@Component
public final class TransferLimitPolicy {
    private final BigDecimal maxAmount;

    public TransferLimitPolicy(TransferProperties properties) {
        this.maxAmount = properties.maxAmount();
    }

    public void check(BigDecimal amount) {
        if (amount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("transfer amount exceeds configured limit");
        }
    }
}
```

Добавьте `TransferLimitPolicy` в конструктор существующего `TransferService` и сохраните в `final`-поле. В
`createTransfer` вызовите `transferLimitPolicy.check(amount)` после имеющихся проверок `null`, положительности и дробной
части, **до изменения балансов и сохранения записей**. `check` предполагает, что эти проверки уже выполнены.

Правило допускает сумму, равную лимиту. Например, при `1000.00` суммы `999.99` и `1000.00` проходят именно проверку
лимита, а `1000.01` — нет. Остальные проверки перевода всё равно обязательны.

Проверка конфигурации и проверка операции отвечают на разные вопросы. Первая запрещает запуск с лимитом `-1`. Вторая
отклоняет конкретный перевод на `1000.01`. Аннотации на `TransferProperties` не проверяют `TransferCommand`. При ручном
`new TransferProperties(...)` проверки Jakarta Validation сами тоже не выполняются.

### Почему не набор `@Value`

`@Value("${wallet.transfer.max-amount}")` подходит для отдельного значения и может использоваться на параметре
конструктора. Но при росте группы настроек отдельный тип даёт единое место для структуры, ограничений и зависимостей. Не
читайте `System.getenv()` внутри `TransferService`: тогда он будет обходить общий механизм конфигурации.

После обычного запуска изменение YAML или переменной окружения не обновляет этот объект автоматически. Для нашего
приложения требуется перезапуск. Динамическое изменение лимитов — самостоятельная задача проектирования.

## 8. Внешняя конфигурация: один JAR, разные значения

Для используемых здесь источников приоритет возрастает слева направо:

```text
application.yml в JAR → профильный файл в JAR → внешние файлы
→ переменные окружения → JVM -D → аргументы приложения --...
```

Это сокращённая схема: тестовые источники и дополнительные механизмы в неё не включены. Полный
порядок: [внешняя конфигурация Boot](https://docs.spring.io/spring-boot/reference/features/external-config.html).

Эксперимент в PowerShell после сборки:

```powershell
$env:WALLET_TRANSFER_MAXAMOUNT = "5000.00"
java -jar target/java-learning-1.0-SNAPSHOT.jar --spring.profiles.active=local --wallet.transfer.max-amount=7000.00
# После остановки приложения:
Remove-Item Env:WALLET_TRANSFER_MAXAMOUNT
```

Ожидаемый лимит — `7000.00`. Уберите аргумент лимита — будет `5000.00`. Удалите переменную — останется
`100000.00` из `local`. В каждом случае запускается тот же JAR; повторная компиляция не нужна.

Имя переменной образовано из `wallet.transfer.max-amount`: точки заменяются подчёркиваниями, дефис удаляется, буквы
становятся заглавными. Поэтому здесь `MAXAMOUNT`, а не `MAX_AMOUNT`.

Ещё один способ — свойство JVM; обратите внимание на положение `-D` перед `-jar`:

```powershell
java -Dwallet.transfer.max-amount=6000.00 -jar target/java-learning-1.0-SNAPSHOT.jar --spring.profiles.active=local
```

Для внешнего каталога настроек:

```powershell
java -jar target/java-learning-1.0-SNAPSHOT.jar --spring.profiles.active=local --spring.config.additional-location=optional:file:./local-config/
```

Создайте в `local-config` файл `application-local.yml` с нужным лимитом. `additional-location` добавляет место поиска,
`location` заменяет стандартные места. `optional:` разрешает отсутствие пути, но не исправляет неверное содержимое.
Относительный путь считается от рабочей папки запуска.

Другой вариант явного имени переменной в YAML:

```yaml
wallet:
  transfer:
    max-amount: ${TRANSFER_LIMIT:100000.00}
```

Это альтернативный пример: `TRANSFER_LIMIT` читается благодаря подстановке, а не правилу преобразования имени. Для
основной практики оставьте прямое связывание через `WALLET_TRANSFER_MAXAMOUNT`.

### Значения по умолчанию и секреты

В основном `application.yml` намеренно нет лимита. Без профиля или внешнего значения приложение после добавления
`TransferProperties` должно отказать в запуске. Это делает обязательную настройку явной. Для порта, наоборот, значение
по умолчанию удобно: его отсутствие не должно блокировать локальную работу.

В будущем пароль БД и другие секреты передаются извне, как требуют технические требования Wallet Service. Не записывайте
секреты в отслеживаемые YAML, README и команды с реальными значениями. Имя профиля `prod` не защищает его содержимое:
ресурс из `src/main/resources` попадёт в JAR независимо от того, активен профиль при сборке или нет. Обычный файл `.env`
Boot автоматически не загружает как Node.js-библиотека dotenv.

## 9. Логирование для диагностики

SLF4J — API, через который код пишет сообщения, а Logback — стандартная реализация при обычном наборе starters.
Используйте logger вместо `System.out.println`, чтобы управлять уровнем и назначением сообщений через настройки.

Фрагмент для компонента проверки лимита; `amount` в нём — аргумент метода:

```java
private static final org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(TransferLimitPolicy.class);

// В методе check, внутри ветки превышения лимита:
log.debug("Transfer rejected: reason=LIMIT_EXCEEDED");
```

Для безопасных технических значений используйте параметры:

```java
log.debug("Configured transfer limit: {}", maxAmount);
```

| Уровень | Что уместно записывать                             |
|---------|----------------------------------------------------|
| ERROR   | Неожиданный сбой, который помешал выполнить работу |
| WARN    | Отклонение, требующее внимания                     |
| INFO    | Значимые события жизненного цикла                  |
| DEBUG   | Подробности для поиска причины проблемы            |
| TRACE   | Очень подробный ход работы, обычно временно        |

Обычный отказ по бизнес-правилу не обязательно является `ERROR`. Не пишите JWT, секреты, полные команды перевода и
персональные данные: это прямо следует из требований проекта. Даже `record.toString()` может раскрыть все поля.
Технический лог также не заменяет будущий аудит денежных операций.

Для расследования проблемы включайте `DEBUG` у своего пакета, как в `local`, вместо глобального `root: DEBUG`. Если
исключение передаётся выше, обычно его журналируют на выбранной границе обработки, чтобы не получить одинаковую
трассировку на каждом уровне. Correlation ID и структурированные журналы разберём с HTTP и наблюдаемостью.

Источник: [настройка журналирования](https://docs.spring.io/spring-boot/reference/features/logging.html).

## 10. Структура пакетов

В проекте уже есть группировка по предметным областям: `accounts`, `clients`, `transfers`, `ledger`. Сохраняем её;
объекты настроек размещаем в существующем `configuration`:

```text
com.example.wallet
├── WalletApplication
├── configuration
│   ├── InfrastructureConfiguration
│   └── TransferProperties
├── accounts
├── clients
├── ledger
├── transfers
│   ├── TransferService
│   └── TransferLimitPolicy
└── shared.money
```

`TransferLimitPolicy` относится к переводам, поэтому живёт рядом с ними. `TransferProperties` описывает входные
параметры приложения. `Money` не должен знать, из YAML или переменной окружения пришёл лимит. Не превращайте `shared` в
общую папку для любых классов: перенос туда оправдан реальным совместным использованием.

Источник: [организация кода Boot-приложения](https://docs.spring.io/spring-boot/reference/using/structuring-your-code.html).

## 11. Практика на текущем Wallet Service

### Шаг 1. Сравнить проект с Initializr

В [Spring Initializr](https://start.spring.io/) выберите Maven, Java 21, совместимую версию Boot 4.1.1, Spring Web и
Validation. Если точной версии уже нет в списке, не обновляйте текущий проект ради генератора:
сравните структуру и оставьте существующую версию. Изучите `pom.xml`, папки ресурсов и Maven Wrapper.

Результат генератора — справочный образец. Продолжайте имеющийся Wallet Service: заменять его целиком и переносить все
прежние упражнения не требуется. В текущем репозитории wrapper отсутствует, поэтому команды выше используют
установленный `mvn`; в сгенерированном проекте эквивалентом будет `.\mvnw.cmd`.

### Шаг 2. Подготовить запуск

Добавьте зависимости и плагин из раздела 3, создайте ресурсы из разделов 5–6. Соберите JAR и запустите с `local`.
Проверьте имя приложения, активный профиль и сообщение о старте сервера. HTTP-ответ `404` по `/` пока ожидаем:
сервер есть, но обработчик этого пути ещё не написан.

### Шаг 3. Подключить ограничение перевода

Добавьте `TransferProperties`, сканирование настроек и `TransferLimitPolicy`. Передайте политику в существующий
`TransferService`. Сохраните проверки суммы, валюты, состояния счетов и достаточности остатка. Для отвергнутого перевода
не должны меняться балансы и появляться записи `Transfer` и ledger.

### Шаг 4. Проверить связывание в тесте

Ниже — отдельный тест настроек; он не требует HTTP-сервера и не заменяет тесты переводов:

```java
package com.example.wallet.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TransferPropertiesTest {
    private final TransferProperties properties;

    @Autowired
    TransferPropertiesTest(TransferProperties properties) {
        this.properties = properties;
    }

    @Test
    void bindsTestLimit() {
        assertThat(properties.maxAmount()).isEqualByComparingTo("1000.00");
    }
}
```

`@Autowired` на конструкторе теста явно включает внедрение через Spring Test. Импорты `org.junit.jupiter`
относятся к Jupiter; конкретную версию тестового стека берите из Boot, не закрепляйте отдельно JUnit 5 только из-за
общего упоминания в плане. Для Boot 4 это особенно важно при чтении старых материалов.

Команда для этого теста:

```powershell
mvn "-Dtest=TransferPropertiesTest" test
```

Перед проверкой уберите переменные окружения из экспериментов, иначе они могут перекрыть файл `test`.
`WebEnvironment.NONE` означает отсутствие сервера. Позже для настоящих HTTP-тестов будем использовать другой режим.
Источник: [тестирование Boot-приложений](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html).

### Шаг 5. Воспроизвести ошибки и границы

Проверьте сценарии последовательно; после каждого завершайте процесс и меняйте только нужный параметр:

| Сценарий                                           | Ожидаемый результат                              |
|----------------------------------------------------|--------------------------------------------------|
| `local`, без внешних переопределений               | Лимит `100000.00`                                |
| Тест с `@ActiveProfiles("test")`                   | Лимит `1000.00`                                  |
| Переменная `5000.00`, аргумент запуска `7000.00`   | Аргумент побеждает: `7000.00`                    |
| `--wallet.transfer.max-amount=abc`                 | Ошибка преобразования, запуск неуспешен          |
| `--wallet.transfer.max-amount=-1.00`               | Ошибка проверки ограничения, запуск неуспешен    |
| `--wallet.transfer.max-amount=1000.001`            | Ошибка дробной части, запуск неуспешен           |
| Нет профиля, внешнего файла и переменной с лимитом | Обязательный лимит отсутствует, запуск неуспешен |
| Сумма перевода равна лимиту                        | Проверка лимита проходит                         |
| Сумма на `0.01` больше лимита                      | Отказ до изменения балансов и создания записей   |

Последние два случая оформите тестами бизнес-логики без контекста Spring; остальные можно сначала исследовать запусками
и тестом связывания. Для чтения фактического лимита используйте отладчик или временное сообщение `DEBUG`
с одним безопасным значением. Не печатайте всё окружение процесса.

Критерий готовности недели: можете собрать и запустить приложение, объяснить источник каждого проверенного значения и
показать, что некорректные параметры не доходят до обработки переводов.

## 12. Как искать ошибку запуска

Читайте описание причины и цепочку `Caused by`, а не только верхнюю строку `Application run failed`.

| Симптом                              | Что проверить первым                                                |
|--------------------------------------|---------------------------------------------------------------------|
| Не найден `TransferProperties`       | Есть ли сканирование настроек, правильный ли пакет                  |
| `Failed to bind properties`          | Префикс, преобразование типа, сообщение нарушения ограничения       |
| Значение неожиданно перекрыто        | Профиль, внешние файлы, окружение IDE, переменные и аргументы       |
| YAML не влияет на приложение         | Попал ли файл в `target/classes`, откуда запущен процесс            |
| Нет сервера или процесс завершился   | Веб-зависимость и тип приложения                                    |
| Порт занят                           | Другой запущенный экземпляр; для опыта задайте `--server.port=8081` |
| JAR не запускается через `java -jar` | Boot Maven Plugin, переупаковка, выбран ли основной JAR             |
| Слишком много beans из упражнений    | Не расширено ли сканирование за `com.example.wallet`                |

После изменения ресурсов пересоберите JAR. Редактирование исходного YAML не меняет содержимое уже собранного архива.

## Самопроверка

1. Чем starter отличается от автоконфигурации и управления версиями?
2. Почему `server.port` не запускает сервер без веб-зависимостей?
3. Какие три возможности объединяет `@SpringBootApplication`?
4. Почему собственный bean не всегда автоматически заменяет инфраструктурный?
5. Почему `application-test.yml` недостаточно для активации `test`?
6. Какое имя переменной соответствует `wallet.transfer.max-amount`?
7. Что победит: профильный YAML, переменная окружения или аргумент `--...`?
8. Чем ошибка преобразования `abc` отличается от отрицательного лимита?
9. Почему нужны одновременно `@NotNull` и `@DecimalMin`?
10. Проверит ли Jakarta Validation объект, созданный обычным `new`, без вызова валидатора?
11. Почему корректный объект настроек ещё не гарантирует корректность перевода?
12. Что произойдёт при изменении YAML после запуска приложения?
13. Почему пароль в неактивном `application-prod.yml` внутри ресурсов всё равно попадёт в JAR?
14. Чем отличаются исходный YAML, ресурс в `target/classes` и внешний конфигурационный файл?

## Что оставить на следующие недели

Сейчас не нужны собственные starters, динамическое обновление настроек, Spring Cloud Config, удалённое хранилище
секретов и подробная настройка Actuator. Следующий шаг — HTTP API из недели 7; там существующий сценарий перевода
получит контроллер, проверку входного запроса и понятный формат ошибки.
