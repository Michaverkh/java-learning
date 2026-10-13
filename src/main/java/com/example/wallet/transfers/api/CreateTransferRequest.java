package com.example.wallet.transfers.api;

import com.example.wallet.shared.money.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransferRequest(
        @NotNull(message = "Укажите исходный счёт") UUID sourceAccountId,
        @NotNull(message = "Укажите счёт получателя") UUID targetAccountId,
        @NotNull(message = "Укажите сумму перевода")
        @DecimalMin(value = "0.00", inclusive = false, message = "Сумма перевода должна быть больше нуля")
        @Digits(integer = 17, fraction = 2,
                message = "Сумма перевода должна содержать не более 17 цифр до запятой и 2 после запятой")
        BigDecimal amount,
        @NotNull(message = "Укажите валюту перевода") Currency currency,
        @Size(max = 500, message = "Описание должно содержать не более 500 символов") String description
) {
}
