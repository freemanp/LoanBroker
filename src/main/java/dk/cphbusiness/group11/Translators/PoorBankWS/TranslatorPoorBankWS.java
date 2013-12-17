/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dk.cphbusiness.group11.Translators.PoorBankWS;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import dk.cphbusiness.group11.PoorBankWS.*;

/**
 *
 * @author Paul
 */
public class TranslatorPoorBankWS {
    private static final String RECEIVING_QUEUE = "translator.group11.PoorBankWS";
    private static final String SENDING_QUEUE = "normalizer.group11";
    private boolean isRunning;
    
    public TranslatorPoorBankWS(){
        this.isRunning = true;
    }
    
    private void parseAndProcessXmlMessage(String xmlMessage) throws Exception{
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        
        Document loanRequestXml = builder.parse(new ByteArrayInputStream(xmlMessage.getBytes()));
        XPath xPath = XPathFactory.newInstance().newXPath();
        Element loanDetailsElement = (Element) xPath.compile("/LoanDetails").evaluate(loanRequestXml, XPathConstants.NODE);
        String ssn = loanDetailsElement.getElementsByTagName("ssn").item(0).getTextContent();
        int creditScore = Integer.parseInt(loanDetailsElement.getElementsByTagName("creditScore").item(0).getTextContent());
        double loanAmount = Double.parseDouble(loanDetailsElement.getElementsByTagName("loanAmount").item(0).getTextContent());
        String temp = loanDetailsElement.getElementsByTagName("loanDurationInMonths").item(0).getTextContent();
        int loanDurationInMonths = Integer.parseInt(temp);
        
        PoorBankService_Service service = new PoorBankService_Service();
        PoorBankService port = service.getPoorBankServiceImplPort();
        String xmlReturnMessage = "";
        
        try {
            PoorLoanResponsee result = port.poorLoan(ssn, creditScore, loanAmount, loanDurationInMonths);
            xmlReturnMessage = "<LoanResponse>" +
                "<interestRate>" + result.getInterestRate() + "</interestRate> \n" +
                "   <ssn>" + result.getSsn() + "</ssn> \n" +
                "</LoanResponse>";
        }
        catch (PoorException_Exception pe){
            xmlReturnMessage = "error";
        }
        finally {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("datdb.cphbusiness.dk");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.exchangeDeclare(SENDING_QUEUE, "fanout");
            
            channel.basicPublish(SENDING_QUEUE, "", null, xmlReturnMessage.getBytes());
        }
        
        
    }
    
    public void run() throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("datdb.cphbusiness.dk");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(RECEIVING_QUEUE, "fanout");
        String queueName = channel.queueDeclare(RECEIVING_QUEUE, false, false, false, null).getQueue();
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
        TranslatorPoorBankWS t = new TranslatorPoorBankWS();
        t.run();
    }
}
