package com.example;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Введите целочисленный идентификатор кошелька: ");
            int walletId = scanner.nextInt();

              WalletGreetingService.printGreeting("Идентификатор кошелька установлен: " + walletId);
        }
    }
}
