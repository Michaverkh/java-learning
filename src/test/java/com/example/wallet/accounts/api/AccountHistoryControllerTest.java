package com.example.wallet.accounts.api;

import com.example.wallet.accounts.AccountService;
import com.example.wallet.accounts.InMemoryAccountRepository;
import com.example.wallet.api.error.ApiExceptionHandler;
import com.example.wallet.api.filter.CorrelationIdFilter;
import com.example.wallet.clients.*;
import com.example.wallet.ledger.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccountHistoryControllerTest {
    private final Instant now = Instant.parse("2026-10-01T00:00:00Z");
    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final InMemoryClientRepository clients = new InMemoryClientRepository();
    private final InMemoryLedgerRepository ledger = new InMemoryLedgerRepository();
    private final AccountService service = new AccountService(
            accounts, clients, ledger, Clock.fixed(now, ZoneOffset.UTC));
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AccountController(service))
            .setControllerAdvice(new ApiExceptionHandler())
            .addFilters(new CorrelationIdFilter()).build();

    private UUID createAccount() {
        ClientId clientId = new ClientId(UUID.randomUUID());
        clients.save(new Client(clientId, "history-client", "Иван", ClientStatus.ACTIVE, now, now));
        return service.createAccount(clientId.value()).id().getValue();
    }

    private void saveHistory(UUID accountId) {
        for (int id = 1; id <= 25; id++) {
            ledger.save(new LedgerEntry(new UUID(0, id),
                    new com.example.wallet.accounts.AccountId(accountId), new UUID(0, 1000 + id),
                    OperationType.DEPOSIT, new BigDecimal("1.00"), new BigDecimal("100.00"),
                    now.plusSeconds(id / 2)));
        }
    }

    @Test
    void emptyHistoryUsesDefaultPagination() throws Exception {
        mvc.perform(get("/api/v1/accounts/{accountId}/operations", createAccount()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void unknownAccountReturns404() throws Exception {
        mvc.perform(get("/api/v1/accounts/{accountId}/operations", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void pagesHaveExpectedSizeAndMoneyFormatAndDoNotChangeHistory() throws Exception {
        UUID accountId = createAccount();
        saveHistory(accountId);
        var before = ledger.findAllByAccountId(new com.example.wallet.accounts.AccountId(accountId));

        mvc.perform(get("/api/v1/accounts/{accountId}/operations", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(20))
                .andExpect(jsonPath("$.items[0].accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.items[0].amount").value("1.00"))
                .andExpect(jsonPath("$.items[0].balanceAfter").value("100.00"))
                .andExpect(jsonPath("$.items[0].operationType").value("DEPOSIT"))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/v1/accounts/{accountId}/operations", accountId).param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(5));
        mvc.perform(get("/api/v1/accounts/{accountId}/operations", accountId).param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(25));

        assertEquals(before, ledger.findAllByAccountId(new com.example.wallet.accounts.AccountId(accountId)));
        assertEquals(0, service.getAccountById(accountId).getBalance().getAmount().signum());
    }

    @Test
    void bindsTypeAndDateFiltersBeforePagination() throws Exception {
        UUID accountId = createAccount();
        saveHistory(accountId);
        mvc.perform(get("/api/v1/accounts/{accountId}/operations", accountId)
                        .param("operationType", "DEPOSIT")
                        .param("dateFrom", now.plusSeconds(1).toString())
                        .param("dateTo", now.plusSeconds(2).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/v1/accounts/{accountId}/operations", accountId)
                        .param("operationType", "WITHDRAWAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"page,-1,Min", "size,0,Min", "size,101,Max", "page,abc,typeMismatch",
            "operationType,UNKNOWN,typeMismatch", "dateFrom,invalid,typeMismatch"})
    void invalidParametersReturn400WithoutChangingState(String field, String value, String errorCode)
            throws Exception {
        UUID accountId = createAccount();
        saveHistory(accountId);
        var before = ledger.findAllByAccountId(new com.example.wallet.accounts.AccountId(accountId));
        var result = mvc.perform(get("/api/v1/accounts/{accountId}/operations", accountId).param(field, value))
                .andExpect(status().isBadRequest())
                .andReturn();
        var exception = assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException());
        var error = exception.getBindingResult().getFieldError(field);
        assertNotNull(error);
        assertEquals(errorCode, error.getCode());
        assertEquals(before, ledger.findAllByAccountId(new com.example.wallet.accounts.AccountId(accountId)));
        assertEquals(0, service.getAccountById(accountId).getBalance().getAmount().signum());
    }

    @ParameterizedTest
    @CsvSource({"0", "-1"})
    void emptyOrReversedDateRangeReturns400(int endOffset) throws Exception {
        mvc.perform(get("/api/v1/accounts/{accountId}/operations", createAccount())
                        .param("dateFrom", now.toString())
                        .param("dateTo", now.plusSeconds(endOffset).toString()))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource({"abc,typeMismatch", "-1,Min"})
    void responseExposesConversionAndConstraintErrors(String value, String errorCode) throws Exception {
        mvc.perform(get("/api/v1/accounts/{accountId}/operations", createAccount()).param("page", value))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("page"))
                .andExpect(jsonPath("$.errors[0].code").value(errorCode));
    }
}
