package com.example;

import com.example.exercises.week_3.streamApi.task_2.AccountId;
import com.example.exercises.week_3.streamApi.task_2.AccountTurnover;
import com.example.exercises.week_3.streamApi.task_2.TurnoverCalculator;
import com.example.exercises.week_3.streamApi.task_2.TurnoverCalculatorTestData;

import java.util.Map;

public class Main {

    public static void main(String[] args) {
        TurnoverCalculatorTestData testData = TurnoverCalculatorTestData.create();
        TurnoverCalculator calculator = new TurnoverCalculator();

        Map<AccountId, AccountTurnover> report = calculator.calculate(
                testData.accountIds(),
                testData.entries()
        );

        System.out.println("Обороты по счетам:");
        printTurnover("A", report.get(testData.accountA()));
        printTurnover("B", report.get(testData.accountB()));
        printTurnover("C", report.get(testData.accountC()));
    }

    private static void printTurnover(String accountName, AccountTurnover turnover) {
        System.out.printf(
                "%s: incoming=%s, outgoing=%s, netChange=%s%n",
                accountName,
                turnover.incoming(),
                turnover.outgoing(),
                turnover.netChange()
        );
    }
}
