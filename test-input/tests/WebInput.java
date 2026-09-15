public class WebInput {

    public static void main(String[] args) {

        int age = 10;

        // 1. Impossible IF Condition
        if (age > 20) {
            System.out.println("Impossible IF");
        }

        // 2. Impossible Loop Condition
        int count = 0;
        while (count > 10) {
            System.out.println("Impossible loop");
        }

        // 3. Division By Zero
        int denominator = 0;
        int result = 10 / denominator;

        // 4. Constant Condition
        if (5 > 2) {
            System.out.println("Constant condition");
        }

        // 5. Infinite Loop
        while (true) {
            break;
        }

        // 6. Missing Loop Update
        int i = 0;
        while (i < 10) {
            System.out.println(i);
        }

        // 7. Uninitialized Variable
        int value;
        System.out.println(value);

        // 8. Dead Assignment
        int number = 10;
        number = 20;
        number = 30;
        System.out.println(number);

        // 9. Unreachable code
        if (age > 0) {
            return;
            // Code after return would be unreachable,
            // but we don't put another statement here
            // because Java would reject it.
        }
    }
}