
package org.bank.services.credit.web.services;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.bank.services.credit.web.services package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _CreditScoreResponse_QNAME = new QName("http://services.web.credit.services.bank.org/", "creditScoreResponse");
    private final static QName _CreditScore_QNAME = new QName("http://services.web.credit.services.bank.org/", "creditScore");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.bank.services.credit.web.services
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link CreditScore }
     * 
     */
    public CreditScore createCreditScore() {
        return new CreditScore();
    }

    /**
     * Create an instance of {@link CreditScoreResponse }
     * 
     */
    public CreditScoreResponse createCreditScoreResponse() {
        return new CreditScoreResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CreditScoreResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://services.web.credit.services.bank.org/", name = "creditScoreResponse")
    public JAXBElement<CreditScoreResponse> createCreditScoreResponse(CreditScoreResponse value) {
        return new JAXBElement<CreditScoreResponse>(_CreditScoreResponse_QNAME, CreditScoreResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CreditScore }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://services.web.credit.services.bank.org/", name = "creditScore")
    public JAXBElement<CreditScore> createCreditScore(CreditScore value) {
        return new JAXBElement<CreditScore>(_CreditScore_QNAME, CreditScore.class, null, value);
    }

}
