package com.example.exercises.premitivesAndReferenceTypes;

import java.util.Objects;

public class PremitivesAndReferenceTypes {
    String id;

    public static void increment(int value) {
        value++;
        System.out.println("value in method increment " + value);
    }

    public void account(String id) {
        this.id = Objects.requireNonNull(id, "id must not be null");
    }

    public String getId() {
        return this.id;
    }
}
