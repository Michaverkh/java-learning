package com.example.wallet.accounts.api;

import com.example.wallet.accounts.Account;
import com.example.wallet.accounts.AccountService;
import com.example.wallet.ledger.LedgerEntry;
import com.example.wallet.shared.pagination.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AccountController {
    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping(
            path = "/clients/{clientId}/accounts",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountResponse> create(
            @PathVariable UUID clientId
    ) {
        Account account = service.createAccount(clientId);
        AccountResponse response = AccountMapper.toResponse(account);

        return ResponseEntity.created(
                URI.create("/api/v1/accounts/" + response.id())
        ).body(response);
    }

    @GetMapping(
            path = "/accounts/{accountId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountResponse> get(
            @PathVariable UUID accountId
    ) {
        Account account = service.getAccountById(accountId);
        AccountResponse response = AccountMapper.toResponse(account);

        return ResponseEntity.ok().body(response);
    }

    @GetMapping(
            path = "/clients/{clientId}/accounts",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AccountResponse>> getList(
            @PathVariable UUID clientId
    ) {
        List<Account> accounts = service.getAccountsByClientId(clientId);
        List<AccountResponse> response = AccountMapper.toListResponse(accounts);

        return ResponseEntity.ok().body(response);
    }

    @PostMapping(
            path = "/accounts/{accountId}/block",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountResponse> block(
            @PathVariable UUID accountId
    ) {
        Account account = service.block(accountId);
        AccountResponse response = AccountMapper.toResponse(account);

        return ResponseEntity.ok().body(response);
    }

    @PostMapping(
            path = "/accounts/{accountId}/unblock",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountResponse> unBlock(
            @PathVariable UUID accountId
    ) {
        Account account = service.unblock(accountId);
        AccountResponse response = AccountMapper.toResponse(account);

        return ResponseEntity.ok().body(response);
    }

    @PostMapping(
            path = "/accounts/{accountId}/close",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountResponse> close(
            @PathVariable UUID accountId
    ) {
        Account account = service.close(accountId);
        AccountResponse response = AccountMapper.toResponse(account);

        return ResponseEntity.ok().body(response);
    }

    @GetMapping(
            path = "/accounts/{accountId}/operations",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PageResponse<LedgerEntryResponse>> getHistory(
            @PathVariable UUID accountId,
            @Valid @ModelAttribute HistoryRequest request
    ) {
        PageResponse<LedgerEntry> page =
                service.getHistory(accountId, request.toQuery());

        PageResponse<LedgerEntryResponse> response = new PageResponse<>(
                page.items().stream()
                        .map(LedgerEntryMapper::toResponse)
                        .toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );

        return ResponseEntity.ok(response);
    }

}
