# Проверка DTO и проверка метода: два исключения Spring MVC

[← Spring MVC и REST](../spring-mvc-and-rest.md#validation) ·
[← Учебный план](../../java-backend-learning-plan.md)

Общие ограничения Bean Validation и бизнес-проверки уже разобраны в основном материале. Здесь рассматриваем только
выбор механизма MVC и последствия для обработки ошибок. Примеры рассчитаны на современную Spring MVC со встроенной
проверкой методов, появившейся в Spring Framework 6.1, без `@Validated` на классе контроллера.

## 1. Главное различие: где объявлено ограничение

**`MethodArgumentNotValidException` относится к проверке отдельного объекта, переданного аргументом контроллера.
`HandlerMethodValidationException` относится к проверке контракта метода контроллера.**

Сравните две ситуации: в первой `@Min` находится внутри DTO, во второй — непосредственно в сигнатуре контроллера.
Само наличие слова `Method` в названии первого исключения не означает проверку всего метода.

`@Valid` просит проверить вложенные ограничения объекта, но сама не запрещает ни одного значения. Прямое ограничение,
например `@Min` или `@NotNull` на аргументе контроллера, включает проверку метода. При ней вложенные ограничения через
`@Valid` тоже учитываются, поэтому отдельная проверка DTO уступает проверке метода.

Источник этих правил: [Validation в Spring MVC](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html).

## 2. Ограничения внутри HistoryRequest

В Wallet Service номер страницы и её размер входят в `HistoryRequest`:

```java
public record HistoryRequest(
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size
) {
    public HistoryRequest {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }

    public LedgerEntriesQuery toQuery() {
        return new LedgerEntriesQuery(page, size, null, null, null);
    }
}
```

Это сокращённый учебный DTO: фильтры и проверку дат опустили. Сигнатура контроллера:

```java
@GetMapping("/accounts/{accountId}/operations")
public ResponseEntity<?> getHistory(
        @PathVariable UUID accountId,
        @Valid @ModelAttribute HistoryRequest request
) {
    // Вызов сервиса после успешной проверки.
    return ResponseEntity.ok(service.getHistory(accountId, request.toQuery()));
}
```

Ограничения `@Min` и `@Max` объявлены на компонентах DTO, а прямо на аргументах контроллера ограничений нет.
Нет и аргумента `BindingResult` сразу после `request`.

Для `?page=-1` Spring создаёт DTO, проверяет его из-за `@Valid` и находит нарушение `@Min(0)`.
Возникает `MethodArgumentNotValidException`; тело контроллера не выполняется.

Для `?page=abc` ошибка появляется при преобразовании строки в число. При `@ModelAttribute` она тоже передаётся через
`MethodArgumentNotValidException`, хотя причиной является привязка данных, а не ограничение Bean Validation.
Поэтому это исключение нельзя понимать как «значения всегда успешно преобразованы».

Источник поведения привязки и проверки DTO: [аргументы с @ModelAttribute](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/modelattrib-method-args.html).

Если заменить источник DTO на `@Valid @RequestBody CreateTransferRequest`, успешное чтение JSON с нарушением ограничения
тоже ведёт к проверке отдельного объекта. Повреждённое тело или значение, которое JSON-преобразователь не может прочитать,
обычно приводит к `HttpMessageNotReadableException` ещё до этой проверки.
Источник: [аргументы с @RequestBody](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html).

## 3. Ограничения прямо на аргументах контроллера

Другой вариант того же входа:

```java
@GetMapping("/accounts/{accountId}/operations")
public ResponseEntity<?> getHistory(
        @PathVariable UUID accountId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
) {
    // Вызов сервиса после успешной проверки аргументов.
    return ResponseEntity.ok(service.getHistory(
            accountId, new LedgerEntriesQuery(page, size, null, null, null)));
}
```

Для `?page=-1&size=101` значения преобразованы в числа, но нарушают ограничения аргументов метода.
Возникает `HandlerMethodValidationException`, в котором результаты сгруппированы по параметрам: отдельно `page`,
отдельно `size`. Тело метода не выполняется. Для этой проверки не требуется `@Valid` у чисел.

Для `?page=abc` число получить невозможно: возникает `MethodArgumentTypeMismatchException` до проверки метода.
Наличие `@Min` не превращает ошибку преобразования в `HandlerMethodValidationException`.
Источники: [HandlerMethodValidator](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/method/annotation/HandlerMethodValidator.html)
и [MethodArgumentTypeMismatchException](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/method/annotation/MethodArgumentTypeMismatchException.html).

## 4. Смешанная сигнатура: DTO и прямое ограничение

```java
@GetMapping("/accounts/{accountId}/operations")
public ResponseEntity<?> getHistory(
        @PathVariable UUID accountId,
        @Valid @ModelAttribute HistoryRequest request,
        @RequestHeader("X-Client-Label") @NotBlank String clientLabel
) {
    return ResponseEntity.ok(service.getHistory(accountId, request.toQuery()));
}
```

Это фрагмент сигнатуры, а не дополнительное требование к API Wallet Service. Предположим, заголовок присутствует
и содержит `training-client`, а запрос задаёт `page=-1`.

Даже при корректном `clientLabel` аннотация `@NotBlank` в сигнатуре включает проверку метода. Она охватывает и
ограничения полей `request` через `@Valid`. Нарушение `@Min` внутри DTO теперь даёт `HandlerMethodValidationException`.
Механизм выбирается по сигнатуре, а не по тому, какое поле оказалось ошибочным.

Но `page=abc` всё ещё может остановить привязку `@ModelAttribute` раньше, с `MethodArgumentNotValidException`.
Отсутствующий обязательный заголовок также отклоняется до проверки ограничения `@NotBlank`.

Аналогично, `@NotNull @Valid @RequestBody CreateTransferRequest request` включает проверку метода:
`@NotNull` ограничивает сам аргумент, а `@Valid` направляет проверку внутрь DTO. Поэтому одного обработчика
`MethodArgumentNotValidException` для всех возможных сигнатур недостаточно.
Реализация передачи нарушений в исключение: [HandlerMethodValidator](https://github.com/spring-projects/spring-framework/blob/main/spring-web/src/main/java/org/springframework/web/method/annotation/HandlerMethodValidator.java).

## 5. Как читать ошибки и выбирать обработчик

| Что нужно узнать                       | `MethodArgumentNotValidException`                         | `HandlerMethodValidationException`                            |
|----------------------------------------|-----------------------------------------------------------|---------------------------------------------------------------|
| Где лежат результаты                   | `getBindingResult()` для одного объекта                   | `getParameterValidationResults()` по параметрам метода        |
| Ошибки полей DTO                       | `getBindingResult().getFieldErrors()`                     | `ParameterErrors` для соответствующего объекта                |
| Ошибки всего DTO                       | `getBindingResult().getGlobalErrors()`                    | `ParameterErrors.getGlobalErrors()`                           |
| Ошибки простого аргумента              | Обычно не относятся к этому пути                          | `ParameterValidationResult.getResolvableErrors()`             |
| Метод в ResponseEntityExceptionHandler | `handleMethodArgumentNotValid(...)`                       | `handleHandlerMethodValidationException(...)`                 |

`ParameterErrors` является разновидностью результата проверки параметра и предоставляет доступ к полям и ошибкам
объекта. Чтобы обработать DTO и простой аргумент одним обходом, проверяйте тип результата или используйте
`exception.visitResults(...)`. Не приводите любой результат к `ParameterErrors`: результат проверки числа или строки
не обязан содержать поля DTO.
Источник: [ParameterErrors](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/validation/method/ParameterErrors.html).

Для первого исключения можно различать причины так:

```java
for (FieldError error : exception.getBindingResult().getFieldErrors()) {
    String field = error.getField();
    String code = error.getCode();
    if (error.isBindingFailure()) {
        // Например, page=abc: code обычно typeMismatch.
    } else {
        // Например, page=-1: code обычно Min.
    }
}
```

`isBindingFailure()` отделяет ошибку привязки от нарушения ограничения; `getCode()` уточняет причину.
Не пересылайте клиенту техническое сообщение преобразователя: сформируйте понятный текст и согласованный публичный код.
Источник: [FieldError](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/validation/FieldError.html).

Если advice уже наследуется от `ResponseEntityExceptionHandler`, переопределяйте два указанных метода: базовый класс
уже направляет в них соответствующие исключения. Дополнительный `@ExceptionHandler` на переопределённых методах не нужен.
Источник: [ResponseEntityExceptionHandler](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/ResponseEntityExceptionHandler.html).

## 6. BindingResult, @Validated и результат метода

`BindingResult` сразу после DTO позволяет получить ошибки в теле контроллера:

```java
public ResponseEntity<?> getHistory(
        @PathVariable UUID accountId,
        @Valid @ModelAttribute HistoryRequest request,
        BindingResult errors
) {
    if (errors.hasErrors()) {
        // Здесь для краткости пустой ответ. В проекте нужен согласованный ProblemDetail.
        return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.ok(service.getHistory(accountId, request.toQuery()));
}
```

Теперь проверка не обязана завершать запрос исключением: ответственность за отказ передана контроллеру.
В случае проверки метода это работает, если все нарушения относятся к параметрам, за которыми стоит соответствующий
`Errors` или `BindingResult`. Нарушение другого параметра всё ещё вызывает `HandlerMethodValidationException`.
Для единого формата ошибок Wallet Service обычно удобнее централизованная обработка без `BindingResult` в сигнатуре.

`@Validated` на **классе контроллера** включает проверку через Spring-прокси, то есть другой механизм.
При стандартной настройке он обычно выбрасывает `ConstraintViolationException`, а при адаптации результатов —
`MethodValidationException`. Для встроенного пути MVC уберите эту аннотацию с класса контроллера.
Источник: [проверка методов через Spring-прокси](https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html#validation-beanvalidation-spring-method).

Проверка возвращаемого значения выполняется после тела метода. Для неё `HandlerMethodValidationException` сообщает
`isForReturnValue() == true` и статус `500`, для нарушений входа — `400`. Не возвращайте клиенту `VALIDATION_ERROR` и
`400`, если неверное значение сформировал сервер. Проверка ответа не откатывает уже выполненные изменения сервиса.
Источник: [HandlerMethodValidationException](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/method/annotation/HandlerMethodValidationException.html).

## 7. Самопроверка на истории Wallet Service

Без `BindingResult` и `@Validated` на классе предскажите результат:

| Сигнатура                                                | Вход                            | Ожидаемый результат                                |
|----------------------------------------------------------|---------------------------------|----------------------------------------------------|
| `@Valid @ModelAttribute HistoryRequest request`          | `page=-1`                       | `MethodArgumentNotValidException`, `Min`           |
| Та же                                                    | `page=abc`                      | `MethodArgumentNotValidException`, ошибка привязки |
| `@RequestParam @Min(0) int page`                         | `page=-1`                       | `HandlerMethodValidationException`                 |
| Та же                                                    | `page=abc`                      | `MethodArgumentTypeMismatchException`              |
| DTO с `@Valid` и заголовок с `@NotBlank`                 | `page=-1`, корректный заголовок | `HandlerMethodValidationException`                 |

Проверяйте через `MockMvc`: прямой Java-вызов контроллера не запускает эти этапы MVC.
В `andReturn().getResolvedException()` можно проверить фактический класс исключения, затем отдельно проверить
HTTP-статус, `code`, поля `errors` и отсутствие изменений балансов и записей истории.
