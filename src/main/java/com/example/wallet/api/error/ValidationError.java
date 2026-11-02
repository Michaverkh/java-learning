package com.example.wallet.api.error;

public record ValidationError(String field, String code, String message) {
}
