package com.example.exercises.objectOrientedProgramming.testClasses;

import com.example.exercises.objectOrientedProgramming.entities.Money;

public class CompareMoney {
    static public void compareMoney() {
        Money first = Money.rubles("500.00");
        Money second = Money.rubles("500.00");

        System.out.println("first == second " + (first == second));
        System.out.println("first.equals(second) " + first.equals(second));
        System.out.println("first.hashCode() == second.hashCode() " + (first.hashCode() == second.hashCode()));
    }
}
