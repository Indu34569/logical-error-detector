public class TestUnreachableWhile {

    public static void main(String[] args) {

        int b = 10;

        while (b < 5) {
            System.out.println("This loop cannot execute");
        }
    }
}
