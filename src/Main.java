import java.util.Scanner;

public class Main {
    static final String GREEN = "\033[32m";
    static final String RESET = "\033[0m";
    static final String RED = "\033[31m";
    static final String BLUE = "\033[94m";
    static Scanner scanner = new Scanner(System.in);
    static boolean logged = false;

    public static void main(String[] args) {
        showWelcomeMenu();
    }

    private static void showUserMenu() {
        boolean running = true;
        while (running) {
            System.out.println("=====================================");
            System.out.println("\033[94mWelcome to Bye Bye Money\033[0m");
            System.out.println("=====================================");
            System.out.println();
            System.out.printf("[%sA%s] Add Transaction\n", GREEN, RESET);
            System.out.printf("[%sV%s] View Transactions\n", GREEN, RESET);
            System.out.printf("[%sQ%s] Quit\n", RED, RESET);
            System.out.println();
            System.out.printf("Please select an %soption%s: ", BLUE, RESET);
            String choice = scanner.nextLine().toLowerCase();

            switch (choice) {
            case "a":
                cleanScreen();
                break;
            case "v":
                cleanScreen();
                break;
            case "q":
                running = false;
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
                break;
            }
        }
    }

    private static Integer tryToParse(String choice) {
        try {
            return Integer.parseInt(choice);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void cleanScreen() {
        System.out.println(new String(new char[50]).replace("\0", "\n"));
    }

    private static void showWelcomeMenu() {
        boolean running = true;
        while (running) {
            System.out.println("=====================================");
            System.out.println("\033[94mWelcome to Bye Bye Money App\033[0m");
            System.out.println("=====================================");
            System.out.println();
            System.out.printf("[%sL%s] Login\n", GREEN, RESET);
            System.out.printf("[%sR%s] Register\n", GREEN, RESET);
            System.out.printf("[%sQ%s] Quit\n", RED, RESET);
            System.out.println();
            System.out.printf("Please select an %soption%s: ", BLUE, RESET);
            String choice = scanner.nextLine().toLowerCase();

            switch (choice) {
            case "l":
                running = !login();
                break;
            case "r":
                register();
                break;
            case "q":
                running = false;
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
                break;
            }
        }
    }

    private static void register() {
        System.out.print("Enter your username: ");
        String username = scanner.nextLine();
        System.out.print("Enter your password: ");
        String password = scanner.nextLine();
        
        if(username == null || username.trim().isBlank() 
            || password == null || password.trim().isBlank()) {
            System.out.println("Registration Failed! username or password not be empty");
            
            return;
        }

        if (addNewLogin(username, password))
            System.out.println("Registration successful! Please login.");
        else
            System.out.println("Registration Failed! username already exists.");
    }
    
    private static boolean addNewLogin(String username, String password) {
        return true;
    }

    private static boolean login() {
        System.out.print("Enter your username: ");
        String username = scanner.nextLine();
        System.out.print("Enter your password: ");
        String password = scanner.nextLine();
//        if (tryLogin(username, password)) {
//            System.out.println("Login successful!");
//            currentUsername = username;
//            logged = true;
//        } else {
//            System.out.println("Incorrect username or password. Please try again.");
//            logged = false;
//        }
        return logged;
    }
    
//    private static boolean tryLogin(String username, String password) {
//        for (int i = 0; i < logins.size(); i+=1)
//            if (logins.get(i).equals(username) && logins.get(i + 1).equals(password))
//                return true;
//        return false;
//    }
}