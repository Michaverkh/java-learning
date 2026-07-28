package com.example.wallet.premium;

import com.example.wallet.account.BaseAccount;

public class PremiumAccount extends BaseAccount {
    public long getBalance() {
        return this.balance;
    }

    public long getSecondBalance(PremiumAccount premiumAccount) {
        return premiumAccount.balance;
    }
// Метод будет недоступен так как из другого пакета наследник получает доступ к protected-части именно своего типа и его наследников, но не право просматривать любой произвольный BaseAccount

//    public long getThirdBalance(BaseAccount baseAccount) {
//        return baseAccount.balance;
//    }
}
