package com.example;


import com.example.exercises.objectOrientedProgramming.entities.AccountId;
import com.example.exercises.typeDesign.optionalBoundary.AccountLookupService;
import com.example.exercises.typeDesign.optionalBoundary.InMemoryAccountRepository;

public class Main {

    public static void main(String[] args) {
        AccountLookupService accountLookupService = new AccountLookupService(new InMemoryAccountRepository());
        System.out.println(accountLookupService.getRequired(new AccountId("account-1")).getBalance().getAmount());
    }
}

