package com.example.exercises.typeDesign;

import java.util.Objects;
import java.util.Set;

public enum AccountStatus {
    ACTIVE(true, Set.of(
            AccountCommand.DEPOSIT,
            AccountCommand.WITHDRAW,
            AccountCommand.TRANSFER,
            AccountCommand.BLOCK,
            AccountCommand.CLOSE)),
    BLOCKED(false, Set.of(
            AccountCommand.UNBLOCK,
            AccountCommand.CLOSE
    )),
    CLOSED(false, Set.of());

    private final boolean operationsAllowed;
    private final Set<AccountCommand> commands;

    AccountStatus(boolean operationsAllowed, Set<AccountCommand> commands) {
        this.operationsAllowed = operationsAllowed;
        this.commands = commands;
    }

    public boolean allowsMoneyOperations() {
        return operationsAllowed;
    }

    public boolean canTransitionTo(AccountStatus target) {
        Objects.requireNonNull(target, "target can not be null");

        return switch (this) {
            case ACTIVE -> switch (target) {
                case ACTIVE -> false;
                case BLOCKED, CLOSED -> true;
            };
            case BLOCKED -> switch (target) {
                case ACTIVE, CLOSED -> true;
                case BLOCKED -> false;
            };
            case CLOSED -> false;
        };
    }

    public Set<AccountCommand> allowedCommands() {
        return commands;
    }

}
