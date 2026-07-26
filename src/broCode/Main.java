package broCode;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        String item;
        char currency = '$';
        int quantity;
        double price;
        double total;

        System.out.println("What type of item do you want to buy?");

        item = scanner.nextLine();
        System.out.println(item);

        scanner.close();
    }
}
