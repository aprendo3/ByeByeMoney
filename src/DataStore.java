
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
            userNode.appendChild(username);
            userNode.appendChild(password);
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
}
