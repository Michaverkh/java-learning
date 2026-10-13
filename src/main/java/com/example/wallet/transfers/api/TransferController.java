package com.example.wallet.transfers.api;

import com.example.wallet.accounts.AccountId;
import com.example.wallet.api.error.InvalidRequestException;
import com.example.wallet.transfers.Transfer;
import com.example.wallet.transfers.TransferCommand;
import com.example.wallet.transfers.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {
    private final TransferService service;

    public TransferController(TransferService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransferResponse> create(
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody CreateTransferRequest request
    ) {
        // Временная проверка до добавления механизма идемпотентности.
        if (key.isBlank() || key.length() > 128) {
            throw new InvalidRequestException("Некорректный Idempotency-Key");
        }
        TransferCommand command = new TransferCommand(
                new AccountId(request.sourceAccountId()),
                new AccountId(request.targetAccountId()),
                request.amount(), request.currency(), request.description());

        Transfer transfer = service.createTransfer(command);
        TransferResponse response = TransferMapper.toResponse(transfer);
        return ResponseEntity.created(
                URI.create("/api/v1/transfers/" + response.id())
        ).body(response);
    }

    @GetMapping(path = "/{transferId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransferResponse> get(@PathVariable UUID transferId) {
        Transfer transfer = service.getTransferById(transferId);
        TransferResponse response = TransferMapper.toResponse(transfer);
        return ResponseEntity.ok(response);
    }
}
