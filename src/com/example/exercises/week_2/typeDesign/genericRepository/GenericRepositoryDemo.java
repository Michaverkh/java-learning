package com.example.exercises.week_2.typeDesign.genericRepository;

import com.example.exercises.week_2.typeDesign.AccountId;
import com.example.exercises.week_2.typeDesign.TransferId;

public final class GenericRepositoryDemo {
    public void demonstrate(
            AccountRepository accountRepository,
            TransferRepository transferRepository,
            AccountId accountId,
            TransferId transferId
    ) {
        accountRepository.findById(accountId);
    }
}
