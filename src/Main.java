import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

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

class SavingsGoal {
    private String name;
    private double targetAmount;
    private double currentAmount;

    public SavingsGoal(String name, double targetAmount, double currentAmount) {
        this.name = name;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
    }

    public String getName() {
        return name;
    }

    public double getTargetAmount() {
        return targetAmount;
    }

    public double getCurrentAmount() {
        return currentAmount;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTargetAmount(double targetAmount) {
        this.targetAmount = targetAmount;
    }

    public void setCurrentAmount(double currentAmount) {
        this.currentAmount = currentAmount;
    }

    public double getPercentComplete() {
        return (currentAmount / targetAmount) * 100;
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

class RecurringTransaction {
    private String description;
    private double amount;
    private TransactionType type;
    private String category;
    private String frequency = "MONTHLY";
    private String nextDueDate;

    public RecurringTransaction(String description, double amount, TransactionType type, String category, String nextDueDate) {
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.nextDueDate = nextDueDate;
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

    public String getFrequency() {
        return frequency;
    }

    public String getNextDueDate() {
        return nextDueDate;
    }

    public void setNextDueDate(String nextDueDate) {
        this.nextDueDate = nextDueDate;
    }

    @Override
    public String toString() {
        return String.format("%s | %-15s | %8.2f | %-10s | %s",
                nextDueDate,
                description.substring(0, Math.min(description.length(), 15)),
                (type == TransactionType.EXPENSE ? -1 : 1) * amount,
                category,
                type);
    }
}

class User {
    private String username;
    private String password;
    private String currencySymbol = "$";
    public List<Transaction> transactions = new ArrayList<>();
    public List<BudgetGoal> goals = new ArrayList<>();
    public List<RecurringTransaction> recurringTransactions = new ArrayList<>();
    public List<SavingsGoal> savingsGoals = new ArrayList<>();

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

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
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
    static final String DELIMITER = ":";

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

    private static String getParentCategory(String category) {
        if (category == null || !category.contains(DELIMITER)) {
            return category;
        }
        return category.split(DELIMITER)[0];
    }

    private static String getChildCategory(String category) {
        if (category == null || !category.contains(DELIMITER)) {
            return "";
        }
        String[] parts = category.split(DELIMITER);
        return parts.length > 1 ? parts[1] : "";
    }

    private static boolean isSubcategory(String category) {
        return category != null && category.contains(DELIMITER);
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
            System.out.printf("[%sT%s] Quick Totals%n", GREEN, RESET);
            System.out.printf("[%sN%s] Analyze Data%n", GREEN, RESET);
            System.out.printf("[%sC%s] Manage Categories%n", GREEN, RESET);
            System.out.printf("[%sG%s] Manage Goals%n", GREEN, RESET);
            System.out.printf("[%sR%s] Manage Recurring%n", GREEN, RESET);
            System.out.printf("[%sL%s] Log Due Recurring%n", GREEN, RESET);
            System.out.printf("[%sB%s] Backup/Restore%n", GREEN, RESET);
            System.out.printf("[%sP%s] Change Password%n", GREEN, RESET);
            System.out.printf("[%sO%s] Options%n", GREEN, RESET);
            System.out.printf("[%sZ%s] Savings Goals%n", GREEN, RESET);
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
            case "t":
                showQuickTotals();
                cleanScreen();
                break;
            case "n":
                showAnalysisMenu();
                cleanScreen();
                break;
            case "r":
                manageRecurringTransactions();
                cleanScreen();
                break;
            case "l":
                logDueRecurringTransactions();
                cleanScreen();
                break;
            case "b":
                showBackupRestoreMenu();
                cleanScreen();
                break;
            case "p":
                changePassword();
                cleanScreen();
                break;
            case "o":
                showOptionsMenu();
                cleanScreen();
                break;
            case "z":
                manageSavingsGoals();
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

        String sortField = "date";
        boolean sortAscending = false;

        boolean viewing = true;

        while (viewing) {
            List<Transaction> filtered = applyTransactionFilters(user.transactions, filterType, filterCategory, filterStartDate, filterEndDate);

            sortTransactions(filtered, sortField, sortAscending);

            int totalItems = filtered.size();
            int totalPages = (totalItems + pageSize - 1) / pageSize;

            if (totalPages == 0) {
                totalPages = 1;
            }

            cleanScreen();

            StringBuilder headerInfo = new StringBuilder();

            if (filterType != null || filterCategory != null || filterStartDate != null) {
                headerInfo.append(" (Filtered by");
                if (filterType != null) {
                    headerInfo.append(String.format(" Type: %s", filterType));
                }
                if (filterCategory != null) {
                    headerInfo.append(String.format("%s Category: %s", filterType != null ? "," : "", filterCategory));
                }
                if (filterStartDate != null) {
                    headerInfo.append(String.format("%s Date: %s", (filterType != null || filterCategory != null) ? "," : "",
                            filterStartDate + (filterEndDate != null ? "-" + filterEndDate : "")));
                }
                headerInfo.append(")");
            }

            String sortInfo = String.format(" (Sorted by %s %s)",
                    sortField.substring(0, 1).toUpperCase() + sortField.substring(1),
                    sortAscending ? "Ascending" : "Descending");
            headerInfo.append(sortInfo);

            System.out.printf("%sBye Bye Money%s > %sView/Manage Transactions%s%s (Page %s%d%s/%s%d%s)%n%n",
                    BLUE, RED, BLUE, RESET, headerInfo.toString(), GREEN, currentPage, RESET, GREEN, totalPages, RESET);

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
                System.out.println("---+---------+------------------+----------+------------+---------");

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
            System.out.printf("[%sO%s] Order By%n", GREEN, RESET);

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
                Object[] filterResult = promptForFilters(filterType, filterCategory, filterStartDate, filterEndDate);
                if (filterResult != null) {
                    filterType = filterResult[0] != null ? (TransactionType) filterResult[0] : null;
                    filterCategory = (String) filterResult[1];
                    filterStartDate = (String) filterResult[2];
                    filterEndDate = (String) filterResult[3];
                    currentPage = 1;
                }
                break;
            case "o":
                Object[] sortResult = promptForSortOrder(sortField, sortAscending);
                if (sortResult != null) {
                    sortField = (String) sortResult[0];
                    sortAscending = (Boolean) sortResult[1];
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

    private static void sortTransactions(List<Transaction> transactions, String sortField, boolean sortAscending) {
        if (transactions.isEmpty()) {
            return;
        }

        Comparator<Transaction> comparator = null;

        switch (sortField.toLowerCase()) {
            case "date":
                comparator = Comparator.comparing(Transaction::getDate);
                break;
            case "description":
                comparator = Comparator.comparing(t -> t.getDescription().toLowerCase());
                break;
            case "amount":
                comparator = Comparator.comparing(Transaction::getAmount);
                break;
            case "category":
                comparator = Comparator.comparing(t -> t.getCategory().toLowerCase());
                break;
            default:
                comparator = Comparator.comparing(Transaction::getDate);
                break;
        }

        if (!sortAscending) {
            comparator = comparator.reversed();
        }

        Collections.sort(transactions, comparator);
    }

    private static Object[] promptForSortOrder(String currentSortField, boolean currentSortAscending) {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sOrder Transactions%s%n%n", BLUE, RED, BLUE, RESET);

        System.out.println("Current Sort Order:");
        System.out.printf("Field: %s%n", currentSortField.substring(0, 1).toUpperCase() + currentSortField.substring(1));
        System.out.printf("Direction: %s%n%n", currentSortAscending ? "Ascending" : "Descending");

        System.out.println("Sort By Field:");
        System.out.printf("[%s1%s] Date%n", GREEN, RESET);
        System.out.printf("[%s2%s] Description%n", GREEN, RESET);
        System.out.printf("[%s3%s] Amount%n", GREEN, RESET);
        System.out.printf("[%s4%s] Category%n", GREEN, RESET);
        System.out.printf("[%sB%s] Back to Transactions%n%n", RED, RESET);

        System.out.printf("Please select an %soption%s: ", BLUE, RESET);
        String fieldChoice = scanner.nextLine();

        String newSortField = currentSortField;
        boolean newSortAscending = currentSortAscending;

        switch (fieldChoice) {
            case "1":
                newSortField = "date";
                break;
            case "2":
                newSortField = "description";
                break;
            case "3":
                newSortField = "amount";
                break;
            case "4":
                newSortField = "category";
                break;
            case "B":
                return null;
            default:
                System.out.println("Invalid choice. Please try again.");
                pausePrompt();
                return promptForSortOrder(currentSortField, currentSortAscending);
        }

        System.out.printf("\nSort Direction for %s:%n",
                newSortField.substring(0, 1).toUpperCase() + newSortField.substring(1));
        System.out.printf("[%s1%s] Ascending%n", GREEN, RESET);
        System.out.printf("[%s2%s] Descending%n%n", GREEN, RESET);

        System.out.printf("Please select an %soption%s: ", BLUE, RESET);
        String directionChoice = scanner.nextLine();

        switch (directionChoice) {
            case "1":
                newSortAscending = true;
                break;
            case "2":
                newSortAscending = false;
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
                pausePrompt();
                return promptForSortOrder(currentSortField, currentSortAscending);
        }

        return new Object[] { newSortField, newSortAscending };
    }

    private static void manageRecurringTransactions() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sManage Recurring%s%n%n", BLUE, RED, BLUE, RESET);

        boolean running = true;
        while (running) {
            System.out.println();
            System.out.printf("[%sA%s] Add Recurring%n", GREEN, RESET);
            System.out.printf("[%sL%s] List Recurring%n", GREEN, RESET);
            System.out.printf("[%sD%s] Delete Recurring%n", GREEN, RESET);
            System.out.printf("[%sQ%s] Back to Main Menu%n", RED, RESET);
            System.out.println();
            System.out.printf("Please select an %soption%s: ", BLUE, RESET);

            String choice = scanner.nextLine().toLowerCase();

            switch (choice) {
            case "a":
                addRecurringTransaction();
                break;
            case "l":
                listRecurringTransactions();
                break;
            case "d":
                deleteRecurringTransaction();
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

    private static void addRecurringTransaction() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sAdd Recurring Transaction%s%n%n", BLUE, RED, BLUE, RESET);

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

        String nextDueDate;
        while (true) {
            System.out.printf("Enter first due date (YYYYMMDD, Enter for today %s): ", LocalDate.now().format(DATE_FORMATTER));
            nextDueDate = scanner.nextLine();
            if (nextDueDate.isEmpty()) {
                nextDueDate = LocalDate.now().format(DATE_FORMATTER);
                break;
            }
            if (isValidDate(nextDueDate)) {
                break;
            } else {
                System.out.println("Invalid date format. Use YYYYMMDD.");
            }
        }

        RecurringTransaction recurring = new RecurringTransaction(description, amount, type, category, nextDueDate);
        user.recurringTransactions.add(recurring);
        store.updateUser(user);

        System.out.println("\nRecurring transaction added successfully!");
        pausePrompt();
    }

    private static void listRecurringTransactions() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sList Recurring Transactions%s%n%n", BLUE, RED, BLUE, RESET);

        if (user.recurringTransactions.isEmpty()) {
            System.out.println("No recurring transactions found.");
            pausePrompt();
            return;
        }

        System.out.println("#  | Next Due  | Description     |   Amount | Category   | Type");
        System.out.println("---+-----------+-----------------+----------+------------+---------");

        for (int i = 0; i < user.recurringTransactions.size(); i++) {
            System.out.printf("%s%2d%s | %s%n", GREEN, i + 1, RESET, user.recurringTransactions.get(i));
        }

        pausePrompt();
    }

    private static void deleteRecurringTransaction() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sDelete Recurring Transaction%s%n%n", BLUE, RED, BLUE, RESET);

        if (user.recurringTransactions.isEmpty()) {
            System.out.println("No recurring transactions found.");
            pausePrompt();
            return;
        }

        System.out.println("#  | Next Due  | Description     |   Amount | Category   | Type");
        System.out.println("---+-----------+-----------------+----------+------------+---------");

        for (int i = 0; i < user.recurringTransactions.size(); i++) {
            System.out.printf("%s%2d%s | %s%n", GREEN, i + 1, RESET, user.recurringTransactions.get(i));
        }

        System.out.print("\nEnter recurring transaction number to delete: ");
        String input = scanner.nextLine();
        Integer choice = tryToParse(input);

        if (choice == null || choice < 1 || choice > user.recurringTransactions.size()) {
            System.out.println("Invalid transaction number.");
            pausePrompt();
            return;
        }

        RecurringTransaction transaction = user.recurringTransactions.get(choice - 1);

        System.out.printf("%nAre you sure you want to delete this recurring transaction?%n");
        System.out.println(transaction);
        System.out.print("Confirm deletion (Y/N): ");

        String confirm = scanner.nextLine().toLowerCase();
        if ("y".equals(confirm)) {
            user.recurringTransactions.remove(choice - 1);
            store.updateUser(user);
            System.out.println("Recurring transaction deleted successfully!");
        } else {
            System.out.println("Deletion cancelled.");
        }

        pausePrompt();
    }

    private static void logDueRecurringTransactions() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sLog Due Recurring Transactions%s%n%n", BLUE, RED, BLUE, RESET);

        if (user.recurringTransactions.isEmpty()) {
            System.out.println("No recurring transactions found.");
            pausePrompt();
            return;
        }

        LocalDate today = LocalDate.now();
        boolean changesMade = false;
        int logged = 0;

        for (RecurringTransaction recurring : user.recurringTransactions) {
            LocalDate dueDate = LocalDate.parse(recurring.getNextDueDate(), DATE_FORMATTER);

            if (!dueDate.isAfter(today)) {
                System.out.println("Due recurring transaction:");
                System.out.println(recurring);
                System.out.printf("Log this transaction for %s? (y/N/s)kip all: ", recurring.getNextDueDate());

                String choice = scanner.nextLine().toLowerCase();

                if ("s".equals(choice)) {
                    System.out.println("Skipping all remaining due transactions.");
                    break;
                } else if ("y".equals(choice)) {
                    Transaction transaction = new Transaction(
                            recurring.getNextDueDate(),
                            recurring.getDescription(),
                            recurring.getAmount(),
                            recurring.getType(),
                            recurring.getCategory());

                    user.transactions.add(transaction);

                    LocalDate nextDate = dueDate.plusMonths(1);
                    recurring.setNextDueDate(nextDate.format(DATE_FORMATTER));

                    changesMade = true;
                    logged++;

                    System.out.println("Transaction logged successfully.");
                    System.out.printf("Next due date set to: %s%n%n", recurring.getNextDueDate());
                }
            }
        }

        if (changesMade) {
            store.updateUser(user);
            System.out.printf("\nLogged %d recurring transactions.%n", logged);
        } else {
            System.out.println("\nNo transactions were logged.");
        }

        pausePrompt();
    }

    private static void showQuickTotals() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sQuick Totals%s%n%n", BLUE, RED, BLUE, RESET);

        System.out.printf("[%sM%s] This Month%n", GREEN, RESET);
        System.out.printf("[%sY%s] This Year%n", GREEN, RESET);
        System.out.printf("[%sQ%s] Back to Main Menu%n", RED, RESET);
        System.out.println();
        System.out.printf("Please select an %soption%s: ", BLUE, RESET);

        String choice = scanner.nextLine().toLowerCase();

        switch (choice) {
        case "m":
            showMonthTotals();
            break;
        case "y":
            showYearTotals();
            break;
        case "q":
            return;
        default:
            System.out.println("Invalid choice. Please try again.");
            pausePrompt();
            showQuickTotals();
            break;
        }
    }

    private static void showMonthTotals() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        String startDate = firstDayOfMonth.format(DATE_FORMATTER);
        String endDate = lastDayOfMonth.format(DATE_FORMATTER);

        showPeriodTotals(startDate, endDate, "This Month");
    }

    private static void showYearTotals() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfYear = today.withDayOfYear(1);
        LocalDate lastDayOfYear = today.withDayOfYear(today.lengthOfYear());

        String startDate = firstDayOfYear.format(DATE_FORMATTER);
        String endDate = lastDayOfYear.format(DATE_FORMATTER);

        showPeriodTotals(startDate, endDate, "This Year");
    }

    private static void showPeriodTotals(String startDate, String endDate, String periodName) {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sQuick Totals%s > %s%s%s%n%n",
                BLUE, RED, BLUE, RESET, BLUE, periodName, RESET);

        List<Transaction> filtered = applyTransactionFilters(user.transactions, null, null, startDate, endDate);

        if (filtered.isEmpty()) {
            System.out.printf("No transactions found for period: %s to %s%n", startDate, endDate);
            pausePrompt();
            return;
        }

        double totalIncome = 0;
        double totalExpenses = 0;

        for (Transaction transaction : filtered) {
            if (transaction.getType() == TransactionType.INCOME) {
                totalIncome += transaction.getAmount();
            } else {
                totalExpenses += transaction.getAmount();
            }
        }

        double netBalance = totalIncome - totalExpenses;
        String netColor = netBalance >= 0 ? GREEN : RED;

        System.out.printf("Quick Totals (Period: %s-%s):%n%n", startDate, endDate);
        System.out.printf("Total Income:  %s%s%10.2f%s%n", GREEN, user.getCurrencySymbol(), totalIncome, RESET);
        System.out.printf("Total Expenses: %s%s%10.2f%s%n", RED, user.getCurrencySymbol(), totalExpenses, RESET);
        System.out.printf("Net Balance:    %s%s%10.2f%s%n", netColor, user.getCurrencySymbol(), netBalance, RESET);

        pausePrompt();
    }

    private static void showAnalysisMenu() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sAnalyze Data%s%n%n", BLUE, RED, BLUE, RESET);

        System.out.printf("[%sC%s] Category Trend%n", GREEN, RESET);
        System.out.printf("[%sP%s] Period Comparison%n", GREEN, RESET);
        System.out.printf("[%sQ%s] Back to Main Menu%n", RED, RESET);
        System.out.println();
        System.out.printf("Please select an %soption%s: ", BLUE, RESET);

        String choice = scanner.nextLine().toLowerCase();

        switch (choice) {
        case "c":
            showCategoryTrendReport();
            break;
        case "p":
            showPeriodComparisonReport();
            break;
        case "q":
            return;
        default:
            System.out.println("Invalid choice. Please try again.");
            pausePrompt();
            showAnalysisMenu();
            break;
        }
    }

    private static void showCategoryTrendReport() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sAnalyze Data%s > %sCategory Trend%s%n%n",
                BLUE, RED, BLUE, RESET, BLUE, RESET);

        if (expCats.isEmpty()) {
            System.out.println("No expense categories available for analysis.");
            pausePrompt();
            return;
        }

        System.out.println("Select an expense category:");
        for (int i = 0; i < expCats.size(); i++) {
            System.out.printf("%s%d%s. %s%n", GREEN, i + 1, RESET, expCats.get(i));
        }

        System.out.print("\nEnter category number: ");
        String input = scanner.nextLine();
        Integer choice = tryToParse(input);

        if (choice == null || choice < 1 || choice > expCats.size()) {
            System.out.println("Invalid category number.");
            pausePrompt();
            return;
        }

        String category = expCats.get(choice - 1);

        System.out.println("\nSelect period:");
        System.out.printf("[%s6%s] Last 6 Months%n", GREEN, RESET);
        System.out.printf("[%s12%s] Last 12 Months%n", GREEN, RESET);
        System.out.print("\nEnter choice: ");

        input = scanner.nextLine();
        Integer months = tryToParse(input);

        if (months == null || (months != 6 && months != 12)) {
            System.out.println("Invalid choice. Please enter 6 or 12.");
            pausePrompt();
            return;
        }

        generateCategoryTrendReport(category, months);
    }

    private static void generateCategoryTrendReport(String category, int months) {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sAnalyze Data%s > %sCategory Trend%s%n%n",
                BLUE, RED, BLUE, RESET, BLUE, RESET);

        LocalDate today = LocalDate.now();
        LocalDate currentMonthStart = today.withDayOfMonth(1);
        LocalDate startDate = currentMonthStart.minusMonths(months);
        LocalDate endDate = today;

        String startDateStr = startDate.format(DATE_FORMATTER);
        String endDateStr = endDate.format(DATE_FORMATTER);

        System.out.printf("Spending Trend for Category: %s%s%s%n", GREEN, category, RESET);
        System.out.printf("Period: %s to %s (%d months)%n%n", startDateStr, endDateStr, months);

        if (user.transactions.isEmpty()) {
            System.out.println("No transactions found in the system.");
            pausePrompt();
            return;
        }

        List<Transaction> expenseTransactions = new ArrayList<>();
        for (Transaction transaction : user.transactions) {
            if (transaction.getType() == TransactionType.EXPENSE) {
                expenseTransactions.add(transaction);
            }
        }

        if (expenseTransactions.isEmpty()) {
            System.out.println("No expense transactions found in the system.");
            pausePrompt();
            return;
        }

        List<Transaction> filtered = new ArrayList<>();
        for (Transaction transaction : expenseTransactions) {
            boolean categoryMatch = false;

            if (transaction.getCategory().equalsIgnoreCase(category)) {
                categoryMatch = true;
            }
            else if (!isSubcategory(category) && isSubcategory(transaction.getCategory()) &&
                     getParentCategory(transaction.getCategory()).equalsIgnoreCase(category)) {
                categoryMatch = true;
            }

            if (categoryMatch) {
                try {
                    LocalDate transactionDate = LocalDate.parse(transaction.getDate(), DATE_FORMATTER);
                    if (!transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate)) {
                        filtered.add(transaction);
                    }
                } catch (Exception e) {
                }
            }
        }

        if (filtered.isEmpty()) {
            System.out.printf("No expenses found for category '%s' in the selected period.%n", category);
            pausePrompt();
            return;
        }

        Map<YearMonth, Double> monthlyTotals = new HashMap<>();

        for (Transaction transaction : filtered) {
            LocalDate date = LocalDate.parse(transaction.getDate(), DATE_FORMATTER);
            YearMonth yearMonth = YearMonth.from(date);

            double currentAmount = monthlyTotals.getOrDefault(yearMonth, 0.0);
            monthlyTotals.put(yearMonth, currentAmount + transaction.getAmount());
        }

        YearMonth current = YearMonth.from(startDate);
        YearMonth end = YearMonth.from(endDate);

        double maxAmount = 0.0;
        for (Double amount : monthlyTotals.values()) {
            if (amount > maxAmount) {
                maxAmount = amount;
            }
        }

        System.out.println("Month      | Amount     | Trend");
        System.out.println("-----------+------------+--------------------");

        while (!current.isAfter(end)) {
            double amount = monthlyTotals.getOrDefault(current, 0.0);
            int barLength = maxAmount > 0 ? (int)((amount / maxAmount) * 20) : 0;
            String bar = "█".repeat(barLength);

            System.out.printf("%s-%02d | %s%9.2f | %s%s%s%n",
                    current.getYear(), current.getMonthValue(),
                    user.getCurrencySymbol(), amount,
                    RED, bar, RESET);

            current = current.plusMonths(1);
        }

        pausePrompt();
    }

    private static void showPeriodComparisonReport() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sAnalyze Data%s > %sPeriod Comparison%s%n%n",
                BLUE, RED, BLUE, RESET, BLUE, RESET);

        System.out.printf("[%sM%s] Monthly Comparison%n", GREEN, RESET);
        System.out.printf("[%sY%s] Yearly Comparison%n", GREEN, RESET);
        System.out.printf("[%sQ%s] Back%n", RED, RESET);
        System.out.println();
        System.out.printf("Please select an %soption%s: ", BLUE, RESET);

        String choice = scanner.nextLine().toLowerCase();

        switch (choice) {
        case "m":
            showMonthlyComparison();
            break;
        case "y":
            showYearlyComparison();
            break;
        case "q":
            return;
        default:
            System.out.println("Invalid choice. Please try again.");
            pausePrompt();
            showPeriodComparisonReport();
            break;
        }
    }

    private static void showMonthlyComparison() {
        LocalDate today = LocalDate.now();

        LocalDate currentMonthStart = today.withDayOfMonth(1);
        LocalDate currentMonthEnd = today;

        LocalDate prevMonthStart = currentMonthStart.minusMonths(1);
        LocalDate prevMonthEnd = currentMonthStart.minusDays(1);

        String currentStartStr = currentMonthStart.format(DATE_FORMATTER);
        String currentEndStr = currentMonthEnd.format(DATE_FORMATTER);
        String prevStartStr = prevMonthStart.format(DATE_FORMATTER);
        String prevEndStr = prevMonthEnd.format(DATE_FORMATTER);

        String currentPeriod = String.format("%s-%s", currentStartStr, currentEndStr);
        String prevPeriod = String.format("%s-%s", prevStartStr, prevEndStr);

        showPeriodComparison("Monthly", currentPeriod, prevPeriod, currentStartStr, currentEndStr, prevStartStr, prevEndStr);
    }

    private static void showYearlyComparison() {
        LocalDate today = LocalDate.now();

        LocalDate currentYearStart = today.withDayOfYear(1);
        LocalDate currentYearEnd = today;

        LocalDate prevYearStart = currentYearStart.minusYears(1);
        LocalDate prevYearEnd = currentYearStart.minusDays(1);

        String currentStartStr = currentYearStart.format(DATE_FORMATTER);
        String currentEndStr = currentYearEnd.format(DATE_FORMATTER);
        String prevStartStr = prevYearStart.format(DATE_FORMATTER);
        String prevEndStr = prevYearEnd.format(DATE_FORMATTER);

        String currentPeriod = String.format("%s-%s", currentStartStr, currentEndStr);
        String prevPeriod = String.format("%s-%s", prevStartStr, prevEndStr);

        showPeriodComparison("Yearly", currentPeriod, prevPeriod, currentStartStr, currentEndStr, prevStartStr, prevEndStr);
    }

    private static void showPeriodComparison(String periodType, String currentPeriod, String prevPeriod,
            String currentStartStr, String currentEndStr, String prevStartStr, String prevEndStr) {

        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sAnalyze Data%s > %s%s Comparison%s%n%n",
                BLUE, RED, BLUE, RESET, BLUE, periodType, RESET);

        System.out.printf("Comparing Current %s (%s) with Previous %s (%s)%n%n",
                periodType, currentPeriod, periodType, prevPeriod);

        List<Transaction> currentTransactions = applyTransactionFilters(user.transactions, null, null, currentStartStr, currentEndStr);
        List<Transaction> prevTransactions = applyTransactionFilters(user.transactions, null, null, prevStartStr, prevEndStr);

        double currentIncome = 0;
        double currentExpenses = 0;
        double prevIncome = 0;
        double prevExpenses = 0;

        for (Transaction transaction : currentTransactions) {
            if (transaction.getType() == TransactionType.INCOME) {
                currentIncome += transaction.getAmount();
            } else {
                currentExpenses += transaction.getAmount();
            }
        }

        for (Transaction transaction : prevTransactions) {
            if (transaction.getType() == TransactionType.INCOME) {
                prevIncome += transaction.getAmount();
            } else {
                prevExpenses += transaction.getAmount();
            }
        }

        double currentNet = currentIncome - currentExpenses;
        double prevNet = prevIncome - prevExpenses;

        double incomeChange = currentIncome - prevIncome;
        double expensesChange = currentExpenses - prevExpenses;
        double netChange = currentNet - prevNet;

        double incomeChangePct = prevIncome > 0 ? (incomeChange / prevIncome) * 100 : 0;
        double expensesChangePct = prevExpenses > 0 ? (expensesChange / prevExpenses) * 100 : 0;
        double netChangePct = prevNet != 0 ? (netChange / Math.abs(prevNet)) * 100 : 0;

        String incomeColor = incomeChange >= 0 ? GREEN : RED;
        String expensesColor = expensesChange <= 0 ? GREEN : RED;
        String netColor = netChange >= 0 ? GREEN : RED;

        System.out.println("Item       | Current Period | Previous Period | Change        | Change %");
        System.out.println("-----------+----------------+-----------------+---------------+----------");

        System.out.printf("Income     | %s%13.2f | %s%13.2f | %s%s%11.2f%s | %s%+7.2f%%%s%n",
                user.getCurrencySymbol(), currentIncome, user.getCurrencySymbol(), prevIncome, incomeColor, user.getCurrencySymbol(), incomeChange, RESET, incomeColor, incomeChangePct, RESET);

        System.out.printf("Expenses   | %s%13.2f | %s%13.2f | %s%s%11.2f%s | %s%+7.2f%%%s%n",
                user.getCurrencySymbol(), currentExpenses, user.getCurrencySymbol(), prevExpenses, expensesColor, user.getCurrencySymbol(), expensesChange, RESET, expensesColor, expensesChangePct, RESET);

        System.out.printf("Net Balance| %s%13.2f | %s%13.2f | %s%s%11.2f%s | %s%+7.2f%%%s%n",
                user.getCurrencySymbol(), currentNet, user.getCurrencySymbol(), prevNet, netColor, user.getCurrencySymbol(), netChange, RESET, netColor, netChangePct, RESET);

        if (currentTransactions.isEmpty() && prevTransactions.isEmpty()) {
            System.out.println("\nNo transactions found in either period.");
        } else if (currentTransactions.isEmpty()) {
            System.out.println("\nNo transactions found in the current period.");
        } else if (prevTransactions.isEmpty()) {
            System.out.println("\nNo transactions found in the previous period.");
        }

        pausePrompt();
    }

    private static String[] getCategoriesForType(TransactionType type) {
        List<String> cats = type == TransactionType.INCOME ? incCats : expCats;
        return cats.toArray(new String[0]);
    }

    private static String promptForCategory(TransactionType type) {
        String[] categories = getCategoriesForType(type);

        Map<String, List<String>> bigParent = new HashMap<>();
        List<String> pCategories = new ArrayList<>();
        List<String> orphans = new ArrayList<>();

        for (String category : categories) {
            if (isSubcategory(category)) {
                String parent = getParentCategory(category);
                boolean parentExists = false;
                for (String cat : categories) {
                    if (!isSubcategory(cat) && cat.equals(parent)) {
                        parentExists = true;
                        break;
                    }
                }

                if (parentExists) {
                    bigParent.computeIfAbsent(parent, k -> new ArrayList<>()).add(category);
                } else {
                    orphans.add(category);
                }
            } else {
                pCategories.add(category);
            }
        }

        List<String> displayCategories = new ArrayList<>();
        for (String parent : pCategories) {
            displayCategories.add(parent);
            List<String> subs = bigParent.get(parent);
            if (subs != null) {
                displayCategories.addAll(subs);
            }
        }
        displayCategories.addAll(orphans);

        System.out.println("\nSelect a category:");
        for (int i = 0; i < displayCategories.size(); i++) {
            String category = displayCategories.get(i);
            if (isSubcategory(category) && bigParent.containsKey(getParentCategory(category))) {
                String childName = getChildCategory(category);
                System.out.printf("%s%d%s. ↳ %s%n", GREEN, i + 1, RESET, childName);
            } else {
                System.out.printf("%s%d%s. %s%n", GREEN, i + 1, RESET, category);
            }
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

            if (choice != null && choice >= 1 && choice <= displayCategories.size()) {
                return displayCategories.get(choice - 1);
            } else {
                System.out.println("Invalid choice. Please enter a number between 1 and " + displayCategories.size() + " or A.");
            }
        }
    }

    private static String promptForNewCategory() {
        System.out.println("You can use Parent:Child format for subcategories (e.g., Food:Groceries)");

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

        Map<String, List<String>> bigParent = new HashMap<>();
        for (String category : incCats) {
            if (isSubcategory(category)) {
                String parent = getParentCategory(category);
                bigParent.computeIfAbsent(parent, k -> new ArrayList<>()).add(category);
            }
        }

        for (String category : incCats) {
            if (!isSubcategory(category)) {
                System.out.printf("  %s (INCOME)%n", category);

                List<String> subs = bigParent.get(category);
                if (subs != null) {
                    for (String sub : subs) {
                        String childName = getChildCategory(sub);
                        System.out.printf("    ↳ %s%n", childName);
                    }
                }
            } else if (!bigParent.containsKey(getParentCategory(category))) {
                System.out.printf("  %s (INCOME)%n", category);
            }
        }

        Map<String, List<String>> eParent = new HashMap<>();
        for (String category : expCats) {
            if (isSubcategory(category)) {
                String parent = getParentCategory(category);
                eParent.computeIfAbsent(parent, k -> new ArrayList<>()).add(category);
            }
        }

        for (String category : expCats) {
            if (!isSubcategory(category)) {
                System.out.printf("  %s (EXPENSE)%n", category);

                List<String> subs = eParent.get(category);
                if (subs != null) {
                    for (String sub : subs) {
                        String childName = getChildCategory(sub);
                        System.out.printf("    ↳ %s%n", childName);
                    }
                }
            } else if (!eParent.containsKey(getParentCategory(category))) {
                System.out.printf("  %s (EXPENSE)%n", category);
            }
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

        if (!isSubcategory(category)) {
            for (Transaction transaction : user.transactions) {
                if (transaction.getType() == type &&
                    isSubcategory(transaction.getCategory()) &&
                    category.equalsIgnoreCase(getParentCategory(transaction.getCategory()))) {
                    return true;
                }
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
            System.out.printf("%s%d%s. %s - Goal: %s%.2f%n", GREEN, i + 1, RESET, goal.getCategory(), user.getCurrencySymbol(), goal.getAmount());
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
            System.out.printf("Budget goal for '%s' updated to %s%.2f%n", category, user.getCurrencySymbol(), amount);
        } else {
            user.goals.add(new BudgetGoal(category, amount));
            System.out.printf("Budget goal for '%s' set to %s%.2f%n", category, user.getCurrencySymbol(), amount);
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
            System.out.printf("%s%d%s. %s - Goal: %s%.2f%n", GREEN, i + 1, RESET, goal.getCategory(), user.getCurrencySymbol(), goal.getAmount());
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

        if (isSubcategory(category)) {
            String parentCategory = getParentCategory(category);
            for (BudgetGoal goal : user.goals) {
                if (goal.getCategory().equals(parentCategory)) {
                    return goal;
                }
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

        System.out.print("Aggregate results by parent category? (y/N): ");
        boolean aggregateByParent = scanner.nextLine().trim().toLowerCase().startsWith("y");

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

                if (aggregateByParent && isSubcategory(category)) {
                    String parentCategory = getParentCategory(category);
                    expensesByCategory.put(parentCategory,
                        expensesByCategory.getOrDefault(parentCategory, 0.0) + transaction.getAmount());
                } else {
                    expensesByCategory.put(category,
                        expensesByCategory.getOrDefault(category, 0.0) + transaction.getAmount());
                }
            }
        }

        double netBalance = totalIncome - totalExpenses;

        System.out.println("==================================================");
        System.out.printf("Report Period: %s to %s\n\n", startDate, endDate);

        System.out.printf("%sTotal Income:   %s%10.2f%s\n", GREEN, user.getCurrencySymbol(), totalIncome, RESET);
        System.out.printf("%sTotal Expenses: %s%10.2f%s\n", RED, user.getCurrencySymbol(), totalExpenses, RESET);
        System.out.println("--------------------------------------------------");

        String balanceColor = netBalance >= 0 ? GREEN : RED;
        System.out.printf("%sNet Balance:    %s%10.2f%s\n\n", balanceColor, user.getCurrencySymbol(), netBalance, RESET);

        if (!expensesByCategory.isEmpty()) {
            String reportTitle = aggregateByParent ? "Expenses by Category (Aggregated by Parent)" : "Expenses by Category";
            System.out.println(reportTitle);
            System.out.println("--------------------------------------------------");

            List<Map.Entry<String, Double>> sortedExpenses = new ArrayList<>(expensesByCategory.entrySet());
            sortedExpenses.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

            Map<String, List<String>> parentToSubs = new HashMap<>();
            if (aggregateByParent) {
                for (Transaction transaction : filteredTransactions) {
                    if (transaction.getType() == TransactionType.EXPENSE && isSubcategory(transaction.getCategory())) {
                        String parent = getParentCategory(transaction.getCategory());
                        String fullCategory = transaction.getCategory();
                        if (!parentToSubs.computeIfAbsent(parent, k -> new ArrayList<>()).contains(fullCategory)) {
                            parentToSubs.computeIfAbsent(parent, k -> new ArrayList<>()).add(fullCategory);
                        }
                    }
                }
            }

            for (Map.Entry<String, Double> entry : sortedExpenses) {
                String category = entry.getKey();
                double spent = entry.getValue();
                double percentage = (spent / totalExpenses) * 100;

                BudgetGoal goal = findGoalForCategory(category);
                if (goal != null) {
                    double goalAmount = goal.getAmount();
                    double progressPercentage = (spent / goalAmount) * 100;
                    String progressColor = progressPercentage <= 100 ? GREEN : RED;

                    System.out.printf("%-15s: %s%10.2f (%5.1f%%) - Goal: %s%.2f %s(%5.1f%%)%s\n",
                            category, user.getCurrencySymbol(), spent, percentage, user.getCurrencySymbol(), goalAmount,
                            progressColor, progressPercentage, RESET);
                } else {
                    System.out.printf("%-15s: %s%10.2f (%5.1f%%)\n",
                            category, user.getCurrencySymbol(), spent, percentage);
                }

                if (aggregateByParent && parentToSubs.containsKey(category)) {
                    Map<String, Double> subCategoryAmounts = new HashMap<>();

                    for (Transaction transaction : filteredTransactions) {
                        if (transaction.getType() == TransactionType.EXPENSE &&
                            isSubcategory(transaction.getCategory()) &&
                            getParentCategory(transaction.getCategory()).equals(category)) {
                            String subCategory = transaction.getCategory();
                            subCategoryAmounts.put(subCategory,
                                subCategoryAmounts.getOrDefault(subCategory, 0.0) + transaction.getAmount());
                        }
                    }

                    List<Map.Entry<String, Double>> sortedSubs = new ArrayList<>(subCategoryAmounts.entrySet());
                    sortedSubs.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

                    for (Map.Entry<String, Double> subEntry : sortedSubs) {
                        String subCategory = subEntry.getKey();
                        double subSpent = subEntry.getValue();
                        double subPercentage = (subSpent / spent) * 100;
                        String childName = getChildCategory(subCategory);

                        System.out.printf("  ↳ %-12s: %s%10.2f (%5.1f%% of parent)\n",
                                childName, user.getCurrencySymbol(), subSpent, subPercentage);
                    }
                }
            }
        }

        System.out.println("==================================================");
        pausePrompt();
    }

    private static void showBackupRestoreMenu() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sBackup/Restore%s%n%n", BLUE, RED, BLUE, RESET);

        boolean running = true;
        while (running) {
            System.out.printf("[%sC%s] Create Backup%n", GREEN, RESET);
            System.out.printf("[%sR%s] Restore from Backup%n", GREEN, RESET);
            System.out.printf("[%sB%s] Back to Main Menu%n", RED, RESET);
            System.out.println();
            System.out.printf("Please select an %soption%s: ", BLUE, RESET);

            String choice = scanner.nextLine().toLowerCase();

            switch (choice) {
            case "c":
                createBackup();
                break;
            case "r":
                restoreBackup();
                break;
            case "b":
                running = false;
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
                break;
            }
        }
    }

    private static void createBackup() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sCreate Backup%s%n%n", BLUE, RED, BLUE, RESET);

        try {
            String fname = store.filename;
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String backupName = fname + "_" + timestamp + ".bak";

            Path source = Paths.get(fname);
            Path target = Paths.get(backupName);

            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

            System.out.printf("Backup created: %s%s%s%n", GREEN, backupName, RESET);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        pausePrompt();
    }

    private static void restoreBackup() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sRestore from Backup%s%n%n", BLUE, RED, BLUE, RESET);

        String fname = store.filename;
        File dataDir = new File(fname).getParentFile();
        String prefix = new File(fname).getName() + "_";

        try {
            List<Path> backups = Files.list(dataDir.toPath())
                    .filter(path -> path.getFileName().toString().startsWith(prefix) &&
                                   path.getFileName().toString().endsWith(".bak"))
                    .sorted((p1, p2) -> p2.getFileName().toString().compareTo(p1.getFileName().toString()))
                    .collect(Collectors.toList());

            if (backups.isEmpty()) {
                System.out.println("No backups.");
                pausePrompt();
                return;
            }

            System.out.println("Available backups:");
            for (int i = 0; i < backups.size(); i++) {
                String filename = backups.get(i).getFileName().toString();
                String timestamp = filename.substring(prefix.length(), filename.length() - 4);
                System.out.printf("%s%d%s. %s (%s)%n", GREEN, i + 1, RESET, filename, formatTimestamp(timestamp));
            }

            System.out.printf("%nPlease select a backup number (1-%d) or 'q' to cancel: ", backups.size());
            String choice = scanner.nextLine().toLowerCase();

            if ("q".equals(choice)) {
                System.out.println("\nRestore cancelled.");
                pausePrompt();
                return;
            }

            Integer selection = tryToParse(choice);
            if (selection == null || selection < 1 || selection > backups.size()) {
                System.out.println("\nInvalid selection. Restore cancelled.");
                pausePrompt();
                return;
            }

            Path selectedBackup = backups.get(selection - 1);
            System.out.printf("%n%sWARNING: current data will be replaced.%s%n", RED, RESET);
            System.out.print("Continue? (y/n): ");
            String confirm = scanner.nextLine().toLowerCase();

            if (!"y".equals(confirm)) {
                System.out.println("\nRestore cancelled.");
                pausePrompt();
                return;
            }

            Path dataPath = Paths.get(fname);
            Files.copy(selectedBackup, dataPath, StandardCopyOption.REPLACE_EXISTING);

            System.out.printf("%n%sBackup restored.%s%n", GREEN, RESET);

            user = store.getUser(user.getUsername());
            categoriesSavedBefore = store.loadCategories(incCats, expCats);

            System.out.println("User data reloaded.");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        pausePrompt();
    }

    private static String formatTimestamp(String timestamp) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return timestamp;
        }
    }

    private static void changePassword() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sChange Password%s%n%n", BLUE, RED, BLUE, RESET);

        System.out.print("Current Password: ");
        String currentPassword = scanner.nextLine();

        if (!currentPassword.equals(user.getPassword())) {
            System.out.printf("%sIncorrect password. Change cancelled.%s%n", RED, RESET);
            pausePrompt();
            return;
        }

        System.out.print("New Password: ");
        String newPassword = scanner.nextLine();

        if (newPassword.trim().isEmpty()) {
            System.out.printf("%sPassword cannot be empty. Change cancelled.%s%n", RED, RESET);
            pausePrompt();
            return;
        }

        System.out.print("Confirm New Password: ");
        String confirmPassword = scanner.nextLine();

        if (!newPassword.equals(confirmPassword)) {
            System.out.printf("%sPasswords do not match. Change cancelled.%s%n", RED, RESET);
            pausePrompt();
            return;
        }

        user.setPassword(newPassword);
        store.updateUser(user);

        System.out.printf("%sPassword changed.%s%n", GREEN, RESET);
        pausePrompt();
    }

    private static void showOptionsMenu() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sOptions%s%n%n", BLUE, RED, BLUE, RESET);

        boolean running = true;
        while (running) {
            System.out.printf("[%sC%s] Set Currency Symbol%n", GREEN, RESET);
            System.out.printf("[%sB%s] Back to Main Menu%n", RED, RESET);
            System.out.println();
            System.out.printf("Please select an %soption%s: ", BLUE, RESET);

            String choice = scanner.nextLine().toLowerCase();

            switch (choice) {
            case "c":
                setCurrencySymbol();
                break;
            case "b":
                running = false;
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
                break;
            }
        }
    }

    private static void setCurrencySymbol() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sOptions%s > %sSet Currency Symbol%s%n%n", BLUE, RED, BLUE, RESET, BLUE, RESET);

        System.out.printf("Current currency symbol: %s%n%n", user.getCurrencySymbol());
        System.out.print("Enter new currency symbol (e.g., $, £, €): ");
        String newSymbol = scanner.nextLine().trim();

        if (!newSymbol.isEmpty()) {
            user.setCurrencySymbol(newSymbol);
            store.updateUser(user);
            System.out.printf("%sCurrency symbol updated.%s%n", GREEN, RESET);
        } else {
            System.out.printf("%sCurrency symbol cannot be empty. No changes made.%s%n", RED, RESET);
        }

        pausePrompt();
    }

    private static void manageSavingsGoals() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sSavings Goals%s%n%n", BLUE, RED, BLUE, RESET);

        boolean running = true;
        while (running) {
            System.out.printf("[%sA%s] Add Goal%n", GREEN, RESET);
            System.out.printf("[%sL%s] List Goals%n", GREEN, RESET);
            System.out.printf("[%sU%s] Update Progress%n", GREEN, RESET);
            System.out.printf("[%sD%s] Delete Goal%n", GREEN, RESET);
            System.out.printf("[%sQ%s] Back to Main Menu%n", RED, RESET);
            System.out.println();
            System.out.printf("Please select an %soption%s: ", BLUE, RESET);

            String choice = scanner.nextLine().toLowerCase();

            switch (choice) {
            case "a":
                addSavingsGoal();
                break;
            case "l":
                listSavingsGoals();
                break;
            case "u":
                updateSavingsGoalProgress();
                break;
            case "d":
                deleteSavingsGoal();
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

    private static void addSavingsGoal() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sSavings Goals%s > %sAdd Goal%s%n%n", BLUE, RED, BLUE, RESET, BLUE, RESET);

        System.out.print("Enter goal: ");
        String name = scanner.nextLine().trim();

        if (name.isEmpty()) {
            System.out.printf("%scannot be empty.%s%n", RED, RESET);
            pausePrompt();
            return;
        }

        double target;
        while (true) {
            System.out.print("Enter target (positive number): ");
            Double amount = tryToParseDouble(scanner.nextLine());
            if (amount != null && amount > 0) {
                target = amount;
                break;
            } else {
                System.out.println("Invalid amount. Please enter a positive number.");
            }
        }

        SavingsGoal goal = new SavingsGoal(name, target, 0);
        user.savingsGoals.add(goal);
        store.updateUser(user);

        System.out.printf("%sGoal '%s' added, target %s%.2f.%s%n",
                GREEN, name, user.getCurrencySymbol(), target, RESET);
        pausePrompt();
    }

    private static void listSavingsGoals() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sSavings Goals%s > %sList Goals%s%n%n", BLUE, RED, BLUE, RESET, BLUE, RESET);

        if (user.savingsGoals.isEmpty()) {
            System.out.println("No savings.");
            pausePrompt();
            return;
        }

        System.out.println("Current Savings:\n");
        System.out.println("#  | Name                | Progress                  | Percentage");
        System.out.println("---+---------------------+--------------------------+------------");

        for (int i = 0; i < user.savingsGoals.size(); i++) {
            SavingsGoal goal = user.savingsGoals.get(i);
            double percentage = goal.getPercentComplete();
            String percentColor = percentage < 100 ? GREEN : BLUE;

            System.out.printf("%s%2d%s | %-20s | %s%.2f%s/%s%.2f%s | %s%.1f%%%s%n",
                    GREEN, i + 1, RESET,
                    goal.getName(),
                    GREEN, goal.getCurrentAmount(), RESET,
                    BLUE, goal.getTargetAmount(), RESET,
                    percentColor, percentage, RESET);
        }

        pausePrompt();
    }

    private static void updateSavingsGoalProgress() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sSavings Goals%s > %sUpdate Progress%s%n%n", BLUE, RED, BLUE, RESET, BLUE, RESET);

        if (user.savingsGoals.isEmpty()) {
            System.out.println("No savings to update.");
            pausePrompt();
            return;
        }

        System.out.println("Select a goal to update:\n");
        for (int i = 0; i < user.savingsGoals.size(); i++) {
            SavingsGoal goal = user.savingsGoals.get(i);
            System.out.printf("%s%d%s. %s - Progress: %s%.2f%s/%s%.2f%s (%.1f%%)%n",
                    GREEN, i + 1, RESET,
                    goal.getName(),
                    GREEN, goal.getCurrentAmount(), RESET,
                    BLUE, goal.getTargetAmount(), RESET,
                    goal.getPercentComplete());
        }

        System.out.print("\nEnter goal number: ");
        String input = scanner.nextLine();
        Integer choice = tryToParse(input);

        if (choice == null || choice < 1 || choice > user.savingsGoals.size()) {
            System.out.println("Invalid choice.");
            pausePrompt();
            return;
        }

        SavingsGoal selectedGoal = user.savingsGoals.get(choice - 1);

        double amount;
        while (true) {
            System.out.print("Enter amount to add to goal (positive number): ");
            Double parsedAmount = tryToParseDouble(scanner.nextLine());
            if (parsedAmount != null && parsedAmount > 0) {
                amount = parsedAmount;
                break;
            } else {
                System.out.println("Invalid amount. Please enter a positive number.");
            }
        }

        double newAmount = selectedGoal.getCurrentAmount() + amount;
        selectedGoal.setCurrentAmount(newAmount);
        store.updateUser(user);

        double percentage = selectedGoal.getPercentComplete();
        boolean completed = percentage >= 100;

        System.out.printf("%sAdded %s%.2f to goal '%s'.%s%n",
                GREEN, user.getCurrencySymbol(), amount, selectedGoal.getName(), RESET);
        System.out.printf("New progress: %s%.2f%s/%s%.2f%s (%.1f%%)%n",
                GREEN, selectedGoal.getCurrentAmount(), RESET,
                BLUE, selectedGoal.getTargetAmount(), RESET,
                percentage);

        if (completed) {
            System.out.printf("%sCongratulations! Goal '%s' has been reached!%s%n",
                    BLUE, selectedGoal.getName(), RESET);
        }

        pausePrompt();
    }

    private static void deleteSavingsGoal() {
        cleanScreen();
        System.out.printf("%sBye Bye Money%s > %sSavings Goals%s > %sDelete Goal%s%n%n", BLUE, RED, BLUE, RESET, BLUE, RESET);

        if (user.savingsGoals.isEmpty()) {
            System.out.println("No savings to delete.");
            pausePrompt();
            return;
        }

        System.out.println("Select to delete:\n");
        for (int i = 0; i < user.savingsGoals.size(); i++) {
            SavingsGoal goal = user.savingsGoals.get(i);
            System.out.printf("%s%d%s. %s - Progress: %s%.2f%s/%s%.2f%s (%.1f%%)%n",
                    GREEN, i + 1, RESET,
                    goal.getName(),
                    GREEN, goal.getCurrentAmount(), RESET,
                    BLUE, goal.getTargetAmount(), RESET,
                    goal.getPercentComplete());
        }

        System.out.print("\nEnter goal number: ");
        String input = scanner.nextLine();
        Integer choice = tryToParse(input);

        if (choice == null || choice < 1 || choice > user.savingsGoals.size()) {
            System.out.println("Invalid choice.");
            pausePrompt();
            return;
        }

        SavingsGoal selectedGoal = user.savingsGoals.get(choice - 1);

        System.out.printf("\nAre you sure to delete the goal '%s'? (y/n): ", selectedGoal.getName());
        String confirm = scanner.nextLine().toLowerCase();

        if ("y".equals(confirm)) {
            user.savingsGoals.remove(choice - 1);
            store.updateUser(user);
            System.out.printf("%sGoal '%s' deleted.%s%n", GREEN, selectedGoal.getName(), RESET);
        } else {
            System.out.println("Deletion cancelled.");
        }

        pausePrompt();
    }
}