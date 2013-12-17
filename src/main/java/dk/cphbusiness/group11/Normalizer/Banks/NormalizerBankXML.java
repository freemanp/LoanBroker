package dk.cphbusiness.group11.Normalizer.Banks;
/*
 * expects:
 * 	<LoanResponse>
 *		<interestRate>5.5</interestRate>
 *		<ssn>1605789787</ssn>
 *	</LoanResponse>
 * returns:
 * 	<LoanResponse>
 *		<interestRate>5.5</interestRate>
 *		<ssn>1605789787</ssn>
 *		<bank>cphbusiness.bankXML</bank>
 *	</LoanResponse>
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import dk.cphbusiness.group11.Normalizer.Normalizer;

public class NormalizerBankXML extends Normalizer {

	private static final String SSN_FIELD = "ssn";
	private static final String INTEREST_RATE_FIELD = "interestRate";
	private static final String BANK_FIELD = "bank";
	private static final String XML_ROOT_ELEMENT = "LoanResponse";
	
	private static final String BANK = "cphbusiness.bankXML";

	public NormalizerBankXML(String queue) throws Exception {
		super(queue);
	}

	@Override
	protected String processMessage(String message) throws Exception {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(
				message.getBytes());

		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLEventReader eventReader = inputFactory
				.createXMLEventReader(inputStream);
		
		String interestRate = "";
		String ssn = "";

		while (eventReader.hasNext()) {
			XMLEvent event = eventReader.nextEvent();

			if (event.isStartElement()) {
				StartElement startElement = event.asStartElement();
				if (startElement.getName().getLocalPart().equals(SSN_FIELD)) {
					event = eventReader.nextEvent();
					interestRate = event.asCharacters().getData();
				} else if (startElement.getName().getLocalPart()
						.equals(INTEREST_RATE_FIELD)) {
					event = eventReader.nextEvent();
					ssn = event.asCharacters().getData();
				}
			}
		}


		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		XMLEventWriter eventWriter = outputFactory
				.createXMLEventWriter(outputStream);
		XMLEventFactory eventFactory = XMLEventFactory.newInstance();

		XMLEvent endLine = eventFactory.createDTD("\n");
		XMLEvent tab = eventFactory.createDTD("\t");

		eventWriter.add(eventFactory.createStartDocument());

		eventWriter.add(endLine);
		eventWriter.add(eventFactory.createStartElement("", "",
				XML_ROOT_ELEMENT));
		eventWriter.add(endLine);

		eventWriter.add(tab);
		eventWriter.add(eventFactory.createStartElement("", "",
				INTEREST_RATE_FIELD));
		eventWriter.add(eventFactory.createCharacters(interestRate));
		eventWriter.add(eventFactory.createEndElement("", "",
				INTEREST_RATE_FIELD));
		eventWriter.add(endLine);
		
		eventWriter.add(tab);
		eventWriter.add(eventFactory.createStartElement("", "",
				BANK_FIELD));
		eventWriter.add(eventFactory.createCharacters(BANK));
		eventWriter.add(eventFactory.createEndElement("", "",
				BANK_FIELD));
		eventWriter.add(endLine);

		eventWriter.add(tab);
		eventWriter.add(eventFactory.createStartElement("", "", SSN_FIELD));
		eventWriter.add(eventFactory.createCharacters(ssn));
		eventWriter.add(eventFactory.createEndElement("", "", SSN_FIELD));
		eventWriter.add(endLine);

		eventWriter
				.add(eventFactory.createEndElement("", "", XML_ROOT_ELEMENT));
		eventWriter.add(endLine);
		eventWriter.add(eventFactory.createEndDocument());
		eventWriter.close();

		return outputStream.toString();
	}

}
