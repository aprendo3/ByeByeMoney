import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.OutputKeys;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

public class DataStore {
    String filename = "data/users.xml";

    public DataStore() {
        init();
    }


    public User getUser(String username) {
        Document doc;
        User user = null;
        try {
            doc = docFromFile(filename);
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            var query = String.format("//user[descendant::username[text()='%s']]", username);
            Node nodeUser = (Node) xpath
                    .compile(query)
                    .evaluate(doc, XPathConstants.NODE);

            if (nodeUser == null) return null;

            String uname = ((Element)nodeUser).getElementsByTagName("username").item(0).getTextContent();
            String password = ((Element)nodeUser).getElementsByTagName("password").item(0).getTextContent();
            System.out.println(uname + "|" + password);
            user = new User(username, password);

            Node currencySymbolNode = ((Element)nodeUser).getElementsByTagName("currencySymbol").item(0);
            if (currencySymbolNode != null) {
                user.setCurrencySymbol(currencySymbolNode.getTextContent());
            }

            Node savingsGoalsNode = ((Element)nodeUser).getElementsByTagName("savingsGoals").item(0);
            if (savingsGoalsNode != null && savingsGoalsNode.hasChildNodes()) {
                NodeList savingsGoals = savingsGoalsNode.getChildNodes();
                for (int i = 0; i < savingsGoals.getLength(); i++) {
                    Node nodeSavingsGoal = savingsGoals.item(i);
                    if (nodeSavingsGoal.getNodeType() == Node.TEXT_NODE) continue;

                    Node nameNode = ((Element)nodeSavingsGoal).getElementsByTagName("name").item(0);
                    Node targetAmountNode = ((Element)nodeSavingsGoal).getElementsByTagName("targetAmount").item(0);
                    Node currentAmountNode = ((Element)nodeSavingsGoal).getElementsByTagName("currentAmount").item(0);

                    if (nameNode != null && targetAmountNode != null && currentAmountNode != null) {
                        String name = nameNode.getTextContent();
                        double targetAmount = Double.parseDouble(targetAmountNode.getTextContent());
                        double currentAmount = Double.parseDouble(currentAmountNode.getTextContent());

                        SavingsGoal savingsGoal = new SavingsGoal(name, targetAmount, currentAmount);
                        user.savingsGoals.add(savingsGoal);
                    }
                }
            }

            Node nodeTransactions = ((Element)nodeUser).getElementsByTagName("transactions").item(0);

            if (nodeTransactions != null && nodeTransactions.hasChildNodes()) {
                NodeList transactions = nodeTransactions.getChildNodes();
                for (int i = 0; i < transactions.getLength(); i++){
                    Node nodeTransaction = transactions.item(i);
                    if (nodeTransaction.getNodeType() == Node.TEXT_NODE) continue;
                    var date = ((Element)nodeTransaction).getElementsByTagName("date").item(0).getTextContent();
                    var description = ((Element)nodeTransaction).getElementsByTagName("description").item(0).getTextContent();
                    var amount = Double.parseDouble(((Element)nodeTransaction).getElementsByTagName("amount").item(0).getTextContent());
                    var type = TransactionType.valueOf(((Element)nodeTransaction).getElementsByTagName("type").item(0).getTextContent());

                    String category = "Other";
                    Node categoryNode = ((Element)nodeTransaction).getElementsByTagName("category").item(0);
                    if (categoryNode != null) {
                        category = categoryNode.getTextContent();
                    }

                    Transaction transaction = new Transaction(date, description, amount, type, category);
                    user.transactions.add(transaction);
                }
            }

            Node nodeGoals = ((Element)nodeUser).getElementsByTagName("goals").item(0);

            if (nodeGoals != null && nodeGoals.hasChildNodes()) {
                NodeList goals = nodeGoals.getChildNodes();
                for (int i = 0; i < goals.getLength(); i++) {
                    Node nodeGoal = goals.item(i);
                    if (nodeGoal.getNodeType() == Node.TEXT_NODE) continue;

                    Node categoryNode = ((Element)nodeGoal).getElementsByTagName("categoryName").item(0);
                    Node amountNode = ((Element)nodeGoal).getElementsByTagName("monthlyAmount").item(0);

                    if (categoryNode != null && amountNode != null) {
                        String category = categoryNode.getTextContent();
                        double amount = Double.parseDouble(amountNode.getTextContent());

                        BudgetGoal goal = new BudgetGoal(category, amount);
                        user.goals.add(goal);
                    }
                }
            }

            Node nodeRecurring = ((Element)nodeUser).getElementsByTagName("recurringTransactions").item(0);

            if (nodeRecurring != null && nodeRecurring.hasChildNodes()) {
                NodeList recurrings = nodeRecurring.getChildNodes();
                for (int i = 0; i < recurrings.getLength(); i++) {
                    Node nodeRec = recurrings.item(i);
                    if (nodeRec.getNodeType() == Node.TEXT_NODE) continue;

                    var description = ((Element)nodeRec).getElementsByTagName("description").item(0).getTextContent();
                    var amount = Double.parseDouble(((Element)nodeRec).getElementsByTagName("amount").item(0).getTextContent());
                    var type = TransactionType.valueOf(((Element)nodeRec).getElementsByTagName("type").item(0).getTextContent());
                    var category = ((Element)nodeRec).getElementsByTagName("category").item(0).getTextContent();
                    var nextDueDate = ((Element)nodeRec).getElementsByTagName("nextDueDate").item(0).getTextContent();

                    RecurringTransaction recurring = new RecurringTransaction(description, amount, type, category, nextDueDate);
                    user.recurringTransactions.add(recurring);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return user;
    }

    public void addUser(User user) {
        Document doc;
        try {
            doc = docFromFile(filename);
            Element userNode = doc.createElement("user");
            Element username = doc.createElement("username");
            username.appendChild(doc.createTextNode(user.getUsername()));
            Element password = doc.createElement("password");
            password.appendChild(doc.createTextNode(user.getPassword()));
            Element currencySymbol = doc.createElement("currencySymbol");
            currencySymbol.appendChild(doc.createTextNode(user.getCurrencySymbol()));
            userNode.appendChild(username);
            userNode.appendChild(password);
            userNode.appendChild(currencySymbol);
            doc.getDocumentElement()
                    .getElementsByTagName("users").item(0)
                    .appendChild(userNode);

            docToFile(doc, filename);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void removeWhitespaceNodes(Node node) {
    NodeList list = node.getChildNodes();
    for (int i = list.getLength()-1; i >= 0; i--) {
        Node child = list.item(i);
        if (child.getNodeType() == Node.TEXT_NODE) {
            if (child.getTextContent().trim().isEmpty()) {
                node.removeChild(child);
            }
        } else if (child.hasChildNodes()) {
            removeWhitespaceNodes(child);
        }
    }
}

    private void docToFile(Document doc, String nameFile) throws TransformerException {
        removeWhitespaceNodes(doc.getDocumentElement());
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(doc), new StreamResult(new File(nameFile)));
    }

    public Document docFromFile(String nameFile) throws SAXException, IOException, ParserConfigurationException {
        File xmlFile = new File(nameFile);
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newDefaultInstance();

        factory.setIgnoringElementContentWhitespace(true);
        Document doc = factory.newDocumentBuilder().parse(xmlFile);
        doc.getDocumentElement().normalize();

        return doc;
    }

    private void init() {
        try {
            File file = new File(filename);
            if (!file.exists()) {
                Document document = DocumentBuilderFactory
                    .newDefaultInstance()
                    .newDocumentBuilder()
                    .newDocument();

                Element root = document.createElement("data");
                document.appendChild(root);

                Element users = document.createElement("users");
                root.appendChild(users);

                docToFile(document, filename);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void updateUser(User user) {
        Document doc;
        try {
            doc = docFromFile(filename);

            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            var query = String.format("//user[descendant::username[text()='%s']]", user.getUsername());
            Node nodeUser = (Node) xpath
                    .compile(query)
                    .evaluate(doc, XPathConstants.NODE);

            if (nodeUser == null) return;

            Node passwordNode = ((Element)nodeUser).getElementsByTagName("password").item(0);
            if (passwordNode != null) {
                passwordNode.setTextContent(user.getPassword());
            }

            Node currencySymbolNode = ((Element)nodeUser).getElementsByTagName("currencySymbol").item(0);
            if (currencySymbolNode != null) {
                currencySymbolNode.setTextContent(user.getCurrencySymbol());
            } else {
                Element currencySymbol = doc.createElement("currencySymbol");
                currencySymbol.appendChild(doc.createTextNode(user.getCurrencySymbol()));
                nodeUser.appendChild(currencySymbol);
            }

            Node nodeTransactions = ((Element)nodeUser).getElementsByTagName("transactions").item(0);

            if (nodeTransactions != null) {
                nodeUser.removeChild(nodeTransactions);
            }

            nodeTransactions = doc.createElement("transactions");

            for (int i = 0; i < user.transactions.size(); i++) {
                Element nodeTransaction = doc.createElement("transaction");
                Element date = doc.createElement("date");
                date.appendChild(doc.createTextNode(user.transactions.get(i).getDate()));
                Element description = doc.createElement("description");
                description.appendChild(doc.createTextNode(user.transactions.get(i).getDescription()));
                Element amount = doc.createElement("amount");
                amount.appendChild(doc.createTextNode(user.transactions.get(i).getAmount() + ""));
                Element type = doc.createElement("type");
                type.appendChild(doc.createTextNode(user.transactions.get(i).getType().toString()));
                Element category = doc.createElement("category");
                category.appendChild(doc.createTextNode(user.transactions.get(i).getCategory()));

                nodeTransaction.appendChild(date);
                nodeTransaction.appendChild(description);
                nodeTransaction.appendChild(amount);
                nodeTransaction.appendChild(type);
                nodeTransaction.appendChild(category);

                nodeTransactions.appendChild(nodeTransaction);
            }

            nodeUser.appendChild(nodeTransactions);

            Node nodeGoals = ((Element)nodeUser).getElementsByTagName("goals").item(0);

            if (nodeGoals != null) {
                nodeUser.removeChild(nodeGoals);
            }

            if (!user.goals.isEmpty()) {
                Element goalsElement = doc.createElement("goals");

                for (BudgetGoal goal : user.goals) {
                    Element goalElement = doc.createElement("goal");

                    Element categoryName = doc.createElement("categoryName");
                    categoryName.appendChild(doc.createTextNode(goal.getCategory()));

                    Element monthlyAmount = doc.createElement("monthlyAmount");
                    monthlyAmount.appendChild(doc.createTextNode(String.valueOf(goal.getAmount())));

                    goalElement.appendChild(categoryName);
                    goalElement.appendChild(monthlyAmount);

                    goalsElement.appendChild(goalElement);
                }

                nodeUser.appendChild(goalsElement);
            }

            Node nodeRecurring = ((Element)nodeUser).getElementsByTagName("recurringTransactions").item(0);

            if (nodeRecurring != null) {
                nodeUser.removeChild(nodeRecurring);
            }

            Node nodeSavingsGoals = ((Element)nodeUser).getElementsByTagName("savingsGoals").item(0);

            if (nodeSavingsGoals != null) {
                nodeUser.removeChild(nodeSavingsGoals);
            }

            if (!user.savingsGoals.isEmpty()) {
                Element savingsGoalsElement = doc.createElement("savingsGoals");

                for (SavingsGoal savingsGoal : user.savingsGoals) {
                    Element savingsGoalElement = doc.createElement("savingsGoal");

                    Element name = doc.createElement("name");
                    name.appendChild(doc.createTextNode(savingsGoal.getName()));

                    Element targetAmount = doc.createElement("targetAmount");
                    targetAmount.appendChild(doc.createTextNode(String.valueOf(savingsGoal.getTargetAmount())));

                    Element currentAmount = doc.createElement("currentAmount");
                    currentAmount.appendChild(doc.createTextNode(String.valueOf(savingsGoal.getCurrentAmount())));

                    savingsGoalElement.appendChild(name);
                    savingsGoalElement.appendChild(targetAmount);
                    savingsGoalElement.appendChild(currentAmount);

                    savingsGoalsElement.appendChild(savingsGoalElement);
                }

                nodeUser.appendChild(savingsGoalsElement);
            }

            if (!user.recurringTransactions.isEmpty()) {
                Element recurringElement = doc.createElement("recurringTransactions");

                for (RecurringTransaction recurring : user.recurringTransactions) {
                    Element recElement = doc.createElement("recurring");

                    Element description = doc.createElement("description");
                    description.appendChild(doc.createTextNode(recurring.getDescription()));

                    Element amount = doc.createElement("amount");
                    amount.appendChild(doc.createTextNode(String.valueOf(recurring.getAmount())));

                    Element type = doc.createElement("type");
                    type.appendChild(doc.createTextNode(recurring.getType().toString()));

                    Element category = doc.createElement("category");
                    category.appendChild(doc.createTextNode(recurring.getCategory()));

                    Element frequency = doc.createElement("frequency");
                    frequency.appendChild(doc.createTextNode(recurring.getFrequency()));

                    Element nextDueDate = doc.createElement("nextDueDate");
                    nextDueDate.appendChild(doc.createTextNode(recurring.getNextDueDate()));

                    recElement.appendChild(description);
                    recElement.appendChild(amount);
                    recElement.appendChild(type);
                    recElement.appendChild(category);
                    recElement.appendChild(frequency);
                    recElement.appendChild(nextDueDate);

                    recurringElement.appendChild(recElement);
                }

                nodeUser.appendChild(recurringElement);
            }

            docToFile(doc, filename);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void saveCategories(List<String> incomeCats, List<String> expenseCats) {
        Document doc;
        try {
            doc = docFromFile(filename);

            Node categoriesNode = doc.getDocumentElement().getElementsByTagName("categories").item(0);

            if (categoriesNode != null) {
                doc.getDocumentElement().removeChild(categoriesNode);
            }

            Element categories = doc.createElement("categories");

            for (String category : incomeCats) {
                Element categoryNode = doc.createElement("category");
                Element name = doc.createElement("name");
                name.appendChild(doc.createTextNode(category));
                Element type = doc.createElement("type");
                type.appendChild(doc.createTextNode("INCOME"));

                categoryNode.appendChild(name);
                categoryNode.appendChild(type);
                categories.appendChild(categoryNode);
            }

            for (String category : expenseCats) {
                Element categoryNode = doc.createElement("category");
                Element name = doc.createElement("name");
                name.appendChild(doc.createTextNode(category));
                Element type = doc.createElement("type");
                type.appendChild(doc.createTextNode("EXPENSE"));

                categoryNode.appendChild(name);
                categoryNode.appendChild(type);
                categories.appendChild(categoryNode);
            }

            doc.getDocumentElement().appendChild(categories);
            docToFile(doc, filename);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public boolean loadCategories(List<String> incomeCats, List<String> expenseCats) {
        Document doc;
        try {
            doc = docFromFile(filename);

            NodeList categoryNodes = doc.getElementsByTagName("category");
            if (categoryNodes == null || categoryNodes.getLength() == 0) {
                return false;
            }

            List<String> loadedIncomeCats = new ArrayList<>();
            List<String> loadedExpenseCats = new ArrayList<>();

            for (int i = 0; i < categoryNodes.getLength(); i++) {
                Element categoryNode = (Element) categoryNodes.item(i);
                if (categoryNode.getNodeType() == Node.TEXT_NODE) continue;

                Node nameNode = categoryNode.getElementsByTagName("name").item(0);
                Node typeNode = categoryNode.getElementsByTagName("type").item(0);

                if (nameNode != null && typeNode != null) {
                    String name = nameNode.getTextContent();
                    String type = typeNode.getTextContent();

                    if ("INCOME".equals(type)) {
                        loadedIncomeCats.add(name);
                    } else if ("EXPENSE".equals(type)) {
                        loadedExpenseCats.add(name);
                    }
                }
            }

            if (!loadedIncomeCats.isEmpty()) {
                incomeCats.clear();
                incomeCats.addAll(loadedIncomeCats);
            }

            if (!loadedExpenseCats.isEmpty()) {
                expenseCats.clear();
                expenseCats.addAll(loadedExpenseCats);
            }

            return !loadedIncomeCats.isEmpty() || !loadedExpenseCats.isEmpty();
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}