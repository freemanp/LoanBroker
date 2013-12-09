/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dk.cphbusiness.group11.loanbroker;

import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.xml.ws.WebServiceRef;
import org.bank.services.credit.web.services.CreditScoreService_Service;

/**
 *
 * @author Paul
 */
@WebService(serviceName = "LoanBrokerWS")
public class LoanBrokerWS {
    @WebServiceRef(wsdlLocation = "WEB-INF/wsdl/datdb.cphbusiness.dk_8080/CreditBureau/CreditScoreService.wsdl")
    private CreditScoreService_Service service;

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
    public String LoanRequest(@WebParam(name = "ssn") String ssn, @WebParam(name = "amount") double amount) {
        
        return "Your credit score is " + creditScore(ssn);
    }

    private int creditScore(java.lang.String ssn) {
        // Note that the injected javax.xml.ws.Service reference as well as port objects are not thread safe.
        // If the calling of port operations may lead to race condition some synchronization is required.
        org.bank.services.credit.web.services.CreditScoreService port = service.getCreditScoreServicePort();
        return port.creditScore(ssn);
    }
}
