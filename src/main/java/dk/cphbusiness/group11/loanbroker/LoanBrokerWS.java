/*
Sends:

<LoanRequest>
   <loanAmount>50000.0</loanAmount> 
   <loanDurationInMonths>36</loanDurationInMonths> 
   <ssn>123456-7890</ssn> 
</LoanRequest>

Receives:

<LoanResponse>
   <ssn>123456-7890</ssn> 
   <bank>PoorBank</bank> 
   <interestRate>64.0</interestRate> 
</LoanResponse>

 */

package dk.cphbusiness.group11.loanbroker;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;

@WebService(serviceName = "LoanBrokerWS")
public class LoanBrokerWS extends LoanBrokerComponent {
    private static final String RECEIVING_QUEUE = "loanbroker.group11.LoanBroakerWS";
    private static final String SENDING_QUEUE = "loanbroker.group11.GetCreditScore";

    public LoanBrokerWS() throws IOException{
        super("LoanBrokerWS");
    }
    
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
        this.log(String.format("received loan request for [ssn=%s, loanAmount=%f, loanDurationInMonths=%d]", ssn, loanAmount, loanDurationInMonths));
        
        String xmlMessage = "<LoanRequest>\n" +
                "   <loanAmount>" + loanAmount + "</loanAmount> \n" +
                "   <loanDurationInMonths>" + loanDurationInMonths + "</loanDurationInMonths> \n" +
                "   <ssn>" + ssn + "</ssn> \n" +
                "</LoanRequest>";
        
        Channel channel = this.getChannel(SENDING_QUEUE);
        this.log(String.format("got channel on queue '%s'", SENDING_QUEUE));
        
        channel.basicPublish(SENDING_QUEUE, "", null, xmlMessage.getBytes());
        this.log(String.format("publishing message %s", xmlMessage));
        
        return "";
    }

    
}
