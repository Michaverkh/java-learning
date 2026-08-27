package com.example.exercises.objectOrientedProgramming.testClasses;

public class Overload {
    public static void run() {
        Account account = new PremiumAccount();
        System.out.println(describe(account));
    }

    static String describe(Account account) {
        return "Account: " + account.type();
    }

    static String describe(PremiumAccount account) {
        return "PremiumAccount: " + account.type();
    }
}

class Account {
    String type() {
        return "account";
    }
}

class PremiumAccount extends Account {
    @Override
    String type() {
        return "premium";
    }
}



