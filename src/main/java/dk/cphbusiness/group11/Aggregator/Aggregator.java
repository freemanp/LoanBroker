package dk.cphbusiness.group11.Aggregator;
/*
 * expects:
 * 	<LoanResponse>
 *		<interestRate>5.5</interestRate>
 *		<ssn>1605789787</ssn>
 *		<bank>cphbusiness.bankXML</bank>
 *	</LoanResponse>
 * returns:
 * 	<LoanResponse>
 *		<interestRate>5.5</interestRate>
 *		<ssn>1605789787</ssn>
 *		<bank>cphbusiness.bankXML</bank>
 *	</LoanResponse>
 */
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import dk.cphbusiness.group11.PoorBankWS.PoorBankService;
import dk.cphbusiness.group11.PoorBankWS.PoorBankService_Service;
import dk.cphbusiness.group11.PoorBankWS.PoorLoanResponsee;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Aggregator {
    private HashMap<String, List<LoanResponseDetails>> activeLoanRequests;
    private static final String RECEIVING_QUEUE = "aggregator.group11";
    private static final String SENDING_QUEUE = "group11.loanRequest.response";
    private boolean isRunning;
    
    public Aggregator(){
        this.activeLoanRequests = new HashMap<String, List<LoanResponseDetails>>();
        this.isRunning = true;
    }
    
    private void parseAndProcessXmlMessage(String xmlMessage) throws Exception{
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        
        Document loanRequestXml = builder.parse(new ByteArrayInputStream(xmlMessage.getBytes()));
        XPath xPath = XPathFactory.newInstance().newXPath();
        Element loanDetailsElement = (Element) xPath.compile("/LoanResponse").evaluate(loanRequestXml, XPathConstants.NODE);
        String ssn = loanDetailsElement.getElementsByTagName("ssn").item(0).getTextContent();
        double interestRate = Double.parseDouble(loanDetailsElement.getElementsByTagName("interestRate").item(0).getTextContent());
        String bank = loanDetailsElement.getElementsByTagName("bank").item(0).getTextContent();
        
        LoanResponseDetails loanResponseDetails = new LoanResponseDetails(ssn, bank, interestRate);
        
        //wait until there are 4 responses, figure out the best and send a response
        List<LoanResponseDetails> currentLoanRequestList = this.activeLoanRequests.get(ssn);
        if (currentLoanRequestList != null){
            if (currentLoanRequestList.size() >= 3){
                LoanResponseDetails bestLoanResponse = currentLoanRequestList.get(0);
                
                for (int i = 1; i < currentLoanRequestList.size(); i++){
                    if (bestLoanResponse.getInterestRate() > currentLoanRequestList.get(i).getInterestRate()){
                        bestLoanResponse = currentLoanRequestList.get(i);
                    }
                }
                
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost("datdb.cphbusiness.dk");
                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();

                channel.exchangeDeclare(SENDING_QUEUE, "fanout");

                String xmlReturnMessage = "<LoanResponse>" + 
                        "   <bank>" + bestLoanResponse.getBank() + "</bank> \n" +
                        "   <interestRate>" + bestLoanResponse.getInterestRate() + "</interestRate> \n" +
                        "   <ssn>" + bestLoanResponse.getSsn() + "</ssn> \n" +
                        "</LoanResponse>";
                
                System.out.println("Sending message: " + xmlReturnMessage);
                channel.basicPublish(SENDING_QUEUE, "", null, xmlReturnMessage.getBytes());
            }
            else {
                currentLoanRequestList.add(loanResponseDetails);
            }
        }
        else {
            currentLoanRequestList = new ArrayList<LoanResponseDetails>();
            currentLoanRequestList.add(loanResponseDetails);
            
            this.activeLoanRequests.put(ssn, currentLoanRequestList);
        }
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
        Aggregator aggro = new Aggregator();
        aggro.run();
    }
}
