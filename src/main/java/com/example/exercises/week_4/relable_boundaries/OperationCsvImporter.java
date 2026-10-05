package com.example.exercises.week_4.relable_boundaries;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Граница импорта операций из CSV-файла.
 *
 * <p>Реализация метода {@link #importOperations(Path)} — практическое задание.
 * Не меняй его контракт: ошибки отдельных строк должны попасть в {@link ImportReport},
 * а невозможность прочитать файл будет обработана в следующем задании.</p>
 */
public final class OperationCsvImporter {
    public ImportReport importOperations(Path path) throws OperationImportException {
        Objects.requireNonNull(path, "path must not be null");

        List<ImportedOperation> operations = new ArrayList<>();
        List<ImportIssue> issues = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            reader.readLine(); // первая строка — заголовок

            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                String[] columns = line.split(";", -1);

                if (columns.length != 5) {
                    issues.add(new ImportIssue(lineNumber, "line", "Expected 5 columns, but got " + columns.length));

                    continue;
                }

                String operationIdValue = columns[0];
                String occurredAtValue = columns[1];
                String amountValue = columns[2];
                String currencyValue = columns[3];
                String typeValue = columns[4];

                OperationId operationId;
                try {
                    operationId = new OperationId(UUID.fromString(operationIdValue));
                } catch (IllegalArgumentException exception) {
                    issues.add(new ImportIssue(
                            lineNumber,
                            "operationId",
                            "Operation ID must be a UUID"
                    ));
                    continue;
                }

                Instant occurredAt;
                try {
                    occurredAt = Instant.parse(occurredAtValue);
                } catch (DateTimeParseException exception) {
                    issues.add(new ImportIssue(
                            lineNumber,
                            "occurredAt",
                            "occurredAt must be a Instant"
                    ));
                    continue;
                }

                BigDecimal amount;
                try {
                    amount = new BigDecimal(amountValue);
                } catch (IllegalArgumentException exception) {
                    issues.add(new ImportIssue(
                            lineNumber,
                            "amount",
                            "amount must be a number"
                    ));
                    continue;
                }

                Currency currency;
                try {
                    currency = Currency.valueOf(currencyValue);
                } catch (IllegalArgumentException exception) {
                    issues.add(new ImportIssue(
                            lineNumber,
                            "currency",
                            "currency must be a RUB EUR or USD"
                    ));
                    continue;
                }

                OperationType type;
                try {
                    type = OperationType.valueOf(typeValue);
                } catch (IllegalArgumentException exception) {
                    issues.add(new ImportIssue(
                            lineNumber,
                            "type",
                            "Unknown operation type"
                    ));
                    continue;
                }

                Money money;
                try {
                    money = new Money(amount, currency);
                } catch (IllegalArgumentException exception) {
                    issues.add(new ImportIssue(
                            lineNumber,
                            "money",
                            "Amount must be positive and have at most two decimal places"
                    ));
                    continue;
                }

                ImportedOperation operation = new ImportedOperation(operationId, occurredAt, money, type);
                operations.add(operation);
            }

            return new ImportReport(operations, issues);
        } catch (IOException exception) {
            throw new OperationImportException("Cannot read operations file: " + path, exception);
        }
    }
}

// // как работает память Хип, стэк, стринг пул где хранится и как достается Как связано с оперативной памятью