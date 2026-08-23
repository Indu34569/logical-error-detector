public class TestUnreachableFor {

    public static void main(String[] args) {

        int j = 10;

        for (j = 10; j < 5; j++) {
            System.out.println("This loop cannot execute");
        }
    }
}
