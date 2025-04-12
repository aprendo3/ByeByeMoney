import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

enum TransactionType {
    INCOME, EXPENSE
}

class Transaction {
    private String date;
    private String description;
    private double amount;
    private String category;
    TransactionType type;

    public Transaction(String date, String description, double amount, TransactionType type, String category) {
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.category = category;
    }

    public String getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return String.format("%s | %-15s | %8.2f | %-10s | %s",
                date,
                description.substring(0, Math.min(description.length(), 15)),
                (type == TransactionType.EXPENSE ? -1 : 1) * amount,
                category,
                type);
    }
}

class User {
    private String username;
    private String password;
    public List<Transaction> transactions = new ArrayList<>();

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}

public class Main {
    static final String GREEN = "\033[32m";
    static final String RESET = "\033[0m";
    static final String RED = "\033[31m";
    static final String BLUE = "\033[94m";
    static Scanner scanner = new Scanner(System.in);
    static boolean logged = false;
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    static DataStore store = new DataStore();
    static User user = null;

    static List<String> incCats = new ArrayList<>(Arrays.asList("Salary", "Bonus", "Gift", "Investment", "Other"));
    static List<String> expCats = new ArrayList<>(Arrays.asList("Food", "Transport", "Housing", "Entertainment", "Utilities", "Other"));
    static boolean categoriesSavedBefore = false;

    public static void main(String[] args) {
        categoriesSavedBefore = store.loadCategories(incCats, expCats);
        showWelcomeMenu();

        if ( logged && user != null) {
            cleanScreen();
            showUserMenu();
        }
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
            System.out.printf("[%sC%s] Manage Categories\n", GREEN, RESET);
            System.out.printf("[%sQ%s] Quit\n", RED, RESET);
            System.out.println();
            System.out.printf("Please select an %soption%s: ", BLUE, RESET);
            String choice = scanner.nextLine().toLowerCase();

            switch (choice) {
            case "a":
                addTransaction();
                cleanScreen();
                break;
            case "v":
                showTransactions();
                cleanScreen();
                break;
            case "c":
                manageCategories();
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
        if (store.getUser(username) != null) return false;

        store.addUser(new User(username, password));
        return true;
    }

    private static boolean login() {
        System.out.print("Enter your username: ");
        String username = scanner.nextLine();
        System.out.print("Enter your password: ");
        String password = scanner.nextLine();

        if (tryLogin(username, password)) {
            System.out.println("Login successful!");
            logged = true;
        } else {
            System.out.println("Incorrect username or password. Please try again.");
            logged = false;
        }
        return logged;
    }

    private static boolean tryLogin(String username, String password) {
        user = store.getUser(username);
        if (user != null && user.getPassword().equals(password)) return true;

        user = null;
        return false;
    }

    private static void addTransaction() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sAdd Transaction%s\n\n", BLUE, RED, BLUE, RESET);

        TransactionType type;
        while (true) {
            System.out.print("Enter type (I for Income, E for Expense): ");
            String typeChoice = scanner.nextLine().toUpperCase();
            if ("I".equals(typeChoice)) {
                type = TransactionType.INCOME;
                break;
            } else if ("E".equals(typeChoice)) {
                type = TransactionType.EXPENSE;
                break;
            } else {
                System.out.println("Invalid type. Please enter I or E.");
            }
        }

        String date;
        while (true) {
            System.out.printf("Enter date (YYYYMMDD, Enter for today %s): ", LocalDate.now().format(DATE_FORMATTER));
            date = scanner.nextLine();
            if (date.isEmpty()) {
                date = LocalDate.now().format(DATE_FORMATTER);
                break;
            }
            if (isValidDate(date)) {
                break;
            } else {
                System.out.println("Invalid date format. Use YYYYMMDD.");
            }
        }

        System.out.print("Enter description: ");
        String description = scanner.nextLine();

        double amount;
        while (true) {
            System.out.print("Enter amount (positive number): ");
            Double parsedAmount = tryToParseDouble(scanner.nextLine());
            if (parsedAmount != null && parsedAmount > 0) {
                amount = parsedAmount;
                break;
            } else {
                System.out.println("Invalid amount. Please enter a positive number.");
            }
        }

        String category = promptForCategory(type);

        Transaction transaction = new Transaction(date, description, amount, type, category);
        user.transactions.add(transaction);
        store.updateUser(user);

        System.out.println("\nTransaction added successfully!");
        pausePrompt();
    }

    private static boolean isValidDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return false;
        try {
            LocalDate.parse(dateStr, DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static Double tryToParseDouble(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void pausePrompt() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    private static void showTransactions() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sView Transactions%s\n\n", BLUE, RED, BLUE, RESET);

        System.out.println("Date     | Description     |   Amount | Category   | Type");
        System.out.println("---------+-----------------+----------+------------+---------");

        for (int i = 0; i < user.transactions.size(); i++) {
            System.out.println(user.transactions.get(i));
        }

        System.out.println("\n-----------------------------------------------------------------");


        pausePrompt();
    }

    private static String[] getCategoriesForType(TransactionType type) {
        List<String> cats = type == TransactionType.INCOME ? incCats : expCats;
        return cats.toArray(new String[0]);
    }

    private static String promptForCategory(TransactionType type) {
        String[] categories = getCategoriesForType(type);

        System.out.println("\nSelect a category:");
        for (int i = 0; i < categories.length; i++) {
            System.out.printf("%d. %s\n", i + 1, categories[i]);
        }
        System.out.printf("[%sA%s] Add New Category\n", GREEN, RESET);

        while (true) {
            System.out.print("Enter category number or A for add new: ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("A")) {
                String newCategory = promptForNewCategory();
                addCustomCategory(type, newCategory);
                return newCategory;
            }

            Integer choice = tryToParse(input);

            if (choice != null && choice >= 1 && choice <= categories.length) {
                return categories[choice - 1];
            } else {
                System.out.println("Invalid choice. Please enter a number between 1 and " + categories.length + " or A.");
            }
        }
    }

    private static String promptForNewCategory() {
        while (true) {
            System.out.print("Enter new category name: ");
            String newCategory = scanner.nextLine().trim();

            if (!newCategory.isEmpty()) {
                return newCategory;
            } else {
                System.out.println("Category name cannot be empty. Please try again.");
            }
        }
    }

    private static void addCustomCategory(TransactionType type, String cat) {
        List<String> cats = type == TransactionType.INCOME ? incCats : expCats;

        if (cats.stream().anyMatch(c -> c.equalsIgnoreCase(cat))) {
            return;
        }

        cats.add(cat);
    }

    private static void manageCategories() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sManage Categories%s\n\n", BLUE, RED, BLUE, RESET);

        boolean running = true;
        while (running) {
            showAllCategories();

            System.out.println();
            System.out.printf("[%sA%s] Add New Category\n", GREEN, RESET);
            System.out.printf("[%sD%s] Delete Category\n", GREEN, RESET);
            System.out.printf("[%sS%s] Save Categories\n", GREEN, RESET);
            System.out.printf("[%sQ%s] Back to Main Menu\n", RED, RESET);
            System.out.println();
            System.out.printf("Please select an %soption%s: ", BLUE, RESET);

            String choice = scanner.nextLine().toLowerCase();

            switch (choice) {
            case "a":
                addNewCategory();
                break;
            case "d":
                deleteCategory();
                break;
            case "s":
                saveCategories();
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

    private static void showAllCategories() {
        System.out.println("Available Categories:\n");
        for (String category : incCats) {
            System.out.printf("  %s (INCOME)\n", category);
        }

        for (String category : expCats) {
            System.out.printf("  %s (EXPENSE)\n", category);
        }
    }

    private static void addNewCategory() {
        System.out.println("\nAdd New Category");
        System.out.print("Enter category type (I for Income, E for Expense): ");
        String typeChoice = scanner.nextLine().toUpperCase();

        TransactionType type;
        if ("I".equals(typeChoice)) {
            type = TransactionType.INCOME;
        } else if ("E".equals(typeChoice)) {
            type = TransactionType.EXPENSE;
        } else {
            System.out.println("Invalid type. Please enter I or E.");
            return;
        }

        String newCategory = promptForNewCategory();
        addCustomCategory(type, newCategory);

        if (categoriesSavedBefore) {
            store.saveCategories(incCats, expCats);
        }

        System.out.println("Category added successfully!");
        pausePrompt();
    }

    private static void deleteCategory() {
        System.out.println("\nDelete Category");
        System.out.print("Enter category type (I for Income, E for Expense): ");
        String typeChoice = scanner.nextLine().toUpperCase();

        List<String> categories;
        TransactionType type;
        if ("I".equals(typeChoice)) {
            categories = incCats;
            type = TransactionType.INCOME;
        } else if ("E".equals(typeChoice)) {
            categories = expCats;
            type = TransactionType.EXPENSE;
        } else {
            System.out.println("Invalid type. Please enter I or E.");
            return;
        }

        if (categories.isEmpty()) {
            System.out.println("No categories available to delete.");
            pausePrompt();
            return;
        }

        System.out.println("\nSelect category to delete:");
        for (int i = 0; i < categories.size(); i++) {
            System.out.printf("%d. %s\n", i + 1, categories.get(i));
        }

        System.out.print("Enter category number: ");
        String input = scanner.nextLine();
        Integer choice = tryToParse(input);

        if (choice != null && choice >= 1 && choice <= categories.size()) {
            String categoryToRemove = categories.get(choice - 1);

            if (isCategoryInUse(categoryToRemove, type)) {
                System.out.printf("Cannot delete category '%s' because it is in use by one or more transactions.\n", categoryToRemove);
            } else {
                categories.remove(choice - 1);

                if (categoriesSavedBefore) {
                    store.saveCategories(incCats, expCats);
                }

                System.out.printf("Category '%s' deleted successfully!\n", categoryToRemove);
            }
        } else {
            System.out.println("Invalid choice.");
        }

        pausePrompt();
    }

    private static void saveCategories() {
        updateFromTransactions();
        store.saveCategories(incCats, expCats);
        categoriesSavedBefore = true;

        System.out.println("Categories saved successfully!");
        pausePrompt();
    }

    private static boolean isCategoryInUse(String category, TransactionType type) {
        for (Transaction transaction : user.transactions) {
            if (transaction.getType() == type &&
                category.equalsIgnoreCase(transaction.getCategory())) {
                return true;
            }
        }

        return false;
    }

    private static void updateFromTransactions() {
        for (Transaction transaction : user.transactions) {
            String category = transaction.getCategory();
            if (category != null && !category.isEmpty()) {
                addCustomCategory(transaction.getType(), category);
            }
        }
    }
}