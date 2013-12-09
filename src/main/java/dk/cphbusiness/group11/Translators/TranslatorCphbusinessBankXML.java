package dk.cphbusiness.group11.Translators;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.sun.org.apache.xerces.internal.dom.NodeImpl;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TranslatorCphbusinessBankXML {
    private static final String LISTNENING_QUEUE = "translator.group11.cphbusiness.bankXML";
    private boolean isRunning;
    
    public TranslatorCphbusinessBankXML(){
        this.isRunning = true;
    }
    
    private void parseAndProcessXmlMessage(String xmlMessage) throws Exception{
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        
        Document loanRequestXml = builder.parse(new ByteArrayInputStream(xmlMessage.getBytes()));
        XPath xPath = XPathFactory.newInstance().newXPath();
        Element loanDetailsElement = (Element) xPath.compile("/LoanDetails").evaluate(loanRequestXml, XPathConstants.NODE);
        String ssn = loanDetailsElement.getElementsByTagName("ssn").item(0).getTextContent();
        String creditScore = loanDetailsElement.getElementsByTagName("creditScore").item(0).getTextContent();
        String loanAmount = loanDetailsElement.getElementsByTagName("loanAmount").item(0).getTextContent();
        String loanDurationInMonths = loanDetailsElement.getElementsByTagName("loanDurationInMonths").item(0).getTextContent();
        
        Document bankRequestXml = builder.newDocument();
        Element root = bankRequestXml.createElement("LoanRequest");
        bankRequestXml.appendChild(root);
        
        Element element = bankRequestXml.createElement("ssn");
        element.appendChild(bankRequestXml.createTextNode(ssn));
        root.appendChild(element);
        
        element = bankRequestXml.createElement("creditScore");
        element.appendChild(bankRequestXml.createTextNode(creditScore));
        root.appendChild(element);
        
        element = bankRequestXml.createElement("loanAmount");
        element.appendChild(bankRequestXml.createTextNode(loanAmount));
        root.appendChild(element);
        
        element = bankRequestXml.createElement("loanDurationInMonths");
        element.appendChild(bankRequestXml.createTextNode(loanDurationInMonths));
        root.appendChild(element);
    }
    
    public void run() throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("datdb.cphbusiness.dk");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(LISTNENING_QUEUE, "fanout");
        String queueName = channel.queueDeclare(LISTNENING_QUEUE,false,  false, false, null).getQueue();
        channel.queueBind(queueName, LISTNENING_QUEUE, "");
        System.out.println("Waiting for messages on queue: " + LISTNENING_QUEUE);

        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(queueName, true, consumer);
        
        while (this.isRunning){
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            String message = new String(delivery.getBody());

            System.out.println("Received '" + message + "'");
            
            this.parseAndProcessXmlMessage(message);
        }
    }
    
    public static void main(String[] args) throws Exception {
        TranslatorCphbusinessBankXML t = new TranslatorCphbusinessBankXML();
        t.run();
    }
}
