package com.example.exercises.premitivesAndReferenceTypes;

public class UnicodeGame {
    public static void getInfo() {
        String a = "A";
        String rub = "₽";
        String smile = "😀";

        // length каждой из строк
        System.out.println("length");
        System.out.println("a " + a.length());
        System.out.println("rub " + rub.length());
        System.out.println("smile " + smile.length());

        // количество code points;
        System.out.println("code points");
        System.out.println("a " + a.codePointCount(0, a.length()));
        System.out.println("rub " + rub.codePointCount(0, rub.length()));
        System.out.println("smile " + smile.codePointCount(0, smile.length()));

        // значения char по индексам;
        System.out.println(" значения char по индексам:");
        System.out.println("a " + a.charAt(0));
        System.out.println("rub " + rub.charAt(0));
        System.out.println("smile " + smile.charAt(0) + " " + smile.charAt(1));

        // code points в hexadecimal form;
        System.out.println(" code points в hexadecimal form");
        UnicodeGame.printCodePoints(a);
        UnicodeGame.printCodePoints(rub);
        UnicodeGame.printCodePoints(smile);

        //  почему обход через charAt(i) не всегда равен обходу символов?
        // - Потому что могут встречаться символы, которые представлены парой char (Например "😀")
    }

    static void printCodePoints(String text) {
        text.codePoints().forEach(codePoint -> System.out.printf("U+%X%n", codePoint));
    }

}

// Для строк "A", "₽" и "😀" выведи:

/*
length();
количество code points;
значения char по индексам;
code points в hexadecimal form.
Объясни, почему обход через charAt(i) не всегда равен обходу символов.
 */