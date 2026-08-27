package com.example.exercises.typeDesign.genericRepository;

import com.example.exercises.typeDesign.AccountId;
import com.example.exercises.typeDesign.TransferId;

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
