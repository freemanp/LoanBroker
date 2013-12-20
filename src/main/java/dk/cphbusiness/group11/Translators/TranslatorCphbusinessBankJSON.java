package dk.cphbusiness.group11.Translators;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.BasicProperties.Builder;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

public class TranslatorCphbusinessBankJSON {
    private static final String RECEIVING_QUEUE = "translator.group11.cphbusiness.bankJSON";
    private static final String SENDING_QUEUE = "cphbusiness.bankJSON";
    private static final String REPLY_TO_QUEUE = "normalizer.group11.bankJSON";
    private boolean isRunning;
    
    public TranslatorCphbusinessBankJSON(){
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
        String temp = loanDetailsElement.getElementsByTagName("loanDurationInMonths").item(0).getTextContent();
        int loanDurationInMonths = Integer.parseInt(temp);
        
        JsonObjectBuilder loanBuilder = Json.createObjectBuilder();
        
        loanBuilder.add("ssn", ssn);
        
        loanBuilder.add("creditScore", creditScore);
        
        loanBuilder.add("loanAmount", loanAmount);
        
        loanBuilder.add("loanDuration", loanDurationInMonths);
        
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("datdb.cphbusiness.dk");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(SENDING_QUEUE, "fanout");
        Builder propertiesBuilder = new BasicProperties.Builder();
        propertiesBuilder.replyTo(REPLY_TO_QUEUE);
        BasicProperties properties = propertiesBuilder.build();
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonWriter jsonWriter = Json.createWriter(outputStream);
        jsonWriter.writeObject(loanBuilder.build());
        jsonWriter.close();
        
        System.out.println("Sent message: " + outputStream.toString());
        channel.basicPublish(SENDING_QUEUE, "", properties, outputStream.toByteArray());
    }
    
    public void run() throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("datdb.cphbusiness.dk");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(RECEIVING_QUEUE, "fanout");
        String queueName = channel.queueDeclare(RECEIVING_QUEUE,false,  false, false, null).getQueue();
        channel.queueBind(queueName, RECEIVING_QUEUE, "");
        System.out.println("Waiting for messages on queue: " + RECEIVING_QUEUE);

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
        TranslatorCphbusinessBankJSON t = new TranslatorCphbusinessBankJSON();
        t.run();
    }
}
