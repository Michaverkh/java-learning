# Java Learning — Wallet Service

# Ссылка на свагер: http://localhost:8080/swagger-ui/index.html

Учебный проект для изучения Java backend. Приложение Wallet Service использует Java 21, Spring Boot и Maven.

- [Учебный план](lerning-materials/java-backend-learning-plan.md)
- [Требования Wallet Service](src/main/java/com/example/wallet/technical-requirements.md)

## Подготовка

Нужны установленный JDK 21 и Maven, доступные через `PATH`, а также Docker Desktop с поддержкой Docker Compose. Перед
запуском базы запустите Docker Desktop. Maven Wrapper в репозитории пока отсутствует, поэтому команды выполняются через
`mvn`.

Проверьте версии в PowerShell:

```powershell
java -version
javac -version
mvn -version
```

В выводе `mvn -version` проверьте, что Maven использует нужный JDK. При необходимости настройте `JAVA_HOME`
на каталог JDK и добавьте его `bin` в `PATH`.

Все команды ниже выполняются из корня репозитория, где находится `pom.xml`:

```powershell
Set-Location D:\my-projects\education\java-learning
```

При первой сборке Maven скачивает зависимости; для этого нужен доступ к репозиториям зависимостей.

## Компиляция и сборка

Скомпилировать основной код без запуска приложения и тестов:

```powershell
mvn compile
```

Скомпилированные классы появятся в `target/classes`.

Запустить тесты (необходимая компиляция выполняется автоматически):

```powershell
mvn test
```

Собрать исполняемый JAR с запуском тестов:

```powershell
mvn package
```

Результат для текущих координат в `pom.xml`: `target/java-learning-1.0-SNAPSHOT.jar`.

Удалить предыдущие результаты сборки и собрать проект заново:

```powershell
mvn clean package
```

`clean` удаляет каталог `target`, включая скомпилированные классы, JAR и отчёты тестов.

Для быстрой сборки без выполнения тестов:

```powershell
mvn package "-DskipTests"
```

При этом тестовый код всё равно компилируется. Такая сборка не подтверждает, что тесты проходят.

## Локальный запуск приложения вместе с базой

В одном окне PowerShell задайте пароль базы, запустите PostgreSQL и затем приложение:

```powershell
$env:WALLET_DB_PASSWORD="wallet_local_password"
docker compose up -d --wait postgres
docker compose ps
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

`--wait` дожидается готовности PostgreSQL. В выводе `docker compose ps` у контейнера должен быть статус `healthy`.
Приложение подключается к базе `wallet` по адресу `127.0.0.1:15432` под пользователем `wallet_app`.

Если база уже создавалась, введите её существующий пароль. Если пароль задан в локальном файле `.env`, введите то же
значение: Docker Compose читает `.env` автоматически, а Maven и Spring Boot — нет. Переменная из текущего PowerShell
доступна обеим командам и имеет приоритет над `.env` для Docker Compose. Изменение переменной не меняет пароль
пользователя в уже созданной базе.

При старте Flyway применяет миграции из `src/main/resources/db/migration`, затем Hibernate проверяет соответствие
сущностей схеме через `ddl-auto: validate`.

Этот профиль включает подробные журналы приложения, ограничение суммы перевода `100000.00`
и создаёт учебного клиента с ID `11111111-1111-4111-8111-111111111111`. Кошельки для него нужно создать отдельно.

По текущим настройкам сервер доступен по адресу `http://localhost:8080`. Для обращения используйте реализованные
маршруты API: отдельная страница на `/` не предусмотрена. Чтобы остановить приложение, нажмите `Ctrl+C` в терминале
запуска.

После остановки приложения остановите базу:

```powershell
docker compose down
```

Данные PostgreSQL сохраняются в томе `wallet-postgres` и будут доступны при следующем запуске.

Если подключение к `127.0.0.1:15432` отклонено, проверьте состояние и журналы контейнера:

```powershell
docker compose ps
docker compose logs postgres
```

## Запуск собранного JAR

Сначала запустите базу и задайте `WALLET_DB_PASSWORD` в текущем окне PowerShell, как описано выше. Затем соберите и
запустите приложение с профилем `local`:

```powershell
mvn package
java -jar target/java-learning-1.0-SNAPSHOT.jar --spring.profiles.active=local
```

Если порт `8080` занят, можно выбрать другой:

```powershell
java -jar target/java-learning-1.0-SNAPSHOT.jar --spring.profiles.active=local --server.port=8081
```

После изменения исходников JAR нужно собрать заново. Текущие хранилища работают в памяти:
при остановке приложения созданные счета и переводы теряются.
