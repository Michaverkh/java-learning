package com.example.wallet.accounts.api;

import com.example.wallet.accounts.AccountService;
import com.example.wallet.accounts.AccountStatus;
import com.example.wallet.accounts.InMemoryAccountRepository;
import com.example.wallet.api.error.ApiExceptionHandler;
import com.example.wallet.api.filter.CorrelationIdFilter;
import com.example.wallet.clients.*;
import com.example.wallet.ledger.InMemoryLedgerRepository;
import com.example.wallet.shared.money.Currency;
import com.example.wallet.shared.money.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccountControllerTest {
    private final ClientId clientId = new ClientId(UUID.randomUUID());
    private final InMemoryClientRepository clients = new InMemoryClientRepository();
    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final Instant now = Instant.parse("2026-10-06T09:00:00Z");
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AccountController(
            new AccountService(accounts, clients, new InMemoryLedgerRepository(), Clock.fixed(now, ZoneOffset.UTC))
    )).setControllerAdvice(new ApiExceptionHandler())
            .addFilters(new CorrelationIdFilter()).build();

    @Test
    void unknownClientReturns404WithoutCreatingAccount() throws Exception {
        mvc.perform(post("/api/v1/clients/{clientId}/accounts", clientId.value()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("CLIENT_NOT_FOUND"));

        assertTrue(accounts.findAllByClientId(clientId).isEmpty());
    }

    @Test
    void existingClientReturns201WithAccountLocation() throws Exception {
        clients.save(new Client(clientId, "test-client", "Иван", ClientStatus.ACTIVE, now, now));

        mvc.perform(post("/api/v1/clients/{clientId}/accounts", clientId.value()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance").value("0.00"))
                .andExpect(header().string("Location",
                        "/api/v1/accounts/" + accounts.findAllByClientId(clientId).getFirst().id().getValue()));
    }

    @Test
    void getsCreatedAccountByLocationWithoutChangingState() throws Exception {
        clients.save(new Client(clientId, "test-client", "Иван", ClientStatus.ACTIVE, now, now));
        var created = mvc.perform(post("/api/v1/clients/{clientId}/accounts", clientId.value()))
                .andExpect(status().isCreated())
                .andReturn().getResponse();

        mvc.perform(get(created.getHeader("Location")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(created.getContentAsString()))
                .andExpect(jsonPath("$.clientId").value(clientId.value().toString()))
                .andExpect(jsonPath("$.currency").value("RUB"))
                .andExpect(jsonPath("$.balance").value("0.00"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").value(now.toString()));

        assertEquals(1, accounts.findAllByClientId(clientId).size());
        assertEquals(0, accounts.findAllByClientId(clientId).getFirst().getBalance().getAmount().signum());
    }

    @Test
    void unknownAccountReturns404() throws Exception {
        mvc.perform(get("/api/v1/accounts/{accountId}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void malformedAccountIdReturns400() throws Exception {
        mvc.perform(get("/api/v1/accounts/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blockReturnsUpdatedAccountAndPreservesItForSubsequentReads() throws Exception {
        clients.save(new Client(clientId, "test-client", "Иван", ClientStatus.ACTIVE, now, now));
        mvc.perform(post("/api/v1/clients/{clientId}/accounts", clientId.value()))
                .andExpect(status().isCreated());
        var account = accounts.findAllByClientId(clientId).getFirst();

        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(post("/api/v1/accounts/{accountId}/block", account.id().getValue()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(account.id().getValue().toString()))
                    .andExpect(jsonPath("$.status").value("BLOCKED"))
                    .andExpect(jsonPath("$.balance").value("0.00"));
        }
        mvc.perform(get("/api/v1/accounts/{accountId}", account.id().getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void blockingUnknownAccountReturns404() throws Exception {
        mvc.perform(post("/api/v1/accounts/{accountId}/block", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"block", "unblock"})
    void changingClosedAccountStatusReturns409WithoutChangingState(String operation) throws Exception {
        clients.save(new Client(clientId, "test-client", "Иван", ClientStatus.ACTIVE, now, now));
        mvc.perform(post("/api/v1/clients/{clientId}/accounts", clientId.value()))
                .andExpect(status().isCreated());
        var account = accounts.findAllByClientId(clientId).getFirst();
        account.close();
        accounts.save(account);

        mvc.perform(post("/api/v1/accounts/{accountId}/" + operation, account.id().getValue()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.type").value("urn:wallet:problem:account-status-change-not-allowed"))
                .andExpect(jsonPath("$.code").value("ACCOUNT_STATUS_CHANGE_NOT_ALLOWED"));

        assertEquals(AccountStatus.CLOSED, accounts.findById(account.id()).orElseThrow().status());
        mvc.perform(get("/api/v1/accounts/{accountId}", account.id().getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.balance").value("0.00"));
    }

    @Test
    void closingAccountWithBalanceReturns409WithoutChangingState() throws Exception {
        clients.save(new Client(clientId, "test-client", "Иван", ClientStatus.ACTIVE, now, now));
        mvc.perform(post("/api/v1/clients/{clientId}/accounts", clientId.value()))
                .andExpect(status().isCreated());
        var account = accounts.findAllByClientId(clientId).getFirst();
        account.deposit(new Money(Currency.RUB, new BigDecimal("10.00")));

        mvc.perform(post("/api/v1/accounts/{accountId}/close", account.id().getValue()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("ACCOUNT_CLOSE_NOT_ALLOWED"))
                .andExpect(jsonPath("$.instance").value("/api/v1/accounts/" + account.id().getValue() + "/close"));

        assertEquals(AccountStatus.ACTIVE, account.status());
        assertEquals(new BigDecimal("10.00"), account.getBalance().getAmount());
    }

    @Test
    void listingAccountsOfUnknownClientReturns404() throws Exception {
        mvc.perform(get("/api/v1/clients/{clientId}/accounts", clientId.value()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("CLIENT_NOT_FOUND"))
                .andExpect(jsonPath("$.instance").value("/api/v1/clients/" + clientId.value() + "/accounts"));
    }
}
