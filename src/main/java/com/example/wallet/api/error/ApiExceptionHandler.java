package com.example.wallet.api.error;

import com.example.wallet.accounts.exceptions.*;
import com.example.wallet.api.filter.CorrelationIdFilter;
import com.example.wallet.clients.ClientNotFoundException;
import com.example.wallet.transfers.exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ProblemDetail> clientNotFound(
            ClientNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "CLIENT_NOT_FOUND", "client-not-found",
                "Клиент не найден", "Клиент не найден", request);
    }

    @ExceptionHandler(AccountNotAllowStatusChangeException.class)
    public ResponseEntity<ProblemDetail> accountStatusChangeNotAllowed(
            AccountNotAllowStatusChangeException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "ACCOUNT_STATUS_CHANGE_NOT_ALLOWED", "account-status-change-not-allowed",
                "Изменение статуса запрещено", "Изменение статуса закрытого счёта запрещено", request);
    }

    @ExceptionHandler(AccountNotAllowCloseException.class)
    public ResponseEntity<ProblemDetail> accountCloseNotAllowed(
            AccountNotAllowCloseException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "ACCOUNT_CLOSE_NOT_ALLOWED", "account-close-not-allowed",
                "Закрытие счета с ненулевым балансом запрещено",
                "Закрытие счета с ненулевым балансом запрещено", request);
    }

    @ExceptionHandler({InvalidRequestException.class, InvalidTransferException.class,
            SameAccountTransferException.class})
    public ResponseEntity<ProblemDetail> invalidRequest(RuntimeException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "validation-error",
                "Некорректный запрос", exception.getMessage(), request);
    }

    @ExceptionHandler(TransferLimitExceededException.class)
    public ResponseEntity<ProblemDetail> transferLimitExceeded(
            TransferLimitExceededException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "TRANSFER_LIMIT_EXCEEDED", "transfer-limit-exceeded",
                "Превышен лимит перевода", exception.getMessage(), request);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ProblemDetail> accountNotFound(
            AccountNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "account-not-found",
                "Счёт не найден", exception.getMessage(), request);
    }

    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<ProblemDetail> accountNotActive(
            AccountNotActiveException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "ACCOUNT_NOT_ACTIVE", "account-not-active",
                "Счёт неактивен", exception.getMessage(), request);
    }

    @ExceptionHandler(TransferCurrencyMismatchException.class)
    public ResponseEntity<ProblemDetail> currencyMismatch(
            TransferCurrencyMismatchException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "CURRENCY_MISMATCH", "currency-mismatch",
                "Недопустимая валюта", exception.getMessage(), request);
    }

    @ExceptionHandler(TransferCreationException.class)
    public ResponseEntity<ProblemDetail> transferCreationFailed(
            TransferCreationException exception, HttpServletRequest request) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "internal-error",
                "Внутренняя ошибка", "Не удалось создать перевод", request);
    }

    @ExceptionHandler(TransferNotFoundException.class)
    public ResponseEntity<ProblemDetail> transferNotFound(
            TransferNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "TRANSFER_NOT_FOUND", "transfer-not-found",
                "Перевод не найден", "Перевод не найден", request);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ProblemDetail> insufficientFunds(
            InsufficientFundsException exception,
            HttpServletRequest request
    ) {
        return problem(HttpStatus.CONFLICT, "INSUFFICIENT_FUNDS", "insufficient-funds",
                "Перевод невозможен", "На исходном счёте недостаточно средств", request);
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status, String code, String type, String title, String detail,
            HttpServletRequest request
    ) {
        return problem(status, code, type, title, detail, request, List.of());
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status, String code, String type, String title, String detail,
            HttpServletRequest request, List<ValidationError> errors
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:wallet:problem:" + type));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        problem.setProperty("correlationId", request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME));
        problem.setProperty("errors", errors);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
