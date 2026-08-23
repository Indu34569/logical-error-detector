public class TestContradictoryCondition {

    public static void main(String[] args) {

        int x = 5;

        if (x > 10 && x < 3) {
            System.out.println("This condition is impossible");
        }
    }
}
