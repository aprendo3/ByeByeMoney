import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

enum TransactionType {
    INCOME, EXPENSE
}

class BudgetGoal {
    private String category;
    private double amount;

    public BudgetGoal(String category, double amount) {
        this.category = category;
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setCategory(String category) {
        this.category = category;
    }
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

    public void setDate(String date) {
        this.date = date;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setCategory(String category) {
        this.category = category;
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
    public List<BudgetGoal> goals = new ArrayList<>();

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
            System.out.printf("[%sA%s] Add Transaction%n", GREEN, RESET);
            System.out.printf("[%sV%s] View/Manage Transactions%n", GREEN, RESET);
            System.out.printf("[%sS%s] Summary Report%n", GREEN, RESET);
            System.out.printf("[%sC%s] Manage Categories%n", GREEN, RESET);
            System.out.printf("[%sG%s] Manage Goals%n", GREEN, RESET);
            System.out.printf("[%sQ%s] Quit%n", RED, RESET);
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
            case "g":
                manageGoals();
                cleanScreen();
                break;
            case "s":
                showSummaryReport();
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
            System.out.printf("[%sL%s] Login%n", GREEN, RESET);
            System.out.printf("[%sR%s] Register%n", GREEN, RESET);
            System.out.printf("[%sQ%s] Quit%n", RED, RESET);
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
        int pageSize = 5;
        int currentPage = 1;

        TransactionType filterType = null;
        String filterCategory = null;
        String filterStartDate = null;
        String filterEndDate = null;

        boolean viewing = true;

        while (viewing) {
            List<Transaction> filtered = applyTransactionFilters(user.transactions, filterType, filterCategory, filterStartDate, filterEndDate);

            int totalItems = filtered.size();
            int totalPages = (totalItems + pageSize - 1) / pageSize;

            if (totalPages == 0) {
                totalPages = 1;
            }

            cleanScreen();

            StringBuilder filterInfo = new StringBuilder();
            if (filterType != null || filterCategory != null || filterStartDate != null) {
                filterInfo.append(" (Filtered by");
                if (filterType != null) {
                    filterInfo.append(String.format(" Type: %s", filterType));
                }
                if (filterCategory != null) {
                    filterInfo.append(String.format("%s Category: %s", filterType != null ? "," : "", filterCategory));
                }
                if (filterStartDate != null) {
                    filterInfo.append(String.format("%s Date: %s", (filterType != null || filterCategory != null) ? "," : "",
                            filterStartDate + (filterEndDate != null ? "-" + filterEndDate : "")));
                }
                filterInfo.append(")");
            }

            System.out.printf("%sBye Bye Money%s > %sView/Manage Transactions%s%s (Page %s%d%s/%s%d%s)%n%n",
                    BLUE, RED, BLUE, RESET, filterInfo.toString(), GREEN, currentPage, RESET, GREEN, totalPages, RESET);

            if (user.transactions.isEmpty()) {
                System.out.println("No transactions found.");
                pausePrompt();
                return;
            }

            if (filtered.isEmpty()) {
                System.out.println("No transactions match the current filters.");
            }

            int startIndex = (currentPage - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, totalItems);

            if (!filtered.isEmpty()) {
                System.out.println("#  | Date     | Description     |   Amount | Category   | Type");
                System.out.println("---+---------+-----------------+----------+------------+---------");

                for (int i = startIndex; i < endIndex; i++) {
                    System.out.printf("%s%2d%s | %s%n", GREEN, i + 1, RESET, filtered.get(i));
                }

                System.out.println("-----------------------------------------------------------------");
                System.out.printf("Showing transactions %s%d%s-%s%d%s of %s%d%s total\n\n",
                        GREEN, startIndex + 1, RESET, GREEN, endIndex, RESET, GREEN, totalItems, RESET);
            }

            System.out.printf("[%sE%s] Edit Transaction%n", GREEN, RESET);
            System.out.printf("[%sD%s] Delete Transaction%n", GREEN, RESET);
            System.out.printf("[%sX%s] Export Transactions%n", GREEN, RESET);
            System.out.printf("[%sI%s] Import Transactions%n", GREEN, RESET);
            System.out.printf("[%sF%s] Filter Transactions%n", GREEN, RESET);

            if (currentPage < totalPages) {
                System.out.printf("[%sN%s] Next Page%n", GREEN, RESET);
            }

            if (currentPage > 1) {
                System.out.printf("[%sP%s] Previous Page%n", GREEN, RESET);
            }

            System.out.printf("[%sB%s] Back to Main Menu%n", RED, RESET);
            System.out.printf("%nPlease select an %soption%s: ", BLUE, RESET);

            String choice = scanner.nextLine().toLowerCase();

            switch (choice) {
            case "e":
                if (!filtered.isEmpty()) {
                    promptForTransactionEdit(filtered);
                } else {
                    System.out.println("No transactions available to edit.");
                    pausePrompt();
                }
                break;
            case "d":
                if (!filtered.isEmpty()) {
                    promptForTransactionDelete(filtered);
                } else {
                    System.out.println("No transactions available to delete.");
                    pausePrompt();
                }
                break;
            case "x":
                exportTransactionsToCsv();
                break;
            case "i":
                importTransactionsFromCsv();
                break;
            case "f":
                Object[] result = promptForFilters(filterType, filterCategory, filterStartDate, filterEndDate);
                if (result != null) {
                    filterType = result[0] != null ? (TransactionType) result[0] : null;
                    filterCategory = (String) result[1];
                    filterStartDate = (String) result[2];
                    filterEndDate = (String) result[3];
                    currentPage = 1;
                }
                break;
            case "n":
                if (currentPage < totalPages) {
                    currentPage++;
                }
                break;
            case "p":
                if (currentPage > 1) {
                    currentPage--;
                }
                break;
            case "b":
                viewing = false;
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
                pausePrompt();
                break;
            }
        }
    }

    private static List<Transaction> applyTransactionFilters(List<Transaction> transactions, TransactionType filterType, String filterCategory, String filterStartDate, String filterEndDate) {
        if (filterType == null && filterCategory == null && filterStartDate == null) {
            return transactions;
        }

        List<Transaction> result = new ArrayList<>();

        for (Transaction transaction : transactions) {
            boolean typeMatch = filterType == null || transaction.getType() == filterType;
            boolean categoryMatch = filterCategory == null || transaction.getCategory().equalsIgnoreCase(filterCategory);
            boolean dateMatch = true;

            if (filterStartDate != null) {
                LocalDate transactionDate = LocalDate.parse(transaction.getDate(), DATE_FORMATTER);
                LocalDate startDate = LocalDate.parse(filterStartDate, DATE_FORMATTER);

                if (filterEndDate != null) {
                    LocalDate endDate = LocalDate.parse(filterEndDate, DATE_FORMATTER);
                    dateMatch = !transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate);
                } else {
                    dateMatch = !transactionDate.isBefore(startDate);
                }
            }

            if (typeMatch && categoryMatch && dateMatch) {
                result.add(transaction);
            }
        }

        return result;
    }

    private static void promptForTransactionEdit(List<Transaction> transactions) {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sEdit Transaction%s%n%n", BLUE, RED, BLUE, RESET);

        System.out.print("Enter transaction number to edit: ");
        String input = scanner.nextLine();
        Integer choice = tryToParse(input);

        if (choice == null || choice < 1 || choice > transactions.size()) {
            System.out.println("Invalid transaction number.");
            pausePrompt();
            return;
        }

        Transaction transaction = transactions.get(choice - 1);
        boolean changed = false;

        System.out.printf("\nEditing Transaction #%d:%n", choice);
        System.out.println(transaction);

        String newDate;
        System.out.printf("Current Date: %s%n", transaction.getDate());
        while (true) {
            System.out.print("Enter new date (YYYYMMDD, Enter to keep): ");
            newDate = scanner.nextLine();

            if (newDate.isEmpty()) {
                break;
            }

            if (isValidDate(newDate)) {
                transaction.setDate(newDate);
                changed = true;
                break;
            } else {
                System.out.println("Invalid date format. Use YYYYMMDD.");
            }
        }

        System.out.printf("%nCurrent Description: %s%n", transaction.getDescription());
        System.out.print("Enter new description (Enter to keep): ");
        String newDescription = scanner.nextLine();

        if (!newDescription.isEmpty()) {
            transaction.setDescription(newDescription);
            changed = true;
        }

        double newAmount;
        System.out.printf("%nCurrent Amount: %.2f%n", transaction.getAmount());
        while (true) {
            System.out.print("Enter new amount (positive number, Enter to keep): ");
            String amountInput = scanner.nextLine();

            if (amountInput.isEmpty()) {
                break;
            }

            Double parsedAmount = tryToParseDouble(amountInput);
            if (parsedAmount != null && parsedAmount > 0) {
                transaction.setAmount(parsedAmount);
                changed = true;
                break;
            } else {
                System.out.println("Invalid amount. Please enter a positive number.");
            }
        }

        System.out.printf("%nCurrent Category: %s%n", transaction.getCategory());
        System.out.print("Change category? (Y/N): ");
        String changeCategory = scanner.nextLine().toLowerCase();

        if ("y".equals(changeCategory)) {
            String newCategory = promptForCategory(transaction.getType());
            if (!newCategory.equals(transaction.getCategory())) {
                transaction.setCategory(newCategory);
                changed = true;
            }
        }

        if (changed) {
            store.updateUser(user);
            System.out.println("\nTransaction updated successfully!");
        } else {
            System.out.println("\nNo changes were made to the transaction.");
        }

        pausePrompt();
    }

    private static void promptForTransactionDelete(List<Transaction> transactions) {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sDelete Transaction%s%n%n", BLUE, RED, BLUE, RESET);

        System.out.print("Enter transaction number to delete: ");
        String input = scanner.nextLine();
        Integer choice = tryToParse(input);

        if (choice == null || choice < 1 || choice > transactions.size()) {
            System.out.println("Invalid transaction number.");
            pausePrompt();
            return;
        }

        Transaction transaction = transactions.get(choice - 1);

        System.out.printf("%nAre you sure you want to delete this transaction?%n");
        System.out.println(transaction);
        System.out.print("\nConfirm deletion (Y/N): ");

        String confirm = scanner.nextLine().toLowerCase();
        if ("y".equals(confirm)) {
            user.transactions.remove(transaction);
            store.updateUser(user);
            System.out.println("\nTransaction deleted successfully!");
        } else {
            System.out.println("\nDeletion cancelled.");
        }

        pausePrompt();
    }

    private static Object[] promptForFilters(TransactionType currentFilterType, String currentFilterCategory,
            String currentFilterStartDate, String currentFilterEndDate) {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sFilter Transactions%s%n%n", BLUE, RED, BLUE, RESET);

        System.out.println("Current Filters:");
        System.out.printf("Type: %s%n", currentFilterType != null ? currentFilterType : "All");
        System.out.printf("Category: %s%n", currentFilterCategory != null ? currentFilterCategory : "All");
        System.out.printf("Date Range: %s%n%n",
                currentFilterStartDate != null ?
                        currentFilterStartDate + (currentFilterEndDate != null ? " to " + currentFilterEndDate : "") :
                        "All");

        System.out.println("Set Filters:");
        System.out.printf("[%s1%s] Set Type Filter%n", GREEN, RESET);
        System.out.printf("[%s2%s] Set Category Filter%n", GREEN, RESET);
        System.out.printf("[%s3%s] Set Date Range Filter%n", GREEN, RESET);
        System.out.printf("[%s4%s] Clear All Filters%n", GREEN, RESET);
        System.out.printf("[%sB%s] Back to Transactions%n%n", RED, RESET);

        System.out.printf("Please select an %soption%s: ", BLUE, RESET);
        String choice = scanner.nextLine();

        TransactionType newFilterType = currentFilterType;
        String newFilterCategory = currentFilterCategory;
        String newFilterStartDate = currentFilterStartDate;
        String newFilterEndDate = currentFilterEndDate;

        switch (choice) {
        case "1":
            System.out.print("\nFilter by type (I=Income, E=Expense, Enter=Clear/All): ");
            String typeInput = scanner.nextLine().toUpperCase();

            if (typeInput.isEmpty()) {
                newFilterType = null;
            } else if ("I".equals(typeInput)) {
                newFilterType = TransactionType.INCOME;
            } else if ("E".equals(typeInput)) {
                newFilterType = TransactionType.EXPENSE;
            } else {
                System.out.println("Invalid type. Please enter I, E, or press Enter.");
                pausePrompt();
                return promptForFilters(currentFilterType, currentFilterCategory, currentFilterStartDate, currentFilterEndDate);
            }
            break;

        case "2":
            System.out.print("\nFilter by category (Enter category name, Enter=Clear/All): ");
            String categoryInput = scanner.nextLine().trim();

            if (categoryInput.isEmpty()) {
                newFilterCategory = null;
            } else {
                newFilterCategory = categoryInput;
            }
            break;

        case "3":
            System.out.print("\nEnter start date (YYYYMMDD, Enter=Clear/All): ");
            String startDateInput = scanner.nextLine().trim();

            if (startDateInput.isEmpty()) {
                newFilterStartDate = null;
                newFilterEndDate = null;
            } else if (isValidDate(startDateInput)) {
                newFilterStartDate = startDateInput;

                System.out.print("Enter end date (YYYYMMDD, Enter=No end date): ");
                String endDateInput = scanner.nextLine().trim();

                if (!endDateInput.isEmpty()) {
                    if (isValidDate(endDateInput)) {
                        LocalDate startDate = LocalDate.parse(startDateInput, DATE_FORMATTER);
                        LocalDate endDate = LocalDate.parse(endDateInput, DATE_FORMATTER);

                        if (endDate.isBefore(startDate)) {
                            System.out.println("End date cannot be before start date.");
                            pausePrompt();
                            return promptForFilters(currentFilterType, currentFilterCategory, currentFilterStartDate, currentFilterEndDate);
                        }

                        newFilterEndDate = endDateInput;
                    } else {
                        System.out.println("Invalid date format. Use YYYYMMDD.");
                        pausePrompt();
                        return promptForFilters(currentFilterType, currentFilterCategory, currentFilterStartDate, currentFilterEndDate);
                    }
                } else {
                    newFilterEndDate = null;
                }
            } else {
                System.out.println("Invalid date format. Use YYYYMMDD.");
                pausePrompt();
                return promptForFilters(currentFilterType, currentFilterCategory, currentFilterStartDate, currentFilterEndDate);
            }
            break;

        case "4":
            newFilterType = null;
            newFilterCategory = null;
            newFilterStartDate = null;
            newFilterEndDate = null;
            System.out.println("\nAll filters cleared.");
            break;

        case "B":
            break;

        default:
            System.out.println("Invalid choice. Please try again.");
            pausePrompt();
            return promptForFilters(currentFilterType, currentFilterCategory, currentFilterStartDate, currentFilterEndDate);
        }

        return new Object[] { newFilterType, newFilterCategory, newFilterStartDate, newFilterEndDate };
    }

    private static String[] getCategoriesForType(TransactionType type) {
        List<String> cats = type == TransactionType.INCOME ? incCats : expCats;
        return cats.toArray(new String[0]);
    }

    private static String promptForCategory(TransactionType type) {
        String[] categories = getCategoriesForType(type);

        System.out.println("\nSelect a category:");
        for (int i = 0; i < categories.length; i++) {
            System.out.printf("%s%d%s. %s%n", GREEN, i + 1, RESET, categories[i]);
        }
        System.out.printf("[%sA%s] Add New Category%n", GREEN, RESET);

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
            System.out.printf("[%sA%s] Add New Category%n", GREEN, RESET);
            System.out.printf("[%sD%s] Delete Category%n", GREEN, RESET);
            System.out.printf("[%sE%s] Edit Category Name%n", GREEN, RESET);
            System.out.printf("[%sS%s] Save Categories%n", GREEN, RESET);
            System.out.printf("[%sQ%s] Back to Main Menu%n", RED, RESET);
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
            case "e":
                editCategoryName();
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
            System.out.printf("  %s (INCOME)%n", category);
        }

        for (String category : expCats) {
            System.out.printf("  %s (EXPENSE)%n", category);
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
            System.out.printf("%s%d%s. %s%n", GREEN, i + 1, RESET, categories.get(i));
        }

        System.out.print("Enter category number: ");
        String input = scanner.nextLine();
        Integer choice = tryToParse(input);

        if (choice != null && choice >= 1 && choice <= categories.size()) {
            String categoryToRemove = categories.get(choice - 1);

            if (isCategoryInUse(categoryToRemove, type)) {
                System.out.printf("Cannot delete category '%s' because it is in use by one or more transactions.%n", categoryToRemove);
            } else {
                categories.remove(choice - 1);

                if (categoriesSavedBefore) {
                    store.saveCategories(incCats, expCats);
                }

                System.out.printf("Category '%s' deleted successfully!%n", categoryToRemove);
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

    private static void exportTransactionsToCsv() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sExport Transactions%s%n%n", BLUE, RED, BLUE, RESET);

        if (user == null || user.transactions.isEmpty()) {
            System.out.println("No transactions to export.");
            pausePrompt();
            return;
        }

        String defaultFilename = String.format("%s_transactions_%s.csv", user.getUsername(), LocalDate.now().format(DATE_FORMATTER));
        System.out.printf("Enter filename for export (Enter for %s): ", defaultFilename);
        String filename = scanner.nextLine().trim();

        if (filename.isEmpty()) {
            filename = defaultFilename;
        }

        if (!filename.toLowerCase().endsWith(".csv")) {
            filename = String.format("%s.csv", filename);
        }

        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("Date,Description,Amount,Type,Category\n");

            int count = 0;
            for (Transaction transaction : user.transactions) {
                String date = transaction.getDate();
                String description = formatCsvField(transaction.getDescription());
                String amount = String.valueOf(transaction.getAmount());
                String type = transaction.getType().toString();
                String category = formatCsvField(transaction.getCategory());

                writer.write(String.format("%s,%s,%s,%s,%s\n", date, description, amount, type, category));
                count++;
            }

            System.out.printf("\nSuccessfully exported %d transactions to %s%n", count, filename);
        } catch (IOException e) {
            System.out.printf("%nError exporting transactions: %s%n", e.getMessage());
        }

        pausePrompt();
    }

    private static void importTransactionsFromCsv() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sImport Transactions%s%n%n", BLUE, RED, BLUE, RESET);

        System.out.print("Enter CSV filename to import: ");
        String filename = scanner.nextLine().trim();

        if (filename.isEmpty()) {
            System.out.println("\nFilename cannot be empty.");
            pausePrompt();
            return;
        }

        File file = new File(filename);
        if (!file.exists() || !file.isFile()) {
            System.out.printf("%nFile '%s' does not exist.%n", filename);
            pausePrompt();
            return;
        }

        System.out.println("\nHow would you like to import the transactions?");
        System.out.printf("[%sA%s] Add to existing transactions%n", GREEN, RESET);
        System.out.printf("[%sR%s] Replace all existing transactions%n", GREEN, RESET);
        System.out.printf("[%sC%s] Cancel import%n", RED, RESET);
        System.out.printf("\nPlease select an %soption%s: ", BLUE, RESET);

        String mode = scanner.nextLine().toLowerCase();

        if ("c".equals(mode) || (!"a".equals(mode) && !"r".equals(mode))) {
            System.out.println("\nImport cancelled. Existing data remains unchanged.");
            pausePrompt();
            return;
        }

        boolean replace = "r".equals(mode);

        List<Transaction> imports = new ArrayList<>();
        boolean catsChanged = false;
        int skipped = 0;
        int lineNum = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            line = reader.readLine();
            lineNum++;

            if (line == null) {
                System.out.println("\nFile is empty.");
                pausePrompt();
                return;
            }

            while ((line = reader.readLine()) != null) {
                lineNum++;

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] parts = line.split(",");

                if (parts.length < 5) {
                    System.out.printf("Skipped line %d: Not enough fields.%n", lineNum);
                    skipped++;
                    continue;
                }

                String date = parts[0].trim();
                String desc = parts[1].trim();
                String amountStr = parts[2].trim();
                String typeStr = parts[3].trim();
                String category = parts[4].trim();

                if (!isValidDate(date)) {
                    System.out.printf("Skipped line %d: Invalid date format.%n", lineNum);
                    skipped++;
                    continue;
                }

                Double amount = tryToParseDouble(amountStr);
                if (amount == null || amount <= 0) {
                    System.out.printf("Skipped line %d: Invalid amount.%n", lineNum);
                    skipped++;
                    continue;
                }

                TransactionType type;
                try {
                    type = TransactionType.valueOf(typeStr);
                } catch (IllegalArgumentException e) {
                    System.out.printf("Skipped line %d: Invalid transaction type.%n", lineNum);
                    skipped++;
                    continue;
                }

                if (category.isEmpty()) {
                    System.out.printf("Skipped line %d: Empty category.%n", lineNum);
                    skipped++;
                    continue;
                }

                List<String> cats = (type == TransactionType.INCOME) ? incCats : expCats;
                if (!cats.contains(category)) {
                    addCustomCategory(type, category);
                    catsChanged = true;
                }

                Transaction transaction = new Transaction(date, desc, amount,  type, category);
                imports.add(transaction);
            }

            if (imports.isEmpty()) {
                System.out.println("\nImport finished. No valid transactions found in the file. Existing data remains unchanged.");
                pausePrompt();
                return;
            }

            if (replace) {
                user.transactions.clear();
                user.transactions.addAll(imports);
                System.out.printf("\nImport successful. Replaced existing data with %d imported transactions. Skipped %d lines due to errors.\n",
                        imports.size(), skipped);
            } else {
                user.transactions.addAll(imports);
                System.out.printf("\nImport successful. Added %d transactions to existing data. Skipped %d lines due to errors.\n",
                        imports.size(), skipped);
            }
            store.updateUser(user);

            if (catsChanged && categoriesSavedBefore) {
                store.saveCategories(incCats, expCats);
            }

        } catch (IOException e) {
            System.out.printf("\nError reading file: %s\n", e.getMessage());
        }

        pausePrompt();
    }

    private static String formatCsvField(String field) {
        if (field == null) {
            return "";
        }

        boolean needsQuoting = field.contains(",") || field.contains("\"") || field.contains("\n");

        if (needsQuoting) {
            return String.format("\"%s\"", field.replace("\"", "\"\""));
        } else {
            return field;
        }
    }

    private static void editCategoryName() {
        System.out.println("\nEdit Category Name");
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
            System.out.println("No categories available to edit.");
            pausePrompt();
            return;
        }

        System.out.println("\nSelect category to edit:");
        for (int i = 0; i < categories.size(); i++) {
            System.out.printf("%s%d%s. %s\n", GREEN, i + 1, RESET, categories.get(i));
        }

        System.out.print("\nEnter category number: ");
        String input = scanner.nextLine();
        Integer choice = tryToParse(input);

        if (choice == null || choice < 1 || choice > categories.size()) {
            System.out.println("Invalid choice.");
            pausePrompt();
            return;
        }

        String oldName = categories.get(choice - 1);

        System.out.printf("\nCurrent category name: %s\n", oldName);
        System.out.print("Enter new category name: ");
        String newName = scanner.nextLine().trim();

        if (newName.isEmpty()) {
            System.out.println("Category name cannot be empty.");
            pausePrompt();
            return;
        }

        if (newName.equalsIgnoreCase(oldName)) {
            System.out.println("New name is the same as the current name. No changes made.");
            pausePrompt();
            return;
        }

        if (categories.stream().anyMatch(c -> c.equalsIgnoreCase(newName))) {
            System.out.printf("Category '%s' already exists. Please choose a different name.\n", newName);
            pausePrompt();
            return;
        }

        categories.set(choice - 1, newName);

        int transactionCount = 0;
        int goalCount = 0;

        for (Transaction transaction : user.transactions) {
            if (transaction.getType() == type && oldName.equals(transaction.getCategory())) {
                transaction.setCategory(newName);
                transactionCount++;
            }
        }

        for (BudgetGoal goal : user.goals) {
            if (oldName.equals(goal.getCategory())) {
                goal.setCategory(newName);
                goalCount++;
            }
        }

        if (categoriesSavedBefore) {
            store.saveCategories(incCats, expCats);
        }

        if (transactionCount > 0 || goalCount > 0) {
            store.updateUser(user);
        }

        System.out.printf("\nCategory '%s' renamed to '%s'.\n", oldName, newName);

        if (transactionCount > 0 || goalCount > 0) {
            System.out.printf("Updated %d transaction(s) and %d goal(s).\n", transactionCount, goalCount);
        }

        pausePrompt();
    }

    private static void manageGoals() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sManage Goals%s%n%n", BLUE, RED, BLUE, RESET);

        boolean running = true;
        while (running) {
            System.out.println();
            System.out.printf("[%sA%s] Add/Update Goal%n", GREEN, RESET);
            System.out.printf("[%sD%s] Delete Goal%n", GREEN, RESET);
            System.out.printf("[%sL%s] List Goals%n", GREEN, RESET);
            System.out.printf("[%sQ%s] Back to Main Menu%n", RED, RESET);
            System.out.println();
            System.out.printf("Please select an %soption%s: ", BLUE, RESET);

            String choice = scanner.nextLine().toLowerCase();

            switch (choice) {
            case "a":
                addOrUpdateGoal();
                break;
            case "d":
                deleteGoal();
                break;
            case "l":
                listGoals();
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

    private static void listGoals() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sManage Goals%s > %sList Goals%s\n\n", BLUE, RED, BLUE, RESET, BLUE, RESET);

        if (user.goals.isEmpty()) {
            System.out.println("No budget goals set.");
            pausePrompt();
            return;
        }

        System.out.println("Current Budget Goals:\n");
        for (int i = 0; i < user.goals.size(); i++) {
            BudgetGoal goal = user.goals.get(i);
            System.out.printf("%s%d%s. %s - Goal: $%.2f%n", GREEN, i + 1, RESET, goal.getCategory(), goal.getAmount());
        }

        pausePrompt();
    }

    private static void addOrUpdateGoal() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sManage Goals%s > %sAdd/Update Goal%s\n\n", BLUE, RED, BLUE, RESET, BLUE, RESET);

        if (expCats.isEmpty()) {
            System.out.println("No expense categories available. Please add expense categories first.");
            pausePrompt();
            return;
        }

        System.out.println("Select an expense category:\n");
        for (int i = 0; i < expCats.size(); i++) {
            System.out.printf("%s%d%s. %s%n", GREEN, i + 1, RESET, expCats.get(i));
        }

        System.out.print("\nEnter category number: ");
        String input = scanner.nextLine();
        Integer choice = tryToParse(input);

        if (choice == null || choice < 1 || choice > expCats.size()) {
            System.out.println("Invalid choice.");
            pausePrompt();
            return;
        }

        String category = expCats.get(choice - 1);

        double amount;
        while (true) {
            System.out.print("Enter monthly budget amount (positive number): ");
            Double parsedAmount = tryToParseDouble(scanner.nextLine());
            if (parsedAmount != null && parsedAmount > 0) {
                amount = parsedAmount;
                break;
            } else {
                System.out.println("Invalid amount. Please enter a positive number.");
            }
        }

        BudgetGoal existingGoal = null;
        for (BudgetGoal goal : user.goals) {
            if (goal.getCategory().equals(category)) {
                existingGoal = goal;
                break;
            }
        }

        if (existingGoal != null) {
            existingGoal.setAmount(amount);
            System.out.printf("Budget goal for '%s' updated to $%.2f%n", category, amount);
        } else {
            user.goals.add(new BudgetGoal(category, amount));
            System.out.printf("Budget goal for '%s' set to $%.2f%n", category, amount);
        }

        store.updateUser(user);
        pausePrompt();
    }

    private static void deleteGoal() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sManage Goals%s > %sDelete Goal%s\n\n", BLUE, RED, BLUE, RESET, BLUE, RESET);

        if (user.goals.isEmpty()) {
            System.out.println("No budget goals to delete.");
            pausePrompt();
            return;
        }

        System.out.println("Select a goal to delete:\n");
        for (int i = 0; i < user.goals.size(); i++) {
            BudgetGoal goal = user.goals.get(i);
            System.out.printf("%s%d%s. %s - Goal: $%.2f%n", GREEN, i + 1, RESET, goal.getCategory(), goal.getAmount());
        }

        System.out.print("\nEnter goal number: ");
        String input = scanner.nextLine();
        Integer choice = tryToParse(input);

        if (choice != null && choice >= 1 && choice <= user.goals.size()) {
            BudgetGoal removed = user.goals.remove(choice - 1);
            store.updateUser(user);
            System.out.printf("Budget goal for '%s' deleted successfully!\n", removed.getCategory());
        } else {
            System.out.println("Invalid choice.");
        }

        pausePrompt();
    }

    private static void editTransaction() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sEdit Transaction%s%n%n", BLUE, RED, BLUE, RESET);

        System.out.print("Enter transaction number to edit: ");
        String input = scanner.nextLine();
        Integer choice = tryToParse(input);

        if (choice == null || choice < 1 || choice > user.transactions.size()) {
            System.out.println("Invalid transaction number.");
            pausePrompt();
            return;
        }

        Transaction transaction = user.transactions.get(choice - 1);
        boolean changed = false;

        System.out.printf("\nEditing Transaction #%d:%n", choice);
        System.out.println(transaction);

        String newDate;
        System.out.printf("Current Date: %s%n", transaction.getDate());
        while (true) {
            System.out.print("Enter new date (YYYYMMDD, Enter to keep): ");
            newDate = scanner.nextLine();

            if (newDate.isEmpty()) {
                break;
            }

            if (isValidDate(newDate)) {
                transaction.setDate(newDate);
                changed = true;
                break;
            } else {
                System.out.println("Invalid date format. Use YYYYMMDD.");
            }
        }

        System.out.printf("%nCurrent Description: %s%n", transaction.getDescription());
        System.out.print("Enter new description (Enter to keep): ");
        String newDescription = scanner.nextLine();

        if (!newDescription.isEmpty()) {
            transaction.setDescription(newDescription);
            changed = true;
        }

        double newAmount;
        System.out.printf("%nCurrent Amount: %.2f%n", transaction.getAmount());
        while (true) {
            System.out.print("Enter new amount (positive number, Enter to keep): ");
            String amountInput = scanner.nextLine();

            if (amountInput.isEmpty()) {
                break;
            }

            Double parsedAmount = tryToParseDouble(amountInput);
            if (parsedAmount != null && parsedAmount > 0) {
                transaction.setAmount(parsedAmount);
                changed = true;
                break;
            } else {
                System.out.println("Invalid amount. Please enter a positive number.");
            }
        }

        System.out.printf("%nCurrent Category: %s%n", transaction.getCategory());
        System.out.print("Change category? (Y/N): ");
        String changeCategory = scanner.nextLine().toLowerCase();

        if ("y".equals(changeCategory)) {
            String newCategory = promptForCategory(transaction.getType());
            if (!newCategory.equals(transaction.getCategory())) {
                transaction.setCategory(newCategory);
                changed = true;
            }
        }

        if (changed) {
            store.updateUser(user);
            System.out.println("\nTransaction updated successfully!");
        } else {
            System.out.println("\nNo changes were made to the transaction.");
        }

        pausePrompt();
    }

    private static void deleteTransaction() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sDelete Transaction%s%n%n", BLUE, RED, BLUE, RESET);

        System.out.print("Enter transaction number to delete: ");
        String input = scanner.nextLine();
        Integer choice = tryToParse(input);

        if (choice == null || choice < 1 || choice > user.transactions.size()) {
            System.out.println("Invalid transaction number.");
            pausePrompt();
            return;
        }

        Transaction transaction = user.transactions.get(choice - 1);

        System.out.printf("%nAre you sure you want to delete this transaction?%n");
        System.out.println(transaction);
        System.out.print("Confirm deletion (Y/N): ");

        String confirm = scanner.nextLine().toLowerCase();
        if ("y".equals(confirm)) {
            user.transactions.remove(choice - 1);
            store.updateUser(user);
            System.out.println("Transaction deleted successfully!");
        } else {
            System.out.println("Deletion cancelled.");
        }

        pausePrompt();
    }

    private static BudgetGoal findGoalForCategory(String category) {
        if (user == null || user.goals.isEmpty()) {
            return null;
        }

        for (BudgetGoal goal : user.goals) {
            if (goal.getCategory().equals(category)) {
                return goal;
            }
        }

        return null;
    }

    private static void showSummaryReport() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sSummary Report%s\n\n", BLUE, RED, BLUE, RESET);

        if (user == null || user.transactions.isEmpty()) {
            System.out.println("No transactions found.");
            pausePrompt();
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);

        String defaultStartDate = firstDayOfMonth.format(DATE_FORMATTER);
        String defaultEndDate = today.format(DATE_FORMATTER);

        String startDate;
        while (true) {
            System.out.printf("Enter start date (YYYYMMDD, Enter for %s): ", defaultStartDate);
            startDate = scanner.nextLine();
            if (startDate.isEmpty()) {
                startDate = defaultStartDate;
                break;
            }
            if (isValidDate(startDate)) {
                break;
            } else {
                System.out.println("Invalid date format. Use YYYYMMDD.");
            }
        }

        String endDate;
        while (true) {
            System.out.printf("Enter end date (YYYYMMDD, Enter for %s): ", defaultEndDate);
            endDate = scanner.nextLine();
            if (endDate.isEmpty()) {
                endDate = defaultEndDate;
                break;
            }
            if (isValidDate(endDate)) {
                break;
            } else {
                System.out.println("Invalid date format. Use YYYYMMDD.");
            }
        }

        LocalDate startLocalDate = LocalDate.parse(startDate, DATE_FORMATTER);
        LocalDate endLocalDate = LocalDate.parse(endDate, DATE_FORMATTER);

        if (startLocalDate.isAfter(endLocalDate)) {
            System.out.println("Start date cannot be after end date.");
            pausePrompt();
            return;
        }

        List<Transaction> filteredTransactions = new ArrayList<>();
        for (Transaction transaction : user.transactions) {
            LocalDate transactionDate = LocalDate.parse(transaction.getDate(), DATE_FORMATTER);
            if (!transactionDate.isBefore(startLocalDate) && !transactionDate.isAfter(endLocalDate)) {
                filteredTransactions.add(transaction);
            }
        }

        if (filteredTransactions.isEmpty()) {
            System.out.printf("No transactions found between %s and %s.\n", startDate, endDate);
            pausePrompt();
            return;
        }

        double totalIncome = 0;
        double totalExpenses = 0;
        Map<String, Double> expensesByCategory = new HashMap<>();

        for (Transaction transaction : filteredTransactions) {
            if (transaction.getType() == TransactionType.INCOME) {
                totalIncome += transaction.getAmount();
            } else {
                totalExpenses += transaction.getAmount();

                String category = transaction.getCategory();
                expensesByCategory.put(category, expensesByCategory.getOrDefault(category, 0.0) + transaction.getAmount());
            }
        }

        double netBalance = totalIncome - totalExpenses;

        System.out.println("==================================================");
        System.out.printf("Report Period: %s to %s\n\n", startDate, endDate);

        System.out.printf("%sTotal Income:   $%10.2f%s\n", GREEN, totalIncome, RESET);
        System.out.printf("%sTotal Expenses: $%10.2f%s\n", RED, totalExpenses, RESET);
        System.out.println("--------------------------------------------------");

        String balanceColor = netBalance >= 0 ? GREEN : RED;
        System.out.printf("%sNet Balance:    $%10.2f%s\n\n", balanceColor, netBalance, RESET);

        if (!expensesByCategory.isEmpty()) {
            System.out.println("Expenses by Category:");
            System.out.println("--------------------------------------------------");

            List<Map.Entry<String, Double>> sortedExpenses = new ArrayList<>(expensesByCategory.entrySet());
            sortedExpenses.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

            for (Map.Entry<String, Double> entry : sortedExpenses) {
                String category = entry.getKey();
                double spent = entry.getValue();
                double percentage = (spent / totalExpenses) * 100;

                BudgetGoal goal = findGoalForCategory(category);
                if (goal != null) {
                    double goalAmount = goal.getAmount();
                    double progressPercentage = (spent / goalAmount) * 100;
                    String progressColor = progressPercentage <= 100 ? GREEN : RED;

                    System.out.printf("%-15s: $%10.2f (%5.1f%%) - Goal: $%.2f %s(%5.1f%%)%s\n",
                            category, spent, percentage, goalAmount,
                            progressColor, progressPercentage, RESET);
                } else {
                    System.out.printf("%-15s: $%10.2f (%5.1f%%)\n",
                            category, spent, percentage);
                }
            }
        }

        System.out.println("==================================================");
        pausePrompt();
    }
}