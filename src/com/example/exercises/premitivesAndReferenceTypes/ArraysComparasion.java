package com.example.exercises.premitivesAndReferenceTypes;

import java.util.Arrays;

public class ArraysComparasion {
    public static void showComparation() {
        int[] arr1 = {1, 2, 3};
        int[] arr2 = {1, 2, 3};

        System.out.println("arr1 equal arr2 " + arr1.equals(arr2)); // false, тк equals не работает с массивами
        System.out.println("arr1 arr2 Arrays.equals  " + Arrays.equals(arr1, arr2)); // true

        int[][] arr3 = {{1}, {1, 2}, {3}};
        int[][] arr4 = {{1}, {1, 2}, {3}};

        System.out.println("arr1 arr2 Arrays.equals  " + Arrays.equals(arr3, arr4)); // false, так как метод производит только поверхностное сравнение
        System.out.println("arr1 arr2 Arrays.equals  " + Arrays.deepEquals(arr3, arr4)); // true
    }
}

/*
Сравни два независимых int[] с одинаковыми элементами через:

==;
equals;
Arrays.equals.
Затем повтори эксперимент для int[][] с Arrays.equals и Arrays.deepEquals.
 */