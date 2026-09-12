public class LargeTestProgram {

    public static void main(String[] args) {

        int age = 25;
        int marks = 45;
        int salary = 30000;
        int experience = 5;
        int temperature = 30;

        if (age < 10) {
            System.out.println("Child");
        }

        if (marks > 90) {
            System.out.println("Excellent");
        }

        if (salary > 20000) {
            System.out.println("Good Salary");
        }

        if (experience > 10) {
            System.out.println("Experienced");
        }

        if (temperature < 20) {
            System.out.println("Cold");
        }

        if (age > 18) {
            System.out.println("Adult");
        }

        if (marks < 50) {
            System.out.println("Pass");
        }

        System.out.println("Analysis completed");

    }

}