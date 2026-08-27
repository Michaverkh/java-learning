package com.example.exercises.typeDesign;

public final class TransferResultMapper {
    public String code(TransferResult result) {
        return switch (result) {
            case Succeeded succeeded -> "TRANSFER_CREATED";
            case Duplicate duplicate -> "TRANSFER_ALREADY_PROCESSED";
            case Rejected rejected -> switch (rejected.reason()) {
                case RejectionReason.EMPTY_BALANCE -> "INSUFFICIENT_FUNDS";
                case RejectionReason.SANCTIONS -> "TRANSFER_RESTRICTED";
            };
            case Pending pending -> "Pending";
        };
    }
}
