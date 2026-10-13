package com.example.wallet.accounts;

import com.example.wallet.accounts.api.AccountMapper;
import com.example.wallet.accounts.exceptions.AccountNotAllowCloseException;
import com.example.wallet.accounts.exceptions.AccountNotAllowStatusChangeException;
import com.example.wallet.clients.ClientId;
import com.example.wallet.shared.money.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {
    private static Account account(String balance) {
        return new Account(
                new AccountId(UUID.randomUUID()),
                new ClientId(UUID.randomUUID()),
                Money.rubles(balance),
                Instant.parse("2026-10-05T10:00:00Z")
        );
    }

    @Test
    void newAccountIsActiveAndCanBeBlockedAndUnblocked() {
        Account account = account("10.00");
        assertEquals(AccountStatus.ACTIVE, account.status());
        assertTrue(account.isActive());

        account.block();
        account.block();
        assertEquals(AccountStatus.BLOCKED, account.status());
        assertFalse(account.isActive());

        account.unblock();
        account.unblock();
        assertTrue(account.isActive());
        account.withdraw(Money.rubles("1.00"));
        assertEquals(Money.rubles("9.00").getAmount(), account.getBalance().getAmount());
    }

    @Test
    void closingWithNonzeroBalancePreservesStatusAndBalance() {
        for (boolean blocked : new boolean[]{false, true}) {
            Account account = account("10.00");
            if (blocked) {
                account.block();
            }
            AccountStatus previousStatus = account.status();

            assertThrows(AccountNotAllowCloseException.class, account::close);
            assertEquals(previousStatus, account.status());
            assertEquals(Money.rubles("10.00").getAmount(), account.getBalance().getAmount());
        }
    }

    @Test
    void zeroBalanceAccountCanBeClosedFromActiveOrBlockedStatus() {
        for (boolean blocked : new boolean[]{false, true}) {
            Account account = account("0.00");
            if (blocked) {
                account.block();
            }

            account.close();
            account.close();
            assertEquals(AccountStatus.CLOSED, account.status());
            assertFalse(account.isActive());
            assertThrows(AccountNotAllowStatusChangeException.class, account::block);
            assertThrows(AccountNotAllowStatusChangeException.class, account::unblock);
            assertEquals(AccountStatus.CLOSED, account.status());
        }
    }

    @Test
    void blockedAndClosedAccountsRejectMoneyOperationsWithoutChangingBalance() {
        Account blocked = account("10.00");
        blocked.block();
        Account closed = account("0.00");
        closed.close();

        for (Account account : new Account[]{blocked, closed}) {
            Money previousBalance = account.getBalance();
            assertThrows(IllegalStateException.class, () -> account.deposit(Money.rubles("1.00")));
            assertThrows(IllegalStateException.class, () -> account.withdraw(Money.rubles("1.00")));
            assertEquals(previousBalance.getAmount(), account.getBalance().getAmount());
        }
    }

    @Test
    void responseContainsActualStatusAndOriginalCreationTime() {
        Account account = account("0.00");
        Instant createdAt = account.createdAt();
        assertEquals("ACTIVE", AccountMapper.toResponse(account).status());
        account.block();
        assertEquals("BLOCKED", AccountMapper.toResponse(account).status());
        account.close();
        assertEquals("CLOSED", AccountMapper.toResponse(account).status());
        assertEquals(createdAt, AccountMapper.toResponse(account).createdAt());
    }
}
