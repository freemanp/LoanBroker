package dk.cphbusiness.group11.RecipientList;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.BasicProperties.Builder;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

/*
Example expected xml message

<RecipientListRequest>
    <LoanDetails>
        <ssn>1234567890</ssn>
        <creditScore>685</creditScore> 
        <loanAmount>1000.0</loanAmount> 
        <loanDurationInMonths>36</loanDurationInMonths> 
    </LoanDetails>
    <BankList>
        <bank>Bank1</bank>
        <bank>Bank2</bank>
        <bank>Bank3</bank>
        <bank>Bank4</bank>
    </BankList>
</RecipientListRequest>
*/


public class RecipientList {
    private static final String LISTNENING_QUEUE = "loanbroker.group11.recipientList";
    
    private boolean isRunning;
    private HashMap<String, String> translatorMap;
    
    public RecipientList(){
        isRunning = true;
        translatorMap = new HashMap<String, String>();
        translatorMap.put("cphbusiness.bankXML", "translator.group11.cphbusiness.bankXML");
        translatorMap.put("cphbusiness.bankJSON", "translator.group11.cphbusiness.bankJSON");
        translatorMap.put("group11.poorBankWS", "translator.group11.PoorBankWS");
        translatorMap.put("group11.messagingBank", "translator.group11.cphbusiness.messagingBank");
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
    
    private void parseAndProcessXmlMessage(String xmlMessage) throws Exception{
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        
        Document xmlDocument = builder.parse(new ByteArrayInputStream(xmlMessage.getBytes()));
        XPath xPath = XPathFactory.newInstance().newXPath();
        
        Node loanDetailsNode = (Node) xPath.compile("/RecipientListRequest/LoanDetails").evaluate(xmlDocument, XPathConstants.NODE);
        NodeList banks = (NodeList) xPath.compile("/RecipientListRequest/BankList/bank").evaluate(xmlDocument, XPathConstants.NODESET);
        
        StringWriter writer = new StringWriter();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(new DOMSource(loanDetailsNode), new StreamResult(writer));
        String loanDetailsXmlString = writer.toString();
        
        ArrayList<String> banksToBeContacted = new ArrayList<String>();
        for (int i = 0; i < banks.getLength(); i++) {
            banksToBeContacted.add(banks.item(i).getFirstChild().getNodeValue());
            System.out.println("Bank to be contacted: " + banksToBeContacted.get(i));
        }
        
        contactBanks(banksToBeContacted, loanDetailsXmlString);
    }
    
    private void contactBanks(List<String> banksToBeContacted, String loanDetailsXml) throws Exception{
        if (banksToBeContacted.size() > 0 && loanDetailsXml != null && !loanDetailsXml.isEmpty()){
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("datdb.cphbusiness.dk");
            Connection connection = factory.newConnection();
        
            for (String bankQueue : banksToBeContacted){
                String translatorQueue = this.translatorMap.get(bankQueue);

                if (translatorQueue != null && !translatorQueue.isEmpty()){
                    Channel channel = connection.createChannel();
                    channel.exchangeDeclare(translatorQueue, "fanout");
                    Builder propertiesBuilder = new BasicProperties.Builder();
                    propertiesBuilder.replyTo(bankQueue);
                    BasicProperties properties = propertiesBuilder.build();
                    
                    channel.basicPublish(translatorQueue, "", properties, loanDetailsXml.getBytes());
                    
                    System.out.println("Contacting translator on queue " + translatorQueue);
                    System.out.println("With message: " + loanDetailsXml);
                    channel.close();
                }
            }
            
            connection.close();
        }
    }

    public static void main(String[] args) throws Exception {
        RecipientList rl = new RecipientList();
        rl.run();
    }
}