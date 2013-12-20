package dk.cphbusiness.group11.GetCreditScore;

/**
 * expects:
 * 	<LoanRequest>
 *		<loanAmount>5.5</loanAmount>
 *		<loanDurationInMonths>60</loanDurationInMonths>
 *		<ssn>1605789787</ssn>
 *	</LoanRequest>
 * returns:
 * 	<LoanRequest>
 *		<loanAmount>5.5</loanAmount>
 *		<loanDurationInMonths>60</loanDurationInMonths>
 *		<ssn>1605789787</ssn>
 *		<creditScore>1605789787</creditScore>
 *	</LoanRequest>
 */

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.ws.WebServiceRef;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.bank.services.credit.web.services.CreditScoreService_Service;

public class GetCreditScore {
    private static final String RECEIVING_QUEUE = "cphbusiness.group11.GetCreditScore";
    private static final String REPLY_TO_QUEUE = "loanbroker.group11.recipientList";
    private static final String SENDING_QUEUE = "group11.GetBanks";
    private boolean isRunning;
    
    @WebServiceRef(wsdlLocation = "WEB-INF/wsdl/datdb.cphbusiness.dk_8080/CreditBureau/CreditScoreService.wsdl")
    private CreditScoreService_Service service;
    
    public GetCreditScore(){
        this.isRunning = true;
    }
    
    private void parseAndProcessXmlMessage(String xmlMessage) throws Exception{
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        
        Document loanRequestXml = builder.parse(new ByteArrayInputStream(xmlMessage.getBytes()));
        XPath xPath = XPathFactory.newInstance().newXPath();
        Element loanDetailsElement = (Element) xPath.compile("/LoanRequest").evaluate(loanRequestXml, XPathConstants.NODE);
        String ssn = loanDetailsElement.getElementsByTagName("ssn").item(0).getTextContent();
        String loanAmount = loanDetailsElement.getElementsByTagName("loanAmount").item(0).getTextContent();
        String loanDurationInMonths = loanDetailsElement.getElementsByTagName("loanDurationInMonths").item(0).getTextContent();
        int creditScore = this.creditScore(ssn);
      
        String xmlReturnMessage = "<LoanRequest>\n" +
                "   <loanAmount>" + loanAmount + "</loanAmount> \n" +
                "   <loanDurationInMonths>" + loanDurationInMonths + "</loanDurationInMonths> \n" +
                "   <creditScore>" + creditScore + "</creditScore> \n" +
                "   <ssn>" + ssn + "</ssn> \n" +
                "</LoanRequest>";
        
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("datdb.cphbusiness.dk");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(SENDING_QUEUE, "fanout");
        AMQP.BasicProperties.Builder propertiesBuilder = new AMQP.BasicProperties.Builder();
        propertiesBuilder.replyTo(REPLY_TO_QUEUE);
        AMQP.BasicProperties properties = propertiesBuilder.build();
        
        System.out.println("Sending to queue '" + SENDING_QUEUE + "' message: " + xmlReturnMessage);
        channel.basicPublish(SENDING_QUEUE, "", properties, xmlReturnMessage.getBytes());
    }
    
    public void run() throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("datdb.cphbusiness.dk");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(RECEIVING_QUEUE, "fanout");
        String queueName = channel.queueDeclare(RECEIVING_QUEUE,false,  false, false, null).getQueue();
        channel.queueBind(queueName, RECEIVING_QUEUE, "");
        System.out.println("GetCreditScore");
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
    
    private int creditScore(java.lang.String ssn) throws Exception{
        org.bank.services.credit.web.services.CreditScoreService_Service service = new org.bank.services.credit.web.services.CreditScoreService_Service();
        org.bank.services.credit.web.services.CreditScoreService port = service.getCreditScoreServicePort();
        // TODO process result here
        int result = port.creditScore(ssn);

        return result;
    }
    
    public static void main(String[] args) throws Exception {
        GetCreditScore gcs = new GetCreditScore();
        gcs.run();
    }
}
