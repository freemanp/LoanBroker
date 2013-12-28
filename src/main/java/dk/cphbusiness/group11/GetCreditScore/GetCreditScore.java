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
import com.rabbitmq.client.QueueingConsumer;
import dk.cphbusiness.group11.loanbroker.LoanBrokerComponent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class GetCreditScore extends LoanBrokerComponent{
    private static final String RECEIVING_QUEUE = "loanbroker.group11.GetCreditScore";
    private static final String REPLY_TO_QUEUE = "loanbroker.group11.recipientList";
    private static final String SENDING_QUEUE = "group11.GetBanks";
    private boolean isRunning;
    
    public GetCreditScore() throws IOException{
        super("GetCreditScore");
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
        
        Channel channel = this.getChannel(SENDING_QUEUE);

        AMQP.BasicProperties.Builder propertiesBuilder = new AMQP.BasicProperties.Builder();
        propertiesBuilder.replyTo(REPLY_TO_QUEUE);
        AMQP.BasicProperties properties = propertiesBuilder.build();
        
        this.log(String.format("sending to queue '%s' with reply-to flag '%s' following message: '%s'", SENDING_QUEUE, REPLY_TO_QUEUE, xmlReturnMessage));
        channel.basicPublish("", SENDING_QUEUE, properties, xmlReturnMessage.getBytes());
    }
    
    public void run() throws Exception{
        this.log("running component");
        
        Channel channel = this.getChannel(RECEIVING_QUEUE);
        String queueName = channel.queueDeclare(RECEIVING_QUEUE,false,  false, false, null).getQueue();
        channel.queueBind(queueName, RECEIVING_QUEUE, "");
        this.log(String.format("waiting for messages on queue: '%s'", RECEIVING_QUEUE));

        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(queueName, true, consumer);
        
        while (this.isRunning){
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            String message = new String(delivery.getBody());

            this.log(String.format("received message: '%s'", message));
            
            this.parseAndProcessXmlMessage(message);
        }
    }
    
    private int creditScore(java.lang.String ssn) throws Exception{
        org.bank.services.credit.web.services.CreditScoreService_Service service = new org.bank.services.credit.web.services.CreditScoreService_Service();
        org.bank.services.credit.web.services.CreditScoreService port = service.getCreditScoreServicePort();
        int result = port.creditScore(ssn);

        this.log(String.format("credit score received for %s: %s", ssn, result));
        
        return result;
    }
    
    public static void main(String[] args) throws Exception {
        GetCreditScore gcs = new GetCreditScore();
        gcs.run();
    }
}
