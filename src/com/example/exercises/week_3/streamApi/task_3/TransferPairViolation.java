package com.example.exercises.week_3.streamApi.task_3;

/** Причина, по которой набор ledger-записей не образует корректный перевод. */
public enum TransferPairViolation {
    WRONG_ENTRY_COUNT,
    MISSING_TRANSFER_OUT,
    MISSING_TRANSFER_IN,
    SAME_ACCOUNT,
    INVALID_TRANSFER_OUT_AMOUNT_SIGN,
    INVALID_TRANSFER_IN_AMOUNT_SIGN,
    AMOUNTS_DO_NOT_BALANCE
}
