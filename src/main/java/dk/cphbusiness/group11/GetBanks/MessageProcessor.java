/*
 * XML parsing:
 * http://www.vogella.com/articles/JavaXML/article.html
 */
package dk.cphbusiness.group11.GetBanks;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import dk.cphbusiness.group11.GetBanks.exceptions.GetBankCreditScoreException;
import dk.cphbusiness.group11.GetBanks.exceptions.GetBankException;
import dk.cphbusiness.group11.GetBanks.exceptions.GetBankParseException;
import dk.cphbusiness.group11.GetBanks.exceptions.GetBankWritingException;

public class MessageProcessor {

	private static final String XML_ROOT_ELEMENT = "RecipientListRequest";
	private static final String XML_LOAN_ELEMENT = "LoanDetails";
	private static final String XML_SSN_ELEMENT = "ssn";
	private static final String XML_CREDIT_SCORE_ELEMENT = "creditScore";
	private static final String XML_LOAN_AMOUNT_ELEMENT = "loanAmount";
	private static final String XML_LOAN_DURATION_IN_MONTHS_ELEMENT = "loanDurationInMonths";
	private static final String XML_BANK_LIST_ELEMENT = "BankList";
	private static final String XML_BANK_ELEMENT = "bank";

	private XMLEvent endLine;
	private XMLEvent tab;

	private int creditScore;
	private ArrayList<Bank> availableBanks;
	private ArrayList<Bank> banks;
	private String response;
	private String message;
	private String ssn;
	private String loanAmount;
	private String loanDurationInMonths;

	public MessageProcessor(String message) {
		this.message = message;
		banks = new ArrayList<Bank>();
		availableBanks = new ArrayList<Bank>();
		availableBanks.add(new Bank("cphbusiness.bankJSON", 700));
		availableBanks.add(new Bank("cphbusiness.bankXML", 400));
		availableBanks.add(new Bank("group11.messagingBank", 0));
	}

	public void processMessage() {
		try {
			parseXML();
			getBanks();
			writeMessage();
		} catch (GetBankException e) {
			response = e.getMessage();
		}
	}

	private void parseXML() throws GetBankParseException {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLEventReader eventReader;
		try {
			eventReader = inputFactory
					.createXMLEventReader(new ByteArrayInputStream(message
							.getBytes()));

			boolean lookingForCreditScore = true;
			creditScore = -1;

			while (eventReader.hasNext() && lookingForCreditScore) {
				XMLEvent event = eventReader.nextEvent();

				if (event.isStartElement()) {
					String partName = event.asStartElement().getName()
							.getLocalPart();

					if (partName.equals(XML_CREDIT_SCORE_ELEMENT)) {
						creditScore = Integer.parseInt(eventReader.nextEvent()
								.asCharacters().getData());
						lookingForCreditScore = false;
					} else if (partName.equals(XML_SSN_ELEMENT)) {
						ssn = eventReader.nextEvent().asCharacters().getData();
					} else if (partName.equals(XML_LOAN_AMOUNT_ELEMENT)) {
						loanAmount = eventReader.nextEvent().asCharacters()
								.getData();
					} else if (partName
							.equals(XML_LOAN_DURATION_IN_MONTHS_ELEMENT)) {
						loanDurationInMonths = eventReader.nextEvent()
								.asCharacters().getData();
					}
				}
			}
		} catch (XMLStreamException e) {
			throw new GetBankParseException(e.getMessage());
		}
	}

	private void getBanks() throws GetBankCreditScoreException {
		if (0 <= creditScore && creditScore <= 800) {
			banks = new ArrayList<Bank>();
			for (int i = 0; i < availableBanks.size(); i++) {
				if (availableBanks.get(i).getMinimumCreditScore() <= creditScore) {
					banks.add(availableBanks.get(i));
				}
			}
		} else {
			throw new GetBankCreditScoreException("" + creditScore);
		}
	}

	private void writeMessage() throws GetBankWritingException {
		// create an XMLOutputFactory
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		// create XMLEventWriter
		XMLEventWriter eventWriter;
		try {
			eventWriter = outputFactory.createXMLEventWriter(outputStream);
			// create an EventFactory
			XMLEventFactory eventFactory = XMLEventFactory.newInstance();
			
			endLine = eventFactory.createDTD("\n");
			tab = eventFactory.createDTD("\t");

			// create and write Start Tag
			eventWriter.add(eventFactory.createStartDocument());

			eventWriter.add(endLine);
			eventWriter.add(eventFactory.createStartElement("", "",
					XML_ROOT_ELEMENT));
			eventWriter.add(endLine);

			writeLoanToXML(eventWriter, eventFactory);
			
			eventWriter.add(tab);
			eventWriter.add(eventFactory.createStartElement("", "",
					XML_BANK_LIST_ELEMENT));
			eventWriter.add(endLine);
			
			for (int i = 0; i < banks.size(); i++) {
				eventWriter.add(tab);
				eventWriter.add(tab);
				eventWriter.add(eventFactory.createStartElement("", "",
						XML_BANK_ELEMENT));
				eventWriter.add(eventFactory.createCharacters(banks.get(i).getId()));
				eventWriter.add(eventFactory.createEndElement("", "",
						XML_BANK_ELEMENT));
				eventWriter.add(endLine);
			}
			
			eventWriter.add(tab);
			eventWriter.add(eventFactory.createEndElement("", "",
					XML_BANK_LIST_ELEMENT));
			eventWriter.add(endLine);

			eventWriter.add(eventFactory.createEndElement("", "",
					XML_ROOT_ELEMENT));
			eventWriter.add(endLine);
			eventWriter.add(eventFactory.createEndDocument());
			eventWriter.close();

			response = outputStream.toString();
		} catch (XMLStreamException e) {
			throw new GetBankWritingException(e.getMessage());
		}

	}

	private void writeLoanToXML(XMLEventWriter eventWriter,
			XMLEventFactory eventFactory) throws XMLStreamException {
		eventWriter.add(tab);
		eventWriter.add(eventFactory.createStartElement("", "",
				XML_LOAN_ELEMENT));
		eventWriter.add(endLine);

		eventWriter.add(tab);
		eventWriter.add(tab);
		eventWriter.add(eventFactory
				.createStartElement("", "", XML_SSN_ELEMENT));
		eventWriter.add(eventFactory.createCharacters(ssn));
		eventWriter.add(eventFactory.createEndElement("", "", XML_SSN_ELEMENT));
		eventWriter.add(endLine);

		eventWriter.add(tab);
		eventWriter.add(tab);
		eventWriter.add(eventFactory.createStartElement("", "",
				XML_CREDIT_SCORE_ELEMENT));
		eventWriter.add(eventFactory.createCharacters("" + creditScore));
		eventWriter.add(eventFactory.createEndElement("", "",
				XML_CREDIT_SCORE_ELEMENT));
		eventWriter.add(endLine);

		eventWriter.add(tab);
		eventWriter.add(tab);
		eventWriter.add(eventFactory.createStartElement("", "",
				XML_LOAN_AMOUNT_ELEMENT));
		eventWriter.add(eventFactory.createCharacters(loanAmount));
		eventWriter.add(eventFactory.createEndElement("", "",
				XML_LOAN_AMOUNT_ELEMENT));
		eventWriter.add(endLine);

		eventWriter.add(tab);
		eventWriter.add(tab);
		eventWriter.add(eventFactory.createStartElement("", "",
				XML_LOAN_DURATION_IN_MONTHS_ELEMENT));
		eventWriter.add(eventFactory
				.createCharacters(loanDurationInMonths));
		eventWriter.add(eventFactory.createEndElement("", "",
				XML_LOAN_DURATION_IN_MONTHS_ELEMENT));
		eventWriter.add(endLine);

		eventWriter.add(tab);
		eventWriter
				.add(eventFactory.createEndElement("", "", XML_LOAN_ELEMENT));
		eventWriter.add(endLine);
	}

	public String getResponse() {
		return response;
	}
}
