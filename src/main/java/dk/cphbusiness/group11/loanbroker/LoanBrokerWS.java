/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dk.cphbusiness.group11.loanbroker;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;

/**
 *
 * @author Paul
 */
@WebService(serviceName = "LoanBrokerWS")
public class LoanBrokerWS {
    private static final String RECEIVING_QUEUE = "cphbusiness.group11.LoanBroakerWS";
    private static final String SENDING_QUEUE = "cphbusiness.group11.GetCreditScore";

    /**
     * This is a sample web service operation
     */
    @WebMethod(operationName = "hello")
    public String hello(@WebParam(name = "name") String txt) {
        return "Hello " + txt + " !";
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "LoanRequest")
    public String LoanRequest(@WebParam(name = "ssn") String ssn, @WebParam(name = "loanAmount") double loanAmount, @WebParam(name = "loanDurationInMonths") int loanDurationInMonths) throws Exception {
        String xmlMessage = "<LoanRequest>\n" +
                "   <loanAmount>" + loanAmount + "</loanAmount> \n" +
                "   <loanDurationInMonths>" + loanDurationInMonths + "</loanDurationInMonths> \n" +
                "   <ssn>" + ssn + "</ssn> \n" +
                "</LoanRequest>";
        
        System.out.println("Received loan request");
        
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("datdb.cphbusiness.dk");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(SENDING_QUEUE, "fanout");
        
        System.out.println("Sending to queue '" + SENDING_QUEUE + "' message: " + xmlMessage);
        channel.basicPublish(SENDING_QUEUE, "", null, xmlMessage.getBytes());
        
        return "";
    }

    
}
