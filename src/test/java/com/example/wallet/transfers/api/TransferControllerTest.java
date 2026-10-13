package com.example.wallet.transfers.api;

import com.example.wallet.accounts.Account;
import com.example.wallet.accounts.AccountId;
import com.example.wallet.accounts.InMemoryAccountRepository;
import com.example.wallet.api.error.ApiExceptionHandler;
import com.example.wallet.api.filter.CorrelationIdFilter;
import com.example.wallet.clients.ClientId;
import com.example.wallet.configuration.TransferProperties;
import com.example.wallet.ledger.InMemoryLedgerRepository;
import com.example.wallet.ledger.LedgerEntry;
import com.example.wallet.ledger.LedgerEntryFactory;
import com.example.wallet.ledger.OperationType;
import com.example.wallet.shared.money.Currency;
import com.example.wallet.shared.money.Money;
import com.example.wallet.transfers.*;
import com.example.wallet.transfers.exceptions.InvalidTransferException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TransferControllerTest {
    private final AccountId sourceId = new AccountId(new UUID(0, 1));
    private final AccountId targetId = new AccountId(new UUID(0, 2));
    private final UUID transferId = new UUID(0, 3);
    private final Instant now = Instant.parse("2026-10-08T09:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final InMemoryTransferRepository transfers = new InMemoryTransferRepository();
    private final InMemoryLedgerRepository ledger = new InMemoryLedgerRepository();
    private final Account source = account(sourceId, "100.00");
    private final Account target = account(targetId, "20.00");

    private Account account(AccountId id, String balance) {
        Account account = new Account(id, new ClientId(new UUID(0, 10)),
                new Money(Currency.RUB, new BigDecimal(balance)), now);
        accounts.save(account);
        return account;
    }

    private TransferService service(Supplier<UUID> transferIds, Supplier<UUID> ledgerIds) {
        return new TransferService(new TransferFactory(transferIds, clock),
                new LedgerEntryFactory(ledgerIds, clock), transfers, accounts, ledger,
                new TransferLimitPolicy(new TransferProperties(new BigDecimal("1000.00"))));
    }

    private MockMvc mvc() {
        return mvc(service(() -> transferId, UUID::randomUUID));
    }

    private MockMvc mvc(TransferService service) {
        return MockMvcBuilders.standaloneSetup(new TransferController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .addFilters(new CorrelationIdFilter()).build();
    }

    private String body(String source, String target, String amount, String currency) {
        return """
                {"sourceAccountId":%s,"targetAccountId":%s,"amount":%s,"currency":%s}
                """.formatted(source, target, amount, currency);
    }

    private String quoted(Object value) {
        return "\"" + value + "\"";
    }

    private String validBody() {
        return body(quoted(sourceId.getValue()), quoted(targetId.getValue()), "10.00", "\"RUB\"");
    }

    private void expectError(MockMvc mvc, String body, String key, int status, String code) throws Exception {
        expectError(mvc, body, key, status, code, null);
    }

    private void expectMvcError(String body, String key, Class<? extends Exception> exceptionType) throws Exception {
        expectError(mvc(), body, key, 400, null, exceptionType);
    }

    private void expectError(MockMvc mvc, String body, String key, int status, String code,
                             Class<? extends Exception> exceptionType) throws Exception {
        var sourceBefore = source.getBalance().getAmount();
        var targetBefore = target.getBalance().getAmount();
        var transferBefore = transfers.findById(transferId);
        var entriesBefore = ledger.findAllByOperationId(transferId);
        var sourceEntriesBefore = ledger.findAllByAccountId(sourceId);
        var targetEntriesBefore = ledger.findAllByAccountId(targetId);

        var request = post("/api/v1/transfers").contentType(MediaType.APPLICATION_JSON).content(body);
        if (key != null) {
            request.header("Idempotency-Key", key);
        }
        var result = mvc.perform(request).andExpect(status().is(status))
                .andExpect(header().exists(CorrelationIdFilter.HEADER_NAME));
        String correlationId = result.andReturn().getResponse().getHeader(CorrelationIdFilter.HEADER_NAME);
        assertEquals(correlationId, UUID.fromString(correlationId).toString());
        if (exceptionType != null) {
            assertInstanceOf(exceptionType, result.andReturn().getResolvedException());
            if (exceptionType == MethodArgumentNotValidException.class) {
                result.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                        .andExpect(jsonPath("$.status").value(400))
                        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                        .andExpect(jsonPath("$.correlationId").value(correlationId))
                        .andExpect(jsonPath("$.instance").value("/api/v1/transfers"))
                        .andExpect(jsonPath("$.errors").isNotEmpty());
            }
        } else {
            result.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(status))
                    .andExpect(jsonPath("$.code").value(code))
                    .andExpect(jsonPath("$.correlationId").value(correlationId))
                    .andExpect(jsonPath("$.instance").value("/api/v1/transfers"));
        }
        if (status == 500) {
            result.andExpect(jsonPath("$.detail").value("Не удалось создать перевод"));
        }

        assertEquals(sourceBefore, source.getBalance().getAmount());
        assertEquals(targetBefore, target.getBalance().getAmount());
        assertEquals(transferBefore, transfers.findById(transferId));
        assertEquals(entriesBefore, ledger.findAllByOperationId(transferId));
        assertEquals(sourceEntriesBefore, ledger.findAllByAccountId(sourceId));
        assertEquals(targetEntriesBefore, ledger.findAllByAccountId(targetId));
    }

    @ParameterizedTest
    @CsvSource({"1000.01,400,TRANSFER_LIMIT_EXCEEDED", "100.01,409,INSUFFICIENT_FUNDS"})
    void invalidAmountPreservesState(String amount, int status, String code) throws Exception {
        expectError(mvc(), body(quoted(sourceId.getValue()), quoted(targetId.getValue()), amount, "\"RUB\""),
                "test-key", status, code);
    }

    @ParameterizedTest
    @CsvSource({"source,missing", "target,missing"})
    void missingAccountPreservesState(String side, String kind) throws Exception {
        String id = kind.equals("null") ? "null" : quoted(new UUID(0, 99));
        expectError(mvc(), body(side.equals("source") ? id : quoted(sourceId.getValue()),
                        side.equals("target") ? id : quoted(targetId.getValue()), "10.00", "\"RUB\""),
                "test-key", kind.equals("null") ? 400 : 404,
                kind.equals("null") ? "VALIDATION_ERROR" : "ACCOUNT_NOT_FOUND");
    }

    @Test
    void sameAccountPreservesState() throws Exception {
        expectError(mvc(), body(quoted(sourceId.getValue()), quoted(sourceId.getValue()), "10.00", "\"RUB\""),
                "test-key", 400, "VALIDATION_ERROR");
    }

    @ParameterizedTest
    @CsvSource({"source", "target"})
    void blockedAccountPreservesState(String side) throws Exception {
        (side.equals("source") ? source : target).block();
        expectError(mvc(), validBody(), "test-key", 409, "ACCOUNT_NOT_ACTIVE");
    }

    @ParameterizedTest
    @CsvSource({"\"USD\",409,CURRENCY_MISMATCH"})
    void invalidCurrencyPreservesState(String currency, int status, String code) throws Exception {
        expectError(mvc(), body(quoted(sourceId.getValue()), quoted(targetId.getValue()), "10.00", currency),
                "test-key", status, code);
    }

    @Test
    void blankKeyPreservesState() throws Exception {
        expectError(mvc(), validBody(), " ", 400, "VALIDATION_ERROR");
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "0", "-1", "1.001", "123456789012345678.00"})
    void invalidAmountIsRejectedBeforeController(String amount) throws Exception {
        expectMvcError(body(quoted(sourceId.getValue()), quoted(targetId.getValue()), amount, "\"RUB\""),
                "test-key", MethodArgumentNotValidException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"sourceAccountId", "targetAccountId", "amount", "currency"})
    void absentRequiredFieldPreservesState(String field) throws Exception {
        String body = validBody().replaceAll("\"" + field + "\":([^,}]+),?", "")
                .replace(",}", "}");
        expectMvcError(body, "test-key", MethodArgumentNotValidException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"source", "target"})
    void nullAccountIdPreservesState(String side) throws Exception {
        expectMvcError(body(side.equals("source") ? "null" : quoted(sourceId.getValue()),
                        side.equals("target") ? "null" : quoted(targetId.getValue()), "10.00", "\"RUB\""),
                "test-key", MethodArgumentNotValidException.class);
    }

    @Test
    void nullCurrencyPreservesState() throws Exception {
        expectMvcError(body(quoted(sourceId.getValue()), quoted(targetId.getValue()), "10.00", "null"),
                "test-key", MethodArgumentNotValidException.class);
    }

    @Test
    void tooLongDescriptionPreservesState() throws Exception {
        String body = validBody().strip().replace("}", ",\"description\":\"" + "я".repeat(501) + "\"}");
        expectMvcError(body, "test-key", MethodArgumentNotValidException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"source", "target"})
    void malformedAccountIdPreservesState(String side) throws Exception {
        expectMvcError(body(side.equals("source") ? "\"bad-uuid\"" : quoted(sourceId.getValue()),
                        side.equals("target") ? "\"bad-uuid\"" : quoted(targetId.getValue()), "10.00", "\"RUB\""),
                "test-key", HttpMessageNotReadableException.class);
    }

    @Test
    void unknownCurrencyPreservesState() throws Exception {
        expectMvcError(body(quoted(sourceId.getValue()), quoted(targetId.getValue()), "10.00", "\"XYZ\""),
                "test-key", HttpMessageNotReadableException.class);
    }

    @Test
    void nonNumericAmountPreservesState() throws Exception {
        expectMvcError(body(quoted(sourceId.getValue()), quoted(targetId.getValue()), "\"не число\"", "\"RUB\""),
                "test-key", HttpMessageNotReadableException.class);
    }

    @Test
    void malformedJsonPreservesState() throws Exception {
        expectMvcError("{\"amount\":", "test-key", HttpMessageNotReadableException.class);
    }

    @Test
    void missingKeyPreservesState() throws Exception {
        expectMvcError(validBody(), null, MissingRequestHeaderException.class);
    }

    @Test
    void tooLongKeyPreservesState() throws Exception {
        expectError(mvc(), validBody(), "k".repeat(129), 400, "VALIDATION_ERROR");
    }

    @Test
    void nullGeneratedTransferIdReturnsSafe500() throws Exception {
        expectError(mvc(service(() -> null, UUID::randomUUID)), validBody(), "test-key", 500, "INTERNAL_ERROR");
    }

    @Test
    void duplicateTransferIdPreservesExistingTransfer() throws Exception {
        transfers.save(new Transfer(transferId, sourceId, targetId, new BigDecimal("5.00"),
                Currency.RUB, TransferStatus.COMPLETED, now, now));
        expectError(mvc(), validBody(), "test-key", 500, "INTERNAL_ERROR");
    }

    @Test
    void duplicateLedgerIdsPreserveState() throws Exception {
        expectError(mvc(service(() -> transferId, () -> new UUID(0, 4))),
                validBody(), "test-key", 500, "INTERNAL_ERROR");
    }

    @Test
    void existingLedgerIdPreservesExistingEntry() throws Exception {
        UUID entryId = new UUID(0, 4);
        LedgerEntry existing = new LedgerEntry(entryId, sourceId, new UUID(0, 99),
                OperationType.DEPOSIT, new BigDecimal("100.00"), new BigDecimal("100.00"), now);
        ledger.save(existing);
        Supplier<UUID> ids = new Supplier<>() {
            private int calls;

            @Override
            public UUID get() {
                return calls++ == 0 ? entryId : new UUID(0, 5);
            }
        };
        expectError(mvc(service(() -> transferId, ids)), validBody(), "test-key", 500, "INTERNAL_ERROR");
        assertEquals(existing, ledger.findById(entryId).orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.00", "\"10.00\""})
    void successfulTransferChangesBothBalancesAndCreatesTwoEntries(String amount) throws Exception {
        String correlationId = UUID.randomUUID().toString();
        String body = body(quoted(sourceId.getValue()), quoted(targetId.getValue()), amount, "\"RUB\"");
        body = body.strip().replace("}", ",\"description\":\"" + "я".repeat(500) + "\"}");
        mvc().perform(post("/api/v1/transfers").header("Idempotency-Key", "test-key")
                        .header(CorrelationIdFilter.HEADER_NAME, correlationId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string(CorrelationIdFilter.HEADER_NAME, correlationId))
                .andExpect(header().string("Location", "/api/v1/transfers/" + transferId))
                .andExpect(jsonPath("$.id").value(transferId.toString()))
                .andExpect(jsonPath("$.amount").value("10.00"));

        assertEquals(new BigDecimal("90.00"), source.getBalance().getAmount());
        assertEquals(new BigDecimal("30.00"), target.getBalance().getAmount());
        assertTrue(transfers.findById(transferId).isPresent());
        assertEquals(2, ledger.findAllByOperationId(transferId).size());
    }

    @Test
    void missingTransferReturns404WithRequestedInstance() throws Exception {
        mvc().perform(get("/api/v1/transfers/" + transferId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("TRANSFER_NOT_FOUND"))
                .andExpect(jsonPath("$.instance").value("/api/v1/transfers/" + transferId));
    }

    @Test
    void existingTransferReturnsResponse() throws Exception {
        transfers.save(new Transfer(transferId, sourceId, targetId, new BigDecimal("5.00"),
                Currency.RUB, TransferStatus.COMPLETED, now, now));
        mvc().perform(get("/api/v1/transfers/" + transferId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transferId.toString()))
                .andExpect(jsonPath("$.amount").value("5.00"));
    }

    @Test
    void negativeAmountReturnsFieldAndReadableMessage() throws Exception {
        String requestBody = body(quoted(sourceId.getValue()), quoted(targetId.getValue()), "-1", "\"RUB\"");
        expectMvcError(requestBody, "test-key", MethodArgumentNotValidException.class);
        mvc().perform(post("/api/v1/transfers").header("Idempotency-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(jsonPath("$.errors[0].field").value("amount"))
                .andExpect(jsonPath("$.errors[0].message").value("Сумма перевода должна быть больше нуля"));
    }

    @Test
    void multipleInvalidFieldsAreReturnedTogether() throws Exception {
        mvc().perform(post("/api/v1/transfers").header("Idempotency-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("null", quoted(targetId.getValue()), "null", "\"RUB\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[*].field", org.hamcrest.Matchers.containsInAnyOrder(
                        "sourceAccountId", "amount")))
                .andExpect(jsonPath("$.errors[*].message", org.hamcrest.Matchers.containsInAnyOrder(
                        "Укажите исходный счёт", "Укажите сумму перевода")));
    }

    @Test
    void nullCommandThrowsDomainException() {
        assertThrows(InvalidTransferException.class,
                () -> service(() -> transferId, UUID::randomUUID).createTransfer(null));
    }
}
