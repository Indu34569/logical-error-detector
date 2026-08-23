public class TestProgram {

    public static void main(String[] args) {

        int x = 5;
        int y = 20;


        // ========================================
        // TEST 1: NORMAL WHILE LOOP
        // ========================================

        int a = 0;

        while (a < 5) {
            a++;
        }


        // ========================================
        // TEST 2: UNREACHABLE WHILE LOOP
        // ========================================

        int b = 10;

        while (b < 5) {
            b++;
        }


        // ========================================
        // TEST 3: WHILE TRUE WITH BREAK
        // ========================================

        while (true) {

            System.out.println("Loop with break");

            break;
        }


        // ========================================
        // TEST 4: DEFINITE INFINITE LOOP
        // ========================================

        while (true) {

            System.out.println("Infinite loop");
        }


        // ========================================
        // TEST 5: NORMAL DO-WHILE
        // ========================================

        int c = 0;

        do {

            c++;

        } while (c < 5);


        // ========================================
        // TEST 6: DO-WHILE EXECUTES ONCE
        // ========================================

        int d = 10;

        do {

            d++;

        } while (d < 5);


        // ========================================
        // TEST 7: NORMAL FOR LOOP
        // ========================================

        for (int i = 0; i < 5; i++) {

            System.out.println(i);
        }


        // ========================================
        // TEST 8: UNREACHABLE FOR LOOP
        // ========================================

        for (int j = 10; j < 5; j++) {

            System.out.println(j);
        }


        // ========================================
        // TEST 9: BREAK
        // ========================================

        for (int k = 0; k < 10; k++) {

            if (k == 5) {

                break;
            }
        }


        // ========================================
        // TEST 10: CONTINUE
        // ========================================

        for (int m = 0; m < 10; m++) {

            if (m == 5) {

                continue;
            }

            System.out.println(m);
        }


        // ========================================
        // TEST 11: NESTED LOOP
        // ========================================

        for (int p = 0; p < 3; p++) {

            int q = 0;

            while (q < 3) {

                q++;
            }
        }


        // ========================================
        // TEST 12: VALID COMPOUND CONDITION
        // ========================================

        if (x < 10 && y > 10) {

            System.out.println("Valid compound condition");
        }


        // ========================================
        // TEST 13: CONTRADICTORY COMPOUND CONDITION
        // ========================================

        if (x > 10 && x < 3) {

            System.out.println("Impossible condition");
        }


        // ========================================
        // TEST 14: VALID OR CONDITION
        // ========================================

        if (x == 5 || y == 100) {

            System.out.println("Valid OR condition");
        }
    }
}