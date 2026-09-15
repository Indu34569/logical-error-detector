public class WebInput {

    public static void main(String[] args) {

        int x = 5;
        int y = 0;

        // 1. Impossible loop condition
        while (x > 10) {
            System.out.println("This loop cannot execute.");
        }

        // 2. Division by zero
        int result = x / y;

        // 3. Constant condition
        if (10 > 20) {
            System.out.println("This condition is always false.");
        }

        // 4. Potential infinite loop
        while (true) {
            System.out.println("Potential infinite loop.");
            break;
        }

    }
}