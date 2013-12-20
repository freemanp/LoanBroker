/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.cphbusiness.group11.BankMessaging;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

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

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

/**
 *
 * @author Anth
 */
public class MessagingBank {

    private static final String EXCHANGE_NAME = "group11.messagingBank";
    private static QueueingConsumer.Delivery delivery;

    public static void main(String[] argv) throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("datdb.cphbusiness.dk");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "");
        
        System.out.println("Messaging Bank");
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(queueName, true, consumer);
        while (true) {
            delivery = consumer.nextDelivery();
            String message = new String(delivery.getBody());

            System.out.println(" [x] Received '" + message + "'");

            //process message
            processMessage(message);
        }
    }

    private static void processMessage(String message) throws Exception {
        // read XML
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        
        Document loanRequestXml = builder.parse(new ByteArrayInputStream(message.getBytes()));
        XPath xPath = XPathFactory.newInstance().newXPath();
        Element loanDetailsElement = (Element) xPath.compile("/LoanRequest").evaluate(loanRequestXml, XPathConstants.NODE);
        String ssn = loanDetailsElement.getElementsByTagName("ssn").item(0).getTextContent();
        String creditScore = loanDetailsElement.getElementsByTagName("creditScore").item(0).getTextContent();
        String loanAmount = loanDetailsElement.getElementsByTagName("loanAmount").item(0).getTextContent();
        String temp = loanDetailsElement.getElementsByTagName("loanDuration").item(0).getTextContent();
     //   int loanDurationInMonths = Integer.parseInt(temp);
        
        //String to int
        int amount = Integer.parseInt(loanAmount);
        int score = Integer.parseInt(creditScore);
            
        //get loan rate
        float rate = getLoanRate(score);
        
        //change float rate to String
        String interestRate = Float.toString(rate);
        
        // build reply message
        // XML reply same format as the school bank xml
        //            <LoanResponse>
        //                <interestRate>4.5600000000000005</interestRate>
        //                 <ssn>12345678</ssn>
        //            </LoanResponse>
        
        
        String replyMessage = "";
        
        Document bankRequestXml = builder.newDocument();
        Element root = bankRequestXml.createElement("LoanResponse");
        bankRequestXml.appendChild(root);
        
        Element element = bankRequestXml.createElement("interestRate");
        element.appendChild(bankRequestXml.createTextNode(interestRate));
        root.appendChild(element);
        
        element = bankRequestXml.createElement("ssn");
        element.appendChild(bankRequestXml.createTextNode(ssn));
        root.appendChild(element);
        
         StringWriter writer = new StringWriter();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(new DOMSource(root), new StreamResult(writer));
        
        
        replyMessage = writer.toString();
        
        
       
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("datdb.cphbusiness.dk");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        //get reply-to queue
        BasicProperties props = delivery.getProperties();
        BasicProperties replyProps = new BasicProperties.Builder()
                .correlationId(props.getCorrelationId()).build();

        channel.basicPublish("", props.getReplyTo(), replyProps, replyMessage.getBytes());
        
        System.out.println(" [x] Sent '" + message + "'");

        channel.close();
        connection.close();
    }
    
    private static float getLoanRate(int creditScore){
        
        float rate = 0;
        
        rate = 5 / ((float)creditScore /800);
        
        return rate;
        
    }
    
}
